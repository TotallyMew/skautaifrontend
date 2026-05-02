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

    private val _selectedType = MutableStateFlow(savedStateHandle.get<String?>("type"))
    val selectedType: StateFlow<String?> = _selectedType.asStateFlow()

    private val _selectedCategory = MutableStateFlow(savedStateHandle.get<String?>("category"))
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedLocationId = MutableStateFlow<String?>(null)
    val selectedLocationId: StateFlow<String?> = _selectedLocationId.asStateFlow()

    private val _selectedStatus = MutableStateFlow("ACTIVE")
    val selectedStatus: StateFlow<String> = _selectedStatus.asStateFlow()

    private val initialCustodianId = savedStateHandle.get<String?>("custodianId")
    private val initialType = savedStateHandle.get<String?>("type")
    private val initialSharedOnly = initialCustodianId == null && initialType == null
    val openedCustodianId: String? = initialCustodianId

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
            val personalOwnerId = if (initialType == "INDIVIDUAL") currentUserId else null
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
                val personalOwnerId = if (initialType == "INDIVIDUAL") currentUserId else null
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

    fun onTypeSelected(type: String?) {
        _selectedType.value = type
        clearSelection()
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
        clearSelection()
    }

    fun onLocationSelected(locationId: String?) {
        _selectedLocationId.value = locationId
        clearSelection()
        loadItems()
    }

    fun onStatusSelected(status: String) {
        _selectedStatus.value = status
        clearSelection()
        loadItems()
    }

    fun clearFilters() {
        _selectedType.value = null
        _selectedCategory.value = null
        _selectedLocationId.value = null
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
            _actionMessage.value = "Sio daikto QR PDF sugeneruoti negalima."
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

    fun onActionMessageShown() {
        _actionMessage.value = null
    }

    fun filteredItems(items: List<ItemDto>): List<ItemDto> {
        val query = _searchQuery.value.trim()
        val byType = _selectedType.value?.let { selected ->
            items.filter { it.type == selected }
        } ?: items
        val byCategory = _selectedCategory.value?.let { selected ->
            byType.filter { it.category == selected }
        } ?: byType
        val byLocation = _selectedLocationId.value?.let { selected ->
            val selectedAndChildren = selectedLocationTreeIds(selected)
            byCategory.filter { item -> item.locationId?.let { it in selectedAndChildren } == true }
        } ?: byCategory

        if (query.isBlank()) return byLocation
        return byLocation.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.notes?.contains(query, ignoreCase = true) == true ||
                it.custodianName?.contains(query, ignoreCase = true) == true
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
}
