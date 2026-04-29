package lt.skautai.android.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.EventReconciliationDto
import lt.skautai.android.data.remote.ReconcileEventPurchaseLineRequestDto
import lt.skautai.android.data.remote.ReconcileEventPurchasesRequestDto
import lt.skautai.android.data.remote.ReconcileEventReturnLineRequestDto
import lt.skautai.android.data.remote.ReconcileEventReturnsRequestDto
import lt.skautai.android.data.repository.EventRepository

sealed interface EventReconciliationUiState {
    data object Loading : EventReconciliationUiState
    data class Error(val message: String) : EventReconciliationUiState
    data class Success(
        val event: EventDto,
        val reconciliation: EventReconciliationDto,
        val isWorking: Boolean = false,
        val error: String? = null
    ) : EventReconciliationUiState
}

@HiltViewModel
class EventReconciliationViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<EventReconciliationUiState>(EventReconciliationUiState.Loading)
    val uiState: StateFlow<EventReconciliationUiState> = _uiState.asStateFlow()

    fun load(eventId: String) {
        viewModelScope.launch {
            _uiState.value = EventReconciliationUiState.Loading
            val event = eventRepository.getEvent(eventId).getOrElse {
                _uiState.value = EventReconciliationUiState.Error(it.message ?: "Nepavyko gauti renginio.")
                return@launch
            }
            val reconciliation = eventRepository.getReconciliation(eventId).getOrElse {
                _uiState.value = EventReconciliationUiState.Error(it.message ?: "Nepavyko gauti suvedimo.")
                return@launch
            }
            _uiState.value = EventReconciliationUiState.Success(event, reconciliation)
        }
    }

    fun complete(eventId: String) {
        val current = _uiState.value as? EventReconciliationUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.completeEvent(eventId)
                .onSuccess { load(eventId) }
                .onFailure {
                    _uiState.value = current.copy(
                        isWorking = false,
                        error = it.message ?: "Nepavyko užbaigti renginio."
                    )
                }
        }
    }

    fun reconcileReturn(eventId: String, custodyId: String, decision: String, quantity: Int) {
        val current = _uiState.value as? EventReconciliationUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            val request = ReconcileEventReturnsRequestDto(
                returns = listOf(
                    ReconcileEventReturnLineRequestDto(
                        custodyId = custodyId,
                        decision = decision,
                        quantity = quantity
                    )
                )
            )
            eventRepository.reconcileReturns(eventId, request)
                .onSuccess { load(eventId) }
                .onFailure {
                    _uiState.value = current.copy(isWorking = false, error = it.message ?: "Nepavyko suvesti grąžinimo.")
                }
        }
    }

    fun reconcilePurchase(
        eventId: String,
        purchaseItemId: String,
        decision: String,
        quantity: Int,
        existingItemId: String? = null
    ) {
        val current = _uiState.value as? EventReconciliationUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            val request = ReconcileEventPurchasesRequestDto(
                purchases = listOf(
                    ReconcileEventPurchaseLineRequestDto(
                        purchaseItemId = purchaseItemId,
                        decision = decision,
                        quantity = quantity,
                        existingItemId = existingItemId
                    )
                )
            )
            eventRepository.reconcilePurchases(eventId, request)
                .onSuccess { load(eventId) }
                .onFailure {
                    _uiState.value = current.copy(isWorking = false, error = it.message ?: "Nepavyko suvesti pirkimo.")
                }
        }
    }

    fun clearError() {
        val current = _uiState.value as? EventReconciliationUiState.Success ?: return
        _uiState.value = current.copy(error = null)
    }
}
