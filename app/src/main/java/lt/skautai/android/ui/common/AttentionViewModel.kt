package lt.skautai.android.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.RequisitionDto
import lt.skautai.android.data.repository.RequisitionRepository
import lt.skautai.android.util.TokenManager
import lt.skautai.android.util.canForwardUnitRequests
import lt.skautai.android.util.canReviewTopLevelRequisitions
import lt.skautai.android.util.hasPermission

data class AttentionUiState(
    val assignedRequisitionCount: Int = 0
)

@HiltViewModel
class AttentionViewModel @Inject constructor(
    private val requisitionRepository: RequisitionRepository,
    private val tokenManager: TokenManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(AttentionUiState())
    val uiState: StateFlow<AttentionUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null

    init {
        observeCachedRequests()
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            requisitionRepository.refreshRequests()
        }
    }

    private fun observeCachedRequests() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            requisitionRepository.observeRequests().collect { response ->
                val userId = tokenManager.userId.first()
                val permissions = tokenManager.permissions.first()
                val activeUnitId = tokenManager.activeOrgUnitId.first()
                _uiState.value = AttentionUiState(
                    assignedRequisitionCount = response.requests.count {
                        it.isAssignedToCurrentUser(userId, permissions, activeUnitId)
                    }
                )
            }
        }
    }
}

private fun RequisitionDto.isAssignedToCurrentUser(
    userId: String?,
    permissions: Set<String>,
    activeUnitId: String?
): Boolean {
    val waitsForActiveUnit = createdByUserId != userId &&
        requestingUnitId == activeUnitId &&
        unitReviewStatus == "PENDING" &&
        (
            permissions.hasPermission("items.request.approve.unit") ||
                permissions.canForwardUnitRequests()
            )
    val waitsForTopLevel = createdByUserId != userId &&
        permissions.canReviewTopLevelRequisitions() &&
        topLevelReviewStatus == "PENDING"
    return waitsForActiveUnit || waitsForTopLevel
}
