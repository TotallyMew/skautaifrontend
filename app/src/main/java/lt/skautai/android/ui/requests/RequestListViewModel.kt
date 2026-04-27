package lt.skautai.android.ui.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.BendrasRequestDto
import lt.skautai.android.data.repository.RequestRepository

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
    private var observeJob: Job? = null

    init {
        observeRequests()
        loadRequests()
    }

    private fun observeRequests() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            requestRepository.observeRequests().collect { response ->
                _uiState.value = RequestListUiState.Success(response.requests)
            }
        }
    }

    fun loadRequests() {
        viewModelScope.launch {
            if (_uiState.value !is RequestListUiState.Success) {
                _uiState.value = RequestListUiState.Loading
            }
            requestRepository.refreshRequests()
                .onSuccess {
                    _uiState.value = RequestListUiState.Success(
                        requestRepository.getRequests().getOrNull()?.requests.orEmpty()
                    )
                }
                .onFailure { error ->
                    _uiState.value = RequestListUiState.Error(
                        error.message ?: "Klaida gaunant prasymus"
                    )
                }
        }
    }
}
