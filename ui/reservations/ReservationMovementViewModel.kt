package lt.skautai.android.ui.reservations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.ReservationDto
import lt.skautai.android.data.remote.ReservationMovementItemRequestDto
import lt.skautai.android.data.remote.ReservationMovementRequestDto
import lt.skautai.android.data.repository.ReservationRepository
import javax.inject.Inject

data class ReservationMovementUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val reservation: ReservationDto? = null,
    val selectedQuantities: Map<String, Int> = emptyMap(),
    val notes: String = ""
)

@HiltViewModel
class ReservationMovementViewModel @Inject constructor(
    private val reservationRepository: ReservationRepository,
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
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            reservationRepository.getReservation(reservationId)
                .onSuccess { reservation ->
                    val defaultQuantities = reservation.items
                        .mapNotNull { item ->
                            val max = maxQuantity(item)
                            if (max > 0) item.itemId to max else null
                        }
                        .toMap()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        reservation = reservation,
                        selectedQuantities = defaultQuantities
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Klaida gaunant rezervacija"
                    )
                }
        }
    }

    fun onNotesChange(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
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
        val items = state.selectedQuantities
            .filterValues { it > 0 }
            .map { (itemId, quantity) -> ReservationMovementItemRequestDto(itemId, quantity) }
        if (items.isEmpty()) {
            _uiState.value = state.copy(error = "Pasirinkite bent viena kieki")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            val request = ReservationMovementRequestDto(items = items, notes = state.notes.ifBlank { null })
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
                        error = error.message ?: "Klaida registruojant veiksma"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun maxQuantity(item: lt.skautai.android.data.remote.ReservationItemDto): Int = when (mode) {
        "return" -> item.remainingToReceive
        "mark_returned" -> item.remainingToMarkReturned
        else -> item.remainingToIssue
    }
}
