package lt.skautai.android.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.PastovykleDto
import lt.skautai.android.data.remote.UpdateEventRequestDto
import lt.skautai.android.data.repository.EventRepository
import lt.skautai.android.util.TokenManager

sealed interface EventDetailUiState {
    data object Loading : EventDetailUiState
    data class Success(
        val event: EventDto,
        val currentUserId: String? = null,
        val pastovykles: List<PastovykleDto> = emptyList(),
        val isCancelling: Boolean = false,
        val isWorking: Boolean = false,
        val error: String? = null
    ) : EventDetailUiState
    data class Error(val message: String) : EventDetailUiState
}

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventDetailUiState>(EventDetailUiState.Loading)
    val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null

    val permissions: StateFlow<Set<String>> = tokenManager.permissions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun loadEvent(id: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            eventRepository.observeEvent(id).collect { event ->
                if (event != null) {
                    val current = _uiState.value as? EventDetailUiState.Success
                    _uiState.value = EventDetailUiState.Success(
                        event = event,
                        currentUserId = current?.currentUserId,
                        pastovykles = current?.pastovykles.orEmpty(),
                        isCancelling = current?.isCancelling == true,
                        isWorking = current?.isWorking == true,
                        error = current?.error
                    )
                } else if (_uiState.value !is EventDetailUiState.Success) {
                    _uiState.value = EventDetailUiState.Loading
                }
            }
        }
        viewModelScope.launch {
            if (_uiState.value !is EventDetailUiState.Success) {
                _uiState.value = EventDetailUiState.Loading
            }
            eventRepository.getEvent(id)
                .onFailure { error ->
                    if (_uiState.value !is EventDetailUiState.Success) {
                        _uiState.value = EventDetailUiState.Error(
                            error.message ?: "Nepavyko gauti renginio informacijos."
                        )
                    }
                    return@launch
                }
            val currentUserId = tokenManager.userId.first()
            val current = _uiState.value as? EventDetailUiState.Success ?: return@launch
            val pastovykles = eventRepository.getPastovyklės(id).getOrNull()?.pastovykles.orEmpty()
            _uiState.value = current.copy(currentUserId = currentUserId, pastovykles = pastovykles)
        }
    }

    fun cancelEvent(id: String, onSuccess: (() -> Unit)? = null) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isCancelling = true, error = null)
            eventRepository.cancelEvent(id)
                .onSuccess { onSuccess?.invoke() }
                .onFailure { error ->
                    (_uiState.value as? EventDetailUiState.Success)?.let {
                        _uiState.value = it.copy(
                            isCancelling = false,
                            error = error.message ?: "Nepavyko atšaukti renginio."
                        )
                    }
                }
        }
    }

    fun updateStatus(id: String, status: String) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(error = null)
            eventRepository.updateEvent(id, UpdateEventRequestDto(status = status))
                .onSuccess { loadEvent(id) }
                .onFailure { error ->
                    (_uiState.value as? EventDetailUiState.Success)?.let {
                        _uiState.value = it.copy(error = error.message ?: "Nepavyko pakeisti renginio būsenos.")
                    }
                }
        }
    }

    fun clearError() {
        (_uiState.value as? EventDetailUiState.Success)?.let { _uiState.value = it.copy(error = null) }
    }
}
