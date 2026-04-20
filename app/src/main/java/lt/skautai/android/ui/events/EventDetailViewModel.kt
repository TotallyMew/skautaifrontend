package lt.skautai.android.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.UpdateEventRequestDto
import lt.skautai.android.data.repository.EventRepository
import lt.skautai.android.util.TokenManager
import javax.inject.Inject

sealed interface EventDetailUiState {
    data object Loading : EventDetailUiState
    data class Success(
        val event: EventDto,
        val isCancelling: Boolean = false,
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

    val permissions: StateFlow<Set<String>> = tokenManager.permissions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun loadEvent(id: String) {
        viewModelScope.launch {
            _uiState.value = EventDetailUiState.Loading
            eventRepository.getEvent(id)
                .onSuccess { event ->
                    _uiState.value = EventDetailUiState.Success(event)
                }
                .onFailure { error ->
                    _uiState.value = EventDetailUiState.Error(
                        error.message ?: "Klaida gaunant renginio informaciją"
                    )
                }
        }
    }

    fun cancelEvent(id: String) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isCancelling = true, error = null)
            eventRepository.cancelEvent(id)
                .onSuccess {
                    loadEvent(id)
                }
                .onFailure { error ->
                    _uiState.value = current.copy(
                        isCancelling = false,
                        error = error.message ?: "Klaida atšaukiant renginį"
                    )
                }
        }
    }

    fun updateStatus(id: String, status: String) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(error = null)
            eventRepository.updateEvent(id, UpdateEventRequestDto(status = status))
                .onSuccess {
                    loadEvent(id)
                }
                .onFailure { error ->
                    _uiState.value = current.copy(error = error.message ?: "Klaida keičiant statusą")
                }
        }
    }

    fun clearError() {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        _uiState.value = current.copy(error = null)
    }
}
