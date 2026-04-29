package lt.skautai.android.ui.reservations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.ReservationDto
import lt.skautai.android.data.remote.ReviewReservationRequestDto
import lt.skautai.android.data.remote.UpdateReservationPickupRequestDto
import lt.skautai.android.data.remote.UpdateReservationReturnTimeRequestDto
import lt.skautai.android.data.repository.ReservationRepository
import lt.skautai.android.util.TokenManager
import javax.inject.Inject

sealed interface ReservationDetailUiState {
    data object Loading : ReservationDetailUiState
    data class Success(
        val reservation: ReservationDto,
        val isCancelling: Boolean = false,
        val actionSuccess: String? = null,
        val error: String? = null
    ) : ReservationDetailUiState
    data class Error(val message: String) : ReservationDetailUiState
}

@HiltViewModel
class ReservationDetailViewModel @Inject constructor(
    private val reservationRepository: ReservationRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReservationDetailUiState>(ReservationDetailUiState.Loading)
    val uiState: StateFlow<ReservationDetailUiState> = _uiState.asStateFlow()

    val permissions: StateFlow<Set<String>> = tokenManager.permissions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val userId: StateFlow<String?> = tokenManager.userId
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val activeOrgUnitId: StateFlow<String?> = tokenManager.activeOrgUnitId
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun loadReservation(id: String) {
        viewModelScope.launch {
            _uiState.value = ReservationDetailUiState.Loading
            reservationRepository.getReservation(id)
                .onSuccess { reservation ->
                    _uiState.value = ReservationDetailUiState.Success(reservation)
                }
                .onFailure { error ->
                    _uiState.value = ReservationDetailUiState.Error(
                        error.message ?: "Klaida gaunant rezervacija"
                    )
                }
        }
    }

    fun cancelReservation(id: String) {
        val current = _uiState.value as? ReservationDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isCancelling = true, error = null)
            reservationRepository.cancelReservation(id)
                .onSuccess {
                    loadReservation(id)
                }
                .onFailure { error ->
                    _uiState.value = current.copy(
                        isCancelling = false,
                        error = error.message ?: "Klaida atšaukiant rezervaciją"
                    )
                }
        }
    }

    fun reviewUnitReservation(id: String, status: String) {
        reviewReservation(id, status) { reservationId, request ->
            reservationRepository.reviewUnitReservation(reservationId, request)
        }
    }

    fun reviewTopLevelReservation(id: String, status: String) {
        reviewReservation(id, status) { reservationId, request ->
            reservationRepository.reviewTopLevelReservation(reservationId, request)
        }
    }

    fun updatePickupTime(id: String, pickupAt: String?, response: String? = null) {
        val current = _uiState.value as? ReservationDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(error = null)
            reservationRepository.updateReservationPickupTime(
                id = id,
                request = UpdateReservationPickupRequestDto(pickupAt = pickupAt, response = response)
            ).onSuccess {
                loadReservation(id)
            }.onFailure { error ->
                _uiState.value = current.copy(
                    error = error.message ?: "Klaida atnaujinant atsiemimo laika"
                )
            }
        }
    }

    fun updateReturnTime(id: String, returnAt: String?, response: String? = null) {
        val current = _uiState.value as? ReservationDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(error = null)
            reservationRepository.updateReservationReturnTime(
                id = id,
                request = UpdateReservationReturnTimeRequestDto(returnAt = returnAt, response = response)
            ).onSuccess {
                loadReservation(id)
            }.onFailure { error ->
                _uiState.value = current.copy(
                    error = error.message ?: "Klaida atnaujinant grąžinimo laiką"
                )
            }
        }
    }

    private fun reviewReservation(
        id: String,
        status: String,
        action: suspend (String, ReviewReservationRequestDto) -> Result<ReservationDto>
    ) {
        val current = _uiState.value as? ReservationDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(error = null)
            action(id, ReviewReservationRequestDto(status = status))
                .onSuccess { loadReservation(id) }
                .onFailure { error ->
                    _uiState.value = current.copy(
                        error = error.message ?: "Klaida tvirtinant rezervacija"
                    )
                }
        }
    }

    fun clearError() {
        val current = _uiState.value as? ReservationDetailUiState.Success ?: return
        _uiState.value = current.copy(error = null)
    }
}
