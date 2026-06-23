package lt.skautai.android.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.InventoryKitDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.LocationDto
import lt.skautai.android.data.remote.UpdateItemRequestDto
import lt.skautai.android.data.repository.InventoryKitRepository
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
import lt.skautai.android.util.canReviewItemAdditions
import lt.skautai.android.util.canSubmitItemAddition
import lt.skautai.android.util.hasPermissionOwnUnit
import javax.inject.Inject

sealed interface InventoryListUiState {
    object Loading : InventoryListUiState
    data class Success(
        val items: List<ItemDto>,
        val pendingItems: List<ItemDto> = emptyList(),
        val kits: List<InventoryKitDto> = emptyList()
    ) : InventoryListUiState
    data class Error(val message: String) : InventoryListUiState
    object Empty : InventoryListUiState
}

enum class InventorySelectionPurpose {
    QR_PDF,
    BULK_EDIT
}

data class InventoryBulkAction(
    val condition: String? = null,
    val locationId: String? = null,
    val clearLocation: Boolean = false,
    val deactivate: Boolean = false
) {
    fun isEmpty(): Boolean =
        condition == null && locationId == null && !clearLocation && !deactivate
}

private data class InventoryListMeta(
    val kits: List<InventoryKitDto>,
    val kitsLoaded: Boolean,
    val selectedStatus: String,
    val permissions: Set<String>,
    val pagedItems: List<ItemDto>?
)

private fun ItemDto.effectiveInventoryType(): String =
    if (origin == "TRANSFERRED_FROM_TUNTAS" && custodianId != null) "COLLECTIVE" else type

private fun ItemDto.isAssignedToPerson(): Boolean =
    effectiveInventoryType() == "ASSIGNED" || responsibleUserId != null

internal fun canManageInventoryItem(
    item: ItemDto,
    permissions: Set<String>,
    leadershipUnitIds: List<String>,
    activeOrgUnitId: String?
): Boolean = when {
    item.custodianId == null -> permissions.canManageAllItems()
    permissions.canManageAllItems() -> true
    item.origin == "TRANSFERRED_FROM_TUNTAS" -> false
    else -> permissions.hasPermissionOwnUnit("items.update") &&
        (item.custodianId in leadershipUnitIds || item.custodianId == activeOrgUnitId)
}

internal fun buildBulkUpdateRequest(action: InventoryBulkAction): UpdateItemRequestDto? {
    if (action.isEmpty()) return null
    return UpdateItemRequestDto(
        condition = action.condition,
        locationId = if (action.clearLocation) null else action.locationId,
        status = if (action.deactivate) "INACTIVE" else null,
        clearLocationId = action.clearLocation
    )
}

