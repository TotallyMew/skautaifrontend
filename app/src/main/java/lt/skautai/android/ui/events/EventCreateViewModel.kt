package lt.skautai.android.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.CreateEventRequestDto
import lt.skautai.android.data.remote.InventoryTemplateDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.remote.UpdateEventRequestDto
import lt.skautai.android.data.repository.EventRepository
import lt.skautai.android.data.repository.MemberRepository
import lt.skautai.android.data.repository.OrganizationalUnitRepository
import lt.skautai.android.util.TokenManager

data class EventAudienceOption(
    val organizationalUnitId: String?,
    val label: String
)

data class EventCreateUiState(
    val eventId: String? = null,
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isAudienceLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val name: String = "",
    val type: String = "STOVYKLA",
    val startDate: String = "",
    val endDate: String = "",
    val notes: String = "",
    val selectedAudienceId: String? = null,
    val selectedAudienceLabel: String = "Bendras renginys",
    val audienceOptions: List<EventAudienceOption> = emptyList(),
    val templates: List<InventoryTemplateDto> = emptyList(),
    val selectedTemplateId: String? = null,
    val isTemplateLoading: Boolean = false,
    val templateApplySummary: String? = null
)

@HiltViewModel
class EventCreateViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val organizationalUnitRepository: OrganizationalUnitRepository,
    private val memberRepository: MemberRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val globalLeadershipRoleNames = setOf("Tuntininkas", "Tuntininko pavaduotojas")
    private val seniorScoutRankNames = setOf("Vyr. skautas", "Vyr. skautas kandidatas")
    private val supportedAudienceUnitTypes = setOf("GILDIJA", "VYR_SKAUTU_VIENETAS", "VYR_SKAUCIU_VIENETAS")

    private val _uiState = MutableStateFlow(EventCreateUiState())
    val uiState: StateFlow<EventCreateUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null

    fun onNameChange(value: String) { _uiState.value = _uiState.value.copy(name = value) }
    fun onTypeChange(value: String) {
        _uiState.value = _uiState.value.copy(type = value, selectedTemplateId = null, templates = emptyList())
        loadTemplates(value)
    }
    fun onStartDateChange(value: String) { _uiState.value = _uiState.value.copy(startDate = value) }
    fun onEndDateChange(value: String) { _uiState.value = _uiState.value.copy(endDate = value) }
    fun onNotesChange(value: String) { _uiState.value = _uiState.value.copy(notes = value) }
    fun onTemplateChange(value: String?) { _uiState.value = _uiState.value.copy(selectedTemplateId = value) }

    fun onAudienceChange(value: String?) {
        _uiState.value = _uiState.value.copy(
            selectedAudienceId = value,
            selectedAudienceLabel = resolveAudienceLabel(value, _uiState.value.audienceOptions)
        )
    }

    fun loadEvent(eventId: String?) {
        observeJob?.cancel()
        val nextState = EventCreateUiState(
            eventId = eventId,
            isEditMode = eventId != null
        )
        _uiState.value = nextState
        loadAudienceOptions()
        if (eventId == null) loadTemplates(nextState.type)

        if (eventId == null) return

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
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
                        notes = event.notes.orEmpty(),
                        selectedAudienceId = event.organizationalUnitId,
                        selectedAudienceLabel = resolveAudienceLabel(
                            event.organizationalUnitId,
                            _uiState.value.audienceOptions
                        )
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
        if (!state.isEditMode && state.audienceOptions.isEmpty()) {
            _uiState.value = state.copy(error = "Jums šiuo metu neleidžiama kurti renginių.")
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
                        type = state.type.trim(),
                        startDate = state.startDate,
                        endDate = state.endDate,
                        organizationalUnitId = state.selectedAudienceId,
                        notes = state.notes.ifBlank { null }
                    )
                )
            }
            result.onSuccess {
                if (!state.isEditMode && state.selectedTemplateId != null) {
                    eventRepository.applyInventoryTemplateWithReservation(it.id, state.selectedTemplateId)
                        .onSuccess { applied ->
                            val summary = "Rezervuota: ${applied.reservedTotal}, į pirkimų sąrašą: ${applied.toPurchaseTotal}"
                            _uiState.value = _uiState.value.copy(
                                isSaving = false,
                                isSuccess = true,
                                templateApplySummary = summary
                            )
                        }
                        .onFailure { error ->
                            _uiState.value = _uiState.value.copy(
                                isSaving = false,
                                error = "Renginys sukurtas, bet šablono pritaikyti nepavyko: ${error.message ?: "nežinoma klaida"}"
                            )
                        }
                } else {
                    _uiState.value = _uiState.value.copy(isSaving = false, isSuccess = true)
                }
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

    private fun loadTemplates(eventType: String = _uiState.value.type) {
        if (_uiState.value.isEditMode) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTemplateLoading = true)
            eventRepository.getInventoryTemplates(eventType)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isTemplateLoading = false,
                        templates = response.templates,
                        selectedTemplateId = _uiState.value.selectedTemplateId
                            ?.takeIf { selected -> response.templates.any { it.id == selected } }
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isTemplateLoading = false,
                        error = error.message ?: "Nepavyko gauti šablonų."
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun loadAudienceOptions() {
        viewModelScope.launch {
            val currentUserId = tokenManager.userId.first()
            if (currentUserId.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(
                    isAudienceLoading = false,
                    audienceOptions = emptyList()
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isAudienceLoading = true)

            val unitsDeferred = async { organizationalUnitRepository.getUnits() }
            val memberDeferred = async { memberRepository.getMember(currentUserId) }

            val units = unitsDeferred.await().getOrNull().orEmpty()
            val member = memberDeferred.await().getOrNull()
            val options = buildAudienceOptions(member, units)

            val selectedAudienceId = when {
                _uiState.value.isEditMode -> _uiState.value.selectedAudienceId
                options.any { it.organizationalUnitId == _uiState.value.selectedAudienceId } -> _uiState.value.selectedAudienceId
                options.any { it.organizationalUnitId == null } -> null
                else -> options.firstOrNull()?.organizationalUnitId
            }

            _uiState.value = _uiState.value.copy(
                isAudienceLoading = false,
                audienceOptions = options,
                selectedAudienceId = selectedAudienceId,
                selectedAudienceLabel = resolveAudienceLabel(selectedAudienceId, options)
            )
        }
    }

    private fun buildAudienceOptions(
        member: MemberDto?,
        units: List<OrganizationalUnitDto>
    ): List<EventAudienceOption> {
        if (member == null) return emptyList()

        val activeLeadershipRoles = member.leadershipRoles.filter {
            it.termStatus == "ACTIVE" && it.leftAt == null
        }
        val leadershipRoleNames = activeLeadershipRoles.map { it.roleName }.toSet()
        val rankNames = member.ranks.map { it.roleName }.toSet()
        val relatedUnitIds = buildSet {
            addAll(member.unitAssignments.orEmpty().map { it.organizationalUnitId })
            addAll(activeLeadershipRoles.mapNotNull { it.organizationalUnitId })
        }

        val isGlobalAdmin = leadershipRoleNames.any { it in globalLeadershipRoleNames }
        val isVadovas = "Vadovas" in rankNames
        val isSeniorScout = rankNames.any { it in seniorScoutRankNames }

        val options = mutableListOf<EventAudienceOption>()
        if (isGlobalAdmin || isVadovas) {
            options += EventAudienceOption(
                organizationalUnitId = null,
                label = "Bendras renginys"
            )
        }

        units
            .filter { it.type in supportedAudienceUnitTypes }
            .sortedBy { it.name.lowercase() }
            .forEach { unit ->
                when (unit.type) {
                    "GILDIJA" -> if (isGlobalAdmin || isVadovas) {
                        options += EventAudienceOption(unit.id, unit.name)
                    }
                    "VYR_SKAUTU_VIENETAS",
                    "VYR_SKAUCIU_VIENETAS" -> if (isGlobalAdmin || (isSeniorScout && unit.id in relatedUnitIds)) {
                        options += EventAudienceOption(unit.id, unit.name)
                    }
                }
            }

        return options.distinctBy { it.organizationalUnitId }
    }

    private fun resolveAudienceLabel(
        organizationalUnitId: String?,
        options: List<EventAudienceOption>
    ): String {
        return options.firstOrNull { it.organizationalUnitId == organizationalUnitId }?.label
            ?: if (organizationalUnitId == null) "Bendras renginys" else "Pasirinktas vienetas"
    }
}
