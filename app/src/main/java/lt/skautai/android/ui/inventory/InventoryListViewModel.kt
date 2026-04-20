package lt.skautai.android.ui.inventory

import androidx.lifecycle.ViewModel
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
        val items: List<ItemDto>,
        val pendingItems: List<ItemDto> = emptyList()
    ) : InventoryListUiState
    data class Error(val message: String) : InventoryListUiState
    object Empty : InventoryListUiState
}

@HiltViewModel
class InventoryListViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<InventoryListUiState>(InventoryListUiState.Loading)
    val uiState: StateFlow<InventoryListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val permissions: StateFlow<Set<String>> = tokenManager.permissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        loadItems()
    }

    fun loadItems() {
        viewModelScope.launch {
            if (_uiState.value !is InventoryListUiState.Success) {
                _uiState.value = InventoryListUiState.Loading
            }
            val itemsResult = itemRepository.getItems()
            val canApprovePending = "items.update" in permissions.value

            val pendingItems = if (canApprovePending) {
                itemRepository.getItems(status = "PENDING_APPROVAL").getOrDefault(emptyList())
            } else emptyList()

            itemsResult
                .onSuccess { items ->
                    _uiState.value = if (items.isEmpty() && pendingItems.isEmpty()) {
                        InventoryListUiState.Empty
                    } else {
                        InventoryListUiState.Success(items, pendingItems)
                    }
                }
                .onFailure { error ->
                    _uiState.value = InventoryListUiState.Error(
                        error.message ?: "Nepavyko gauti inventoriaus"
                    )
                }
        }
    }

    fun approveItem(itemId: String) {
        viewModelScope.launch {
            itemRepository.updateItem(itemId, UpdateItemRequestDto(status = "ACTIVE"))
                .onSuccess { loadItems() }
        }
    }

    fun rejectItem(itemId: String) {
        viewModelScope.launch {
            itemRepository.updateItem(itemId, UpdateItemRequestDto(status = "INACTIVE"))
                .onSuccess { loadItems() }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun filteredItems(items: List<ItemDto>): List<ItemDto> {
        val query = _searchQuery.value.trim()
        if (query.isBlank()) return items
        return items.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.notes?.contains(query, ignoreCase = true) == true
        }
    }
}