@HiltViewModel
class InventoryListViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val inventoryKitRepository: InventoryKitRepository,
    private val locationRepository: LocationRepository,
    private val tokenManager: TokenManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<InventoryListUiState>(InventoryListUiState.Loading)
    val uiState: StateFlow<InventoryListUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMoreItems = MutableStateFlow(false)
    val hasMoreItems: StateFlow<Boolean> = _hasMoreItems.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()

    private val _selectionPurpose = MutableStateFlow<InventorySelectionPurpose?>(null)
    val selectionPurpose: StateFlow<InventorySelectionPurpose?> = _selectionPurpose.asStateFlow()

    private val _selectedItemIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedItemIds: StateFlow<Set<String>> = _selectedItemIds.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _kits = MutableStateFlow<List<InventoryKitDto>>(emptyList())
    private val _kitsLoaded = MutableStateFlow(false)
    private val _pagedItems = MutableStateFlow<List<ItemDto>?>(null)
    private var loadJob: Job? = null
    private var searchLoadJob: Job? = null

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

    private val _consumablesOnly = MutableStateFlow(false)
    val consumablesOnly: StateFlow<Boolean> = _consumablesOnly.asStateFlow()

    private val _lowStockOnly = MutableStateFlow(false)
    val lowStockOnly: StateFlow<Boolean> = _lowStockOnly.asStateFlow()

    private val initialCustodianId = savedStateHandle.get<String?>("custodianId")
    private val initialType = savedStateHandle.get<String?>("type")
    private val initialSharedOnly = savedStateHandle.get<Boolean>("sharedOnly") ?: false
    private val initialPersonalOwner = savedStateHandle.get<String?>("personalOwner")
    private val personalOwnerOnly = !initialPersonalOwner.isNullOrBlank()
    val openedType: String? = initialType
    val openedCustodianId: String? = initialCustodianId
    val openedSharedOnly: Boolean = initialSharedOnly
    val openedPersonalOwnerOnly: Boolean = personalOwnerOnly

    val permissions: StateFlow<Set<String>> = tokenManager.permissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val activeOrgUnitId: StateFlow<String?> = tokenManager.activeOrgUnitId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val leadershipUnitIds: StateFlow<List<String>> = tokenManager.leadershipUnitIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val locations: StateFlow<List<LocationDto>> = locationRepository.observeLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        observeCachedItems()
        loadItems()
    }

    private fun observeCachedItems() {
        viewModelScope.launch {
            val currentUserId = tokenManager.userId.first()
            val personalOwnerId = personalOwnerFilterId(currentUserId)
            combine(
                itemRepository.observeItems(
                    custodianId = initialCustodianId,
                    sharedOnly = initialSharedOnly,
                    createdByUserId = personalOwnerId
                ),
                itemRepository.observeItems(
                    custodianId = initialCustodianId,
                    status = "INACTIVE",
                    sharedOnly = initialSharedOnly,
                    createdByUserId = personalOwnerId
                ),
                itemRepository.observeItems(status = "PENDING_APPROVAL"),
                combine(_kits, _kitsLoaded, selectedStatus, permissions, _pagedItems) { kits, kitsLoaded, status, userPermissions, pagedItems ->
                    InventoryListMeta(kits, kitsLoaded, status, userPermissions, pagedItems)
                }
            ) { activeItems, inactiveItems, pendingItems, meta ->
                if (!meta.kitsLoaded && _uiState.value !is InventoryListUiState.Success) {
                    return@combine InventoryListUiState.Loading
                }
                val kits = meta.kits
                val selectedStatus = meta.selectedStatus
                val permissions = meta.permissions
                val visibleInactiveItems = if (permissions.canManageAllItems()) inactiveItems else emptyList()
                val canSeePending = permissions.canReviewItemAdditions() || permissions.canSubmitItemAddition()
                val visiblePendingItems = if (canSeePending) pendingItems else emptyList()
                val visibleItems = when (selectedStatus) {
                    "INACTIVE" -> visibleInactiveItems
                    "PENDING_APPROVAL" -> visiblePendingItems
                    else -> activeItems
                }
                val pagedItems = meta.pagedItems
                val shownItems = pagedItems ?: visibleItems
                if (selectedStatus == "ACTIVE" && shownItems.isEmpty() && visiblePendingItems.isEmpty()) {
                    InventoryListUiState.Empty
                } else {
                    InventoryListUiState.Success(shownItems, visiblePendingItems, kits)
                }
            }.collect { state ->
                if (!_isRefreshing.value ||
                    state is InventoryListUiState.Success
                ) {
                    _uiState.value = state
                }
            }
        }
    }

    fun loadItems() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isRefreshing.value = true
            if (_uiState.value !is InventoryListUiState.Success) {
                _uiState.value = InventoryListUiState.Loading
            }
            try {
                val currentUserId = tokenManager.userId.first()
                val personalOwnerId = personalOwnerFilterId(currentUserId)
                val currentPermissions = permissions.value
                val canSeePending = currentPermissions.canReviewItemAdditions() || currentPermissions.canSubmitItemAddition()
                val selectedStatus = _selectedStatus.value
                val targetStatus = targetStatusForLoad(selectedStatus, canSeePending, currentPermissions)
                val serverType = _selectedTypes.value.singleOrNull()
                val serverCategory = _selectedCategories.value.singleOrNull()
                val serverSearchQuery = _searchQuery.value.trim().takeIf { it.isNotBlank() }
                val canLoadTarget = targetStatus != null
                val itemsResult = if (canLoadTarget) {
                    itemRepository.refreshItemsPage(
                        custodianId = initialCustodianId,
                        status = targetStatus,
                        type = serverType,
                        category = serverCategory,
                        sharedOnly = initialSharedOnly,
                        createdByUserId = personalOwnerId,
                        searchQuery = serverSearchQuery,
                        limit = INVENTORY_PAGE_SIZE,
                        offset = 0,
                        replaceCache = true
                    ).also { result ->
                        result.onSuccess { page ->
                            _pagedItems.value = page.items
                            _hasMoreItems.value = page.hasMore
                        }
                            .onFailure { _hasMoreItems.value = false }
                    }.map { Unit }
                } else {
                    _pagedItems.value = emptyList()
                    _hasMoreItems.value = false
                    Result.success(Unit)
                }

                inventoryKitRepository.getKits()
                    .onSuccess { _kits.value = it.kits }
                _kitsLoaded.value = true

                val error = itemsResult.exceptionOrNull()
                if (error != null && _uiState.value !is InventoryListUiState.Success) {
                    val currentState = _uiState.value
                    if (currentState !is InventoryListUiState.Empty) {
                        _uiState.value = InventoryListUiState.Error(
                            error.message ?: "Nepavyko gauti inventoriaus"
                        )
                    }
                } else if (_uiState.value is InventoryListUiState.Loading) {
                    val cachedItems = itemRepository.getCachedItems(
                        custodianId = initialCustodianId,
                        status = when {
                            selectedStatus == "PENDING_APPROVAL" && canSeePending -> "PENDING_APPROVAL"
                            selectedStatus == "INACTIVE" && currentPermissions.canManageAllItems() -> "INACTIVE"
                            else -> "ACTIVE"
                        },
                        sharedOnly = initialSharedOnly,
                        createdByUserId = personalOwnerId
                    )
                    _uiState.value = if (cachedItems.isEmpty() && _kits.value.isEmpty()) {
                        InventoryListUiState.Empty
                    } else {
                        InventoryListUiState.Success(
                            items = cachedItems,
                            pendingItems = if (selectedStatus == "PENDING_APPROVAL") cachedItems else emptyList(),
                            kits = _kits.value
                        )
                    }
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadNextPage() {
        if (_isRefreshing.value || _isLoadingMore.value || !_hasMoreItems.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val currentUserId = tokenManager.userId.first()
                val personalOwnerId = personalOwnerFilterId(currentUserId)
                val currentPermissions = permissions.value
                val canSeePending = currentPermissions.canReviewItemAdditions() || currentPermissions.canSubmitItemAddition()
                val targetStatus = targetStatusForLoad(_selectedStatus.value, canSeePending, currentPermissions)
                    ?: return@launch
                val currentOffset = _pagedItems.value.orEmpty().size
                val serverType = _selectedTypes.value.singleOrNull()
                val serverCategory = _selectedCategories.value.singleOrNull()
                val serverSearchQuery = _searchQuery.value.trim().takeIf { it.isNotBlank() }
                itemRepository.refreshItemsPage(
                    custodianId = initialCustodianId,
                    status = targetStatus,
                    type = serverType,
                    category = serverCategory,
                    sharedOnly = initialSharedOnly,
                    createdByUserId = personalOwnerId,
                    searchQuery = serverSearchQuery,
                    limit = INVENTORY_PAGE_SIZE,
                    offset = currentOffset,
                    replaceCache = false
                ).onSuccess { page ->
                    _pagedItems.value = (_pagedItems.value.orEmpty() + page.items).distinctBy { it.id }
                    _hasMoreItems.value = page.hasMore
                }.onFailure { error ->
                    _actionMessage.value = error.message ?: "Nepavyko pakrauti daugiau inventoriaus"
                }
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    private fun targetStatusForLoad(
        selectedStatus: String,
        canSeePending: Boolean,
        currentPermissions: Set<String>
    ): String? = when {
        selectedStatus == "PENDING_APPROVAL" && canSeePending -> "PENDING_APPROVAL"
        selectedStatus == "INACTIVE" && currentPermissions.canManageAllItems() -> "INACTIVE"
        selectedStatus == "ACTIVE" -> "ACTIVE"
        else -> null
    }

    private fun personalOwnerFilterId(currentUserId: String?): String? = when {
        initialPersonalOwner.isNullOrBlank() -> null
        initialPersonalOwner == "me" -> currentUserId
        else -> initialPersonalOwner
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
        searchLoadJob?.cancel()
        searchLoadJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            loadItems()
        }
    }

    fun onTypeSelected(type: String) {
        _selectedTypes.value = _selectedTypes.value.toggle(type)
        clearSelection()
        loadItemsImmediately()
    }

    fun onCategorySelected(category: String) {
        _selectedCategories.value = _selectedCategories.value.toggle(category)
        clearSelection()
        loadItemsImmediately()
    }

    fun onLocationSelected(locationId: String) {
        _selectedLocationIds.value = _selectedLocationIds.value.toggle(locationId)
        clearSelection()
        loadItemsImmediately()
    }

    fun onStatusSelected(status: String) {
        _selectedStatus.value = status
        clearSelection()
        loadItemsImmediately()
    }

    fun onAssignedOnlyChange(enabled: Boolean) {
        _assignedOnly.value = enabled
        clearSelection()
    }

    fun onConsumablesOnlyChange(enabled: Boolean) {
        _consumablesOnly.value = enabled
        clearSelection()
    }

    fun onLowStockOnlyChange(enabled: Boolean) {
        _lowStockOnly.value = enabled
        clearSelection()
    }

    fun clearFilters() {
        _selectedTypes.value = emptySet()
        _selectedCategories.value = emptySet()
        _selectedLocationIds.value = emptySet()
        _assignedOnly.value = false
        _consumablesOnly.value = false
        _lowStockOnly.value = false
        clearSelection()
        loadItemsImmediately()
    }

    fun clearTypeFilters() {
        _selectedTypes.value = emptySet()
        clearSelection()
        loadItemsImmediately()
    }

    fun enterSelectionMode() {
        enterQrSelectionMode()
    }

    fun enterQrSelectionMode() {
        _selectionMode.value = true
        _selectionPurpose.value = InventorySelectionPurpose.QR_PDF
        _selectedItemIds.value = emptySet()
    }

    fun enterBulkSelectionMode() {
        _selectionMode.value = true
        _selectionPurpose.value = InventorySelectionPurpose.BULK_EDIT
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

    fun canBulkManage(item: ItemDto): Boolean =
        canManageInventoryItem(
            item = item,
            permissions = permissions.value,
            leadershipUnitIds = leadershipUnitIds.value,
            activeOrgUnitId = activeOrgUnitId.value
        ) && item.status != "PENDING_APPROVAL"

    fun applyBulkAction(action: InventoryBulkAction) {
        val request = buildBulkUpdateRequest(action)
        if (request == null) {
            _actionMessage.value = "Pasirink bent vieną masinį veiksmą."
            return
        }
        viewModelScope.launch {
            val current = _uiState.value as? InventoryListUiState.Success ?: return@launch
            val selectedItems = current.items.filter { it.id in _selectedItemIds.value && canBulkManage(it) }
            if (selectedItems.isEmpty()) {
                _actionMessage.value = "Pasirink bent vieną redaguojamą daiktą."
                return@launch
            }
            var successCount = 0
            var failedCount = 0
            selectedItems.forEach { item ->
                itemRepository.updateItem(item.id, request)
                    .onSuccess { successCount += 1 }
                    .onFailure { failedCount += 1 }
            }
            clearSelection()
            if (successCount > 0) {
                loadItems()
            }
            _actionMessage.value = buildString {
                append("Masiškai atnaujinta: $successCount")
                if (failedCount > 0) append(". Nepavyko: $failedCount.")
            }
        }
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

    fun updateInventoryImportHeaderRow(headerRowIndex: Int) {
        val draft = _importDraft.value ?: return
        _importDraft.value = InventoryCsv.withHeaderRow(draft, headerRowIndex)
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
                                isConsumable = requestWithLocation.isConsumable || existing.isConsumable,
                                unitOfMeasure = requestWithLocation.unitOfMeasure,
                                minimumQuantity = requestWithLocation.minimumQuantity ?: existing.minimumQuantity,
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
        val consumablesOnly = _consumablesOnly.value
        val lowStockOnly = _lowStockOnly.value
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
        val byConsumable = if (consumablesOnly) {
            byAssigned.filter { it.isConsumable }
        } else {
            byAssigned
        }
        val byStock = if (lowStockOnly) {
            byConsumable.filter { it.isLowStock }
        } else {
            byConsumable
        }

        if (query.isBlank()) return byStock
        return byStock.filter {
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
        _selectionPurpose.value = null
        _selectedItemIds.value = emptySet()
    }

    private fun loadItemsImmediately() {
        searchLoadJob?.cancel()
        loadItems()
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

    companion object {
        private const val INVENTORY_PAGE_SIZE = 50
        private const val SEARCH_DEBOUNCE_MS = 300L
    }
}
