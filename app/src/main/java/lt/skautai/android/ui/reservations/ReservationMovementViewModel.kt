package lt.skautai.android.ui.reservations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.LocationDto
import lt.skautai.android.data.remote.ReservationDto
import lt.skautai.android.data.remote.ReservationItemDto
import lt.skautai.android.data.remote.ReservationMovementItemRequestDto
import lt.skautai.android.data.remote.ReservationMovementRequestDto
import lt.skautai.android.data.repository.LocationRepository
import lt.skautai.android.data.repository.ReservationRepository
import javax.inject.Inject

data class ReservationMovementUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val reservation: ReservationDto? = null,
    val selectedQuantities: Map<String, Int> = emptyMap(),
    val notes: String = "",
    val locations: List<LocationDto> = emptyList(),
    val selectedLocationId: String? = null
)

@HiltViewModel
class ReservationMovementViewModel @Inject constructor(
    private val reservationRepository: ReservationRepository,
    private val locationRepository: LocationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val reservationId: String = savedStateHandle["reservationId"] ?: ""
    val mode: String = savedStateHandle["mode"] ?: "issue"

    private val _uiState = MutableStateFlow(ReservationMovementUiState())
    val uiState: StateFlow<ReservationMovementUiState> = _uiState.asStateFlow()

    init {
        loadReservation()
    }

    fun loadReservation() {
        viewModelScope.launch {
            val cachedReservation = reservationRepository.getCachedReservation(reservationId)
            val cachedLocations = locationRepository.getCachedLocations()
            if (cachedReservation != null) {
                applyReservation(cachedReservation, cachedLocations)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            reservationRepository.getReservation(reservationId)
                .onSuccess { reservation ->
                    val locations = locationRepository.getLocations().getOrDefault(cachedLocations)
                    applyReservation(reservation, locations)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Klaida gaunant rezervaciją"
                    )
                }
        }
    }

    fun onNotesChange(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }

    fun onLocationChange(locationId: String?) {
        _uiState.value = _uiState.value.copy(selectedLocationId = locationId)
    }

    fun increase(itemId: String) {
        val state = _uiState.value
        val item = state.reservation?.items?.firstOrNull { it.itemId == itemId } ?: return
        val max = maxQuantity(item)
        val current = state.selectedQuantities[itemId] ?: 0
        if (current >= max) return
        _uiState.value = state.copy(selectedQuantities = state.selectedQuantities + (itemId to current + 1))
    }

    fun decrease(itemId: String) {
        val state = _uiState.value
        val current = state.selectedQuantities[itemId] ?: 0
        if (current <= 0) return
        val next = if (current == 1) {
            state.selectedQuantities - itemId
        } else {
            state.selectedQuantities + (itemId to current - 1)
        }
        _uiState.value = state.copy(selectedQuantities = next)
    }

    fun submit() {
        val state = _uiState.value
        if (state.isSaving) return
        val items = state.selectedQuantities
            .filterValues { it > 0 }
            .map { (itemId, quantity) -> ReservationMovementItemRequestDto(itemId, quantity) }
        if (items.isEmpty()) {
            _uiState.value = state.copy(error = "Pasirinkite bent vieną kiekį")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            val request = ReservationMovementRequestDto(
                items = items,
                locationId = state.selectedLocationId,
                notes = state.notes.ifBlank { null }
            )
            val result = when (mode) {
                "return" -> reservationRepository.returnReservationItems(reservationId, request)
                "mark_returned" -> reservationRepository.markReservationItemsReturned(reservationId, request)
                else -> reservationRepository.issueReservationItems(reservationId, request)
            }
            result
                .onSuccess { _uiState.value = _uiState.value.copy(isSaving = false, isSuccess = true) }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = error.message ?: "Klaida registruojant veiksmą"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun maxQuantity(item: ReservationItemDto): Int {
        return allowedQuantity(item)
    }

    private fun allowedQuantity(item: ReservationItemDto): Int {
        return when (mode) {
            "return" -> if (item.canConfirmReturn) item.remainingToReceive else 0
            "mark_returned" -> if (item.canMarkReturned) item.remainingToMarkReturned else 0
            else -> if (item.canIssue) item.remainingToIssue else 0
        }
    }

    private suspend fun applyReservation(reservation: ReservationDto, locations: List<LocationDto>) {
        val defaultQuantities = reservation.items
            .mapNotNull { item ->
                val max = allowedQuantity(item)
                if (max > 0) item.itemId to max else null
            }
            .toMap()
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            reservation = reservation,
            selectedQuantities = defaultQuantities,
            locations = locations,
            selectedLocationId = when (mode) {
                "return" -> reservation.returnLocationId
                else -> reservation.pickupLocationId
            }
        )
    }
}
