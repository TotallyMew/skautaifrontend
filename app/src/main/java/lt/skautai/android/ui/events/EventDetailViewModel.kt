package lt.skautai.android.ui.events

import android.net.Uri
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
import lt.skautai.android.data.remote.CreateEventInventoryAllocationRequestDto
import lt.skautai.android.data.remote.CreateEventInventoryBucketRequestDto
import lt.skautai.android.data.remote.CreateEventInventoryItemRequestDto
import lt.skautai.android.data.remote.CreateEventInventoryItemsBulkRequestDto
import lt.skautai.android.data.remote.CreateEventInventoryMovementRequestDto
import lt.skautai.android.data.remote.CreatePastovykleInventoryRequestRequestDto
import lt.skautai.android.data.remote.CreateEventPurchaseItemRequestDto
import lt.skautai.android.data.remote.CreateEventPurchaseRequestDto
import lt.skautai.android.data.remote.CreatePastovykleRequestDto
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.EventInventoryAllocationDto
import lt.skautai.android.data.remote.EventInventoryCustodyDto
import lt.skautai.android.data.remote.EventInventoryPlanDto
import lt.skautai.android.data.remote.EventInventoryMovementDto
import lt.skautai.android.data.remote.EventInventoryRequestDto
import lt.skautai.android.data.remote.EventPurchaseDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.PastovykleDto
import lt.skautai.android.data.remote.PastovykleInventoryDto
import lt.skautai.android.data.remote.UpdateEventInventoryAllocationRequestDto
import lt.skautai.android.data.remote.UpdateEventInventoryBucketRequestDto
import lt.skautai.android.data.remote.UpdateEventInventoryItemRequestDto
import lt.skautai.android.data.remote.UpdateEventRequestDto
import lt.skautai.android.data.repository.EventRepository
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.data.repository.MemberRepository
import lt.skautai.android.data.repository.UploadRepository
import lt.skautai.android.util.TokenManager
import java.util.UUID

