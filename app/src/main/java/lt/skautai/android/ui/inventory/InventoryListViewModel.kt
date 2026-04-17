package lt.skautai.android.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.repository.ItemRepository
import javax.inject.Inject

sealed interface InventoryListUiState {
    object Loading : InventoryListUiState
    data class Success(val items: List<ItemDto>) : InventoryListUiState
    data class Error(val message: String) : InventoryListUiState
    object Empty : InventoryListUiState
}

@HiltViewModel
class InventoryListViewModel @Inject constructor(
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<InventoryListUiState>(InventoryListUiState.Loading)
    val uiState: StateFlow<InventoryListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadItems()
    }

    fun loadItems() {
        viewModelScope.launch {
            _uiState.value = InventoryListUiState.Loading
            itemRepository.getItems()
                .onSuccess { items ->
                    _uiState.value = if (items.isEmpty()) {
                        InventoryListUiState.Empty
                    } else {
                        InventoryListUiState.Success(items)
                    }
                }
                .onFailure { error ->
                    _uiState.value = InventoryListUiState.Error(
                        error.message ?: "Nepavyko gauti inventoriaus"
                    )
                }
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