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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.CreateEventInventoryAllocationRequestDto
import lt.skautai.android.data.remote.CreateEventInventoryBucketRequestDto
import lt.skautai.android.data.remote.CreateEventPurchaseItemRequestDto
import lt.skautai.android.data.remote.CreateEventPurchaseRequestDto
import lt.skautai.android.data.remote.CreatePastovykleInventoryRequestRequestDto
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.EventInventoryPlanDto
import lt.skautai.android.data.remote.EventInventoryRequestDto
import lt.skautai.android.data.remote.EventPurchaseDto
import lt.skautai.android.data.remote.PastovykleDto
import lt.skautai.android.data.remote.PastovykleInventoryDto
import lt.skautai.android.data.remote.UpdateEventInventoryAllocationRequestDto
import lt.skautai.android.data.remote.UpdateEventInventoryBucketRequestDto
import lt.skautai.android.data.remote.UpdateEventPurchaseRequestDto
import lt.skautai.android.data.repository.EventRepository
import lt.skautai.android.data.repository.UploadRepository
import lt.skautai.android.util.TokenManager

sealed interface EventUkvedysUiState {
    data object Loading : EventUkvedysUiState
    data class Success(
        val event: EventDto,
        val inventoryPlan: EventInventoryPlanDto? = null,
        val pastovykles: List<PastovykleDto> = emptyList(),
        val purchases: List<EventPurchaseDto> = emptyList(),
        val createdPurchase: EventPurchaseDto? = null,
        val selectedPastovykleId: String? = null,
        val pastovykleInventoryById: Map<String, List<PastovykleInventoryDto>> = emptyMap(),
        val pastovykleRequestsById: Map<String, List<EventInventoryRequestDto>> = emptyMap(),
        val isWorking: Boolean = false,
        val error: String? = null
    ) : EventUkvedysUiState
    data class Error(val message: String) : EventUkvedysUiState
}

