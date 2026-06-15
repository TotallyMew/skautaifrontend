package lt.skautai.android.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.ReservationDto
import lt.skautai.android.data.repository.EventRepository
import lt.skautai.android.data.repository.ReservationRepository

sealed interface CalendarUiState {
    object Loading : CalendarUiState
    data class Success(
        val events: List<EventDto>,
        val reservations: List<ReservationDto>
    ) : CalendarUiState
    data class Error(val message: String) : CalendarUiState
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val reservationRepository: ReservationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<CalendarUiState>(CalendarUiState.Loading)
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadCachedOrRefresh()
    }

    private fun loadCachedOrRefresh() {
        viewModelScope.launch {
            _uiState.value = CalendarUiState.Loading
            val eventsDeferred = async { eventRepository.getCachedEvents() }
            val reservationsDeferred = async { reservationRepository.getCachedReservations() }
            val events = eventsDeferred.await()
            val reservations = reservationsDeferred.await()

            if (events.events.isNotEmpty() || reservations.reservations.isNotEmpty()) {
                _uiState.value = CalendarUiState.Success(
                    events = events.events,
                    reservations = reservations.reservations
                )
            } else {
                refresh()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = CalendarUiState.Loading
            val eventsDeferred = async { eventRepository.getEvents() }
            val reservationsDeferred = async { reservationRepository.getReservations() }
            val events = eventsDeferred.await()
            val reservations = reservationsDeferred.await()

            if (events.isSuccess || reservations.isSuccess) {
                _uiState.value = CalendarUiState.Success(
                    events = events.getOrNull()?.events.orEmpty(),
                    reservations = reservations.getOrNull()?.reservations.orEmpty()
                )
            } else {
                _uiState.value = CalendarUiState.Error(
                    events.exceptionOrNull()?.message
                        ?: reservations.exceptionOrNull()?.message
                        ?: "Nepavyko gauti kalendoriaus duomenų"
                )
            }
        }
    }
}
