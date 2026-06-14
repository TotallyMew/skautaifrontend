package lt.skautai.android.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.CreateEventPackingContainerRequestDto
import lt.skautai.android.data.remote.EventPackingListDto
import lt.skautai.android.data.remote.UpdateEventPackingLineRequestDto
import lt.skautai.android.data.repository.EventRepository

sealed interface EventPackingUiState {
    data object Loading : EventPackingUiState
    data class Success(
        val packingList: EventPackingListDto,
        val isWorking: Boolean = false,
        val error: String? = null
    ) : EventPackingUiState
    data class Error(val message: String) : EventPackingUiState
}

@HiltViewModel
class EventPackingViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventPackingUiState>(EventPackingUiState.Loading)
    val uiState: StateFlow<EventPackingUiState> = _uiState.asStateFlow()

    fun load(eventId: String) {
        viewModelScope.launch {
            if (_uiState.value !is EventPackingUiState.Success) {
                _uiState.value = EventPackingUiState.Loading
            }
            eventRepository.getPackingList(eventId)
                .onSuccess { _uiState.value = EventPackingUiState.Success(it) }
                .onFailure { error ->
                    _uiState.value = EventPackingUiState.Error(
                        error.message ?: "Nepavyko gauti pakavimo saraso."
                    )
                }
        }
    }

    fun generate(eventId: String) {
        val current = _uiState.value as? EventPackingUiState.Success
        viewModelScope.launch {
            if (current != null) {
                _uiState.value = current.copy(isWorking = true, error = null)
            } else {
                _uiState.value = EventPackingUiState.Loading
            }
            eventRepository.generatePackingList(eventId)
                .onSuccess { _uiState.value = EventPackingUiState.Success(it) }
                .onFailure { error ->
                    val message = error.message ?: "Nepavyko sugeneruoti pakavimo saraso."
                    (_uiState.value as? EventPackingUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = message)
                    } ?: run {
                        _uiState.value = EventPackingUiState.Error(message)
                    }
                }
        }
    }

    fun updateLineStatus(eventId: String, lineId: String, status: String) {
        val current = _uiState.value as? EventPackingUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.updatePackingLine(
                eventId = eventId,
                lineId = lineId,
                request = UpdateEventPackingLineRequestDto(status = status)
            )
                .onSuccess { _uiState.value = EventPackingUiState.Success(it) }
                .onFailure { error ->
                    (_uiState.value as? EventPackingUiState.Success)?.let {
                        _uiState.value = it.copy(
                            isWorking = false,
                            error = error.message ?: "Nepavyko atnaujinti pakavimo eilutes."
                        )
                    }
                }
        }
    }

    fun createContainer(eventId: String, name: String) {
        val current = _uiState.value as? EventPackingUiState.Success ?: return
        if (name.isBlank()) {
            _uiState.value = current.copy(error = "Ivesk pakavimo vietos pavadinima.")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.createPackingContainer(
                eventId = eventId,
                request = CreateEventPackingContainerRequestDto(name = name.trim())
            )
                .onSuccess { _uiState.value = EventPackingUiState.Success(it) }
                .onFailure { error ->
                    (_uiState.value as? EventPackingUiState.Success)?.let {
                        _uiState.value = it.copy(
                            isWorking = false,
                            error = error.message ?: "Nepavyko sukurti pakavimo vietos."
                        )
                    }
                }
        }
    }

    fun updateLineDetails(
        eventId: String,
        lineId: String,
        containerId: String?,
        notes: String?
    ) {
        val current = _uiState.value as? EventPackingUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.updatePackingLine(
                eventId = eventId,
                lineId = lineId,
                request = UpdateEventPackingLineRequestDto(
                    containerId = containerId,
                    clearContainer = containerId == null,
                    notes = notes?.trim()?.takeIf { it.isNotBlank() }
                )
            )
                .onSuccess { _uiState.value = EventPackingUiState.Success(it) }
                .onFailure { error ->
                    (_uiState.value as? EventPackingUiState.Success)?.let {
                        _uiState.value = it.copy(
                            isWorking = false,
                            error = error.message ?: "Nepavyko atnaujinti pakavimo eilutes."
                        )
                    }
                }
        }
    }

    fun clearError() {
        (_uiState.value as? EventPackingUiState.Success)?.let {
            _uiState.value = it.copy(error = null)
        }
    }
}
