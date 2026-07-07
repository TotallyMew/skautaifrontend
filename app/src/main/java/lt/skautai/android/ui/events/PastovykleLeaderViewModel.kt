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
import lt.skautai.android.data.remote.AddPastovykleMemberRequestDto
import lt.skautai.android.data.remote.AssignPastovykleInventoryRequestDto
import lt.skautai.android.data.remote.CreateEventInventoryItemRequestDto
import lt.skautai.android.data.remote.CreatePastovykleInventoryRequestRequestDto
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.EventInventoryPlanDto
import lt.skautai.android.data.remote.EventInventoryReadinessDto
import lt.skautai.android.data.remote.EventInventoryRequestDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.PastovykleDto
import lt.skautai.android.data.remote.PastovykleInventoryDto
import lt.skautai.android.data.remote.PastovykleMemberDto
import lt.skautai.android.data.remote.UpdatePastovykleInventoryRequestDto
import lt.skautai.android.data.remote.UpdateEventInventoryRequestRequestDto
import lt.skautai.android.data.repository.EventRepository
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.util.TokenManager

sealed interface PastovykleLeaderUiState {
    data object Loading : PastovykleLeaderUiState
    data class Success(
        val event: EventDto,
        val pastovykles: List<PastovykleDto> = emptyList(),
        val currentUserId: String? = null,
        val activeOrgUnitId: String? = null,
        val inventoryPlan: EventInventoryPlanDto? = null,
        val readiness: EventInventoryReadinessDto? = null,
        val pastovykleInventoryById: Map<String, List<PastovykleInventoryDto>> = emptyMap(),
        val pastovykleRequestsById: Map<String, List<EventInventoryRequestDto>> = emptyMap(),
        val pastovykleMembersById: Map<String, List<PastovykleMemberDto>> = emptyMap(),
        val items: List<ItemDto> = emptyList(),
        val candidateMembers: List<MemberDto> = emptyList(),
        val isWorking: Boolean = false,
        val error: String? = null
    ) : PastovykleLeaderUiState
    data class Error(val message: String) : PastovykleLeaderUiState
}

