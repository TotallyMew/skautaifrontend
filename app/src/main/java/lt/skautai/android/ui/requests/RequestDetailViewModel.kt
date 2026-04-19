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

sealed interface RequestDetailUiState {
    data object Loading : RequestDetailUiState
    data class Success(
        val request: BendrasRequestDto,
        val isActioning: Boolean = false,
        val error: String? = null
    ) : RequestDetailUiState
    data class Error(val message: String) : RequestDetailUiState
}

@HiltViewModel
class RequestDetailViewModel @Inject constructor(
    private val requestRepository: RequestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RequestDetailUiState>(RequestDetailUiState.Loading)
    val uiState: StateFlow<RequestDetailUiState> = _uiState.asStateFlow()

    fun loadRequest(id: String) {
        viewModelScope.launch {
            _uiState.value = RequestDetailUiState.Loading
            requestRepository.getRequest(id)
                .onSuccess { request ->
                    _uiState.value = RequestDetailUiState.Success(request)
                }
                .onFailure { error ->
                    _uiState.value = RequestDetailUiState.Error(
                        error.message ?: "Klaida gaunant prašymą"
                    )
                }
        }
    }

    fun cancelRequest(id: String) {
        val current = _uiState.value as? RequestDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isActioning = true, error = null)
            requestRepository.cancelRequest(id)
                .onSuccess { loadRequest(id) }
                .onFailure { error ->
                    _uiState.value = current.copy(
                        isActioning = false,
                        error = error.message ?: "Klaida atšaukiant prašymą"
                    )
                }
        }
    }

    fun draugininkasForward(id: String) {
        draugininkasReview(id, "FORWARDED", null)
    }

    fun draugininkasReject(id: String, reason: String?) {
        draugininkasReview(id, "REJECTED", reason)
    }

    fun topLevelApprove(id: String) {
        topLevelReview(id, "APPROVED", null)
    }

    fun topLevelReject(id: String, reason: String?) {
        topLevelReview(id, "REJECTED", reason)
    }

    private fun draugininkasReview(id: String, action: String, reason: String?) {
        val current = _uiState.value as? RequestDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isActioning = true, error = null)
            requestRepository.draugininkasReview(id, action, reason)
                .onSuccess { loadRequest(id) }
                .onFailure { error ->
                    _uiState.value = current.copy(
                        isActioning = false,
                        error = error.message ?: "Klaida atliekant peržiūrą"
                    )
                }
        }
    }

    private fun topLevelReview(id: String, action: String, reason: String?) {
        val current = _uiState.value as? RequestDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isActioning = true, error = null)
            requestRepository.topLevelReview(id, action, reason)
                .onSuccess { loadRequest(id) }
                .onFailure { error ->
                    _uiState.value = current.copy(
                        isActioning = false,
                        error = error.message ?: "Klaida atliekant peržiūrą"
                    )
                }
        }
    }

    fun clearError() {
        val current = _uiState.value as? RequestDetailUiState.Success ?: return
        _uiState.value = current.copy(error = null)
    }
}