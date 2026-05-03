package lt.skautai.android.ui.reservations

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
import lt.skautai.android.data.remote.ReservationDto
import lt.skautai.android.data.remote.ReservationItemDto
import lt.skautai.android.data.repository.ReservationRepository
import lt.skautai.android.util.TokenManager

sealed interface ReservationListUiState {
    data object Loading : ReservationListUiState
    data class Success(
        val reservations: List<ReservationDto>,
        val myCount: Int,
        val assignedCount: Int,
        val trackedCount: Int,
        val canUseReviewModes: Boolean
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
    private var observeJob: Job? = null

    init {
        observeReservations()
        loadReservations()
    }

    private fun observeReservations() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            reservationRepository.observeReservations().collect { response ->
                val userId = tokenManager.userId.first()
                val permissions = tokenManager.permissions.first()
                val activeUnitId = tokenManager.activeOrgUnitId.first()
                val allReservations = response.reservations
                val canUseReviewModes = permissions.canUseReviewModes()
                _uiState.value = ReservationListUiState.Success(
                    reservations = allReservations.filterForMode(mode, userId, permissions, activeUnitId),
                    myCount = allReservations.filterForMode("my_active", userId, permissions, activeUnitId).size,
                    assignedCount = if (canUseReviewModes) allReservations.filterForMode("assigned", userId, permissions, activeUnitId).size else 0,
                    trackedCount = if (canUseReviewModes) allReservations.filterForMode("tracked", userId, permissions, activeUnitId).size else 0,
                    canUseReviewModes = canUseReviewModes
                )
            }
        }
    }

    fun loadReservations() {
        viewModelScope.launch {
            if (_uiState.value !is ReservationListUiState.Success) {
                _uiState.value = ReservationListUiState.Loading
            }
            reservationRepository.refreshReservations()
                .onSuccess {
                    val userId = tokenManager.userId.first()
                    val permissions = tokenManager.permissions.first()
                    val activeUnitId = tokenManager.activeOrgUnitId.first()
                    val allReservations = reservationRepository.getReservations().getOrNull()?.reservations.orEmpty()
                    val canUseReviewModes = permissions.canUseReviewModes()
                    _uiState.value = ReservationListUiState.Success(
                        reservations = allReservations.filterForMode(mode, userId, permissions, activeUnitId),
                        myCount = allReservations.filterForMode("my_active", userId, permissions, activeUnitId).size,
                        assignedCount = if (canUseReviewModes) allReservations.filterForMode("assigned", userId, permissions, activeUnitId).size else 0,
                        trackedCount = if (canUseReviewModes) allReservations.filterForMode("tracked", userId, permissions, activeUnitId).size else 0,
                        canUseReviewModes = canUseReviewModes
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
            it.isTrackableBy(permissions, activeUnitId) &&
                (
                    it.items.any { item -> item.canBeMovementManagedBy(permissions, activeUnitId) && item.remainingToIssue > 0 } ||
                        it.items.any { item -> item.canBeMovementManagedBy(permissions, activeUnitId) && item.remainingToReceive > 0 } ||
                        it.items.any { item -> item.canBeMovementManagedBy(permissions, activeUnitId) && item.remainingToReturn > 0 }
                    )
        }
        else -> filter { it.canBeViewedBy(userId, permissions, activeUnitId) }
    }

private fun ReservationDto.canBeViewedBy(
    userId: String?,
    permissions: Set<String>,
    activeUnitId: String?
): Boolean =
    reservedByUserId == userId ||
        permissions.canApproveTopLevelReservations() ||
        (
            permissions.canApproveUnitReservations() &&
                (
                    requestingUnitId == activeUnitId ||
                        items.any { item -> item.custodianId != null && item.custodianId == activeUnitId }
                    )
            )

private fun ReservationDto.isTrackableBy(permissions: Set<String>, activeUnitId: String?): Boolean =
    status in listOf("APPROVED", "ACTIVE") ||
        (
            status == "PENDING" &&
                (
                    (unitReviewStatus == "APPROVED" && items.any { it.custodianId != null && it.custodianId == activeUnitId }) ||
                        (topLevelReviewStatus == "APPROVED" && items.any { it.custodianId == null } && permissions.canApproveTopLevelReservations())
                    )
            )

private fun ReservationDto.canBeManagedBy(permissions: Set<String>, activeUnitId: String?): Boolean =
    items.any { item ->
        item.canBeMovementManagedBy(permissions, activeUnitId)
    }

private fun ReservationItemDto.canBeMovementManagedBy(permissions: Set<String>, activeUnitId: String?): Boolean =
    if (custodianId == null) {
        permissions.canApproveTopLevelReservations()
    } else {
        permissions.canApproveOwnUnitReservations() && custodianId == activeUnitId
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

private fun Set<String>.canApproveOwnUnitReservations(): Boolean =
    "reservations.approve:OWN_UNIT" in this

private fun Set<String>.canUseReviewModes(): Boolean =
    canApproveUnitReservations() || canApproveTopLevelReservations()
