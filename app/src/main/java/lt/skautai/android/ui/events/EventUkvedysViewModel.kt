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
import lt.skautai.android.data.remote.CreateEventInventoryAllocationRequestDto
import lt.skautai.android.data.remote.CreateEventInventoryBucketRequestDto
import lt.skautai.android.data.remote.CreateEventPurchaseItemRequestDto
import lt.skautai.android.data.remote.CreateEventPurchaseRequestDto
import lt.skautai.android.data.remote.CreatePastovykleInventoryRequestRequestDto
import lt.skautai.android.data.remote.CreatePastovykleRequestDto
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.EventInventoryPlanDto
import lt.skautai.android.data.remote.EventInventoryRequestDto
import lt.skautai.android.data.remote.PastovykleDto
import lt.skautai.android.data.remote.PastovykleInventoryDto
import lt.skautai.android.data.remote.UpdateEventInventoryAllocationRequestDto
import lt.skautai.android.data.remote.UpdateEventInventoryBucketRequestDto
import lt.skautai.android.data.repository.EventRepository
import lt.skautai.android.util.TokenManager

sealed interface EventUkvedysUiState {
    data object Loading : EventUkvedysUiState
    data class Success(
        val event: EventDto,
        val inventoryPlan: EventInventoryPlanDto? = null,
        val pastovykles: List<PastovykleDto> = emptyList(),
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
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventUkvedysUiState>(EventUkvedysUiState.Loading)
    val uiState: StateFlow<EventUkvedysUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null

    val permissions: StateFlow<Set<String>> = tokenManager.permissions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

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
            val pastovykles = eventRepository.getPastovykles(eventId).getOrNull()?.pastovykles.orEmpty()
            val inventoryByPastovykle = pastovykles.associate { p ->
                p.id to eventRepository.getPastovykleInventory(eventId, p.id).getOrNull()?.inventory.orEmpty()
            }
            val requestsByPastovykle = pastovykles.associate { p ->
                p.id to eventRepository.getPastovykleRequests(eventId, p.id).getOrNull()?.requests.orEmpty()
            }
            val current = _uiState.value as? EventUkvedysUiState.Success ?: return@launch
            _uiState.value = current.copy(
                inventoryPlan = inventoryPlan,
                pastovykles = pastovykles,
                pastovykleInventoryById = inventoryByPastovykle,
                pastovykleRequestsById = requestsByPastovykle
            )
        }
    }

    fun createPurchaseFromSelected(eventId: String, selectedInventoryItemIds: Set<String>) {
        val current = _uiState.value as? EventUkvedysUiState.Success ?: return
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
                    (_uiState.value as? EventUkvedysUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko sukurti pirkimo.")
                    }
                }
        }
    }

    fun createInventoryBucket(eventId: String, name: String, type: String, pastovykleId: String?, notes: String) {
        val current = _uiState.value as? EventUkvedysUiState.Success ?: return
        if (name.isBlank()) {
            _uiState.value = current.copy(error = "Įveskite bucket pavadinimą.")
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
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko sukurti bucket.")
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
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko atnaujinti bucket.")
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
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko ištrinti bucket.")
                    }
                }
        }
    }

    fun createAllocation(eventId: String, eventInventoryItemId: String, bucketId: String, quantityText: String, notes: String) {
        val current = _uiState.value as? EventUkvedysUiState.Success ?: return
        val quantity = quantityText.toIntOrNull()
        if (eventInventoryItemId.isBlank() || bucketId.isBlank() || quantity == null || quantity <= 0) {
            _uiState.value = current.copy(error = "Pasirinkite daiktą, bucket ir teigiamą kiekį.")
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
