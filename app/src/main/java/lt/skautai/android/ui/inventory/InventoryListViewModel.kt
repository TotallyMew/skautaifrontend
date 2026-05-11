package lt.skautai.android.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.LocationDto
import lt.skautai.android.data.remote.UpdateItemRequestDto
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.data.repository.LocationRepository
import lt.skautai.android.util.InventoryCsv
import lt.skautai.android.util.InventoryImportDraft
import lt.skautai.android.util.InventoryImportDuplicateMode
import lt.skautai.android.util.InventoryImportField
import lt.skautai.android.util.InventoryImportPreview
import lt.skautai.android.util.TokenManager
import lt.skautai.android.util.canManageAllItems
import lt.skautai.android.util.canManageSharedInventory
import javax.inject.Inject

sealed interface InventoryListUiState {
    object Loading : InventoryListUiState
    data class Success(
        val items: List<ItemDto>,
        val pendingItems: List<ItemDto> = emptyList()
    ) : InventoryListUiState
    data class Error(val message: String) : InventoryListUiState
    object Empty : InventoryListUiState
}

private fun ItemDto.effectiveInventoryType(): String =
    if (origin == "TRANSFERRED_FROM_TUNTAS" && custodianId != null) "COLLECTIVE" else type

private fun ItemDto.isAssignedToPerson(): Boolean =
    effectiveInventoryType() == "ASSIGNED" || responsibleUserId != null

