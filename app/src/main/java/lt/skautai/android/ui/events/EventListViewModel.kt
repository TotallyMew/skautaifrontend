package lt.skautai.android.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.repository.EventRepository
import javax.inject.Inject

sealed interface EventListUiState {
    data object Loading : EventListUiState
    data class Success(val events: List<EventDto>, val activeFilter: String?) : EventListUiState
    data class Error(val message: String) : EventListUiState
}

@HiltViewModel
class EventListViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventListUiState>(EventListUiState.Loading)
    val uiState: StateFlow<EventListUiState> = _uiState.asStateFlow()

    private var currentTypeFilter: String? = null
    private var observeJob: Job? = null

    init {
        startObserving()
        loadEvents()
    }

    fun loadEvents(type: String? = currentTypeFilter) {
        if (currentTypeFilter != type) {
            currentTypeFilter = type
            startObserving()
        } else {
            currentTypeFilter = type
        }
        viewModelScope.launch {
            if (_uiState.value !is EventListUiState.Success) {
                _uiState.value = EventListUiState.Loading
            }
            eventRepository.refreshEvents(type = type)
                .onSuccess {
                    val current = _uiState.value as? EventListUiState.Success
                    if (current != null) {
                        _uiState.value = current.copy(activeFilter = currentTypeFilter)
                    }
                }
                .onFailure { error ->
                    val current = _uiState.value as? EventListUiState.Success
                    if (current == null || current.events.isEmpty()) {
                        _uiState.value = EventListUiState.Error(
                            error.message ?: "Klaida gaunant renginius"
                        )
                    }
                }
        }
    }

    fun setTypeFilter(type: String?) {
        loadEvents(type)
    }

    private fun startObserving() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            eventRepository.observeEvents(type = currentTypeFilter).collect { events ->
                _uiState.value = EventListUiState.Success(events.events, currentTypeFilter)
            }
        }
    }
}
