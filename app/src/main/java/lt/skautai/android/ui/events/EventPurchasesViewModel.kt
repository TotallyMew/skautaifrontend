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
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.EventPurchaseDto
import lt.skautai.android.data.remote.UpdateEventPurchaseRequestDto
import lt.skautai.android.data.repository.EventRepository
import lt.skautai.android.data.repository.UploadRepository
import lt.skautai.android.util.TokenManager

sealed interface EventPurchasesUiState {
    data object Loading : EventPurchasesUiState
    data class Success(
        val event: EventDto,
        val purchases: List<EventPurchaseDto> = emptyList(),
        val isWorking: Boolean = false,
        val error: String? = null
    ) : EventPurchasesUiState
    data class Error(val message: String) : EventPurchasesUiState
}

@HiltViewModel
class EventPurchasesViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val uploadRepository: UploadRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventPurchasesUiState>(EventPurchasesUiState.Loading)
    val uiState: StateFlow<EventPurchasesUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null

    val permissions: StateFlow<Set<String>> = tokenManager.permissions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun load(eventId: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            eventRepository.observeEvent(eventId).collect { event ->
                if (event != null) {
                    val current = _uiState.value as? EventPurchasesUiState.Success
                    _uiState.value = EventPurchasesUiState.Success(
                        event = event,
                        purchases = current?.purchases.orEmpty(),
                        isWorking = current?.isWorking == true,
                        error = current?.error
                    )
                } else if (_uiState.value !is EventPurchasesUiState.Success) {
                    _uiState.value = EventPurchasesUiState.Loading
                }
            }
        }
        viewModelScope.launch {
            if (_uiState.value !is EventPurchasesUiState.Success) {
                _uiState.value = EventPurchasesUiState.Loading
            }
            eventRepository.getEvent(eventId)
                .onFailure { error ->
                    if (_uiState.value !is EventPurchasesUiState.Success) {
                        _uiState.value = EventPurchasesUiState.Error(error.message ?: "Nepavyko gauti renginio informacijos.")
                    }
                    return@launch
                }
            val purchases = eventRepository.getPurchases(eventId).getOrNull()?.purchases.orEmpty()
            val current = _uiState.value as? EventPurchasesUiState.Success ?: return@launch
            _uiState.value = current.copy(purchases = purchases, isWorking = false)
        }
    }

    fun completePurchase(eventId: String, purchaseId: String, totalAmount: Double?) {
        val current = _uiState.value as? EventPurchasesUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            if (totalAmount != null) {
                val updateResult = eventRepository.updatePurchase(
                    eventId,
                    purchaseId,
                    UpdateEventPurchaseRequestDto(totalAmount = totalAmount)
                )
                if (updateResult.isFailure) {
                    _uiState.value = current.copy(
                        isWorking = false,
                        error = updateResult.exceptionOrNull()?.message ?: "Nepavyko išsaugoti pirkimo sumos."
                    )
                    return@launch
                }
            }
            eventRepository.completePurchase(eventId, purchaseId)
                .onSuccess { load(eventId) }
                .onFailure { error ->
                    (_uiState.value as? EventPurchasesUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko užbaigti pirkimo.")
                    }
                }
        }
    }

    fun updatePurchaseAmount(eventId: String, purchaseId: String, totalAmount: Double?) {
        val current = _uiState.value as? EventPurchasesUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.updatePurchase(
                eventId,
                purchaseId,
                UpdateEventPurchaseRequestDto(totalAmount = totalAmount)
            )
                .onSuccess { load(eventId) }
                .onFailure { error ->
                    (_uiState.value as? EventPurchasesUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko išsaugoti pirkimo sumos.")
                    }
                }
        }
    }

    fun attachInvoice(eventId: String, purchaseId: String, uri: Uri) {
        val current = _uiState.value as? EventPurchasesUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            uploadRepository.uploadDocument(uri)
                .onSuccess { url ->
                    eventRepository.attachPurchaseInvoice(eventId, purchaseId, url)
                        .onSuccess { load(eventId) }
                        .onFailure { error ->
                            (_uiState.value as? EventPurchasesUiState.Success)?.let {
                                _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko prisegti sąskaitos.")
                            }
                        }
                }
                .onFailure { error ->
                    (_uiState.value as? EventPurchasesUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko įkelti sąskaitos.")
                    }
                }
        }
    }

    fun downloadInvoice(eventId: String, purchaseId: String) {
        val current = _uiState.value as? EventPurchasesUiState.Success ?: return
        val invoiceFileUrl = current.purchases.firstOrNull { it.id == purchaseId }?.invoiceFileUrl
        viewModelScope.launch {
            uploadRepository.downloadEventPurchaseInvoice(eventId, purchaseId, invoiceFileUrl)
                .onFailure { error ->
                    (_uiState.value as? EventPurchasesUiState.Success)?.let {
                        _uiState.value = it.copy(error = error.message ?: "Nepavyko atsisiųsti sąskaitos.")
                    }
                }
        }
    }

    fun clearError() {
        (_uiState.value as? EventPurchasesUiState.Success)?.let { _uiState.value = it.copy(error = null) }
    }
}
