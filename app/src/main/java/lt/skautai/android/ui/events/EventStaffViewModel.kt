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
import lt.skautai.android.data.remote.AssignEventRoleRequestDto
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.repository.EventRepository
import lt.skautai.android.data.repository.MemberRepository
import lt.skautai.android.util.TokenManager

sealed interface EventStaffUiState {
    data object Loading : EventStaffUiState
    data class Success(
        val event: EventDto,
        val members: List<MemberDto> = emptyList(),
        val currentUserId: String? = null,
        val isWorking: Boolean = false,
        val error: String? = null
    ) : EventStaffUiState
    data class Error(val message: String) : EventStaffUiState
}

@HiltViewModel
class EventStaffViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val memberRepository: MemberRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventStaffUiState>(EventStaffUiState.Loading)
    val uiState: StateFlow<EventStaffUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null

    val permissions: StateFlow<Set<String>> = tokenManager.permissions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun load(eventId: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            eventRepository.observeEvent(eventId).collect { event ->
                if (event != null) {
                    val current = _uiState.value as? EventStaffUiState.Success
                    _uiState.value = EventStaffUiState.Success(
                        event = event,
                        members = current?.members.orEmpty(),
                        currentUserId = current?.currentUserId,
                        isWorking = current?.isWorking == true,
                        error = current?.error
                    )
                } else if (_uiState.value !is EventStaffUiState.Success) {
                    _uiState.value = EventStaffUiState.Loading
                }
            }
        }
        viewModelScope.launch {
            if (_uiState.value !is EventStaffUiState.Success) {
                _uiState.value = EventStaffUiState.Loading
            }
            eventRepository.getEvent(eventId)
                .onFailure { error ->
                    if (_uiState.value !is EventStaffUiState.Success) {
                        _uiState.value = EventStaffUiState.Error(error.message ?: "Nepavyko gauti renginio informacijos.")
                    }
                    return@launch
                }
            val members = memberRepository.getMembers().getOrNull()?.members.orEmpty()
            val currentUserId = tokenManager.userId.first()
            val current = _uiState.value as? EventStaffUiState.Success ?: return@launch
            _uiState.value = current.copy(members = members, currentUserId = currentUserId)
        }
    }

    fun assignRole(eventId: String, userId: String, role: String) {
        val current = _uiState.value as? EventStaffUiState.Success ?: return
        if (userId.isBlank()) {
            _uiState.value = current.copy(error = "Pasirinkite žmogų.")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.assignEventRole(eventId, AssignEventRoleRequestDto(userId, role))
                .onSuccess { load(eventId) }
                .onFailure { error ->
                    (_uiState.value as? EventStaffUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko pridėti štabo nario.")
                    }
                }
        }
    }

    fun removeRole(eventId: String, roleId: String) {
        val current = _uiState.value as? EventStaffUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.removeEventRole(eventId, roleId)
                .onSuccess { load(eventId) }
                .onFailure { error ->
                    (_uiState.value as? EventStaffUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko pašalinti štabo nario.")
                    }
                }
        }
    }

    fun clearError() {
        (_uiState.value as? EventStaffUiState.Success)?.let { _uiState.value = it.copy(error = null) }
    }
}
