package lt.skautai.android.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.CreateEventRequestDto
import lt.skautai.android.data.remote.UpdateEventRequestDto
import lt.skautai.android.data.repository.EventRepository

data class EventCreateUiState(
    val eventId: String? = null,
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val name: String = "",
    val type: String = "STOVYKLA",
    val startDate: String = "",
    val endDate: String = "",
    val notes: String = ""
)

@HiltViewModel
class EventCreateViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventCreateUiState())
    val uiState: StateFlow<EventCreateUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null

    fun onNameChange(value: String) { _uiState.value = _uiState.value.copy(name = value) }
    fun onTypeChange(value: String) { _uiState.value = _uiState.value.copy(type = value) }
    fun onStartDateChange(value: String) { _uiState.value = _uiState.value.copy(startDate = value) }
    fun onEndDateChange(value: String) { _uiState.value = _uiState.value.copy(endDate = value) }
    fun onNotesChange(value: String) { _uiState.value = _uiState.value.copy(notes = value) }

    fun loadEvent(eventId: String?) {
        observeJob?.cancel()
        if (eventId == null) {
            _uiState.value = EventCreateUiState()
            return
        }
        _uiState.value = _uiState.value.copy(eventId = eventId, isEditMode = true, isLoading = true, error = null)
        observeJob = viewModelScope.launch {
            eventRepository.observeEvent(eventId).collect { event ->
                if (event != null) {
                    _uiState.value = _uiState.value.copy(
                        eventId = event.id,
                        isEditMode = true,
                        isLoading = false,
                        name = event.name,
                        type = event.type,
                        startDate = event.startDate,
                        endDate = event.endDate,
                        notes = event.notes.orEmpty()
                    )
                }
            }
        }
        viewModelScope.launch {
            eventRepository.getEvent(eventId).onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message ?: "Nepavyko gauti renginio."
                )
            }
        }
    }

    fun saveEvent() {
        val state = _uiState.value

        if (state.name.isBlank()) {
            _uiState.value = state.copy(error = "Įveskite renginio pavadinimą.")
            return
        }
        if (state.startDate.isBlank()) {
            _uiState.value = state.copy(error = "Pasirinkite pradžios datą.")
            return
        }
        if (state.endDate.isBlank()) {
            _uiState.value = state.copy(error = "Pasirinkite pabaigos datą.")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            val result = if (state.isEditMode && state.eventId != null) {
                eventRepository.updateEvent(
                    state.eventId,
                    UpdateEventRequestDto(
                        name = state.name.trim(),
                        notes = state.notes.ifBlank { null }
                    )
                )
            } else {
                eventRepository.createEvent(
                    CreateEventRequestDto(
                        name = state.name.trim(),
                        type = state.type,
                        startDate = state.startDate,
                        endDate = state.endDate,
                        notes = state.notes.ifBlank { null }
                    )
                )
            }
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, isSuccess = true)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = error.message ?: if (state.isEditMode) {
                        "Nepavyko atnaujinti renginio."
                    } else {
                        "Nepavyko sukurti renginio."
                    }
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
