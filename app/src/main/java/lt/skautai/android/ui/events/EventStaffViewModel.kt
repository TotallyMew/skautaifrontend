package lt.skautai.android.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.Normalizer
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
import lt.skautai.android.data.remote.EventRoleDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.PastovykleDto
import lt.skautai.android.data.remote.UpdatePastovykleRequestDto
import lt.skautai.android.data.repository.EventRepository
import lt.skautai.android.util.TokenManager

data class EventStaffSlotUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val role: String,
    val targetGroup: String? = null,
    val pastovykleId: String? = null,
    val pastovykleAgeGroup: String? = null,
    val assignedUserId: String? = null,
    val assignedUserName: String? = null,
    val linkedRoleId: String? = null,
    val isLocked: Boolean = false,
    val opensPastovykleScreen: Boolean = false
)

private val coreStaffRoles = setOf("VIRSININKAS", "UKVEDYS", "KOMENDANTAS", "MAISTININKAS")

sealed interface EventStaffUiState {
    data object Loading : EventStaffUiState
    data class Success(
        val event: EventDto,
        val members: List<MemberDto> = emptyList(),
        val pastovykles: List<PastovykleDto> = emptyList(),
        val slots: List<EventStaffSlotUiModel> = emptyList(),
        val additionalRoles: List<EventRoleDto> = emptyList(),
        val currentUserId: String? = null,
        val isWorking: Boolean = false,
        val error: String? = null
    ) : EventStaffUiState {
        val coreSlots: List<EventStaffSlotUiModel>
            get() = slots.filter { it.role in coreStaffRoles }
        val programSlots: List<EventStaffSlotUiModel>
            get() = slots.filter { it.role == "PROGRAMERIS" }
        val pastovykleSlots: List<EventStaffSlotUiModel>
            get() = slots.filter { it.role == "PASTOVYKLE_LEADER" }
    }
    data class Error(val message: String) : EventStaffUiState
}