@HiltViewModel
class PastovykleLeaderViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val itemRepository: ItemRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<PastovykleLeaderUiState>(PastovykleLeaderUiState.Loading)
    val uiState: StateFlow<PastovykleLeaderUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null

    val permissions: StateFlow<Set<String>> = tokenManager.permissions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun load(eventId: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            eventRepository.observeEvent(eventId).collect { event ->
                if (event != null) {
                    val current = _uiState.value as? PastovykleLeaderUiState.Success
                    _uiState.value = PastovykleLeaderUiState.Success(
                        event = event,
                        pastovykles = current?.pastovykles.orEmpty(),
                        currentUserId = current?.currentUserId,
                        activeOrgUnitId = current?.activeOrgUnitId,
                        inventoryPlan = current?.inventoryPlan,
                        readiness = current?.readiness,
                        pastovykleInventoryById = current?.pastovykleInventoryById.orEmpty(),
                        pastovykleRequestsById = current?.pastovykleRequestsById.orEmpty(),
                        pastovykleMembersById = current?.pastovykleMembersById.orEmpty(),
                        items = current?.items.orEmpty(),
                        candidateMembers = current?.candidateMembers.orEmpty(),
                        isWorking = false,
                        error = current?.error
                    )
                } else if (_uiState.value !is PastovykleLeaderUiState.Success) {
                    _uiState.value = PastovykleLeaderUiState.Loading
                }
            }
        }
        viewModelScope.launch {
            if (_uiState.value !is PastovykleLeaderUiState.Success) {
                _uiState.value = PastovykleLeaderUiState.Loading
            }
            eventRepository.getEvent(eventId)
                .onFailure { error ->
                    if (_uiState.value !is PastovykleLeaderUiState.Success) {
                        _uiState.value = PastovykleLeaderUiState.Error(error.message ?: "Nepavyko gauti renginio informacijos.")
                    }
                    return@launch
                }
            val currentUserId = tokenManager.userId.first()
            val activeOrgUnitId = tokenManager.activeOrgUnitId.first()
            applyCachedEventData(eventId, currentUserId, activeOrgUnitId)
            val pastovykles = eventRepository.getPastovyklės(eventId).getOrNull()?.pastovykles.orEmpty()
            val inventoryPlan = eventRepository.getInventoryPlan(eventId).getOrNull()
            val readiness = eventRepository.getInventoryReadiness(eventId).getOrNull()
            val candidateMembers = eventRepository.getCandidateMembers(eventId).getOrNull()?.members.orEmpty()
            val inventoryByPastovykle = pastovykles.associate { p ->
                p.id to eventRepository.getPastovykleInventory(eventId, p.id).getOrNull()?.inventory.orEmpty()
            }
            val requestsByPastovykle = pastovykles.associate { p ->
                p.id to eventRepository.getPastovykleRequests(eventId, p.id).getOrNull()?.requests.orEmpty()
            }
            val membersByPastovykle = pastovykles.associate { p ->
                p.id to eventRepository.getPastovykleMembers(eventId, p.id).getOrNull()?.members.orEmpty()
            }
            val items = itemRepository.getItems(status = "ACTIVE").getOrNull().orEmpty()
            val current = _uiState.value as? PastovykleLeaderUiState.Success ?: return@launch
            _uiState.value = current.copy(
                currentUserId = currentUserId,
                activeOrgUnitId = activeOrgUnitId,
                pastovykles = pastovykles,
                inventoryPlan = inventoryPlan,
                readiness = readiness,
                pastovykleInventoryById = inventoryByPastovykle,
                pastovykleRequestsById = requestsByPastovykle,
                pastovykleMembersById = membersByPastovykle,
                items = items,
                candidateMembers = candidateMembers
            )
        }
    }

    fun createPastovykleRequest(
        eventId: String,
        pastovykleId: String,
        itemId: String?,
        customName: String?,
        quantityText: String,
        notes: String,
        provider: String,
        dueAt: String?,
        responsibleUserId: String?
    ) {
        val current = _uiState.value as? PastovykleLeaderUiState.Success ?: return
        if (current.isWorking) return
        val quantity = quantityText.toIntOrNull()
        val item = itemId?.let { id -> current.items.firstOrNull { it.id == id } }
        val normalizedName = customName?.trim().orEmpty()
        if (quantity == null || quantity <= 0) {
            _uiState.value = current.copy(error = "Įveskite teigiamą kiekį.")
            return
        }
        if (item == null && normalizedName.isBlank()) {
            _uiState.value = current.copy(error = "Pasirinkite daiktą arba įveskite laisvą poreikį.")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            val eventInventoryItem = eventRepository.createInventoryItem(
                eventId,
                CreateEventInventoryItemRequestDto(
                    itemId = item?.id,
                    name = item?.name ?: normalizedName,
                    plannedQuantity = quantity,
                    notes = notes.ifBlank { null }
                )
            ).getOrElse { error ->
                (_uiState.value as? PastovykleLeaderUiState.Success)?.let {
                    _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko pridėti daikto į renginio poreikius.")
                }
                return@launch
            }
            eventRepository.createPastovykleRequest(
                eventId, pastovykleId,
                CreatePastovykleInventoryRequestRequestDto(
                    eventInventoryItemId = eventInventoryItem.id,
                    quantity = quantity,
                    notes = notes.ifBlank { null },
                    provider = provider,
                    dueAt = dueAt,
                    responsibleUserId = responsibleUserId
                )
            )
                .onSuccess { refreshAfterSuccessfulMutation(eventId, pastovykleId, refreshPlan = true) }
                .onFailure { error ->
                    (_uiState.value as? PastovykleLeaderUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko sukurti pastovyklės poreikio.")
                    }
                }
        }
    }

    fun switchRequestProvider(
        eventId: String,
        pastovykleId: String,
        requestId: String,
        provider: String
    ) {
        val current = _uiState.value as? PastovykleLeaderUiState.Success ?: return
        if (current.isWorking) return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.updatePastovykleRequest(
                eventId,
                pastovykleId,
                requestId,
                UpdateEventInventoryRequestRequestDto(provider = provider)
            )
                .onSuccess { refreshAfterSuccessfulMutation(eventId, pastovykleId) }
                .onFailure { error ->
                    (_uiState.value as? PastovykleLeaderUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko pakeisti poreikio tiekejo.")
                    }
                }
        }
    }

    fun updatePastovykleRequest(
        eventId: String,
        pastovykleId: String,
        requestId: String,
        provider: String,
        dueDate: String,
        responsibleUserId: String?,
        notes: String
    ) {
        val current = _uiState.value as? PastovykleLeaderUiState.Success ?: return
        if (current.isWorking) return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.updatePastovykleRequest(
                eventId,
                pastovykleId,
                requestId,
                UpdateEventInventoryRequestRequestDto(
                    provider = provider,
                    dueAt = dueDate.takeIf { it.isNotBlank() }?.let { "${it}T23:59:59Z" },
                    clearDueAt = dueDate.isBlank(),
                    responsibleUserId = responsibleUserId,
                    clearResponsibleUserId = responsibleUserId == null,
                    notes = notes
                )
            )
                .onSuccess { refreshAfterSuccessfulMutation(eventId, pastovykleId) }
                .onFailure { error ->
                    (_uiState.value as? PastovykleLeaderUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko atnaujinti poreikio.")
                    }
                }
        }
    }

    fun assignFromUnitInventory(eventId: String, pastovykleId: String, itemId: String, quantityText: String, notes: String) {
        val current = _uiState.value as? PastovykleLeaderUiState.Success ?: return
        if (current.isWorking) return
        val quantity = quantityText.toIntOrNull()
        if (itemId.isBlank() || quantity == null || quantity <= 0) {
            _uiState.value = current.copy(error = "Pasirinkite vieneto daiktą ir teigiamą kiekį.")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.assignFromUnitInventory(eventId, pastovykleId, itemId, quantity, notes.ifBlank { null })
                .onSuccess { refreshAfterSuccessfulMutation(eventId, pastovykleId) }
                .onFailure { error ->
                    (_uiState.value as? PastovykleLeaderUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko priskirti inventoriaus iš vieneto.")
                    }
                }
        }
    }

    fun selfProvidePastovykleRequest(eventId: String, pastovykleId: String, requestId: String, notes: String) {
        val current = _uiState.value as? PastovykleLeaderUiState.Success ?: return
        if (current.isWorking) return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.selfProvidePastovykleRequest(eventId, pastovykleId, requestId, notes.ifBlank { null })
                .onSuccess { refreshAfterSuccessfulMutation(eventId, pastovykleId) }
                .onFailure { error ->
                    (_uiState.value as? PastovykleLeaderUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko atnaujinti pastovyklės poreikio.")
                    }
                }
        }
    }

    fun addPastovykleMember(eventId: String, pastovykleId: String, userId: String) {
        val current = _uiState.value as? PastovykleLeaderUiState.Success ?: return
        if (current.isWorking) return
        if (userId.isBlank()) {
            _uiState.value = current.copy(error = "Pasirinkite narį.")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.addPastovykleMember(eventId, pastovykleId, AddPastovykleMemberRequestDto(userId))
                .onSuccess { refreshAfterSuccessfulMutation(eventId, pastovykleId, refreshMembers = true) }
                .onFailure { error ->
                    (_uiState.value as? PastovykleLeaderUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko pridėti nario.")
                    }
                }
        }
    }

    fun removePastovykleMember(eventId: String, pastovykleId: String, memberId: String) {
        val current = _uiState.value as? PastovykleLeaderUiState.Success ?: return
        if (current.isWorking) return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.removePastovykleMember(eventId, pastovykleId, memberId)
                .onSuccess { refreshAfterSuccessfulMutation(eventId, pastovykleId, refreshMembers = true) }
                .onFailure { error ->
                    (_uiState.value as? PastovykleLeaderUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko pašalinti nario.")
                    }
                }
        }
    }

    fun issueToMember(
        eventId: String,
        pastovykleId: String,
        itemId: String,
        recipientUserId: String,
        quantityText: String,
        notes: String
    ) {
        val current = _uiState.value as? PastovykleLeaderUiState.Success ?: return
        if (current.isWorking) return
        val quantity = quantityText.toIntOrNull()
        if (itemId.isBlank() || recipientUserId.isBlank() || quantity == null || quantity <= 0) {
            _uiState.value = current.copy(error = "Pasirinkite daiktą, gavėją ir teigiamą kiekį.")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.assignPastovykleInventory(
                eventId,
                pastovykleId,
                AssignPastovykleInventoryRequestDto(
                    itemId = itemId,
                    quantity = quantity,
                    recipientUserId = recipientUserId,
                    recipientType = "MEMBER",
                    notes = notes.ifBlank { null }
                )
            )
                .onSuccess { refreshAfterSuccessfulMutation(eventId, pastovykleId) }
                .onFailure { error ->
                    (_uiState.value as? PastovykleLeaderUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko išduoti inventoriaus.")
                    }
                }
        }
    }

    fun markInventoryReturned(eventId: String, pastovykleId: String, inventory: PastovykleInventoryDto) {
        val current = _uiState.value as? PastovykleLeaderUiState.Success ?: return
        if (current.isWorking) return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.updatePastovykleInventory(
                eventId,
                pastovykleId,
                inventory.id,
                UpdatePastovykleInventoryRequestDto(quantityReturned = inventory.quantityAssigned)
            )
                .onSuccess { refreshAfterSuccessfulMutation(eventId, pastovykleId) }
                .onFailure { error ->
                    (_uiState.value as? PastovykleLeaderUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko pažymėti grąžinimo.")
                    }
                }
        }
    }

    fun clearError() {
        (_uiState.value as? PastovykleLeaderUiState.Success)?.let { _uiState.value = it.copy(error = null) }
    }

    private fun refreshAfterSuccessfulMutation(
        eventId: String,
        pastovykleId: String,
        refreshPlan: Boolean = false,
        refreshMembers: Boolean = false
    ) {
        viewModelScope.launch {
            val cachedInventory = eventRepository.getCachedPastovykleInventory(eventId, pastovykleId)?.inventory.orEmpty()
            val cachedRequests = eventRepository.getCachedPastovykleRequests(eventId, pastovykleId)?.requests.orEmpty()
            val cachedPlan = if (refreshPlan) eventRepository.getCachedInventoryPlan(eventId) else null
            (_uiState.value as? PastovykleLeaderUiState.Success)?.let {
                _uiState.value = it.copy(
                    inventoryPlan = cachedPlan ?: it.inventoryPlan,
                    pastovykleInventoryById = if (cachedInventory.isNotEmpty()) {
                        it.pastovykleInventoryById + (pastovykleId to cachedInventory)
                    } else {
                        it.pastovykleInventoryById
                    },
                    pastovykleRequestsById = if (cachedRequests.isNotEmpty()) {
                        it.pastovykleRequestsById + (pastovykleId to cachedRequests)
                    } else {
                        it.pastovykleRequestsById
                    }
                )
            }

            val inventory = eventRepository.getPastovykleInventory(eventId, pastovykleId).getOrNull()?.inventory.orEmpty()
            val requests = eventRepository.getPastovykleRequests(eventId, pastovykleId).getOrNull()?.requests.orEmpty()
            val members = if (refreshMembers) {
                eventRepository.getPastovykleMembers(eventId, pastovykleId).getOrNull()?.members
            } else {
                null
            }
            val plan = if (refreshPlan) eventRepository.getInventoryPlan(eventId).getOrNull() else null
            (_uiState.value as? PastovykleLeaderUiState.Success)?.let {
                _uiState.value = it.copy(
                    isWorking = false,
                    error = null,
                    inventoryPlan = plan ?: it.inventoryPlan,
                    pastovykleInventoryById = it.pastovykleInventoryById + (pastovykleId to inventory),
                    pastovykleRequestsById = it.pastovykleRequestsById + (pastovykleId to requests),
                    pastovykleMembersById = if (members != null) {
                        it.pastovykleMembersById + (pastovykleId to members)
                    } else {
                        it.pastovykleMembersById
                    }
                )
            }
        }
    }

    private suspend fun applyCachedEventData(eventId: String, currentUserId: String?, activeOrgUnitId: String?) {
        val current = _uiState.value as? PastovykleLeaderUiState.Success ?: return
        val cachedPastovykles = eventRepository.getCachedPastovykles(eventId)?.pastovykles.orEmpty()
        val cachedPlan = eventRepository.getCachedInventoryPlan(eventId)
        val cachedItems = itemRepository.getCachedItems(status = "ACTIVE")
        val cachedInventoryById = cachedPastovykles.associate { pastovykle ->
            pastovykle.id to eventRepository.getCachedPastovykleInventory(eventId, pastovykle.id)?.inventory.orEmpty()
        }.filterValues { it.isNotEmpty() }
        val cachedRequestsById = cachedPastovykles.associate { pastovykle ->
            pastovykle.id to eventRepository.getCachedPastovykleRequests(eventId, pastovykle.id)?.requests.orEmpty()
        }.filterValues { it.isNotEmpty() }
        if (cachedPastovykles.isNotEmpty() || cachedPlan != null || cachedItems.isNotEmpty()) {
            _uiState.value = current.copy(
                currentUserId = currentUserId,
                activeOrgUnitId = activeOrgUnitId,
                pastovykles = if (cachedPastovykles.isNotEmpty()) cachedPastovykles else current.pastovykles,
                inventoryPlan = cachedPlan ?: current.inventoryPlan,
                pastovykleInventoryById = current.pastovykleInventoryById + cachedInventoryById,
                pastovykleRequestsById = current.pastovykleRequestsById + cachedRequestsById,
                items = if (cachedItems.isNotEmpty()) cachedItems else current.items
            )
        }
    }
}
