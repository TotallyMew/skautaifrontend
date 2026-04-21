package lt.skautai.android.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.UpdateItemRequestDto
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.util.TokenManager
import javax.inject.Inject

sealed interface InventoryListUiState {
    object Loading : InventoryListUiState
    data class Success(
        val activeItems: List<ItemDto>,
        val pendingItems: List<ItemDto> = emptyList()
    ) : InventoryListUiState
    data class Error(val message: String) : InventoryListUiState
    object Empty : InventoryListUiState
}

enum class InventoryListTab {
    INVENTORY,
    APPROVALS
}

@HiltViewModel
class InventoryListViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val tokenManager: TokenManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<InventoryListUiState>(InventoryListUiState.Loading)
    val uiState: StateFlow<InventoryListUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedType = MutableStateFlow(savedStateHandle.get<String?>("type"))
    val selectedType: StateFlow<String?> = _selectedType.asStateFlow()

    private val _selectedCategory = MutableStateFlow(savedStateHandle.get<String?>("category"))
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val initialCustodianId = savedStateHandle.get<String?>("custodianId")
    val openedCustodianId: String? = initialCustodianId

    private val _selectedTab = MutableStateFlow(InventoryListTab.INVENTORY)
    val selectedTab: StateFlow<InventoryListTab> = _selectedTab.asStateFlow()

    val permissions: StateFlow<Set<String>> = tokenManager.permissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        loadItems()
    }

    fun loadItems() {
        viewModelScope.launch {
            _isRefreshing.value = true
            if (_uiState.value !is InventoryListUiState.Success) {
                _uiState.value = InventoryListUiState.Loading
            }
            try {
                val itemsResult = itemRepository.getItems(custodianId = initialCustodianId)
                val canApprovePending = "items.transfer" in permissions.value

                val pendingItems = if (canApprovePending) {
                    itemRepository.getItems(status = "PENDING_APPROVAL").getOrDefault(emptyList())
                } else emptyList()

                itemsResult
                    .onSuccess { activeItems ->
                        _uiState.value = if (activeItems.isEmpty() && pendingItems.isEmpty()) {
                            InventoryListUiState.Empty
                        } else {
                            InventoryListUiState.Success(activeItems, pendingItems)
                        }
                    }
                    .onFailure { error ->
                        _uiState.value = InventoryListUiState.Error(
                            error.message ?: "Nepavyko gauti inventoriaus"
                        )
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
                        _uiState.value = current.copy(
                            activeItems = (current.activeItems + updatedItem)
                                .distinctBy { it.id }
                                .sortedBy { it.name.lowercase() },
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
                        _uiState.value = if (current.activeItems.isEmpty() && remainingPending.isEmpty()) {
                            InventoryListUiState.Empty
                        } else {
                            current.copy(pendingItems = remainingPending)
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

            _uiState.value = current.copy(
                activeItems = (current.activeItems + approvedItems)
                    .distinctBy { it.id }
                    .sortedBy { it.name.lowercase() },
                pendingItems = current.pendingItems.filterNot { it.id in pendingIds }
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onTypeSelected(type: String?) {
        _selectedType.value = type
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
    }

    fun clearFilters() {
        _selectedType.value = null
        _selectedCategory.value = null
    }

    fun onTabSelected(tab: InventoryListTab) {
        _selectedTab.value = tab
    }

    fun filteredItems(items: List<ItemDto>): List<ItemDto> {
        val query = _searchQuery.value.trim()
        val byType = _selectedType.value?.let { selected ->
            items.filter { it.type == selected }
        } ?: items
        val byCategory = _selectedCategory.value?.let { selected ->
            byType.filter { it.category == selected }
        } ?: byType

        if (query.isBlank()) return byCategory
        return byCategory.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.notes?.contains(query, ignoreCase = true) == true ||
                it.custodianName?.contains(query, ignoreCase = true) == true ||
                it.locationId?.contains(query, ignoreCase = true) == true
        }
    }
}
