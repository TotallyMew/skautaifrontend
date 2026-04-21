package lt.skautai.android.ui.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.RequisitionDto
import lt.skautai.android.data.repository.RequisitionRepository
import javax.inject.Inject

sealed interface RequisitionListUiState {
    data object Loading : RequisitionListUiState
    data class Success(val requests: List<RequisitionDto>) : RequisitionListUiState
    data class Error(val message: String) : RequisitionListUiState
}

@HiltViewModel
class RequisitionListViewModel @Inject constructor(
    private val requisitionRepository: RequisitionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RequisitionListUiState>(RequisitionListUiState.Loading)
    val uiState: StateFlow<RequisitionListUiState> = _uiState.asStateFlow()

    init {
        loadRequests()
    }

    fun loadRequests() {
        viewModelScope.launch {
            if (_uiState.value !is RequisitionListUiState.Success) {
                _uiState.value = RequisitionListUiState.Loading
            }
            requisitionRepository.getRequests()
                .onSuccess { response ->
                    _uiState.value = RequisitionListUiState.Success(response.requests)
                }
                .onFailure { error ->
                    _uiState.value = RequisitionListUiState.Error(
                        error.message ?: "Klaida gaunant prasymus"
                    )
                }
        }
    }
}