@HiltViewModel
class EventStaffViewModel @Inject constructor(
    private val eventRepository: EventRepository,
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
                    _uiState.value = buildSuccessState(
                        event = event,
                        members = current?.members.orEmpty(),
                        pastovykles = current?.pastovykles.orEmpty(),
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

            val event = (_uiState.value as? EventStaffUiState.Success)?.event ?: return@launch
            val members = eventRepository.getCandidateMembers(eventId).getOrNull()?.members.orEmpty()
            val pastovykles = eventRepository.getPastovyklės(eventId).getOrNull()?.pastovykles.orEmpty()
            val currentUserId = tokenManager.userId.first()
            _uiState.value = buildSuccessState(
                event = event,
                members = members,
                pastovykles = pastovykles,
                currentUserId = currentUserId
            )
        }
    }

    fun assignToSlot(eventId: String, slot: EventStaffSlotUiModel, userId: String) {
        val current = _uiState.value as? EventStaffUiState.Success ?: return
        if (userId.isBlank()) {
            _uiState.value = current.copy(error = "Pasirinkite zmogu.")
            return
        }
        val member = current.members.firstOrNull { it.userId == userId }
        if (member == null) {
            _uiState.value = current.copy(error = "Pasirinktas narys nerastas.")
            return
        }
        activeStaffRoleForMember(userId, current.event, excludingSlot = slot)?.let { occupiedRole ->
            _uiState.value = current.copy(
                error = "${member.fullName()} jau turi stabo pareiga \"${staffRoleLabel(occupiedRole.role)}\". Pirmiausia nuimkite nuo ankstesnes pareigos."
            )
            return
        }
        if (!memberEligibleForPastovykleAgeGroup(member, slot.pastovykleAgeGroup)) {
            _uiState.value = current.copy(
                error = when (normalizePastovykleAgeGroupCode(slot.pastovykleAgeGroup)) {
                    "VYR_SKAUTAI" -> "Siai pastovyklei galima priskirti tik vyr. skauta."
                    "VYR_SKAUTES" -> "Siai pastovyklei galima priskirti tik vyr. skaute."
                    else -> "Sis narys netinka pasirinktai pastovyklės amziaus grupei."
                }
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            val result = when (slot.role) {
                "PASTOVYKLE_LEADER" -> assignPastovykleLeader(eventId, slot, userId)
                else -> reassignRole(eventId, slot, userId)
            }
            result
                .onSuccess { load(eventId) }
                .onFailure { error ->
                    (_uiState.value as? EventStaffUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko priskirti pareigos.")
                    }
                }
        }
    }

    fun assignAdditionalRole(eventId: String, userId: String, role: String) {
        val current = _uiState.value as? EventStaffUiState.Success ?: return
        if (userId.isBlank()) {
            _uiState.value = current.copy(error = "Pasirinkite zmogu.")
            return
        }
        val member = current.members.firstOrNull { it.userId == userId }
        if (member == null) {
            _uiState.value = current.copy(error = "Pasirinktas narys nerastas.")
            return
        }
        activeStaffRoleForMember(userId, current.event)?.let { occupiedRole ->
            _uiState.value = current.copy(
                error = "${member.fullName()} jau turi stabo pareiga \"${staffRoleLabel(occupiedRole.role)}\". Pirmiausia nuimkite nuo ankstesnes pareigos."
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.assignEventRole(eventId, AssignEventRoleRequestDto(userId = userId, role = role))
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
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko pasalinti stabo nario.")
                    }
                }
        }
    }

    fun removeFromSlot(eventId: String, slot: EventStaffSlotUiModel) {
        val current = _uiState.value as? EventStaffUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            val result = when (slot.role) {
                "PASTOVYKLE_LEADER" -> removePastovykleLeader(eventId, slot)
                else -> slot.linkedRoleId?.let { eventRepository.removeEventRole(eventId, it) }
                    ?: Result.failure(Exception("Pareiga nėra priskirta."))
            }
            result
                .onSuccess { load(eventId) }
                .onFailure { error ->
                    (_uiState.value as? EventStaffUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko pasalinti pareigos.")
                    }
                }
        }
    }

    fun clearError() {
        (_uiState.value as? EventStaffUiState.Success)?.let { _uiState.value = it.copy(error = null) }
    }

    private suspend fun reassignRole(eventId: String, slot: EventStaffSlotUiModel, userId: String): Result<Unit> {
        slot.linkedRoleId?.let { existingRoleId ->
            eventRepository.removeEventRole(eventId, existingRoleId).getOrElse { return Result.failure(it) }
        }
        return eventRepository.assignEventRole(
            eventId,
            AssignEventRoleRequestDto(userId = userId, role = slot.role, targetGroup = slot.targetGroup)
        )
    }

    private suspend fun assignPastovykleLeader(eventId: String, slot: EventStaffSlotUiModel, userId: String): Result<Unit> {
        val pastovykleId = slot.pastovykleId ?: return Result.failure(Exception("Pastovykle n?rasta."))
        slot.linkedRoleId?.let { existingRoleId ->
            eventRepository.removeEventRole(eventId, existingRoleId).getOrElse { return Result.failure(it) }
        }
        eventRepository.updatePastovykle(
            eventId,
            pastovykleId,
            UpdatePastovykleRequestDto(responsibleUserId = userId)
        ).getOrElse { return Result.failure(it) }
        return eventRepository.assignEventRole(
            eventId,
            AssignEventRoleRequestDto(userId = userId, role = "PASTOVYKLE_LEADER")
        )
    }

    private suspend fun removePastovykleLeader(eventId: String, slot: EventStaffSlotUiModel): Result<Unit> {
        val pastovykleId = slot.pastovykleId ?: return Result.failure(Exception("Pastovykle n?rasta."))
        eventRepository.updatePastovykle(
            eventId,
            pastovykleId,
            UpdatePastovykleRequestDto(responsibleUserId = null)
        ).getOrElse { return Result.failure(it) }
        return slot.linkedRoleId?.let { eventRepository.removeEventRole(eventId, it) } ?: Result.success(Unit)
    }

    private fun buildSuccessState(
        event: EventDto,
        members: List<MemberDto>,
        pastovykles: List<PastovykleDto>,
        currentUserId: String?,
        isWorking: Boolean = false,
        error: String? = null
    ): EventStaffUiState.Success {
        val slots = buildSlots(event, members, pastovykles, currentUserId)
        val slotRoleIds = slots.mapNotNull { it.linkedRoleId }.toSet()
        val additionalRoles = event.eventRoles.filter { it.id !in slotRoleIds }
        return EventStaffUiState.Success(
            event = event,
            members = members,
            pastovykles = pastovykles,
            slots = slots,
            additionalRoles = additionalRoles,
            currentUserId = currentUserId,
            isWorking = isWorking,
            error = error
        )
    }

    private fun buildSlots(
        event: EventDto,
        members: List<MemberDto>,
        pastovykles: List<PastovykleDto>,
        currentUserId: String?
    ): List<EventStaffSlotUiModel> {
        val slots = mutableListOf<EventStaffSlotUiModel>()

        slots += createRoleSlot(
            id = "chief",
            title = "Virsininkas",
            subtitle = "Pagrindinis renginio vadovas",
            role = "VIRSININKAS",
            event = event,
            members = members,
            isLocked = true,
            currentUserId = currentUserId
        )
        slots += createRoleSlot(
            id = "quartermaster",
            title = "Ukvedys",
            subtitle = "Atsako uz inventoriaus suvestine",
            role = "UKVEDYS",
            event = event,
            members = members,
            currentUserId = currentUserId
        )
        slots += createRoleSlot(
            id = "commandant",
            title = "Komendantas",
            subtitle = "Renginio tvarka ir veikimo logistika",
            role = "KOMENDANTAS",
            event = event,
            members = members,
            currentUserId = currentUserId
        )
        slots += createRoleSlot(
            id = "food",
            title = "Maistininkas",
            subtitle = "Maitinimo atsakomybe",
            role = "MAISTININKAS",
            event = event,
            members = members,
            currentUserId = currentUserId
        )
        slots += createRoleSlot(
            id = "program-vilkai-skautai",
            title = "Programeris",
            subtitle = "Vilku / skautu grupei",
            role = "PROGRAMERIS",
            targetGroup = "SKAUTAI_VILKAI",
            event = event,
            members = members,
            currentUserId = currentUserId
        )
        slots += createRoleSlot(
            id = "program-patyre",
            title = "Programeris",
            subtitle = "Patyrusiu skautu grupei",
            role = "PROGRAMERIS",
            targetGroup = "PATYRE_SKAUTAI",
            event = event,
            members = members,
            currentUserId = currentUserId
        )

        pastovykles.sortedBy { it.name.lowercase() }.forEach { pastovykle ->
            val linkedRole = event.eventRoles.firstOrNull { role ->
                role.role == "PASTOVYKLE_LEADER" && role.userId == pastovykle.responsibleUserId
            }
            val assignedUser = members.firstOrNull { it.userId == pastovykle.responsibleUserId }
            slots += EventStaffSlotUiModel(
                id = "pastovykle_${pastovykle.id}",
                title = pastovykle.name,
                subtitle = "Pastovyklės pagrindinis vadovas",
                role = "PASTOVYKLE_LEADER",
                pastovykleId = pastovykle.id,
                pastovykleAgeGroup = pastovykle.ageGroup,
                assignedUserId = pastovykle.responsibleUserId ?: linkedRole?.userId,
                assignedUserName = assignedUser?.fullName() ?: linkedRole?.userName,
                linkedRoleId = linkedRole?.id,
                opensPastovykleScreen = pastovykle.responsibleUserId == currentUserId
            )
        }

        return slots
    }

    private fun createRoleSlot(
        id: String,
        title: String,
        subtitle: String,
        role: String,
        event: EventDto,
        members: List<MemberDto>,
        targetGroup: String? = null,
        isLocked: Boolean = false,
        currentUserId: String?
    ): EventStaffSlotUiModel {
        val assignedRole = event.eventRoles.firstOrNull { item ->
            item.role == role && item.targetGroup == targetGroup
        }
        val assignedMember = members.firstOrNull { it.userId == assignedRole?.userId }
        return EventStaffSlotUiModel(
            id = id,
            title = title,
            subtitle = subtitle,
            role = role,
            targetGroup = targetGroup,
            assignedUserId = assignedRole?.userId,
            assignedUserName = assignedMember?.fullName() ?: assignedRole?.userName,
            linkedRoleId = assignedRole?.id,
            isLocked = isLocked,
            opensPastovykleScreen = role == "PASTOVYKLE_LEADER" && assignedRole?.userId == currentUserId
        )
    }
}

