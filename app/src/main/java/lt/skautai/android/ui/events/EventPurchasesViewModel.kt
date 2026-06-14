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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.CreateEventExtraCostRequestDto
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.EventFinanceDto
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
        val finance: EventFinanceDto? = null,
        val currentUserId: String? = null,
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
                        finance = current?.finance,
                        currentUserId = current?.currentUserId ?: tokenManager.userId.first(),
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
            val finance = eventRepository.getEventFinance(eventId).getOrNull()
            val current = _uiState.value as? EventPurchasesUiState.Success ?: return@launch
            _uiState.value = current.copy(purchases = purchases, finance = finance, isWorking = false)
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
                        error = updateResult.exceptionOrNull()?.message ?: "Nepavyko iÅ¡saugoti pirkimo sumos."
                    )
                    return@launch
                }
            }
            eventRepository.completePurchase(eventId, purchaseId)
                .onSuccess { load(eventId) }
                .onFailure { error ->
                    (_uiState.value as? EventPurchasesUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko uÅ¾baigti pirkimo.")
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
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko iÅ¡saugoti pirkimo sumos.")
                    }
                }
        }
    }

    fun attachInvoices(eventId: String, purchaseId: String, uris: List<Uri>) {
        val current = _uiState.value as? EventPurchasesUiState.Success ?: return
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            uris.forEach { uri ->
                val uploadResult = uploadRepository.uploadDocument(uri)
                if (uploadResult.isFailure) {
                    (_uiState.value as? EventPurchasesUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = uploadResult.exceptionOrNull()?.message ?: "Nepavyko įkelti sąskaitos.")
                    }
                    return@launch
                }
                val attachResult = eventRepository.attachPurchaseInvoice(eventId, purchaseId, uploadResult.getOrThrow())
                if (attachResult.isFailure) {
                    (_uiState.value as? EventPurchasesUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = attachResult.exceptionOrNull()?.message ?: "Nepavyko prisegti sąskaitos.")
                    }
                    return@launch
                }
            }
            load(eventId)
        }
    }

    fun downloadInvoice(eventId: String, purchaseId: String, invoiceId: String? = null, invoiceFileUrl: String? = null) {
        val current = _uiState.value as? EventPurchasesUiState.Success ?: return
        val resolvedInvoiceFileUrl = invoiceFileUrl
            ?: current.purchases.firstOrNull { it.id == purchaseId }?.invoiceFileUrl
        viewModelScope.launch {
            uploadRepository.downloadEventPurchaseInvoice(eventId, purchaseId, resolvedInvoiceFileUrl, invoiceId)
                .onFailure { error ->
                    (_uiState.value as? EventPurchasesUiState.Success)?.let {
                        _uiState.value = it.copy(error = error.message ?: "Nepavyko atsisiųsti sąskaitos.")
                    }
                }
        }
    }

    fun updateBudget(eventId: String, amount: Double?) {
        val current = _uiState.value as? EventPurchasesUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.updateEventFinanceBudget(eventId, amount)
                .onSuccess { finance ->
                    (_uiState.value as? EventPurchasesUiState.Success)?.let {
                        _uiState.value = it.copy(finance = finance, isWorking = false)
                    }
                }
                .onFailure { error ->
                    (_uiState.value as? EventPurchasesUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko atnaujinti biudžeto.")
                    }
                }
        }
    }

    fun addExtraCost(
        eventId: String,
        category: String,
        label: String,
        quantity: Double?,
        unit: String?,
        unitPrice: Double?,
        totalAmount: Double?,
        notes: String?
    ) {
        val current = _uiState.value as? EventPurchasesUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.createEventExtraCost(
                eventId,
                CreateEventExtraCostRequestDto(
                    category = category,
                    label = label,
                    quantity = quantity,
                    unit = unit,
                    unitPrice = unitPrice,
                    totalAmount = totalAmount,
                    notes = notes
                )
            )
                .onSuccess { finance ->
                    (_uiState.value as? EventPurchasesUiState.Success)?.let {
                        _uiState.value = it.copy(finance = finance, isWorking = false)
                    }
                }
                .onFailure { error ->
                    (_uiState.value as? EventPurchasesUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko pridėti išlaidų.")
                    }
                }
        }
    }

    fun deleteExtraCost(eventId: String, costId: String) {
        val current = _uiState.value as? EventPurchasesUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isWorking = true, error = null)
            eventRepository.deleteEventExtraCost(eventId, costId)
                .onSuccess { finance ->
                    (_uiState.value as? EventPurchasesUiState.Success)?.let {
                        _uiState.value = it.copy(finance = finance, isWorking = false)
                    }
                }
                .onFailure { error ->
                    (_uiState.value as? EventPurchasesUiState.Success)?.let {
                        _uiState.value = it.copy(isWorking = false, error = error.message ?: "Nepavyko pašalinti išlaidų.")
                    }
                }
        }
    }

    fun clearError() {
        (_uiState.value as? EventPurchasesUiState.Success)?.let { _uiState.value = it.copy(error = null) }
    }
}
