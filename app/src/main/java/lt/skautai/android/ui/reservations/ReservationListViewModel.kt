package lt.skautai.android.ui.reservations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.ReservationDto
import lt.skautai.android.data.repository.ReservationRepository
import javax.inject.Inject

sealed interface ReservationListUiState {
    data object Loading : ReservationListUiState
    data class Success(val reservations: List<ReservationDto>) : ReservationListUiState
    data class Error(val message: String) : ReservationListUiState
}

@HiltViewModel
class ReservationListViewModel @Inject constructor(
    private val reservationRepository: ReservationRepository
) : ViewModel() {

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
                    _uiState.value = ReservationListUiState.Success(response.reservations)
                }
                .onFailure { error ->
                    _uiState.value = ReservationListUiState.Error(
                        error.message ?: "Klaida gaunant rezervacijas"
                    )
                }
        }
    }
}