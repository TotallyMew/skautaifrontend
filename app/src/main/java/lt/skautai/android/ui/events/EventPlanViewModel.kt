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
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.EventInventoryPlanDto
import lt.skautai.android.data.remote.InventoryTemplateDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.UpdateEventInventoryItemRequestDto
import lt.skautai.android.data.repository.EventRepository
import lt.skautai.android.util.TokenManager

sealed interface EventPlanUiState {
    data object Loading : EventPlanUiState
    data class Success(
        val event: EventDto,
        val inventoryPlan: EventInventoryPlanDto? = null,
        val templates: List<InventoryTemplateDto> = emptyList(),
        val members: List<MemberDto> = emptyList(),
        val currentUserId: String? = null,
        val isWorking: Boolean = false,
        val error: String? = null
    ) : EventPlanUiState
    data class Error(val message: String) : EventPlanUiState
}

@HiltViewModel
class EventPlanViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventPlanUiState>(EventPlanUiState.Loading)
    val uiState: StateFlow<EventPlanUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null

    val permissions: StateFlow<Set<String>> = tokenManager.permissions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun load(eventId: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            eventRepository.observeEvent(eventId).collect { event ->
                if (event != null) {
                    val current = _uiState.value as? EventPlanUiState.Success
                    _uiState.value = EventPlanUiState.Success(
                        event = event,
                        inventoryPlan = current?.inventoryPlan,
                templates = current?.templates.orEmpty(),
                members = current?.members.orEmpty(),
                currentUserId = current?.currentUserId ?: tokenManager.userId.first(),
                isWorking = current?.isWorking == true,
                error = current?.error
                    )
                } else if (_uiState.value !is EventPlanUiState.Success) {
                    _uiState.value = EventPlanUiState.Loading
                }
            }
        }
        viewModelScope.launch {
            if (_uiState.value !is EventPlanUiState.Success) {
                _uiState.value = EventPlanUiState.Loading
            }
            eventRepository.getEvent(eventId)
                .onFailure { error ->
                    if (_uiState.value !is EventPlanUiState.Success) {
                        _uiState.value = EventPlanUiState.Error(error.message ?: "Nepavyko gauti renginio informacijos.")
                    }
                    return@launch
                }
            val inventoryPlan = eventRepository.getInventoryPlan(eventId).getOrNull()
            val current = _uiState.value as? EventPlanUiState.Success ?: return@launch
            _uiState.value = current.copy(inventoryPlan = inventoryPlan)
        }
    }

    fun loadMembers() {
        val current = _uiState.value as? EventPlanUiState.Success ?: return
        if (current.members.isNotEmpty()) return
        viewModelScope.launch {
            val members = eventRepository.getCandidateMembers(current.event.id).getOrNull()?.members.orEmpty()
            (_uiState.value as? EventPlanUiState.Success)?.let {
                _uiState.value = it.copy(members = members)
            }
        }
    }

    fun loadTemplates() {
        val current = _uiState.value as? EventPlanUiState.Success ?: return
        if (current.templates.isNotEmpty()) return
        viewModelScope.launch {
            eventRepository.getInventoryTemplates(current.event.type)
                .onSuccess { templates ->
                    (_uiState.value as? EventPlanUiState.Success)?.let {
                        _uiState.value = it.copy(templates = templates.templates)
                    }
                }
                .onFailure { error ->
                    (_uiState.value as? EventPlanUiState.Success)?.let {
                        _uiState.value = it.copy(error = error.message ?: "Nepavyko gauti šablonų.")
                    }
                }
        }
    }

    fun applyTemplate(eventId: String, template: InventoryTemplateDto) {
        val current = _uiState.value as? EventPlanUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.applyInventoryTemplate(eventId, template)
                .onSuccess { load(eventId) }
                .onFailure { error ->
                    (_uiState.value as? EventPlanUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko pritaikyti šablono.")
                    }
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
        val current = _uiState.value as? EventPlanUiState.Success ?: return
        val quantity = quantityText.toIntOrNull()
        if (name.isBlank() || quantity == null || quantity <= 0) {
            _uiState.value = current.copy(error = "Įveskite pavadinimą ir teigiamą kiekį.")
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
                .onSuccess { load(eventId) }
                .onFailure { error ->
                    (_uiState.value as? EventPlanUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko atnaujinti plano.")
                    }
                }
        }
    }

    fun deleteNeed(eventId: String, inventoryItemId: String) {
        val current = _uiState.value as? EventPlanUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.deleteInventoryItem(eventId, inventoryItemId)
                .onSuccess { load(eventId) }
                .onFailure { error ->
                    (_uiState.value as? EventPlanUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko ištrinti plano eilutės.")
                    }
                }
        }
    }

    fun clearError() {
        (_uiState.value as? EventPlanUiState.Success)?.let { _uiState.value = it.copy(error = null) }
    }
}
