package lt.skautai.android.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.CreateBendrasRequestDto
import lt.skautai.android.data.remote.CreateBendrasRequestItemDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.ReservationDto
import lt.skautai.android.data.remote.UpdateItemRequestDto
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.data.repository.RequestRepository
import lt.skautai.android.data.repository.ReservationRepository
import lt.skautai.android.util.TokenManager
import javax.inject.Inject

sealed interface InventoryDetailUiState {
    object Loading : InventoryDetailUiState
    data class Success(
        val item: ItemDto,
        val reservations: List<ReservationDto> = emptyList()
    ) : InventoryDetailUiState
    data class Error(val message: String) : InventoryDetailUiState
}

@HiltViewModel
class InventoryDetailViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val requestRepository: RequestRepository,
    private val reservationRepository: ReservationRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<InventoryDetailUiState>(InventoryDetailUiState.Loading)
    val uiState: StateFlow<InventoryDetailUiState> = _uiState.asStateFlow()

    val permissions: StateFlow<Set<String>> = tokenManager.permissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val activeOrgUnitId: StateFlow<String?> = tokenManager.activeOrgUnitId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _sharedRequestCreated = MutableStateFlow(false)
    val sharedRequestCreated: StateFlow<Boolean> = _sharedRequestCreated.asStateFlow()

    private val _shareMessage = MutableStateFlow<String?>(null)
    val shareMessage: StateFlow<String?> = _shareMessage.asStateFlow()

    private val _isCreatingSharedRequest = MutableStateFlow(false)
    val isCreatingSharedRequest: StateFlow<Boolean> = _isCreatingSharedRequest.asStateFlow()

    private val _isUpdatingStatus = MutableStateFlow(false)
    val isUpdatingStatus: StateFlow<Boolean> = _isUpdatingStatus.asStateFlow()

    private var itemObserverJob: Job? = null

    fun loadItem(itemId: String) {
        itemObserverJob?.cancel()
        itemObserverJob = viewModelScope.launch {
            itemRepository.observeItem(itemId).collect { item ->
                if (item != null) {
                    val reservations = (_uiState.value as? InventoryDetailUiState.Success)?.reservations.orEmpty()
                    _uiState.value = InventoryDetailUiState.Success(item, reservations)
                }
            }
        }

        viewModelScope.launch {
            if (_uiState.value !is InventoryDetailUiState.Success) {
                _uiState.value = InventoryDetailUiState.Loading
            }
            itemRepository.refreshItem(itemId)
                .onSuccess {
                    loadItemReservations(itemId)
                }
                .onFailure { error ->
                    if (_uiState.value !is InventoryDetailUiState.Success) {
                        itemRepository.getItem(itemId)
                            .onFailure {
                                _uiState.value = InventoryDetailUiState.Error(
                                    error.message ?: "Nepavyko gauti daikto"
                                )
                            }
                    }
                }
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            if (_deleted.value) return@launch

            itemRepository.deleteItem(itemId)
                .onSuccess {
                    _deleted.value = true
                }
                .onFailure { error ->
                    _actionError.value = error.message ?: "Nepavyko istrinti daikto"
                }
        }
    }

    fun updateStatus(itemId: String, status: String) {
        viewModelScope.launch {
            if (_deleted.value || _isUpdatingStatus.value) return@launch

            _isUpdatingStatus.value = true
            itemRepository.updateItem(itemId, UpdateItemRequestDto(status = status))
                .onSuccess { item ->
                    val reservations = (_uiState.value as? InventoryDetailUiState.Success)?.reservations.orEmpty()
                    _uiState.value = InventoryDetailUiState.Success(item, reservations)
                    loadItemReservations(itemId)
                }
                .onFailure { error ->
                    _actionError.value = error.message ?: "Nepavyko pakeisti busenos"
                }
            _isUpdatingStatus.value = false
        }
    }

    fun requestSharedItemForActiveUnit(itemId: String) {
        viewModelScope.launch {
            val activeUnitId = tokenManager.activeOrgUnitId.first()
            if (activeUnitId.isNullOrBlank()) {
                _actionError.value = "Pasirink aktyvų vienetą, kuriam nori gauti daiktą"
                return@launch
            }

            if (_isCreatingSharedRequest.value) return@launch

            _isCreatingSharedRequest.value = true
            requestRepository.createRequest(
                CreateBendrasRequestDto(
                    requestingUnitId = activeUnitId,
                    neededByDate = null,
                    notes = "Prašymas paimti daiktą iš bendro tunto inventoriaus",
                    items = listOf(CreateBendrasRequestItemDto(itemId = itemId, quantity = 1))
                )
            ).onSuccess {
                _sharedRequestCreated.value = true
            }.onFailure { error ->
                _actionError.value = error.message ?: "Nepavyko sukurti paėmimo prašymo"
            }
            _isCreatingSharedRequest.value = false
        }
    }

    private fun loadItemReservations(itemId: String) {
        viewModelScope.launch {
            reservationRepository.getReservations(itemId = itemId)
                .onSuccess { response ->
                    val reservations = response.reservations
                        .filter { it.status in listOf("APPROVED", "ACTIVE") }
                        .sortedWith(compareBy<ReservationDto> { it.startDate }.thenBy { it.endDate })
                    val current = _uiState.value as? InventoryDetailUiState.Success ?: return@onSuccess
                    _uiState.value = current.copy(reservations = reservations)
                }
        }
    }

    fun onActionErrorShown() {
        _actionError.value = null
    }

    fun onSharedRequestMessageShown() {
        _sharedRequestCreated.value = false
    }

    fun onQrPdfShared() {
        _shareMessage.value = "QR PDF paruostas bendrinimui."
    }

    fun onQrPdfShareFailed(message: String) {
        _shareMessage.value = message
    }

    fun onShareMessageShown() {
        _shareMessage.value = null
    }
}
