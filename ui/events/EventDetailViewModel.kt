package lt.skautai.android.ui.events

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.AssignEventRoleRequestDto
import lt.skautai.android.data.remote.CreateEventInventoryItemRequestDto
import lt.skautai.android.data.remote.CreateEventInventoryItemsBulkRequestDto
import lt.skautai.android.data.remote.CreateEventPurchaseItemRequestDto
import lt.skautai.android.data.remote.CreateEventPurchaseRequestDto
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.EventInventoryPlanDto
import lt.skautai.android.data.remote.EventPurchaseDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.UpdateEventInventoryItemRequestDto
import lt.skautai.android.data.remote.UpdateEventRequestDto
import lt.skautai.android.data.repository.EventRepository
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.data.repository.MemberRepository
import lt.skautai.android.data.repository.UploadRepository
import lt.skautai.android.util.TokenManager

sealed interface EventDetailUiState {
    data object Loading : EventDetailUiState
    data class Success(
        val event: EventDto,
        val inventoryPlan: EventInventoryPlanDto? = null,
        val purchases: List<EventPurchaseDto> = emptyList(),
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

    val permissions: StateFlow<Set<String>> = tokenManager.permissions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun loadEvent(id: String) {
        viewModelScope.launch {
            _uiState.value = EventDetailUiState.Loading
            eventRepository.getEvent(id)
                .onSuccess { event ->
                    val inventoryPlan = eventRepository.getInventoryPlan(id).getOrNull()
                    val purchases = eventRepository.getPurchases(id).getOrNull()?.purchases.orEmpty()
                    val items = itemRepository.getItems(status = "ACTIVE").getOrNull().orEmpty()
                    val members = memberRepository.getMembers().getOrNull()?.members.orEmpty()
                    _uiState.value = EventDetailUiState.Success(event, inventoryPlan, purchases, items, members)
                }
                .onFailure { error ->
                    _uiState.value = EventDetailUiState.Error(
                        error.message ?: "Klaida gaunant renginio informacija"
                    )
                }
        }
    }

    fun cancelEvent(id: String) {
        val current = _uiState.value as? EventDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isCancelling = true, error = null)
            eventRepository.cancelEvent(id)
                .onSuccess { loadEvent(id) }
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
}
