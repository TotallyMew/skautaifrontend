package lt.skautai.android.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.CreateEventRequestDto
import lt.skautai.android.data.repository.EventRepository
import javax.inject.Inject

data class EventCreateUiState(
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val name: String = "",
    val type: String = "STOVYKLA",
    val startDate: String = "",
    val endDate: String = "",
    val notes: String = "",
    val registrationDeadline: String = "",
    val expectedParticipants: String = ""
)

@HiltViewModel
class EventCreateViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventCreateUiState())
    val uiState: StateFlow<EventCreateUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) { _uiState.value = _uiState.value.copy(name = value) }
    fun onTypeChange(value: String) { _uiState.value = _uiState.value.copy(type = value) }
    fun onStartDateChange(value: String) { _uiState.value = _uiState.value.copy(startDate = value) }
    fun onEndDateChange(value: String) { _uiState.value = _uiState.value.copy(endDate = value) }
    fun onNotesChange(value: String) { _uiState.value = _uiState.value.copy(notes = value) }
    fun onRegistrationDeadlineChange(value: String) { _uiState.value = _uiState.value.copy(registrationDeadline = value) }
    fun onExpectedParticipantsChange(value: String) { _uiState.value = _uiState.value.copy(expectedParticipants = value) }

    fun createEvent() {
        val state = _uiState.value

        if (state.name.isBlank()) {
            _uiState.value = state.copy(error = "Įveskite renginio pavadinimą")
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

        val expectedParticipants = if (state.expectedParticipants.isNotBlank()) {
            state.expectedParticipants.toIntOrNull()?.also {
                if (it < 1) {
                    _uiState.value = state.copy(error = "Dalyvių skaičius turi būti teigiamas")
                    return
                }
            } ?: run {
                _uiState.value = state.copy(error = "Neteisingas dalyvių skaičius")
                return
            }
        } else null

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            eventRepository.createEvent(
                CreateEventRequestDto(
                    name = state.name.trim(),
                    type = state.type,
                    startDate = state.startDate,
                    endDate = state.endDate,
                    notes = state.notes.ifBlank { null },
                    registrationDeadline = state.registrationDeadline.ifBlank { null },
                    expectedParticipants = expectedParticipants
                )
            ).onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, isSuccess = true)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = error.message ?: "Klaida kuriant renginį"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