@HiltViewModel
class InventoryListViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val locationRepository: LocationRepository,
    private val tokenManager: TokenManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<InventoryListUiState>(InventoryListUiState.Loading)
    val uiState: StateFlow<InventoryListUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()

    private val _selectedItemIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedItemIds: StateFlow<Set<String>> = _selectedItemIds.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _importDraft = MutableStateFlow<InventoryImportDraft?>(null)
    val importDraft: StateFlow<InventoryImportDraft?> = _importDraft.asStateFlow()

    private val _selectedTypes = MutableStateFlow(savedStateHandle.get<String?>("type")?.let { setOf(it) } ?: emptySet())
    val selectedTypes: StateFlow<Set<String>> = _selectedTypes.asStateFlow()

    private val _selectedCategories = MutableStateFlow(savedStateHandle.get<String?>("category")?.let { setOf(it) } ?: emptySet())
    val selectedCategories: StateFlow<Set<String>> = _selectedCategories.asStateFlow()

    private val _selectedLocationIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedLocationIds: StateFlow<Set<String>> = _selectedLocationIds.asStateFlow()

    private val _selectedStatus = MutableStateFlow("ACTIVE")
    val selectedStatus: StateFlow<String> = _selectedStatus.asStateFlow()

    private val _assignedOnly = MutableStateFlow(false)
    val assignedOnly: StateFlow<Boolean> = _assignedOnly.asStateFlow()

    private val initialCustodianId = savedStateHandle.get<String?>("custodianId")
    private val initialType = savedStateHandle.get<String?>("type")
    private val initialSharedOnly = savedStateHandle.get<Boolean>("sharedOnly") ?: false
    private val initialPersonalOwner = savedStateHandle.get<String?>("personalOwner")
    private val personalOwnerOnly = initialPersonalOwner == "me"
    val openedCustodianId: String? = initialCustodianId
    val openedPersonalOwnerOnly: Boolean = personalOwnerOnly

    val permissions: StateFlow<Set<String>> = tokenManager.permissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val locations: StateFlow<List<LocationDto>> = locationRepository.observeLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        observeCachedItems()
        loadItems()
    }

    private fun observeCachedItems() {
        viewModelScope.launch {
            val currentUserId = tokenManager.userId.first()
            val personalOwnerId = if (personalOwnerOnly) currentUserId else null
            combine(
                itemRepository.observeItems(
                    custodianId = initialCustodianId,
                    type = initialType,
                    sharedOnly = initialSharedOnly,
                    createdByUserId = personalOwnerId
                ),
                itemRepository.observeItems(
                    custodianId = initialCustodianId,
                    status = "INACTIVE",
                    type = initialType,
                    sharedOnly = initialSharedOnly,
                    createdByUserId = personalOwnerId
                ),
                itemRepository.observeItems(status = "PENDING_APPROVAL"),
                selectedStatus,
                permissions
            ) { activeItems, inactiveItems, pendingItems, selectedStatus, permissions ->
                val visibleInactiveItems = if (permissions.canManageAllItems()) inactiveItems else emptyList()
                val visiblePendingItems = if (permissions.canManageSharedInventory()) pendingItems else emptyList()
                val visibleItems = when (selectedStatus) {
                    "INACTIVE" -> visibleInactiveItems
                    "PENDING_APPROVAL" -> visiblePendingItems
                    else -> activeItems
                }
                if (visibleItems.isEmpty() && visiblePendingItems.isEmpty()) {
                    InventoryListUiState.Empty
                } else {
                    InventoryListUiState.Success(visibleItems, visiblePendingItems)
                }
            }.collect { state ->
                if (!_isRefreshing.value ||
                    state is InventoryListUiState.Success ||
                    state is InventoryListUiState.Empty
                ) {
                    _uiState.value = state
                }
            }
        }
    }

    fun loadItems() {
        viewModelScope.launch {
            _isRefreshing.value = true
            if (_uiState.value !is InventoryListUiState.Success) {
                _uiState.value = InventoryListUiState.Loading
            }
            try {
                val currentUserId = tokenManager.userId.first()
                val personalOwnerId = if (personalOwnerOnly) currentUserId else null
                val itemsResult = itemRepository.refreshItems(
                    custodianId = initialCustodianId,
                    type = initialType,
                    sharedOnly = initialSharedOnly,
                    createdByUserId = personalOwnerId
                )
                val currentPermissions = permissions.value
                val canApprovePending = currentPermissions.canManageSharedInventory()

                val pendingItemsResult = if (canApprovePending) {
                    itemRepository.refreshItems(status = "PENDING_APPROVAL")
                } else Result.success(Unit)

                val inactiveItemsResult = if (currentPermissions.canManageAllItems()) {
                    itemRepository.refreshItems(
                        custodianId = initialCustodianId,
                        status = "INACTIVE",
                        type = initialType,
                        sharedOnly = initialSharedOnly,
                        createdByUserId = personalOwnerId
                    )
                } else Result.success(Unit)

                val error = itemsResult.exceptionOrNull()
                    ?: pendingItemsResult.exceptionOrNull()
                    ?: inactiveItemsResult.exceptionOrNull()
                if (error != null && _uiState.value !is InventoryListUiState.Success) {
                    val currentState = _uiState.value
                    if (currentState !is InventoryListUiState.Empty) {
                        _uiState.value = InventoryListUiState.Error(
                            error.message ?: "Nepavyko gauti inventoriaus"
                        )
                    }
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun approveItem(itemId: String) {
        viewModelScope.launch {
            itemRepository.updateItem(itemId, UpdateItemRequestDto(status = "ACTIVE"))
                .onSuccess { updatedItem ->
                    val current = _uiState.value
                    if (current is InventoryListUiState.Success) {
                        val nextItems = if (_selectedStatus.value == "ACTIVE") {
                            (current.items + updatedItem)
                                .distinctBy { it.id }
                                .sortedBy { it.name.lowercase() }
                        } else {
                            current.items.filterNot { it.id == itemId }
                        }
                        _uiState.value = current.copy(
                            items = nextItems,
                            pendingItems = current.pendingItems.filterNot { it.id == itemId }
                        )
                    } else {
                        loadItems()
                    }
                }
        }
    }

    fun rejectItem(itemId: String) {
        viewModelScope.launch {
            itemRepository.updateItem(itemId, UpdateItemRequestDto(status = "INACTIVE"))
                .onSuccess {
                    val current = _uiState.value
                    if (current is InventoryListUiState.Success) {
                        val remainingPending = current.pendingItems.filterNot { it.id == itemId }
                        val nextItems = current.items.filterNot { it.id == itemId }
                        _uiState.value = if (nextItems.isEmpty() && remainingPending.isEmpty()) {
                            InventoryListUiState.Empty
                        } else {
                            current.copy(items = nextItems, pendingItems = remainingPending)
                        }
                    } else {
                        loadItems()
                    }
                }
        }
    }

    fun approveAllPending() {
        viewModelScope.launch {
            val current = _uiState.value as? InventoryListUiState.Success ?: return@launch
            val pendingIds = current.pendingItems.map { it.id }
            if (pendingIds.isEmpty()) return@launch

            val approvedItems = pendingIds.mapNotNull { itemId ->
                itemRepository.updateItem(itemId, UpdateItemRequestDto(status = "ACTIVE")).getOrNull()
            }

            val nextItems = if (_selectedStatus.value == "ACTIVE") {
                (current.items + approvedItems)
                    .distinctBy { it.id }
                    .sortedBy { it.name.lowercase() }
            } else {
                current.items.filterNot { it.id in pendingIds }
            }
            _uiState.value = current.copy(
                items = nextItems,
                pendingItems = current.pendingItems.filterNot { it.id in pendingIds }
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        clearSelection()
    }

    fun onTypeSelected(type: String) {
        _selectedTypes.value = _selectedTypes.value.toggle(type)
        clearSelection()
    }

    fun onCategorySelected(category: String) {
        _selectedCategories.value = _selectedCategories.value.toggle(category)
        clearSelection()
    }

    fun onLocationSelected(locationId: String) {
        _selectedLocationIds.value = _selectedLocationIds.value.toggle(locationId)
        clearSelection()
        loadItems()
    }

    fun onStatusSelected(status: String) {
        _selectedStatus.value = status
        clearSelection()
        loadItems()
    }

    fun onAssignedOnlyChange(enabled: Boolean) {
        _assignedOnly.value = enabled
        clearSelection()
    }

    fun clearFilters() {
        _selectedTypes.value = emptySet()
        _selectedCategories.value = emptySet()
        _selectedLocationIds.value = emptySet()
        _assignedOnly.value = false
        clearSelection()
    }

    fun clearTypeFilters() {
        _selectedTypes.value = emptySet()
        clearSelection()
    }

    fun enterSelectionMode() {
        _selectionMode.value = true
        _selectedItemIds.value = emptySet()
    }

    fun exitSelectionMode() {
        clearSelection()
    }

    fun toggleSelectedItem(itemId: String, isEligible: Boolean) {
        if (!_selectionMode.value) return
        if (!isEligible) {
            _actionMessage.value = "Šio daikto QR PDF sugeneruoti negalima."
            return
        }
        _selectedItemIds.value = _selectedItemIds.value.toMutableSet().apply {
            if (!add(itemId)) remove(itemId)
        }
    }

    fun onPdfShared() {
        clearSelection()
    }

    fun onPdfShareFailed(message: String) {
        _actionMessage.value = message
    }

    fun inventoryExportCsv(items: List<ItemDto>): String = InventoryCsv.exportInventory(items)

    fun inventoryImportTemplateCsv(): String = InventoryCsv.inventoryTemplate()

    fun importInventoryCsv(csv: String) {
        prepareInventoryImport("inventorius.csv", InventoryCsv.parseTextTable(csv))
    }

    fun prepareInventoryImport(fileName: String, table: List<List<String>>) {
        if (table.isEmpty()) {
            _actionMessage.value = "Importo failas tuščias arba nepavyko perskaityti lentelės."
            return
        }
        if (table.size == 1) {
            _actionMessage.value = "Faile yra tik antraštė, be importuojamų eilučių."
            return
        }
        _importDraft.value = InventoryCsv.analyzeInventoryTable(fileName, table)
    }

    fun previewInventoryImport(
        mapping: Map<InventoryImportField, Int?>,
        duplicateMode: InventoryImportDuplicateMode
    ): InventoryImportPreview? {
        val draft = _importDraft.value ?: return null
        val existingItems = (_uiState.value as? InventoryListUiState.Success)?.items.orEmpty()
        return InventoryCsv.previewInventoryImport(
            draft = draft,
            mapping = mapping,
            type = initialType ?: "COLLECTIVE",
            custodianId = initialCustodianId,
            existingItems = existingItems,
            duplicateMode = duplicateMode
        )
    }

    fun cancelInventoryImport() {
        _importDraft.value = null
    }

    fun executeInventoryImport(
        mapping: Map<InventoryImportField, Int?>,
        duplicateMode: InventoryImportDuplicateMode
    ) {
        viewModelScope.launch {
            val type = initialType ?: "COLLECTIVE"
            val draft = _importDraft.value ?: return@launch
            val result = InventoryCsv.parseInventoryRows(
                rows = draft.rows,
                mapping = mapping,
                type = type,
                custodianId = initialCustodianId,
                unknownColumns = draft.unknownColumns
            )
            if (result.hasFatalErrors || result.rows.isEmpty()) {
                _actionMessage.value = result.errors.firstOrNull()
                    ?: "Importe nerasta tinkamų inventoriaus eilučių."
                return@launch
            }

            val existingItems = (_uiState.value as? InventoryListUiState.Success)?.items.orEmpty()
            val existingByKey = existingItems.associateBy { item ->
                InventoryCsv.inventoryKey(
                    name = item.name,
                    category = item.category,
                    condition = item.condition,
                    type = item.type
                )
            }
            var created = 0
            var updated = 0
            var skippedExisting = 0
            var failed = 0

            result.rows.forEach { request ->
                val requestWithLocation = resolveImportedLocation(request)
                val existing = existingByKey[InventoryCsv.inventoryKey(request.name, request.category, request.condition, request.type)]
                val actionResult = when {
                    existing != null && duplicateMode == InventoryImportDuplicateMode.Merge -> {
                        itemRepository.updateItem(
                            existing.id,
                            UpdateItemRequestDto(
                                quantity = existing.quantity + requestWithLocation.quantity,
                                locationId = requestWithLocation.locationId ?: existing.locationId,
                                temporaryStorageLabel = requestWithLocation.temporaryStorageLabel ?: existing.temporaryStorageLabel,
                                notes = mergeNotes(existing.notes, requestWithLocation.notes),
                                customFields = mergeCustomFields(existing.customFields, requestWithLocation.customFields),
                                clearLocationId = false
                            )
                        )
                    }
                    existing != null && duplicateMode == InventoryImportDuplicateMode.SkipExisting -> {
                        skippedExisting += 1
                        null
                    }
                    else -> itemRepository.createItem(requestWithLocation.copy(duplicateHandling = "CREATE_NEW"))
                }
                actionResult?.onSuccess {
                    if (existing != null && duplicateMode == InventoryImportDuplicateMode.Merge) updated += 1 else created += 1
                }?.onFailure { failed += 1 }
            }

            _importDraft.value = null
            loadItems()
            _actionMessage.value = buildString {
                append(result.summary(created = created, updated = updated))
                if (skippedExisting > 0) append(" Praleista esamų: $skippedExisting.")
                if (failed > 0) append(" Nepavyko: $failed.")
            }
        }
    }

    fun onActionMessageShown() {
        _actionMessage.value = null
    }

    fun filteredItems(items: List<ItemDto>): List<ItemDto> {
        val query = _searchQuery.value.trim()
        val selectedTypes = _selectedTypes.value
        val selectedCategories = _selectedCategories.value
        val selectedLocationIds = _selectedLocationIds.value
        val assignedOnly = _assignedOnly.value
        val byType = if (selectedTypes.isEmpty()) {
            items
        } else {
            items.filter { it.effectiveInventoryType() in selectedTypes }
        }
        val byCategory = if (selectedCategories.isEmpty()) {
            byType
        } else {
            byType.filter { it.category in selectedCategories }
        }
        val byLocation = if (selectedLocationIds.isEmpty()) {
            byCategory
        } else {
            val selectedAndChildren = selectedLocationIds.flatMap { selectedLocationTreeIds(it) }.toSet()
            byCategory.filter { item -> item.locationId?.let { it in selectedAndChildren } == true }
        }
        val byAssigned = if (assignedOnly) {
            byLocation.filter { it.isAssignedToPerson() }
        } else {
            byLocation
        }

        if (query.isBlank()) return byAssigned
        return byAssigned.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.notes?.contains(query, ignoreCase = true) == true ||
                it.custodianName?.contains(query, ignoreCase = true) == true ||
                it.locationPath?.contains(query, ignoreCase = true) == true ||
                it.locationName?.contains(query, ignoreCase = true) == true ||
                it.condition.contains(query, ignoreCase = true) ||
                it.customFields.any { field ->
                    field.fieldName.contains(query, ignoreCase = true) ||
                        field.fieldValue?.contains(query, ignoreCase = true) == true
                }
        }
    }

    private fun clearSelection() {
        _selectionMode.value = false
        _selectedItemIds.value = emptySet()
    }

    fun selectedLocationTreeIds(locationId: String): Set<String> {
        val allLocations = locations.value
        val result = mutableSetOf(locationId)
        var added: Boolean
        do {
            added = false
            allLocations.forEach { location ->
                if (location.parentLocationId in result && result.add(location.id)) {
                    added = true
                }
            }
        } while (added)
        return result
    }

    private fun Set<String>.toggle(value: String): Set<String> =
        if (value in this) this - value else this + value

    private fun mergeNotes(existing: String?, imported: String?): String? {
        if (imported.isNullOrBlank()) return existing
        if (existing.isNullOrBlank()) return imported
        return listOf(existing.trim(), imported.trim()).distinct().joinToString("; ")
    }

    private fun resolveImportedLocation(request: lt.skautai.android.data.remote.CreateItemRequestDto): lt.skautai.android.data.remote.CreateItemRequestDto {
        val label = request.temporaryStorageLabel?.trim()?.takeIf { it.isNotBlank() } ?: return request
        val normalized = label.normalizedImportKey()
        val match = locations.value.firstOrNull { location ->
            location.fullPath.normalizedImportKey() == normalized ||
                location.name.normalizedImportKey() == normalized
        } ?: return request
        return request.copy(locationId = match.id, temporaryStorageLabel = null)
    }

    private fun mergeCustomFields(
        existing: List<lt.skautai.android.data.remote.ItemCustomFieldDto>,
        imported: List<lt.skautai.android.data.remote.ItemCustomFieldDto>
    ): List<lt.skautai.android.data.remote.ItemCustomFieldDto> {
        val byName = (existing + imported).groupBy { it.fieldName.trim().lowercase() }
        return byName.values.mapNotNull { group ->
            val name = group.firstOrNull()?.fieldName?.trim().orEmpty()
            if (name.isBlank()) null else lt.skautai.android.data.remote.ItemCustomFieldDto(
                fieldName = name,
                fieldValue = group.mapNotNull { it.fieldValue?.trim()?.takeIf(String::isNotBlank) }
                    .distinct()
                    .joinToString("; ")
                    .ifBlank { null }
            )
        }
    }

    private fun String.normalizedImportKey(): String =
        trim().lowercase()
            .replace("ą", "a")
            .replace("č", "c")
            .replace("ę", "e")
            .replace("ė", "e")
            .replace("į", "i")
            .replace("š", "s")
            .replace("ų", "u")
            .replace("ū", "u")
            .replace("ž", "z")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
}
