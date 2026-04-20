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
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.util.TokenManager
import javax.inject.Inject

sealed interface InventoryDetailUiState {
    object Loading : InventoryDetailUiState
    data class Success(val item: ItemDto) : InventoryDetailUiState
    data class Error(val message: String) : InventoryDetailUiState
}

@HiltViewModel
class InventoryDetailViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<InventoryDetailUiState>(InventoryDetailUiState.Loading)
    val uiState: StateFlow<InventoryDetailUiState> = _uiState.asStateFlow()

    val permissions: StateFlow<Set<String>> = tokenManager.permissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    fun loadItem(itemId: String) {
        viewModelScope.launch {
            _uiState.value = InventoryDetailUiState.Loading
            itemRepository.getItem(itemId)
                .onSuccess { item ->
                    _uiState.value = InventoryDetailUiState.Success(item)
                }
                .onFailure { error ->
                    _uiState.value = InventoryDetailUiState.Error(
                        error.message ?: "Nepavyko gauti daikto"
                    )
                }
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            itemRepository.deleteItem(itemId)
                .onSuccess {
                    _deleted.value = true
                }
                .onFailure { error ->
                    _deleteError.value = error.message ?: "Nepavyko ištrinti daikto"
                }
        }
    }

    fun onDeleteErrorShown() {
        _deleteError.value = null
    }
}