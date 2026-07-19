package lt.skautai.android.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.RequisitionDto
import lt.skautai.android.data.repository.RequisitionRepository

data class AttentionUiState(
    val assignedRequisitionCount: Int = 0
)

@HiltViewModel
class AttentionViewModel @Inject constructor(
    private val requisitionRepository: RequisitionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AttentionUiState())
    val uiState: StateFlow<AttentionUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null
    private var refreshJob: Job? = null

    init {
        observeCachedRequests()
        refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            requisitionRepository.getRequests()
        }
    }

    private fun observeCachedRequests() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            requisitionRepository.observeRequests().collect { response ->
                _uiState.value = AttentionUiState(
                    assignedRequisitionCount = response.requests.count {
                        it.isAssignedToCurrentUser()
                    }
                )
            }
        }
    }
}

private fun RequisitionDto.isAssignedToCurrentUser(): Boolean =
    capabilities?.let { it.canReviewUnit || it.canReviewTopLevel } == true
