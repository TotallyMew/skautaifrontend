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
import lt.skautai.android.data.remote.UpdateReservationStatusRequestDto
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

    fun loadReservation(id: String) {
        viewModelScope.launch {
            _uiState.value = ReservationDetailUiState.Loading
            reservationRepository.getReservation(id)
                .onSuccess { reservation ->
                    _uiState.value = ReservationDetailUiState.Success(reservation)
                }
                .onFailure { error ->
                    _uiState.value = ReservationDetailUiState.Error(
                        error.message ?: "Klaida gaunant rezervaciją"
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

    fun approveReservation(id: String) {
        updateStatus(id, "APPROVED")
    }

    fun rejectReservation(id: String) {
        updateStatus(id, "REJECTED")
    }

    private fun updateStatus(id: String, status: String) {
        val current = _uiState.value as? ReservationDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(error = null)
            reservationRepository.updateReservationStatus(
                id = id,
                request = UpdateReservationStatusRequestDto(status = status, notes = null)
            ).onSuccess {
                loadReservation(id)
            }.onFailure { error ->
                _uiState.value = current.copy(
                    error = error.message ?: "Klaida atnaujinant statusą"
                )
            }
        }
    }

    fun clearError() {
        val current = _uiState.value as? ReservationDetailUiState.Success ?: return
        _uiState.value = current.copy(error = null)
    }
}