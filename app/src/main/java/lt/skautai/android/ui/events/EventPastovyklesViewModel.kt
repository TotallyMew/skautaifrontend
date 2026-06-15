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

sealed interface EventPastovyklesUiState {
    data object Loading : EventPastovyklesUiState
    data class Success(
        val event: EventDto,
        val pastovykles: List<PastovykleDto> = emptyList(),
        val members: List<MemberDto> = emptyList(),
        val currentUserId: String? = null,
        val isWorking: Boolean = false,
        val error: String? = null
    ) : EventPastovyklesUiState
    data class Error(val message: String) : EventPastovyklesUiState
}

@HiltViewModel
class EventPastovyklėsViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventPastovyklesUiState>(EventPastovyklesUiState.Loading)
    val uiState: StateFlow<EventPastovyklesUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null

    val permissions: StateFlow<Set<String>> = tokenManager.permissions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun load(eventId: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            eventRepository.observeEvent(eventId).collect { event ->
                if (event != null) {
                    val current = _uiState.value as? EventPastovyklesUiState.Success
                    _uiState.value = EventPastovyklesUiState.Success(
                        event = event,
                        pastovykles = current?.pastovykles.orEmpty(),
                        members = current?.members.orEmpty(),
                        currentUserId = current?.currentUserId,
                        isWorking = current?.isWorking == true,
                        error = current?.error
                    )
                } else if (_uiState.value !is EventPastovyklesUiState.Success) {
                    _uiState.value = EventPastovyklesUiState.Loading
                }
            }
        }

        viewModelScope.launch {
            if (_uiState.value !is EventPastovyklesUiState.Success) {
                _uiState.value = EventPastovyklesUiState.Loading
            }
            val eventResult = eventRepository.getEvent(eventId)
            val event = eventResult.getOrElse { error ->
                _uiState.value = EventPastovyklesUiState.Error(error.message ?: "Nepavyko gauti renginio.")
                return@launch
            }
            val pastovykles = eventRepository.getPastovyklės(eventId).getOrNull()?.pastovykles.orEmpty()
            val members = eventRepository.getCandidateMembers(eventId).getOrNull()?.members.orEmpty()
            _uiState.value = EventPastovyklesUiState.Success(
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
        val current = _uiState.value as? EventPastovyklesUiState.Success ?: return
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
                it.role == "PASTOVYKLES_GURU" && it.userId == previous?.responsibleUserId && it.pastovykleId == null
            }?.id
            val currentSlot = EventStaffSlotUiModel(
                id = pastovykleId ?: "new_pastovykle",
                title = cleanName,
                subtitle = "Pastovyklės pagrindinis vadovas",
                role = "PASTOVYKLES_GURU",
                pastovykleId = pastovykleId,
                pastovykleAgeGroup = ageGroup,
                assignedUserId = previous?.responsibleUserId,
                linkedRoleId = currentLeaderRoleId
            )
            activeStaffRoleForMember(responsibleUserId, current.event, excludingSlot = currentSlot)?.let { occupiedRole ->
                _uiState.value = current.copy(
                    error = "${member.fullName()} jau turi štabo pareigą \"${occupiedRole.role}\". Pirmiausia nuimkite nuo ankstesnės pareigos."
                )
                return
            }
            if (!memberEligibleForPastovykleAgeGroup(member, ageGroup)) {
                _uiState.value = current.copy(
                    error = when (normalizePastovykleAgeGroupCode(ageGroup)) {
                        "VYR_SKAUTAI" -> "Šiai pastovyklei galima priskirti tik vyr. skautą."
                        "VYR_SKAUTES" -> "Šiai pastovyklei galima priskirti tik vyr. skautę."
                        else -> "Šis narys netinka pasirinktai pastovyklės amžiaus grupei."
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
                    (_uiState.value as? EventPastovyklesUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko išsaugoti pastovyklės.")
                    }
                }
        }
    }

    fun deletePastovykle(eventId: String, pastovykle: PastovykleDto) {
        val current = _uiState.value as? EventPastovyklesUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.deletePastovykle(eventId, pastovykle.id)
                .onSuccess {
                    removeLeaderRoleFor(eventId, pastovykle.responsibleUserId, excludingPastovykleId = pastovykle.id)
                    load(eventId)
                }
                .onFailure { error ->
                    (_uiState.value as? EventPastovyklesUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko ištrinti pastovyklės.")
                    }
                }
        }
    }

    fun clearError() {
        (_uiState.value as? EventPastovyklesUiState.Success)?.let { _uiState.value = it.copy(error = null) }
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
            val event = (_uiState.value as? EventPastovyklesUiState.Success)?.event
            val alreadyAssigned = event?.eventRoles.orEmpty().any {
                it.role == "PASTOVYKLES_GURU" && it.userId == savedResponsibleUserId && it.pastovykleId == null
            }
            if (!alreadyAssigned) {
                eventRepository.assignEventRole(
                    eventId,
                    AssignEventRoleRequestDto(userId = savedResponsibleUserId, role = "PASTOVYKLES_GURU")
                )
            }
        }
    }

    private suspend fun removeLeaderRoleFor(eventId: String, userId: String?, excludingPastovykleId: String?) {
        if (userId.isNullOrBlank()) return
        val state = _uiState.value as? EventPastovyklesUiState.Success ?: return
        val stillLeadsAnotherPastovykle = state.pastovykles.any {
            it.id != excludingPastovykleId && it.responsibleUserId == userId
        }
        if (stillLeadsAnotherPastovykle) return
        val event = state.event
        event.eventRoles
            .filter { it.role == "PASTOVYKLES_GURU" && it.userId == userId && it.pastovykleId == null }
            .forEach { role: EventRoleDto -> eventRepository.removeEventRole(eventId, role.id) }
    }

    fun addCoLeader(eventId: String, pastovykleId: String, userId: String) {
        val current = _uiState.value as? EventPastovyklesUiState.Success ?: return
        if (current.isWorking || userId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.assignPastovykleLeader(eventId, pastovykleId, userId)
                .onSuccess { role ->
                    updateCoLeaderRole(role.copy(pastovykleId = role.pastovykleId ?: pastovykleId))
                    load(eventId)
                }
                .onFailure { error ->
                    (_uiState.value as? EventPastovyklesUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko prideti bendravadovio.")
                    }
                }
        }
    }

    fun removeCoLeader(eventId: String, pastovykleId: String, roleId: String) {
        val current = _uiState.value as? EventPastovyklesUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.removePastovykleLeader(eventId, pastovykleId, roleId)
                .onSuccess {
                    removeCoLeaderRole(roleId)
                    load(eventId)
                }
                .onFailure { error ->
                    (_uiState.value as? EventPastovyklesUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko pašalinti bendravadovio.")
                    }
                }
        }
    }

    private fun updateCoLeaderRole(role: EventRoleDto) {
        val latest = _uiState.value as? EventPastovyklesUiState.Success ?: return
        _uiState.value = latest.copy(
            event = latest.event.copy(
                eventRoles = (latest.event.eventRoles + role).distinctBy { it.id }
            ),
            isWorking = false,
            error = null
        )
    }

    private fun removeCoLeaderRole(roleId: String) {
        val latest = _uiState.value as? EventPastovyklesUiState.Success ?: return
        _uiState.value = latest.copy(
            event = latest.event.copy(
                eventRoles = latest.event.eventRoles.filterNot { it.id == roleId }
            ),
            isWorking = false,
            error = null
        )
    }
}