internal fun activeStaffRoleForMember(
    memberId: String,
    event: EventDto,
    excludingSlot: EventStaffSlotUiModel? = null
): EventRoleDto? {
    val excludedRoleId = excludingSlot?.linkedRoleId
    val excludedPastovykleLeaderUserId = excludingSlot
        ?.takeIf { it.role == "PASTOVYKLE_LEADER" }
        ?.assignedUserId

    return event.eventRoles.firstOrNull { role ->
        role.userId == memberId &&
            role.id != excludedRoleId &&
            !(role.role == "PASTOVYKLE_LEADER" && role.userId == excludedPastovykleLeaderUserId)
    }
}

internal fun memberHasAnotherStaffRole(
    memberId: String,
    event: EventDto,
    excludingSlot: EventStaffSlotUiModel? = null
): Boolean = activeStaffRoleForMember(memberId, event, excludingSlot) != null

internal fun memberEligibleForPastovykleAgeGroup(
    member: MemberDto,
    pastovykleAgeGroup: String?
): Boolean {
    return when (normalizePastovykleAgeGroupCode(pastovykleAgeGroup)) {
        "VYR_SKAUTAI" -> member.matchesAnyRoleName("vyr skautas", "vyr skautas kandidatas")
        "VYR_SKAUTES" -> member.matchesAnyRoleName("vyr skaute", "vyr skautes")
        else -> true
    }
}

private fun MemberDto.matchesAnyRoleName(vararg expectedNames: String): Boolean {
    val roleNames = ranks.map { it.roleName } + leadershipRoles.map { it.roleName }
    return roleNames.any { roleName ->
        val normalizedRoleName = roleName.normalizeRoleName()
        expectedNames.any { expected -> normalizedRoleName.contains(expected.normalizeRoleName()) }
    }
}

private fun String.normalizeRoleName(): String = Normalizer.normalize(this, Normalizer.Form.NFD)
    .replace("\\p{M}+".toRegex(), "")
    .lowercase()
    .replace("[^a-z0-9 ]".toRegex(), " ")
    .replace("\\s+".toRegex(), " ")
    .trim()

private fun staffRoleLabel(role: String): String = when (role) {
    "VIRSININKAS" -> "Virsininkas"
    "KOMENDANTAS" -> "Komendantas"
    "UKVEDYS" -> "Ukvedys"
    "PROGRAMERIS" -> "Programeris"
    "MAISTININKAS" -> "Maistininkas"
    "VADOVAS" -> "Vadovas"
    "SAVANORIS" -> "Savanoris"
    "PASTOVYKLE_LEADER" -> "Pastovykles vadovas"
    else -> role
}
