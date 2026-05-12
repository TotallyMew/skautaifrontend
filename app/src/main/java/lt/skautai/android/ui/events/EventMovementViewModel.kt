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
import lt.skautai.android.data.remote.CreateEventInventoryMovementRequestDto
import lt.skautai.android.data.remote.CreatePastovykleRequestDto
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.EventInventoryCustodyDto
import lt.skautai.android.data.remote.EventInventoryMovementDto
import lt.skautai.android.data.remote.EventInventoryPlanDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.PastovykleDto
import lt.skautai.android.data.repository.EventRepository
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.util.TokenManager
import java.util.UUID

sealed interface EventMovementUiState {
    data object Loading : EventMovementUiState
    data class Success(
        val event: EventDto,
        val inventoryPlan: EventInventoryPlanDto? = null,
        val pastovykles: List<PastovykleDto> = emptyList(),
        val members: List<MemberDto> = emptyList(),
        val inventoryItems: List<ItemDto> = emptyList(),
        val custody: List<EventInventoryCustodyDto> = emptyList(),
        val movements: List<EventInventoryMovementDto> = emptyList(),
        val isWorking: Boolean = false,
        val error: String? = null
    ) : EventMovementUiState
    data class Error(val message: String) : EventMovementUiState
}

@HiltViewModel
class EventMovementViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val itemRepository: ItemRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventMovementUiState>(EventMovementUiState.Loading)
    val uiState: StateFlow<EventMovementUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null

    val permissions: StateFlow<Set<String>> = tokenManager.permissions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun load(eventId: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            eventRepository.observeEvent(eventId).collect { event ->
                if (event != null) {
                    val current = _uiState.value as? EventMovementUiState.Success
                    _uiState.value = EventMovementUiState.Success(
                        event = event,
                        inventoryPlan = current?.inventoryPlan,
                        pastovykles = current?.pastovykles.orEmpty(),
                        members = current?.members.orEmpty(),
                        inventoryItems = current?.inventoryItems.orEmpty(),
                        custody = current?.custody.orEmpty(),
                        movements = current?.movements.orEmpty(),
                        isWorking = current?.isWorking == true,
                        error = current?.error
                    )
                } else if (_uiState.value !is EventMovementUiState.Success) {
                    _uiState.value = EventMovementUiState.Loading
                }
            }
        }
        viewModelScope.launch {
            if (_uiState.value !is EventMovementUiState.Success) {
                _uiState.value = EventMovementUiState.Loading
            }
            eventRepository.getEvent(eventId)
                .onFailure { error ->
                    if (_uiState.value !is EventMovementUiState.Success) {
                        _uiState.value = EventMovementUiState.Error(error.message ?: "Nepavyko gauti renginio informacijos.")
                    }
                    return@launch
                }
            val inventoryPlan = eventRepository.getInventoryPlan(eventId).getOrNull()
            val pastovykles = eventRepository.getPastovyklės(eventId).getOrNull()?.pastovykles.orEmpty()
            val members = eventRepository.getCandidateMembers(eventId).getOrNull()?.members.orEmpty()
            val inventoryItems = itemRepository.getItems(status = "ACTIVE").getOrNull().orEmpty()
            val custody = eventRepository.getInventoryCustody(eventId).getOrNull()?.custody.orEmpty()
            val movements = eventRepository.getInventoryMovements(eventId).getOrNull()?.movements.orEmpty()
            val current = _uiState.value as? EventMovementUiState.Success ?: return@launch
            _uiState.value = current.copy(
                inventoryPlan = inventoryPlan,
                pastovykles = pastovykles,
                members = members,
                inventoryItems = inventoryItems,
                custody = custody,
                movements = movements
            )
        }
    }

    fun createPastovykle(eventId: String, name: String, responsibleUserId: String?, notes: String) {
        val current = _uiState.value as? EventMovementUiState.Success ?: return
        if (name.isBlank()) {
            _uiState.value = current.copy(error = "Įveskite pastovyklės pavadinimą.")
            return
        }
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.createPastovykle(
                eventId,
                CreatePastovykleRequestDto(name = name.trim(), responsibleUserId = responsibleUserId, notes = notes.ifBlank { null })
            )
                .onSuccess { load(eventId) }
                .onFailure { error ->
                    (_uiState.value as? EventMovementUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko sukurti pastovyklės.")
                    }
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
        val current = _uiState.value as? EventMovementUiState.Success ?: return
        val quantity = quantityText.toIntOrNull()
        if (eventInventoryItemId.isBlank() || quantity == null || quantity <= 0) {
            _uiState.value = current.copy(error = "Pasirinkite daiktą ir teigiamą kiekį.")
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
                .onSuccess { load(eventId) }
                .onFailure { error ->
                    (_uiState.value as? EventMovementUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko užregistruoti judėjimo.")
                    }
                }
        }
    }

    fun clearError() {
        (_uiState.value as? EventMovementUiState.Success)?.let { _uiState.value = it.copy(error = null) }
    }
}
