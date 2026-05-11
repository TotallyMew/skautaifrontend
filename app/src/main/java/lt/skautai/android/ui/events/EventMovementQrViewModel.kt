package lt.skautai.android.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.CreateEventInventoryMovementRequestDto
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.EventInventoryCustodyDto
import lt.skautai.android.data.remote.EventInventoryPlanDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.PastovykleDto
import lt.skautai.android.data.repository.EventRepository
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.util.TokenManager
import java.util.UUID

sealed interface EventMovementQrUiState {
    data object Loading : EventMovementQrUiState
    data class Success(
        val event: EventDto,
        val inventoryPlan: EventInventoryPlanDto,
        val pastovykles: List<PastovykleDto>,
        val members: List<MemberDto>,
        val custody: List<EventInventoryCustodyDto>,
        val currentUserId: String? = null,
        val isWorking: Boolean = false,
        val isResolving: Boolean = false,
        val message: String? = null
    ) : EventMovementQrUiState

    data class Error(val message: String) : EventMovementQrUiState
}

@HiltViewModel
class EventMovementQrViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val itemRepository: ItemRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventMovementQrUiState>(EventMovementQrUiState.Loading)
    val uiState: StateFlow<EventMovementQrUiState> = _uiState.asStateFlow()

    val permissions: StateFlow<Set<String>> = tokenManager.permissions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun load(eventId: String) {
        viewModelScope.launch {
            val current = _uiState.value as? EventMovementQrUiState.Success
            _uiState.value = if (current == null) {
                EventMovementQrUiState.Loading
            } else {
                current.copy(isWorking = true, message = null)
            }

            val eventResult = eventRepository.getEvent(eventId)
            if (eventResult.isFailure) {
                _uiState.value = EventMovementQrUiState.Error(
                    eventResult.exceptionOrNull()?.message ?: "Nepavyko gauti renginio."
                )
                return@launch
            }

            val inventoryPlanResult = eventRepository.getInventoryPlan(eventId)
            if (inventoryPlanResult.isFailure) {
                _uiState.value = EventMovementQrUiState.Error(
                    inventoryPlanResult.exceptionOrNull()?.message ?: "Nepavyko gauti inventoriaus plano."
                )
                return@launch
            }

            val event = eventResult.getOrThrow()
            val plan = inventoryPlanResult.getOrThrow()
            val pastovykles = eventRepository.getPastovyklės(eventId).getOrNull()?.pastovykles.orEmpty()
            val members = eventRepository.getCandidateMembers(eventId).getOrNull()?.members.orEmpty()
            val custody = eventRepository.getInventoryCustody(eventId).getOrNull()?.custody.orEmpty()
            val currentUserId = tokenManager.userId.first()

            _uiState.value = EventMovementQrUiState.Success(
                event = event,
                inventoryPlan = plan,
                pastovykles = pastovykles,
                members = members,
                custody = custody,
                currentUserId = currentUserId,
                isWorking = false
            )
        }
    }

    fun resolveToken(token: String, onResolved: (String) -> Unit) {
        val current = _uiState.value as? EventMovementQrUiState.Success ?: return
        if (current.isResolving) return

        viewModelScope.launch {
            _uiState.value = current.copy(isResolving = true, message = null)
            itemRepository.resolveQrToken(token)
                .onSuccess { itemId ->
                    (_uiState.value as? EventMovementQrUiState.Success)?.let {
                        _uiState.value = it.copy(isResolving = false, message = null)
                    }
                    onResolved(itemId)
                }
                .onFailure { error ->
                    (_uiState.value as? EventMovementQrUiState.Success)?.let {
                        _uiState.value = it.copy(
                            isResolving = false,
                            message = error.message ?: "Nepavyko atpažinti QR kodo."
                        )
                    }
                }
        }
    }

    fun createMovement(
        eventId: String,
        request: CreateEventInventoryMovementRequestDto,
        onSuccess: () -> Unit
    ) {
        val current = _uiState.value as? EventMovementQrUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, message = null)
            eventRepository.createInventoryMovement(eventId, request)
                .onSuccess {
                    load(eventId)
                    onSuccess()
                }
                .onFailure { error ->
                    (_uiState.value as? EventMovementQrUiState.Success)?.let {
                        _uiState.value = it.copy(
                            isWorking = false,
                            message = error.message ?: "Nepavyko užregistruoti judėjimo."
                        )
                    }
                }
        }
    }

    fun showMessage(message: String) {
        (_uiState.value as? EventMovementQrUiState.Success)?.let {
            _uiState.value = it.copy(message = message)
        }
    }

    fun clearMessage() {
        (_uiState.value as? EventMovementQrUiState.Success)?.let {
            _uiState.value = it.copy(message = null)
        }
    }

    fun newRequest(
        eventInventoryItemId: String,
        movementType: String,
        quantity: Int,
        pastovykleId: String? = null,
        toUserId: String? = null,
        fromCustodyId: String? = null,
        notes: String? = null
    ): CreateEventInventoryMovementRequestDto = CreateEventInventoryMovementRequestDto(
        eventInventoryItemId = eventInventoryItemId,
        movementType = movementType,
        quantity = quantity,
        pastovykleId = pastovykleId,
        toUserId = toUserId,
        fromCustodyId = fromCustodyId,
        requestId = UUID.randomUUID().toString(),
        notes = notes
    )
}