@HiltViewModel
class EventUkvedysViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val uploadRepository: UploadRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventUkvedysUiState>(EventUkvedysUiState.Loading)
    val uiState: StateFlow<EventUkvedysUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null

    val permissions: StateFlow<Set<String>> = tokenManager.permissions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val inventoryCache = mutableMapOf<String, List<PastovykleInventoryDto>>()
    private val requestsCache = mutableMapOf<String, List<EventInventoryRequestDto>>()

    fun load(eventId: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            eventRepository.observeEvent(eventId).collect { event ->
                if (event != null) {
                    val current = _uiState.value as? EventUkvedysUiState.Success
                    _uiState.value = EventUkvedysUiState.Success(
                        event = event,
                        inventoryPlan = current?.inventoryPlan,
                        pastovykles = current?.pastovykles.orEmpty(),
                        purchases = current?.purchases.orEmpty(),
                        createdPurchase = current?.createdPurchase,
                        selectedPastovykleId = current?.selectedPastovykleId,
                        pastovykleInventoryById = current?.pastovykleInventoryById.orEmpty(),
                        pastovykleRequestsById = current?.pastovykleRequestsById.orEmpty(),
                        isWorking = current?.isWorking == true,
                        error = current?.error
                    )
                } else if (_uiState.value !is EventUkvedysUiState.Success) {
                    _uiState.value = EventUkvedysUiState.Loading
                }
            }
        }
        viewModelScope.launch {
            if (_uiState.value !is EventUkvedysUiState.Success) {
                _uiState.value = EventUkvedysUiState.Loading
            }
            eventRepository.getEvent(eventId)
                .onFailure { error ->
                    if (_uiState.value !is EventUkvedysUiState.Success) {
                        _uiState.value = EventUkvedysUiState.Error(error.message ?: "Nepavyko gauti renginio informacijos.")
                    }
                    return@launch
                }
            val inventoryPlan = eventRepository.getInventoryPlan(eventId).getOrNull()
            val pastovykles = eventRepository.getPastovyklės(eventId).getOrNull()?.pastovykles.orEmpty()
            val purchases = eventRepository.getPurchases(eventId).getOrNull()?.purchases.orEmpty()
            val current = _uiState.value as? EventUkvedysUiState.Success ?: return@launch
            val firstId = current.selectedPastovykleId ?: pastovykles.firstOrNull()?.id
            _uiState.value = current.copy(
                inventoryPlan = inventoryPlan,
                pastovykles = pastovykles,
                purchases = purchases,
                selectedPastovykleId = firstId
            )
            if (firstId != null) selectPastovykle(eventId, firstId)
        }
    }

    fun selectPastovykle(eventId: String, pastovykleId: String) {
        val current = _uiState.value as? EventUkvedysUiState.Success ?: return
        if (inventoryCache.containsKey(pastovykleId) && requestsCache.containsKey(pastovykleId)) {
            _uiState.value = current.copy(
                selectedPastovykleId = pastovykleId,
                pastovykleInventoryById = current.pastovykleInventoryById + (pastovykleId to inventoryCache[pastovykleId].orEmpty()),
                pastovykleRequestsById = current.pastovykleRequestsById + (pastovykleId to requestsCache[pastovykleId].orEmpty())
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(selectedPastovykleId = pastovykleId, isWorking = true)
            val inventory = eventRepository.getPastovykleInventory(eventId, pastovykleId).getOrNull()?.inventory.orEmpty()
            val requests = eventRepository.getPastovykleRequests(eventId, pastovykleId).getOrNull()?.requests.orEmpty()
            inventoryCache[pastovykleId] = inventory
            requestsCache[pastovykleId] = requests
            (_uiState.value as? EventUkvedysUiState.Success)?.let {
                _uiState.value = it.copy(
                    isWorking = false,
                    pastovykleInventoryById = it.pastovykleInventoryById + (pastovykleId to inventory),
                    pastovykleRequestsById = it.pastovykleRequestsById + (pastovykleId to requests)
                )
            }
        }
    }

    fun selectAllPastovyklės(eventId: String) {
        val current = _uiState.value as? EventUkvedysUiState.Success ?: return
        val missingPastovyklės = current.pastovykles.filter {
            !inventoryCache.containsKey(it.id) || !requestsCache.containsKey(it.id)
        }
        if (missingPastovyklės.isEmpty()) {
            _uiState.value = current.copy(selectedPastovykleId = null)
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(selectedPastovykleId = null, isWorking = true)
            missingPastovyklės.forEach { pastovykle ->
                val inventory = eventRepository.getPastovykleInventory(eventId, pastovykle.id).getOrNull()?.inventory.orEmpty()
                val requests = eventRepository.getPastovykleRequests(eventId, pastovykle.id).getOrNull()?.requests.orEmpty()
                inventoryCache[pastovykle.id] = inventory
                requestsCache[pastovykle.id] = requests
            }
            (_uiState.value as? EventUkvedysUiState.Success)?.let {
                _uiState.value = it.copy(
                    isWorking = false,
                    selectedPastovykleId = null,
                    pastovykleInventoryById = inventoryCache.toMap(),
                    pastovykleRequestsById = requestsCache.toMap()
                )
            }
        }
    }

    fun createPurchaseFromSelected(eventId: String, selectedInventoryItemIds: Set<String>) {
        val current = _uiState.value as? EventUkvedysUiState.Success ?: return
        val activePurchaseItemIds = current.purchases
            .filter { it.status in listOf("DRAFT", "PURCHASED") }
            .flatMap { it.items }
            .map { it.eventInventoryItemId }
            .toSet()
        val selected = current.inventoryPlan?.items.orEmpty()
            .filter { it.id in selectedInventoryItemIds && it.shortageQuantity > 0 && it.id !in activePurchaseItemIds }
        if (selected.isEmpty()) {
            _uiState.value = current.copy(error = "Pažymėkite bent vieną trūkstamą daiktą, kuris dar nėra aktyviame pirkime.")
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
                .onSuccess { purchase ->
                    val updatedPurchases = eventRepository.getPurchases(eventId).getOrNull()?.purchases.orEmpty()
                    val updatedPlan = eventRepository.getInventoryPlan(eventId).getOrNull()
                    (_uiState.value as? EventUkvedysUiState.Success)?.let {
                        _uiState.value = it.copy(
                            inventoryPlan = updatedPlan ?: it.inventoryPlan,
                            purchases = if (updatedPurchases.isEmpty()) it.purchases + purchase else updatedPurchases,
                            createdPurchase = purchase,
                            isWorking = false,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    (_uiState.value as? EventUkvedysUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko sukurti pirkimo.")
                    }
                }
        }
    }

    fun saveCreatedPurchaseDetails(eventId: String, purchaseId: String, totalAmountText: String, invoiceUri: Uri?) {
        val current = _uiState.value as? EventUkvedysUiState.Success ?: return
        val totalAmount = totalAmountText.replace(',', '.').toDoubleOrNull()
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            if (totalAmount != null) {
                val updateResult = eventRepository.updatePurchase(
                    eventId,
                    purchaseId,
                    UpdateEventPurchaseRequestDto(totalAmount = totalAmount)
                )
                if (updateResult.isFailure) {
                    _uiState.value = current.copy(isWorking = false, error = updateResult.exceptionOrNull()?.message ?: "Nepavyko išsaugoti pirkimo sumos.")
                    return@launch
                }
            }
            if (invoiceUri != null) {
                val uploadResult = uploadRepository.uploadDocument(invoiceUri)
                if (uploadResult.isFailure) {
                    _uiState.value = current.copy(isWorking = false, error = uploadResult.exceptionOrNull()?.message ?: "Nepavyko įkelti sąskaitos.")
                    return@launch
                }
                val attachResult = eventRepository.attachPurchaseInvoice(eventId, purchaseId, uploadResult.getOrThrow())
                if (attachResult.isFailure) {
                    _uiState.value = current.copy(isWorking = false, error = attachResult.exceptionOrNull()?.message ?: "Nepavyko prisegti sąskaitos.")
                    return@launch
                }
            }
            val purchases = eventRepository.getPurchases(eventId).getOrNull()?.purchases.orEmpty()
            (_uiState.value as? EventUkvedysUiState.Success)?.let {
                _uiState.value = it.copy(
                    purchases = if (purchases.isEmpty()) it.purchases else purchases,
                    createdPurchase = null,
                    isWorking = false
                )
            }
        }
    }

    fun dismissCreatedPurchase() {
        (_uiState.value as? EventUkvedysUiState.Success)?.let {
            _uiState.value = it.copy(createdPurchase = null)
        }
    }

    fun createInventoryBucket(eventId: String, name: String, type: String, pastovykleId: String?, notes: String) {
        val current = _uiState.value as? EventUkvedysUiState.Success ?: return
        if (name.isBlank()) {
            _uiState.value = current.copy(error = "Įveskite paskirties pavadinimą.")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.createInventoryBucket(
                eventId,
                CreateEventInventoryBucketRequestDto(name = name.trim(), type = type, pastovykleId = pastovykleId, notes = notes.ifBlank { null })
            )
                .onSuccess { load(eventId) }
                .onFailure { error ->
                    (_uiState.value as? EventUkvedysUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko sukurti paskirties.")
                    }
                }
        }
    }

    fun updateInventoryBucket(eventId: String, bucketId: String, name: String, type: String, pastovykleId: String?, notes: String) {
        val current = _uiState.value as? EventUkvedysUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.updateInventoryBucket(
                eventId, bucketId,
                UpdateEventInventoryBucketRequestDto(name = name.trim(), type = type, pastovykleId = pastovykleId, notes = notes.ifBlank { null })
            )
                .onSuccess { load(eventId) }
                .onFailure { error ->
                    (_uiState.value as? EventUkvedysUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko atnaujinti paskirties.")
                    }
                }
        }
    }

    fun deleteInventoryBucket(eventId: String, bucketId: String) {
        val current = _uiState.value as? EventUkvedysUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.deleteInventoryBucket(eventId, bucketId)
                .onSuccess { load(eventId) }
                .onFailure { error ->
                    (_uiState.value as? EventUkvedysUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko ištrinti paskirties.")
                    }
                }
        }
    }

    fun createAllocation(eventId: String, eventInventoryItemId: String, bucketId: String, quantityText: String, notes: String) {
        val current = _uiState.value as? EventUkvedysUiState.Success ?: return
        val quantity = quantityText.toIntOrNull()
        if (eventInventoryItemId.isBlank() || bucketId.isBlank() || quantity == null || quantity <= 0) {
            _uiState.value = current.copy(error = "Pasirinkite daiktą, paskirtį ir teigiamą kiekį.")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.createInventoryAllocation(
                eventId,
                CreateEventInventoryAllocationRequestDto(eventInventoryItemId = eventInventoryItemId, bucketId = bucketId, quantity = quantity, notes = notes.ifBlank { null })
            )
                .onSuccess { load(eventId) }
                .onFailure { error ->
                    (_uiState.value as? EventUkvedysUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko sukurti paskirstymo.")
                    }
                }
        }
    }

    fun updateAllocation(eventId: String, allocationId: String, quantityText: String, notes: String) {
        val current = _uiState.value as? EventUkvedysUiState.Success ?: return
        val quantity = quantityText.toIntOrNull()
        if (quantity == null || quantity <= 0) {
            _uiState.value = current.copy(error = "Įveskite teigiamą kiekį.")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.updateInventoryAllocation(
                eventId, allocationId,
                UpdateEventInventoryAllocationRequestDto(quantity = quantity, notes = notes.ifBlank { null })
            )
                .onSuccess { load(eventId) }
                .onFailure { error ->
                    (_uiState.value as? EventUkvedysUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko atnaujinti paskirstymo.")
                    }
                }
        }
    }

    fun deleteAllocation(eventId: String, allocationId: String) {
        val current = _uiState.value as? EventUkvedysUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.deleteInventoryAllocation(eventId, allocationId)
                .onSuccess { load(eventId) }
                .onFailure { error ->
                    (_uiState.value as? EventUkvedysUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko ištrinti paskirstymo.")
                    }
                }
        }
    }

    fun createPastovykleRequest(eventId: String, pastovykleId: String, eventInventoryItemId: String, quantityText: String, notes: String) {
        val current = _uiState.value as? EventUkvedysUiState.Success ?: return
        val quantity = quantityText.toIntOrNull()
        if (eventInventoryItemId.isBlank() || quantity == null || quantity <= 0) {
            _uiState.value = current.copy(error = "Pasirinkite daiktą ir teigiamą kiekį.")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.createPastovykleRequest(
                eventId, pastovykleId,
                CreatePastovykleInventoryRequestRequestDto(eventInventoryItemId, quantity, notes.ifBlank { null })
            )
                .onSuccess { load(eventId) }
                .onFailure { error ->
                    (_uiState.value as? EventUkvedysUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko sukurti pastovyklės poreikio.")
                    }
                }
        }
    }

    fun approvePastovykleRequest(eventId: String, pastovykleId: String, requestId: String) {
        mutatePastovykleRequest(eventId) { eventRepository.approvePastovykleRequest(eventId, pastovykleId, requestId) }
    }

    fun rejectPastovykleRequest(eventId: String, pastovykleId: String, requestId: String) {
        mutatePastovykleRequest(eventId) { eventRepository.rejectPastovykleRequest(eventId, pastovykleId, requestId) }
    }

    fun fulfillPastovykleRequest(eventId: String, pastovykleId: String, requestId: String) {
        mutatePastovykleRequest(eventId) { eventRepository.fulfillPastovykleRequest(eventId, pastovykleId, requestId) }
    }

    private fun mutatePastovykleRequest(
        eventId: String,
        block: suspend () -> Result<EventInventoryRequestDto>
    ) {
        val current = _uiState.value as? EventUkvedysUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            block()
                .onSuccess { load(eventId) }
                .onFailure { error ->
                    (_uiState.value as? EventUkvedysUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko atnaujinti pastovyklės poreikio.")
                    }
                }
        }
    }

    fun clearError() {
        (_uiState.value as? EventUkvedysUiState.Success)?.let { _uiState.value = it.copy(error = null) }
    }
}
