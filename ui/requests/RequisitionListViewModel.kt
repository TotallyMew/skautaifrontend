package lt.skautai.android.ui.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.RequisitionDto
import lt.skautai.android.data.repository.RequisitionRepository
import lt.skautai.android.util.TokenManager
import javax.inject.Inject

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
                    val userId = tokenManager.userId.first()
                    val permissions = tokenManager.permissions.first()
                    val activeUnitId = tokenManager.activeOrgUnitId.first()
                    _uiState.value = RequisitionListUiState.Success(
                        response.requests.filterForMode(mode, userId, permissions, activeUnitId)
                    )
                }
                .onFailure { error ->
                    _uiState.value = RequisitionListUiState.Error(
                        error.message ?: "Klaida gaunant prasymus"
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
        "my_active" -> filter {
            it.createdByUserId == userId &&
                it.status == "APPROVED" &&
                it.topLevelReviewStatus == "APPROVED"
        }
        "assigned" -> filter {
            val waitsForActiveUnit = it.createdByUserId != userId &&
                it.requestingUnitId == activeUnitId &&
                it.unitReviewStatus == "PENDING"
            val waitsForTopLevel = it.createdByUserId != userId &&
                "requisitions.approve" in permissions &&
                it.topLevelReviewStatus == "PENDING"
            waitsForActiveUnit || waitsForTopLevel
        }
        else -> this
    }
