package lt.skautai.android.ui.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.BendrasRequestDto
import lt.skautai.android.data.repository.RequestRepository
import javax.inject.Inject

sealed interface RequestListUiState {
    data object Loading : RequestListUiState
    data class Success(val requests: List<BendrasRequestDto>) : RequestListUiState
    data class Error(val message: String) : RequestListUiState
}

@HiltViewModel
class RequestListViewModel @Inject constructor(
    private val requestRepository: RequestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RequestListUiState>(RequestListUiState.Loading)
    val uiState: StateFlow<RequestListUiState> = _uiState.asStateFlow()

    init {
        loadRequests()
    }

    fun loadRequests() {
        viewModelScope.launch {
            if (_uiState.value !is RequestListUiState.Success) {
                _uiState.value = RequestListUiState.Loading
            }
            requestRepository.getRequests()
                .onSuccess { response ->
                    _uiState.value = RequestListUiState.Success(response.requests)
                }
                .onFailure { error ->
                    _uiState.value = RequestListUiState.Error(
                        error.message ?: "Klaida gaunant prašymus"
                    )
                }
        }
    }
}