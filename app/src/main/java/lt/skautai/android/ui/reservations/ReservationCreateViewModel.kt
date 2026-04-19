package lt.skautai.android.ui.reservations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.CreateReservationRequestDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.data.repository.ReservationRepository
import javax.inject.Inject

data class ReservationCreateUiState(
    val isLoadingItems: Boolean = true,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val items: List<ItemDto> = emptyList(),
    val selectedItemId: String = "",
    val quantity: String = "1",
    val startDate: String = "",
    val endDate: String = "",
    val notes: String = ""
)

@HiltViewModel
class ReservationCreateViewModel @Inject constructor(
    private val reservationRepository: ReservationRepository,
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReservationCreateUiState())
    val uiState: StateFlow<ReservationCreateUiState> = _uiState.asStateFlow()

    init {
        loadItems()
    }

    private fun loadItems() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingItems = true, error = null)
            itemRepository.getItems(status = "ACTIVE")
                .onSuccess { items ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingItems = false,
                        items = items
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingItems = false,
                        error = error.message ?: "Klaida gaunant daiktus"
                    )
                }
        }
    }

    fun onItemSelected(itemId: String) {
        _uiState.value = _uiState.value.copy(selectedItemId = itemId)
    }

    fun onQuantityChange(value: String) {
        _uiState.value = _uiState.value.copy(quantity = value)
    }

    fun onStartDateChange(value: String) {
        _uiState.value = _uiState.value.copy(startDate = value)
    }

    fun onEndDateChange(value: String) {
        _uiState.value = _uiState.value.copy(endDate = value)
    }

    fun onNotesChange(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun createReservation() {
        val state = _uiState.value

        if (state.selectedItemId.isBlank()) {
            _uiState.value = state.copy(error = "Pasirinkite daiktą")
            return
        }
        val qty = state.quantity.toIntOrNull()
        if (qty == null || qty < 1) {
            _uiState.value = state.copy(error = "Kiekis turi būti teigiamas skaičius")
            return
        }
        if (state.startDate.isBlank()) {
            _uiState.value = state.copy(error = "Įveskite pradžios datą")
            return
        }
        if (state.endDate.isBlank()) {
            _uiState.value = state.copy(error = "Įveskite pabaigos datą")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            reservationRepository.createReservation(
                CreateReservationRequestDto(
                    itemId = state.selectedItemId,
                    quantity = qty,
                    startDate = state.startDate,
                    endDate = state.endDate,
                    notes = state.notes.ifBlank { null }
                )
            ).onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, isSuccess = true)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = error.message ?: "Klaida kuriant rezervaciją"
                )
            }
        }
    }
}