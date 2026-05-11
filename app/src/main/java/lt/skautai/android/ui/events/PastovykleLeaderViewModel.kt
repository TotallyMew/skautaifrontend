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
import lt.skautai.android.data.remote.CreateEventInventoryItemRequestDto
import lt.skautai.android.data.remote.CreatePastovykleInventoryRequestRequestDto
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.EventInventoryPlanDto
import lt.skautai.android.data.remote.EventInventoryRequestDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.PastovykleDto
import lt.skautai.android.data.remote.PastovykleInventoryDto
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
        val pastovykleInventoryById: Map<String, List<PastovykleInventoryDto>> = emptyMap(),
        val pastovykleRequestsById: Map<String, List<EventInventoryRequestDto>> = emptyMap(),
        val items: List<ItemDto> = emptyList(),
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
                        pastovykleInventoryById = current?.pastovykleInventoryById.orEmpty(),
                        pastovykleRequestsById = current?.pastovykleRequestsById.orEmpty(),
                        items = current?.items.orEmpty(),
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
            val pastovykles = eventRepository.getPastovyklės(eventId).getOrNull()?.pastovykles.orEmpty()
            val inventoryPlan = eventRepository.getInventoryPlan(eventId).getOrNull()
            val inventoryByPastovykle = pastovykles.associate { p ->
                p.id to eventRepository.getPastovykleInventory(eventId, p.id).getOrNull()?.inventory.orEmpty()
            }
            val requestsByPastovykle = pastovykles.associate { p ->
                p.id to eventRepository.getPastovykleRequests(eventId, p.id).getOrNull()?.requests.orEmpty()
            }
            val items = itemRepository.getItems(status = "ACTIVE").getOrNull().orEmpty()
            val current = _uiState.value as? PastovykleLeaderUiState.Success ?: return@launch
            _uiState.value = current.copy(
                currentUserId = currentUserId,
                activeOrgUnitId = activeOrgUnitId,
                pastovykles = pastovykles,
                inventoryPlan = inventoryPlan,
                pastovykleInventoryById = inventoryByPastovykle,
                pastovykleRequestsById = requestsByPastovykle,
                items = items
            )
        }
    }

    fun createPastovykleRequest(eventId: String, pastovykleId: String, itemId: String, quantityText: String, notes: String) {
        val current = _uiState.value as? PastovykleLeaderUiState.Success ?: return
        val quantity = quantityText.toIntOrNull()
        val item = current.items.firstOrNull { it.id == itemId }
        if (itemId.isBlank() || item == null || quantity == null || quantity <= 0) {
            _uiState.value = current.copy(error = "Pasirinkite daiktą ir teigiamą kiekį.")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            val eventInventoryItem = eventRepository.createInventoryItem(
                eventId,
                CreateEventInventoryItemRequestDto(
                    itemId = item.id,
                    name = item.name,
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
                CreatePastovykleInventoryRequestRequestDto(eventInventoryItem.id, quantity, notes.ifBlank { null })
            )
                .onSuccess { refreshAfterSuccessfulMutation(eventId) }
                .onFailure { error ->
                    (_uiState.value as? PastovykleLeaderUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko sukurti pastovyklės poreikio.")
                    }
                }
        }
    }

    fun assignFromUnitInventory(eventId: String, pastovykleId: String, itemId: String, quantityText: String, notes: String) {
        val current = _uiState.value as? PastovykleLeaderUiState.Success ?: return
        val quantity = quantityText.toIntOrNull()
        if (itemId.isBlank() || quantity == null || quantity <= 0) {
            _uiState.value = current.copy(error = "Pasirinkite vieneto daiktą ir teigiamą kiekį.")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.assignFromUnitInventory(eventId, pastovykleId, itemId, quantity, notes.ifBlank { null })
                .onSuccess { refreshAfterSuccessfulMutation(eventId) }
                .onFailure { error ->
                    (_uiState.value as? PastovykleLeaderUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko priskirti inventoriaus iš vieneto.")
                    }
                }
        }
    }

    fun selfProvidePastovykleRequest(eventId: String, pastovykleId: String, requestId: String, notes: String) {
        val current = _uiState.value as? PastovykleLeaderUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.selfProvidePastovykleRequest(eventId, pastovykleId, requestId, notes.ifBlank { null })
                .onSuccess { refreshAfterSuccessfulMutation(eventId) }
                .onFailure { error ->
                    (_uiState.value as? PastovykleLeaderUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko atnaujinti pastovyklės poreikio.")
                    }
                }
        }
    }

    fun clearError() {
        (_uiState.value as? PastovykleLeaderUiState.Success)?.let { _uiState.value = it.copy(error = null) }
    }

    private fun refreshAfterSuccessfulMutation(eventId: String) {
        (_uiState.value as? PastovykleLeaderUiState.Success)?.let {
            _uiState.value = it.copy(isWorking = false, error = null)
        }
        load(eventId)
    }
}
