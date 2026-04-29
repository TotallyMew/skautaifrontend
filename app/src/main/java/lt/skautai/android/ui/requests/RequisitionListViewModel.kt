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
            if (_uiState.value !is RequisitionListUiState.Success) {
                _uiState.value = RequisitionListUiState.Loading
            }
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
                it.unitReviewStatus == "PENDING"
            val waitsForTopLevel = it.createdByUserId != userId &&
                "requisitions.approve" in permissions &&
                it.topLevelReviewStatus == "PENDING"
            waitsForActiveUnit || waitsForTopLevel
        }
        else -> filter {
            it.createdByUserId == userId ||
                "requisitions.approve:ALL" in permissions ||
                (
                    it.requestingUnitId == activeUnitId &&
                        (
                            "requisitions.create:OWN_UNIT" in permissions ||
                            "requisitions.approve:OWN_UNIT" in permissions ||
                                "items.request.forward.bendras:OWN_UNIT" in permissions
                            )
                    )
        }
    }
