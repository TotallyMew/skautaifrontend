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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.CreateEventInventoryItemRequestDto
import lt.skautai.android.data.remote.CreateEventInventoryItemsBulkRequestDto
import lt.skautai.android.data.remote.CreateEventPurchaseItemRequestDto
import lt.skautai.android.data.remote.CreateEventPurchaseRequestDto
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.EventInventoryPlanDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.repository.EventRepository
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.data.repository.MemberRepository
import lt.skautai.android.util.TokenManager

sealed interface EventNeedsUiState {
    data object Loading : EventNeedsUiState
    data class Success(
        val event: EventDto,
        val inventoryPlan: EventInventoryPlanDto? = null,
        val items: List<ItemDto> = emptyList(),
        val members: List<MemberDto> = emptyList(),
        val isWorking: Boolean = false,
        val error: String? = null
    ) : EventNeedsUiState
    data class Error(val message: String) : EventNeedsUiState
}

data class ManualEventNeedInput(
    val name: String,
    val quantity: Int,
    val bucketId: String?,
    val notes: String
)

@HiltViewModel
class EventNeedsViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val itemRepository: ItemRepository,
    private val memberRepository: MemberRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventNeedsUiState>(EventNeedsUiState.Loading)
    val uiState: StateFlow<EventNeedsUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null

    val permissions: StateFlow<Set<String>> = tokenManager.permissions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun load(eventId: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            eventRepository.observeEvent(eventId).collect { event ->
                if (event != null) {
                    val current = _uiState.value as? EventNeedsUiState.Success
                    _uiState.value = EventNeedsUiState.Success(
                        event = event,
                        inventoryPlan = current?.inventoryPlan,
                        items = current?.items.orEmpty(),
                        members = current?.members.orEmpty(),
                        isWorking = current?.isWorking == true,
                        error = current?.error
                    )
                } else if (_uiState.value !is EventNeedsUiState.Success) {
                    _uiState.value = EventNeedsUiState.Loading
                }
            }
        }
        viewModelScope.launch {
            if (_uiState.value !is EventNeedsUiState.Success) {
                _uiState.value = EventNeedsUiState.Loading
            }
            eventRepository.getEvent(eventId)
                .onFailure { error ->
                    if (_uiState.value !is EventNeedsUiState.Success) {
                        _uiState.value = EventNeedsUiState.Error(error.message ?: "Nepavyko gauti renginio informacijos.")
                    }
                    return@launch
                }
            val inventoryPlan = eventRepository.getInventoryPlan(eventId).getOrNull()
            val current = _uiState.value as? EventNeedsUiState.Success ?: return@launch
            _uiState.value = current.copy(inventoryPlan = inventoryPlan)
        }
    }

    fun loadMembers() {
        val current = _uiState.value as? EventNeedsUiState.Success ?: return
        if (current.members.isNotEmpty()) return
        viewModelScope.launch {
            val members = memberRepository.getMembers().getOrNull()?.members.orEmpty()
            (_uiState.value as? EventNeedsUiState.Success)?.let {
                _uiState.value = it.copy(members = members)
            }
        }
    }

    fun loadItemCatalog(eventId: String) {
        val current = _uiState.value as? EventNeedsUiState.Success ?: return
        if (current.items.isNotEmpty()) return
        viewModelScope.launch {
            val items = itemRepository.getItems(status = "ACTIVE").getOrNull().orEmpty()
            (_uiState.value as? EventNeedsUiState.Success)?.let {
                _uiState.value = it.copy(items = items)
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
        val current = _uiState.value as? EventNeedsUiState.Success ?: return
        val quantity = quantityText.toIntOrNull()
        val selectedItem = current.items.firstOrNull { it.id == itemId }
        val finalName = name.ifBlank { selectedItem?.name.orEmpty() }
        if (finalName.isBlank() || quantity == null || quantity <= 0) {
            _uiState.value = current.copy(error = "Įveskite pavadinimą ir teigiamą kiekį.")
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
                .onSuccess { load(eventId) }
                .onFailure { error ->
                    (_uiState.value as? EventNeedsUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko sukurti poreikio.")
                    }
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
        val current = _uiState.value as? EventNeedsUiState.Success ?: return
        val selectedItems = selectedQuantities
            .filterValues { it > 0 }
            .mapNotNull { (itemId, quantity) ->
                current.items.firstOrNull { it.id == itemId }?.let { item -> item to quantity }
            }
        if (selectedItems.isEmpty()) {
            _uiState.value = current.copy(error = "Pažymėkite bent vieną inventoriaus daiktą.")
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
                .onSuccess { load(eventId) }
                .onFailure { error ->
                    (_uiState.value as? EventNeedsUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko sukurti poreikių.")
                    }
                }
        }
    }

    fun createManualNeedsBulk(eventId: String, needs: List<ManualEventNeedInput>) {
        val current = _uiState.value as? EventNeedsUiState.Success ?: return
        val validNeeds = needs
            .map { it.copy(name = it.name.trim(), notes = it.notes.trim()) }
            .filter { it.name.isNotBlank() && it.quantity > 0 }
        if (validNeeds.isEmpty()) {
            _uiState.value = current.copy(error = "Įtraukite bent vieną poreikį su pavadinimu ir kiekiu.")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            val request = CreateEventInventoryItemsBulkRequestDto(
                items = validNeeds.map { need ->
                    CreateEventInventoryItemRequestDto(
                        itemId = null,
                        name = need.name,
                        plannedQuantity = need.quantity,
                        bucketId = need.bucketId,
                        responsibleUserId = null,
                        notes = need.notes.ifBlank { null }
                    )
                }
            )
            eventRepository.createInventoryItemsBulk(eventId, request)
                .onSuccess { load(eventId) }
                .onFailure { error ->
                    (_uiState.value as? EventNeedsUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko sukurti poreikių.")
                    }
                }
        }
    }

    fun createPurchaseFromSelected(eventId: String, selectedInventoryItemIds: Set<String>) {
        val current = _uiState.value as? EventNeedsUiState.Success ?: return
        val selected = current.inventoryPlan?.items.orEmpty()
            .filter { it.id in selectedInventoryItemIds && it.shortageQuantity > 0 }
        if (selected.isEmpty()) {
            _uiState.value = current.copy(error = "Pažymėkite bent vieną trūkstamą daiktą.")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            val request = CreateEventPurchaseRequestDto(
                notes = "Sukurta iš pažymėtų renginio trūkumų",
                items = selected.map {
                    CreateEventPurchaseItemRequestDto(
                        eventInventoryItemId = it.id,
                        purchasedQuantity = it.shortageQuantity
                    )
                }
            )
            eventRepository.createPurchase(eventId, request)
                .onSuccess { load(eventId) }
                .onFailure { error ->
                    (_uiState.value as? EventNeedsUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko sukurti pirkimo.")
                    }
                }
        }
    }

    fun clearError() {
        (_uiState.value as? EventNeedsUiState.Success)?.let { _uiState.value = it.copy(error = null) }
    }
}
