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
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
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
                val allReservations = response.reservations
                val canUseReviewModes = response.capabilities.canUseReviewModes
                _uiState.value = ReservationListUiState.Success(
                    reservations = allReservations.filterForMode(mode, userId),
                    myCount = allReservations.filterForMode("my_active", userId).size,
                    assignedCount = if (canUseReviewModes) allReservations.filterForMode("assigned", userId).size else 0,
                    trackedCount = if (canUseReviewModes) allReservations.filterForMode("tracked", userId).size else 0,
                    canUseReviewModes = canUseReviewModes
                )
            }
        }
    }

    fun loadReservations() {
        viewModelScope.launch {
            val refreshOnly = _uiState.value is ReservationListUiState.Success
            if (refreshOnly) _isRefreshing.value = true
            if (_uiState.value !is ReservationListUiState.Success) {
                _uiState.value = ReservationListUiState.Loading
            }
            try {
                reservationRepository.refreshReservations()
                    .onSuccess {
                        val userId = tokenManager.userId.first()
                        val cached = reservationRepository.getCachedReservations()
                        val allReservations = cached.reservations
                        val canUseReviewModes = cached.capabilities.canUseReviewModes
                        _uiState.value = ReservationListUiState.Success(
                            reservations = allReservations.filterForMode(mode, userId),
                            myCount = allReservations.filterForMode("my_active", userId).size,
                            assignedCount = if (canUseReviewModes) allReservations.filterForMode("assigned", userId).size else 0,
                            trackedCount = if (canUseReviewModes) allReservations.filterForMode("tracked", userId).size else 0,
                            canUseReviewModes = canUseReviewModes
                        )
                    }
                    .onFailure { error ->
                        _uiState.value = ReservationListUiState.Error(
                            error.message ?: "Klaida gaunant rezervacijas"
                        )
                    }
            } finally {
                if (refreshOnly) _isRefreshing.value = false
            }
        }
    }
}

private fun List<ReservationDto>.filterForMode(
    mode: String,
    userId: String?
): List<ReservationDto> =
    when (mode) {
        "my_active" -> filter {
            it.reservedByUserId == userId && it.status in listOf("APPROVED", "ACTIVE")
        }
        "assigned" -> filter { reservation ->
            reservation.capabilities?.let { it.canReviewUnit || it.canReviewTopLevel } == true
        }
        "tracked" -> filter {
            it.capabilities?.let { capabilities ->
                capabilities.canIssue || capabilities.canConfirmReturn || capabilities.canMarkReturned
            } == true
        }
        else -> this
    }
