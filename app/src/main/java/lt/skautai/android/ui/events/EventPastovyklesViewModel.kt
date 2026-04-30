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
import lt.skautai.android.data.remote.CreatePastovykleRequestDto
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.EventRoleDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.PastovykleDto
import lt.skautai.android.data.remote.UpdatePastovykleRequestDto
import lt.skautai.android.data.repository.EventRepository
import lt.skautai.android.util.TokenManager

sealed interface EventPastovyklėsUiState {
    data object Loading : EventPastovyklėsUiState
    data class Success(
        val event: EventDto,
        val pastovykles: List<PastovykleDto> = emptyList(),
        val members: List<MemberDto> = emptyList(),
        val currentUserId: String? = null,
        val isWorking: Boolean = false,
        val error: String? = null
    ) : EventPastovyklėsUiState
    data class Error(val message: String) : EventPastovyklėsUiState
}

@HiltViewModel
class EventPastovyklėsViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventPastovyklėsUiState>(EventPastovyklėsUiState.Loading)
    val uiState: StateFlow<EventPastovyklėsUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null

    val permissions: StateFlow<Set<String>> = tokenManager.permissions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun load(eventId: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            eventRepository.observeEvent(eventId).collect { event ->
                if (event != null) {
                    val current = _uiState.value as? EventPastovyklėsUiState.Success
                    _uiState.value = EventPastovyklėsUiState.Success(
                        event = event,
                        pastovykles = current?.pastovykles.orEmpty(),
                        members = current?.members.orEmpty(),
                        currentUserId = current?.currentUserId,
                        isWorking = current?.isWorking == true,
                        error = current?.error
                    )
                } else if (_uiState.value !is EventPastovyklėsUiState.Success) {
                    _uiState.value = EventPastovyklėsUiState.Loading
                }
            }
        }

        viewModelScope.launch {
            if (_uiState.value !is EventPastovyklėsUiState.Success) {
                _uiState.value = EventPastovyklėsUiState.Loading
            }
            val eventResult = eventRepository.getEvent(eventId)
            val event = eventResult.getOrElse { error ->
                _uiState.value = EventPastovyklėsUiState.Error(error.message ?: "Nepavyko gauti renginio.")
                return@launch
            }
            val pastovykles = eventRepository.getPastovyklės(eventId).getOrNull()?.pastovykles.orEmpty()
            val members = eventRepository.getCandidateMembers(eventId).getOrNull()?.members.orEmpty()
            _uiState.value = EventPastovyklėsUiState.Success(
                event = event,
                pastovykles = pastovykles.sortedBy { it.name.lowercase() },
                members = members,
                currentUserId = tokenManager.userId.first()
            )
        }
    }

    fun savePastovykle(
        eventId: String,
        pastovykleId: String?,
        name: String,
        ageGroup: String?,
        responsibleUserId: String?,
        notes: String?
    ) {
        val current = _uiState.value as? EventPastovyklėsUiState.Success ?: return
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            _uiState.value = current.copy(error = "Įveskite pastovyklės pavadinimą.")
            return
        }

        val previous = pastovykleId?.let { id -> current.pastovykles.firstOrNull { it.id == id } }
        if (!responsibleUserId.isNullOrBlank()) {
            val member = current.members.firstOrNull { it.userId == responsibleUserId }
            if (member == null) {
                _uiState.value = current.copy(error = "Pasirinktas narys nerastas.")
                return
            }
            val currentLeaderRoleId = current.event.eventRoles.firstOrNull {
                it.role == "PASTOVYKLE_LEADER" && it.userId == previous?.responsibleUserId
            }?.id
            val currentSlot = EventStaffSlotUiModel(
                id = pastovykleId ?: "new_pastovykle",
                title = cleanName,
                subtitle = "PastovyklÄ—s pagrindinis vadovas",
                role = "PASTOVYKLE_LEADER",
                pastovykleId = pastovykleId,
                pastovykleAgeGroup = ageGroup,
                assignedUserId = previous?.responsibleUserId,
                linkedRoleId = currentLeaderRoleId
            )
            activeStaffRoleForMember(responsibleUserId, current.event, excludingSlot = currentSlot)?.let { occupiedRole ->
                _uiState.value = current.copy(
                    error = "${member.fullName()} jau turi stabo pareiga \"${occupiedRole.role}\". Pirmiausia nuimkite nuo ankstesnes pareigos."
                )
                return
            }
            if (!memberEligibleForPastovykleAgeGroup(member, ageGroup)) {
                _uiState.value = current.copy(
                    error = when (normalizePastovykleAgeGroupCode(ageGroup)) {
                        "VYR_SKAUTAI" -> "Siai pastovyklei galima priskirti tik vyr. skauta."
                        "VYR_SKAUTES" -> "Siai pastovyklei galima priskirti tik vyr. skaute."
                        else -> "Sis narys netinka pasirinktai pastovyklÄ—s amziaus grupei."
                    }
                )
                return
            }
        }

        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            val result = if (pastovykleId == null) {
                eventRepository.createPastovykle(
                    eventId,
                    CreatePastovykleRequestDto(
                        name = cleanName,
                        responsibleUserId = responsibleUserId,
                        ageGroup = ageGroup?.trim().takeUnless { it.isNullOrBlank() },
                        notes = notes?.trim().takeUnless { it.isNullOrBlank() }
                    )
                )
            } else {
                eventRepository.updatePastovykle(
                    eventId,
                    pastovykleId,
                    UpdatePastovykleRequestDto(
                        name = cleanName,
                        responsibleUserId = responsibleUserId,
                        ageGroup = ageGroup?.trim().takeUnless { it.isNullOrBlank() },
                        notes = notes?.trim().takeUnless { it.isNullOrBlank() }
                    )
                )
            }

            result
                .onSuccess { saved ->
                    syncLeaderRole(
                        eventId = eventId,
                        previousPastovykleId = previous?.id,
                        previousResponsibleUserId = previous?.responsibleUserId,
                        savedResponsibleUserId = saved.responsibleUserId
                    )
                    load(eventId)
                }
                .onFailure { error ->
                    (_uiState.value as? EventPastovyklėsUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko issaugoti pastovykles.")
                    }
                }
        }
    }

    fun deletePastovykle(eventId: String, pastovykle: PastovykleDto) {
        val current = _uiState.value as? EventPastovyklėsUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.deletePastovykle(eventId, pastovykle.id)
                .onSuccess {
                    removeLeaderRoleFor(eventId, pastovykle.responsibleUserId, excludingPastovykleId = pastovykle.id)
                    load(eventId)
                }
                .onFailure { error ->
                    (_uiState.value as? EventPastovyklėsUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko istrinti pastovykles.")
                    }
                }
        }
    }

    fun clearError() {
        (_uiState.value as? EventPastovyklėsUiState.Success)?.let { _uiState.value = it.copy(error = null) }
    }

    private suspend fun syncLeaderRole(
        eventId: String,
        previousPastovykleId: String?,
        previousResponsibleUserId: String?,
        savedResponsibleUserId: String?
    ) {
        if (!previousResponsibleUserId.isNullOrBlank() && previousResponsibleUserId != savedResponsibleUserId) {
            removeLeaderRoleFor(eventId, previousResponsibleUserId, excludingPastovykleId = previousPastovykleId)
        }
        if (!savedResponsibleUserId.isNullOrBlank()) {
            val event = (_uiState.value as? EventPastovyklėsUiState.Success)?.event
            val alreadyAssigned = event?.eventRoles.orEmpty().any {
                it.role == "PASTOVYKLE_LEADER" && it.userId == savedResponsibleUserId
            }
            if (!alreadyAssigned) {
                eventRepository.assignEventRole(
                    eventId,
                    AssignEventRoleRequestDto(userId = savedResponsibleUserId, role = "PASTOVYKLE_LEADER")
                )
            }
        }
    }

    private suspend fun removeLeaderRoleFor(eventId: String, userId: String?, excludingPastovykleId: String?) {
        if (userId.isNullOrBlank()) return
        val state = _uiState.value as? EventPastovyklėsUiState.Success ?: return
        val stillLeadsAnotherPastovykle = state.pastovykles.any {
            it.id != excludingPastovykleId && it.responsibleUserId == userId
        }
        if (stillLeadsAnotherPastovykle) return
        val event = state.event
        event.eventRoles
            .filter { it.role == "PASTOVYKLE_LEADER" && it.userId == userId }
            .forEach { role: EventRoleDto -> eventRepository.removeEventRole(eventId, role.id) }
    }
}