sealed interface EventDetailUiState {
    data object Loading : EventDetailUiState
    data class Success(
        val event: EventDto,
        val inventoryPlan: EventInventoryPlanDto? = null,
        val purchases: List<EventPurchaseDto> = emptyList(),
        val pastovykles: List<PastovykleDto> = emptyList(),
        val currentUserId: String? = null,
        val activeOrgUnitId: String? = null,
        val pastovykleInventoryById: Map<String, List<PastovykleInventoryDto>> = emptyMap(),
        val pastovykleRequestsById: Map<String, List<EventInventoryRequestDto>> = emptyMap(),
        val custody: List<EventInventoryCustodyDto> = emptyList(),
        val movements: List<EventInventoryMovementDto> = emptyList(),
        val items: List<ItemDto> = emptyList(),
        val members: List<MemberDto> = emptyList(),
        val isCancelling: Boolean = false,
        val isWorking: Boolean = false,
        val error: String? = null
    ) : EventDetailUiState
    data class Error(val message: String) : EventDetailUiState
}

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val itemRepository: ItemRepository,
    private val memberRepository: MemberRepository,
    private val uploadRepository: UploadRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventDetailUiState>(EventDetailUiState.Loading)
    val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null

    val permissions: StateFlow<Set<String>> = tokenManager.permissions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun loadEvent(id: String) {
        observeEvent(id)
        viewModelScope.launch {
            if (_uiState.value !is EventDetailUiState.Success) {
                _uiState.value = EventDetailUiState.Loading
            }
            eventRepository.getEvent(id)
                .onFailure { error ->
                    _uiState.value = EventDetailUiState.Error(
                        error.message ?: "Klaida gaunant renginio informacija"
                    )
                }
            val current = (_uiState.value as? EventDetailUiState.Success)?.event
            if (current == null) return@launch
            val inventoryPlan = eventRepository.getInventoryPlan(id).getOrNull()
            val purchases = eventRepository.getPurchases(id).getOrNull()?.purchases.orEmpty()
            val pastovykles = eventRepository.getPastovykles(id).getOrNull()?.pastovykles.orEmpty()
            val currentUserId = tokenManager.userId.collectAsStateValue()
            val activeOrgUnitId = tokenManager.activeOrgUnitId.collectAsStateValue()
            val inventoryByPastovykle = pastovykles.associate { pastovykle ->
                pastovykle.id to eventRepository.getPastovykleInventory(id, pastovykle.id)
                    .getOrNull()
                    ?.inventory
                    .orEmpty()
            }
            val requestsByPastovykle = pastovykles.associate { pastovykle ->
                pastovykle.id to eventRepository.getPastovykleRequests(id, pastovykle.id)
                    .getOrNull()
                    ?.requests
                    .orEmpty()
            }
            val custody = eventRepository.getInventoryCustody(id).getOrNull()?.custody.orEmpty()
            val movements = eventRepository.getInventoryMovements(id).getOrNull()?.movements.orEmpty()
            val items = itemRepository.getItems(status = "ACTIVE").getOrNull().orEmpty()
            val members = memberRepository.getMembers().getOrNull()?.members.orEmpty()
            _uiState.value = EventDetailUiState.Success(
                event = current,
                inventoryPlan = inventoryPlan,
                purchases = purchases,
                pastovykles = pastovykles,
                currentUserId = currentUserId,
                activeOrgUnitId = activeOrgUnitId,
                pastovykleInventoryById = inventoryByPastovykle,
                pastovykleRequestsById = requestsByPastovykle,
                custody = custody,
                movements = movements,
                items = items,
                members = members
            )
        }
    }

    fun cancelEvent(id: String, onSuccess: (() -> Unit)? = null) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isCancelling = true, error = null)
            eventRepository.cancelEvent(id)
                .onSuccess {
                    onSuccess?.invoke()
                }
                .onFailure { error ->
                    _uiState.value = current.copy(
                        isCancelling = false,
                        error = error.message ?: "Klaida atsaukiant rengini"
                    )
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
                    _uiState.value = current.copy(error = error.message ?: "Klaida keiciant statusa")
                }
        }
    }

    fun clearError() {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        _uiState.value = current.copy(error = null)
    }

    fun createPurchaseFromSelected(eventId: String, selectedInventoryItemIds: Set<String>) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        val selected = current.inventoryPlan?.items.orEmpty()
            .filter { it.id in selectedInventoryItemIds && it.shortageQuantity > 0 }
        if (selected.isEmpty()) {
            _uiState.value = current.copy(error = "Pazymek bent viena trukstama daikta")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            val request = CreateEventPurchaseRequestDto(
                notes = "Sukurta is pazymetu renginio trukumu",
                items = selected.map {
                    CreateEventPurchaseItemRequestDto(
                        eventInventoryItemId = it.id,
                        purchasedQuantity = it.shortageQuantity
                    )
                }
            )
            eventRepository.createPurchase(eventId, request)
                .onSuccess { loadEvent(eventId) }
                .onFailure { error ->
                    _uiState.value = current.copy(
                        isWorking = false,
                        error = error.message ?: "Klaida kuriant pirkima"
                    )
                }
        }
    }

    fun createNeed(
        eventId: String,
        itemId: String?,
        name: String,
        quantityText: String,
        bucketId: String?,
        responsibleUserId: String?,
        notes: String
    ) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        val quantity = quantityText.toIntOrNull()
        val selectedItem = current.items.firstOrNull { it.id == itemId }
        val finalName = name.ifBlank { selectedItem?.name.orEmpty() }
        if (finalName.isBlank() || quantity == null || quantity <= 0) {
            _uiState.value = current.copy(error = "Ivesk pavadinima ir teigiama kieki")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.createInventoryItem(
                eventId,
                CreateEventInventoryItemRequestDto(
                    itemId = itemId,
                    name = finalName.trim(),
                    plannedQuantity = quantity,
                    bucketId = bucketId,
                    responsibleUserId = responsibleUserId,
                    notes = notes.ifBlank { null }
                )
            )
                .onSuccess { loadEvent(eventId) }
                .onFailure { error ->
                    _uiState.value = current.copy(
                        isWorking = false,
                        error = error.message ?: "Klaida kuriant poreiki"
                    )
                }
        }
    }

    fun createNeedsBulk(
        eventId: String,
        selectedQuantities: Map<String, Int>,
        bucketId: String?,
        responsibleUserId: String?,
        notes: String
    ) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        val selectedItems = selectedQuantities
            .filterValues { it > 0 }
            .mapNotNull { (itemId, quantity) ->
                current.items.firstOrNull { it.id == itemId }?.let { item -> item to quantity }
            }
        if (selectedItems.isEmpty()) {
            _uiState.value = current.copy(error = "Pazymek bent viena inventoriaus daikta")
            return
        }

        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            val request = CreateEventInventoryItemsBulkRequestDto(
                items = selectedItems.map { (item, quantity) ->
                    CreateEventInventoryItemRequestDto(
                        itemId = item.id,
                        name = item.name,
                        plannedQuantity = quantity,
                        bucketId = bucketId,
                        responsibleUserId = responsibleUserId,
                        notes = notes.ifBlank { null }
                    )
                }
            )
            eventRepository.createInventoryItemsBulk(eventId, request)
                .onSuccess { loadEvent(eventId) }
                .onFailure { error ->
                    _uiState.value = current.copy(
                        isWorking = false,
                        error = error.message ?: "Klaida kuriant poreikius"
                    )
                }
        }
    }

    fun updateNeed(
        eventId: String,
        inventoryItemId: String,
        name: String,
        quantityText: String,
        bucketId: String?,
        responsibleUserId: String?,
        notes: String
    ) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        val quantity = quantityText.toIntOrNull()
        if (name.isBlank() || quantity == null || quantity <= 0) {
            _uiState.value = current.copy(error = "Ivesk pavadinima ir teigiama kieki")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.updateInventoryItem(
                eventId,
                inventoryItemId,
                UpdateEventInventoryItemRequestDto(
                    name = name.trim(),
                    plannedQuantity = quantity,
                    bucketId = bucketId,
                    responsibleUserId = responsibleUserId,
                    notes = notes.ifBlank { null }
                )
            )
                .onSuccess { loadEvent(eventId) }
                .onFailure { error ->
                    _uiState.value = current.copy(
                        isWorking = false,
                        error = error.message ?: "Klaida redaguojant plana"
                    )
                }
        }
    }

    fun assignRole(eventId: String, userId: String, role: String) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        if (userId.isBlank()) {
            _uiState.value = current.copy(error = "Pasirink zmogu")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.assignEventRole(eventId, AssignEventRoleRequestDto(userId, role))
                .onSuccess { loadEvent(eventId) }
                .onFailure { error ->
                    _uiState.value = current.copy(isWorking = false, error = error.message ?: "Klaida pridedant stabo nari")
                }
        }
    }

    fun removeRole(eventId: String, roleId: String) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.removeEventRole(eventId, roleId)
                .onSuccess { loadEvent(eventId) }
                .onFailure { error ->
                    _uiState.value = current.copy(isWorking = false, error = error.message ?: "Klaida salinant stabo nari")
                }
        }
    }

    fun attachInvoice(eventId: String, purchaseId: String, uri: Uri) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            uploadRepository.uploadDocument(uri)
                .onSuccess { url ->
                    eventRepository.attachPurchaseInvoice(eventId, purchaseId, url)
                        .onSuccess { loadEvent(eventId) }
                        .onFailure { error ->
                            _uiState.value = current.copy(
                                isWorking = false,
                                error = error.message ?: "Klaida prisegant saskaita"
                            )
                        }
                }
                .onFailure { error ->
                    _uiState.value = current.copy(
                        isWorking = false,
                        error = error.message ?: "Klaida ikeliant saskaita"
                    )
                }
        }
    }

    fun downloadInvoice(eventId: String, purchaseId: String) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        val invoiceFileUrl = current.purchases.firstOrNull { it.id == purchaseId }?.invoiceFileUrl
        viewModelScope.launch {
            uploadRepository.downloadEventPurchaseInvoice(eventId, purchaseId, invoiceFileUrl)
                .onFailure { error ->
                    _uiState.value = current.copy(error = error.message ?: "Klaida parsisiunciant saskaita")
                }
        }
    }

    fun completePurchase(eventId: String, purchaseId: String) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.completePurchase(eventId, purchaseId)
                .onSuccess { loadEvent(eventId) }
                .onFailure { error ->
                    _uiState.value = current.copy(
                        isWorking = false,
                        error = error.message ?: "Klaida uzbaigiant pirkima"
                    )
                }
        }
    }

    fun addPurchaseToInventory(eventId: String, purchaseId: String) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.addPurchaseToInventory(eventId, purchaseId)
                .onSuccess { loadEvent(eventId) }
                .onFailure { error ->
                    _uiState.value = current.copy(
                        isWorking = false,
                        error = error.message ?: "Klaida pridedant i inventoriu"
                    )
                }
        }
    }

    fun createPastovykle(eventId: String, name: String, responsibleUserId: String?, notes: String) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        if (name.isBlank()) {
            _uiState.value = current.copy(error = "Ivesk pastovykles pavadinima")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.createPastovykle(
                eventId,
                CreatePastovykleRequestDto(
                    name = name.trim(),
                    responsibleUserId = responsibleUserId,
                    notes = notes.ifBlank { null }
                )
            )
                .onSuccess { loadEvent(eventId) }
                .onFailure { error ->
                    _uiState.value = current.copy(isWorking = false, error = error.message ?: "Klaida kuriant pastovykle")
                }
        }
    }

    fun createMovement(
        eventId: String,
        movementType: String,
        eventInventoryItemId: String,
        quantityText: String,
        pastovykleId: String?,
        toUserId: String?,
        fromCustodyId: String?,
        notes: String
    ) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        val quantity = quantityText.toIntOrNull()
        if (eventInventoryItemId.isBlank() || quantity == null || quantity <= 0) {
            _uiState.value = current.copy(error = "Pasirink daikta ir teigiama kieki")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.createInventoryMovement(
                eventId,
                CreateEventInventoryMovementRequestDto(
                    eventInventoryItemId = eventInventoryItemId,
                    movementType = movementType,
                    quantity = quantity,
                    pastovykleId = pastovykleId,
                    toUserId = toUserId,
                    fromCustodyId = fromCustodyId,
                    requestId = UUID.randomUUID().toString(),
                    notes = notes.ifBlank { null }
                )
            )
                .onSuccess { loadEvent(eventId) }
                .onFailure { error ->
                    _uiState.value = current.copy(isWorking = false, error = error.message ?: "Klaida registruojant judejima")
                }
        }
    }

    private fun observeEvent(id: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            eventRepository.observeEvent(id).collect { event ->
                val current = _uiState.value as? EventDetailUiState.Success
                if (event != null) {
                    _uiState.value = EventDetailUiState.Success(
                        event = event,
                        inventoryPlan = current?.inventoryPlan,
                        purchases = current?.purchases.orEmpty(),
                        pastovykles = current?.pastovykles.orEmpty(),
                        currentUserId = current?.currentUserId,
                        activeOrgUnitId = current?.activeOrgUnitId,
                        pastovykleInventoryById = current?.pastovykleInventoryById.orEmpty(),
                        pastovykleRequestsById = current?.pastovykleRequestsById.orEmpty(),
                        custody = current?.custody.orEmpty(),
                        movements = current?.movements.orEmpty(),
                        items = current?.items.orEmpty(),
                        members = current?.members.orEmpty(),
                        isCancelling = current?.isCancelling == true,
                        isWorking = current?.isWorking == true,
                        error = current?.error
                    )
                } else if (current == null) {
                    _uiState.value = EventDetailUiState.Loading
                }
            }
        }
    }

    fun createInventoryBucket(eventId: String, name: String, type: String, pastovykleId: String?, notes: String) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        if (name.isBlank()) {
            _uiState.value = current.copy(error = "Ivesk bucket pavadinima")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.createInventoryBucket(
                eventId,
                CreateEventInventoryBucketRequestDto(name = name.trim(), type = type, pastovykleId = pastovykleId, notes = notes.ifBlank { null })
            ).onSuccess { loadEvent(eventId) }
                .onFailure { error ->
                    _uiState.value = current.copy(isWorking = false, error = error.message ?: "Klaida kuriant bucket")
                }
        }
    }

    fun updateInventoryBucket(eventId: String, bucketId: String, name: String, type: String, pastovykleId: String?, notes: String) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.updateInventoryBucket(
                eventId,
                bucketId,
                UpdateEventInventoryBucketRequestDto(name = name.trim(), type = type, pastovykleId = pastovykleId, notes = notes.ifBlank { null })
            ).onSuccess { loadEvent(eventId) }
                .onFailure { error ->
                    _uiState.value = current.copy(isWorking = false, error = error.message ?: "Klaida atnaujinant bucket")
                }
        }
    }

    fun deleteInventoryBucket(eventId: String, bucketId: String) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.deleteInventoryBucket(eventId, bucketId)
                .onSuccess { loadEvent(eventId) }
                .onFailure { error ->
                    _uiState.value = current.copy(isWorking = false, error = error.message ?: "Klaida trinant bucket")
                }
        }
    }

    fun deleteNeed(eventId: String, inventoryItemId: String) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.deleteInventoryItem(eventId, inventoryItemId)
                .onSuccess { loadEvent(eventId) }
                .onFailure { error ->
                    _uiState.value = current.copy(isWorking = false, error = error.message ?: "Klaida trinant plano eilute")
                }
        }
    }

    fun createAllocation(eventId: String, eventInventoryItemId: String, bucketId: String, quantityText: String, notes: String) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        val quantity = quantityText.toIntOrNull()
        if (eventInventoryItemId.isBlank() || bucketId.isBlank() || quantity == null || quantity <= 0) {
            _uiState.value = current.copy(error = "Pasirink daikta, bucket ir teigiama kieki")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.createInventoryAllocation(
                eventId,
                CreateEventInventoryAllocationRequestDto(eventInventoryItemId = eventInventoryItemId, bucketId = bucketId, quantity = quantity, notes = notes.ifBlank { null })
            ).onSuccess { loadEvent(eventId) }
                .onFailure { error ->
                    _uiState.value = current.copy(isWorking = false, error = error.message ?: "Klaida kuriant paskirstyma")
                }
        }
    }

    fun updateAllocation(eventId: String, allocationId: String, quantityText: String, notes: String) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        val quantity = quantityText.toIntOrNull()
        if (quantity == null || quantity <= 0) {
            _uiState.value = current.copy(error = "Ivesk teigiama kieki")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.updateInventoryAllocation(
                eventId,
                allocationId,
                UpdateEventInventoryAllocationRequestDto(quantity = quantity, notes = notes.ifBlank { null })
            ).onSuccess { loadEvent(eventId) }
                .onFailure { error ->
                    _uiState.value = current.copy(isWorking = false, error = error.message ?: "Klaida atnaujinant paskirstyma")
                }
        }
    }

    fun deleteAllocation(eventId: String, allocationId: String) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.deleteInventoryAllocation(eventId, allocationId)
                .onSuccess { loadEvent(eventId) }
                .onFailure { error ->
                    _uiState.value = current.copy(isWorking = false, error = error.message ?: "Klaida trinant paskirstyma")
                }
        }
    }

    fun createPastovykleRequest(eventId: String, pastovykleId: String, eventInventoryItemId: String, quantityText: String, notes: String) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        val quantity = quantityText.toIntOrNull()
        if (eventInventoryItemId.isBlank() || quantity == null || quantity <= 0) {
            _uiState.value = current.copy(error = "Pasirink daikta ir teigiama kieki")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.createPastovykleRequest(
                eventId,
                pastovykleId,
                CreatePastovykleInventoryRequestRequestDto(eventInventoryItemId, quantity, notes.ifBlank { null })
            ).onSuccess { loadEvent(eventId) }
                .onFailure { error ->
                    _uiState.value = current.copy(isWorking = false, error = error.message ?: "Klaida kuriant pastovykles poreiki")
                }
        }
    }

    fun approvePastovykleRequest(eventId: String, pastovykleId: String, requestId: String) {
        mutatePastovykleRequest(eventId) { eventRepository.approvePastovykleRequest(eventId, pastovykleId, requestId) }
    }

    fun rejectPastovykleRequest(eventId: String, pastovykleId: String, requestId: String) {
        mutatePastovykleRequest(eventId) { eventRepository.rejectPastovykleRequest(eventId, pastovykleId, requestId) }
    }

    fun selfProvidePastovykleRequest(eventId: String, pastovykleId: String, requestId: String, notes: String) {
        mutatePastovykleRequest(eventId) { eventRepository.selfProvidePastovykleRequest(eventId, pastovykleId, requestId, notes.ifBlank { null }) }
    }

    fun fulfillPastovykleRequest(eventId: String, pastovykleId: String, requestId: String) {
        mutatePastovykleRequest(eventId) { eventRepository.fulfillPastovykleRequest(eventId, pastovykleId, requestId) }
    }

    fun assignFromUnitInventory(eventId: String, pastovykleId: String, itemId: String, quantityText: String, notes: String) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        val quantity = quantityText.toIntOrNull()
        if (itemId.isBlank() || quantity == null || quantity <= 0) {
            _uiState.value = current.copy(error = "Pasirink vieneto daikta ir teigiama kieki")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.assignFromUnitInventory(eventId, pastovykleId, itemId, quantity, notes.ifBlank { null })
                .onSuccess { loadEvent(eventId) }
                .onFailure { error ->
                    _uiState.value = current.copy(isWorking = false, error = error.message ?: "Klaida atsivezant inventoriu is vieneto")
                }
        }
    }

    private fun mutatePastovykleRequest(
        eventId: String,
        block: suspend () -> Result<EventInventoryRequestDto>
    ) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            block()
                .onSuccess { loadEvent(eventId) }
                .onFailure { error ->
                    _uiState.value = current.copy(isWorking = false, error = error.message ?: "Klaida atnaujinant pastovykles poreiki")
                }
        }
    }

    private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.collectAsStateValue(): T = first()
}
