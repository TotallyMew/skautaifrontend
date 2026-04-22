package lt.skautai.android.ui.reservations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.ReservationDto
import lt.skautai.android.data.repository.ReservationRepository
import lt.skautai.android.util.TokenManager
import javax.inject.Inject

sealed interface ReservationListUiState {
    data object Loading : ReservationListUiState
    data class Success(
        val reservations: List<ReservationDto>,
        val myCount: Int,
        val assignedCount: Int,
        val trackedCount: Int
    ) : ReservationListUiState
    data class Error(val message: String) : ReservationListUiState
}

@HiltViewModel
class ReservationListViewModel @Inject constructor(
    private val reservationRepository: ReservationRepository,
    private val tokenManager: TokenManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val mode: String = savedStateHandle["mode"] ?: "all"

    private val _uiState = MutableStateFlow<ReservationListUiState>(ReservationListUiState.Loading)
    val uiState: StateFlow<ReservationListUiState> = _uiState.asStateFlow()

    init {
        loadReservations()
    }

    fun loadReservations() {
        viewModelScope.launch {
            if (_uiState.value !is ReservationListUiState.Success) {
                _uiState.value = ReservationListUiState.Loading
            }
            reservationRepository.getReservations()
                .onSuccess { response ->
                    val userId = tokenManager.userId.first()
                    val permissions = tokenManager.permissions.first()
                    val activeUnitId = tokenManager.activeOrgUnitId.first()
                    val allReservations = response.reservations
                    _uiState.value = ReservationListUiState.Success(
                        reservations = allReservations.filterForMode(mode, userId, permissions, activeUnitId),
                        myCount = allReservations.filterForMode("my_active", userId, permissions, activeUnitId).size,
                        assignedCount = allReservations.filterForMode("assigned", userId, permissions, activeUnitId).size,
                        trackedCount = allReservations.filterForMode("tracked", userId, permissions, activeUnitId).size
                    )
                }
                .onFailure { error ->
                    _uiState.value = ReservationListUiState.Error(
                        error.message ?: "Klaida gaunant rezervacijas"
                    )
                }
        }
    }
}

private fun List<ReservationDto>.filterForMode(
    mode: String,
    userId: String?,
    permissions: Set<String>,
    activeUnitId: String?
): List<ReservationDto> =
    when (mode) {
        "my_active" -> filter {
            it.reservedByUserId == userId && it.status in listOf("APPROVED", "ACTIVE")
        }
        "assigned" -> filter {
            it.status == "PENDING" &&
                (
                    (permissions.canApproveUnitReservations() &&
                        it.isUnitReviewPending() &&
                        it.items.any { item -> item.custodianId != null && item.custodianId == activeUnitId }) ||
                        (permissions.canApproveTopLevelReservations() &&
                            it.isTopLevelReviewPending() &&
                            it.items.any { item -> item.custodianId == null })
                    )
        }
        "tracked" -> filter {
            it.status in listOf("APPROVED", "ACTIVE") &&
                it.canBeManagedBy(permissions, activeUnitId) &&
                (
                    it.items.any { item -> item.remainingToIssue > 0 } ||
                        it.items.any { item -> item.remainingToReceive > 0 } ||
                        it.items.any { item -> item.remainingToReturn > 0 }
                    )
        }
        else -> this
    }

private fun ReservationDto.canBeManagedBy(permissions: Set<String>, activeUnitId: String?): Boolean =
    items.any { item ->
        when {
            item.custodianId == null -> permissions.canApproveTopLevelReservations()
            permissions.canApproveTopLevelReservations() -> true
            else -> permissions.canApproveUnitReservations() && item.custodianId == activeUnitId
        }
    }

private fun ReservationDto.isUnitReviewPending(): Boolean =
    unitReviewStatus == "PENDING" ||
        (unitReviewStatus == null && status == "PENDING" && items.any { it.custodianId != null })

private fun ReservationDto.isTopLevelReviewPending(): Boolean =
    topLevelReviewStatus == "PENDING" ||
        (topLevelReviewStatus == null && status == "PENDING" && items.any { it.custodianId == null })

private fun Set<String>.canApproveTopLevelReservations(): Boolean =
    "reservations.approve:ALL" in this

private fun Set<String>.canApproveUnitReservations(): Boolean =
    "reservations.approve:OWN_UNIT" in this || canApproveTopLevelReservations()
