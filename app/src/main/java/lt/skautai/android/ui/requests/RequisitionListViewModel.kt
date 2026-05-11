package lt.skautai.android.ui.requests

import androidx.lifecycle.SavedStateHandle
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

sealed interface RequisitionListUiState {
    data object Loading : RequisitionListUiState
    data class Success(val requests: List<RequisitionDto>) : RequisitionListUiState
    data class Error(val message: String) : RequisitionListUiState
}

@HiltViewModel
class RequisitionListViewModel @Inject constructor(
    private val requisitionRepository: RequisitionRepository,
    private val tokenManager: TokenManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val mode: String = savedStateHandle["mode"] ?: "all"

    private val _uiState = MutableStateFlow<RequisitionListUiState>(RequisitionListUiState.Loading)
    val uiState: StateFlow<RequisitionListUiState> = _uiState.asStateFlow()
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    private var observeJob: Job? = null

    init {
        observeRequests()
        loadRequests()
    }

    private fun observeRequests() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            requisitionRepository.observeRequests().collect { response ->
                val userId = tokenManager.userId.first()
                val permissions = tokenManager.permissions.first()
                val activeUnitId = tokenManager.activeOrgUnitId.first()
                _uiState.value = RequisitionListUiState.Success(
                    response.requests.filterForMode(mode, userId, permissions, activeUnitId)
                )
            }
        }
    }

    fun loadRequests() {
        viewModelScope.launch {
            val refreshOnly = _uiState.value is RequisitionListUiState.Success
            if (refreshOnly) _isRefreshing.value = true
            if (_uiState.value !is RequisitionListUiState.Success) {
                _uiState.value = RequisitionListUiState.Loading
            }
            try {
                requisitionRepository.refreshRequests()
                .onSuccess {
                    val userId = tokenManager.userId.first()
                    val permissions = tokenManager.permissions.first()
                    val activeUnitId = tokenManager.activeOrgUnitId.first()
                    _uiState.value = RequisitionListUiState.Success(
                        requisitionRepository.getRequests()
                            .getOrNull()
                            ?.requests
                            .orEmpty()
                            .filterForMode(mode, userId, permissions, activeUnitId)
                    )
                }
                .onFailure { error ->
                    _uiState.value = RequisitionListUiState.Error(
                        error.message ?: "Klaida gaunant prašymus"
                    )
                }
            } finally {
                if (refreshOnly) _isRefreshing.value = false
            }
        }
    }
}

private fun List<RequisitionDto>.filterForMode(
    mode: String,
    userId: String?,
    permissions: Set<String>,
    activeUnitId: String?
): List<RequisitionDto> =
    when (mode) {
        "my_active" -> filter { it.createdByUserId == userId }
        "assigned" -> filter {
            val waitsForActiveUnit = it.createdByUserId != userId &&
                it.requestingUnitId == activeUnitId &&
                it.unitReviewStatus == "PENDING" &&
                (
                    permissions.hasPermission("items.request.approve.unit") ||
                        permissions.canForwardUnitRequests()
                    )
            val waitsForTopLevel = it.createdByUserId != userId &&
                permissions.canReviewTopLevelRequisitions() &&
                it.topLevelReviewStatus == "PENDING"
            waitsForActiveUnit || waitsForTopLevel
        }
        else -> filter {
            it.createdByUserId == userId ||
                permissions.canReviewTopLevelRequisitions() ||
                (
                    it.requestingUnitId == activeUnitId &&
                        (
                            permissions.hasPermission("requisitions.create") ||
                                permissions.hasPermission("requisitions.approve") ||
                                permissions.canForwardUnitRequests()
                            )
                    )
        }
    }
