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
import lt.skautai.android.data.remote.ItemAssignmentDto
import lt.skautai.android.data.remote.ItemConditionLogDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.ItemHistoryDto
import lt.skautai.android.data.remote.ItemTransferDto
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.remote.ReturnItemToSharedRequestDto
import lt.skautai.android.data.remote.ReservationDto
import lt.skautai.android.data.remote.RestockItemRequestDto
import lt.skautai.android.data.remote.TransferItemToUnitRequestDto
import lt.skautai.android.data.remote.UpdateItemRequestDto
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.data.repository.OrganizationalUnitRepository
import lt.skautai.android.data.repository.RequestRepository
import lt.skautai.android.data.repository.ReservationRepository
import lt.skautai.android.util.TokenManager
import lt.skautai.android.util.canManageSharedInventory
import javax.inject.Inject

sealed interface InventoryDetailUiState {
    object Loading : InventoryDetailUiState
    data class Success(
        val item: ItemDto,
        val reservations: List<ReservationDto> = emptyList(),
        val assignments: List<ItemAssignmentDto> = emptyList(),
        val conditionLog: List<ItemConditionLogDto> = emptyList(),
        val itemHistory: List<ItemHistoryDto> = emptyList(),
        val transfers: List<ItemTransferDto> = emptyList()
    ) : InventoryDetailUiState
    data class Error(val message: String) : InventoryDetailUiState
}

@HiltViewModel
class InventoryDetailViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val requestRepository: RequestRepository,
    private val reservationRepository: ReservationRepository,
    private val orgUnitRepository: OrganizationalUnitRepository,
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

    private val _orgUnits = MutableStateFlow<List<OrganizationalUnitDto>>(emptyList())
    val orgUnits: StateFlow<List<OrganizationalUnitDto>> = _orgUnits.asStateFlow()

    private val _isTransferring = MutableStateFlow(false)
    val isTransferring: StateFlow<Boolean> = _isTransferring.asStateFlow()

    private var itemObserverJob: Job? = null

    fun loadItem(itemId: String) {
        itemObserverJob?.cancel()
        itemObserverJob = viewModelScope.launch {
            itemRepository.observeItem(itemId).collect { item ->
                if (item != null) {
                    val current = _uiState.value as? InventoryDetailUiState.Success
                    _uiState.value = InventoryDetailUiState.Success(
                        item = item,
                        reservations = current?.reservations.orEmpty(),
                        assignments = current?.assignments.orEmpty(),
                        conditionLog = current?.conditionLog.orEmpty(),
                        itemHistory = current?.itemHistory.orEmpty(),
                        transfers = current?.transfers.orEmpty()
                    )
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
                    loadItemHistory(itemId)
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

        viewModelScope.launch {
            orgUnitRepository.getUnits()
                .onSuccess { units -> _orgUnits.value = units }
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
            _actionError.value = error.message ?: "Nepavyko ištrinti daikto"
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
                    val current = _uiState.value as? InventoryDetailUiState.Success
                    _uiState.value = InventoryDetailUiState.Success(
                        item = item,
                        reservations = reservations,
                        assignments = current?.assignments.orEmpty(),
                        conditionLog = current?.conditionLog.orEmpty(),
                        itemHistory = current?.itemHistory.orEmpty(),
                        transfers = current?.transfers.orEmpty()
                    )
                    loadItemReservations(itemId)
                    loadItemHistory(itemId)
                }
                .onFailure { error ->
                    _actionError.value = error.message ?: "Nepavyko pakeisti būsenos"
                }
            _isUpdatingStatus.value = false
        }
    }

    fun transferToUnit(itemId: String, targetUnitId: String, quantity: Int, notes: String?) {
        viewModelScope.launch {
            if (_isTransferring.value) return@launch
            _isTransferring.value = true
            itemRepository.transferItemToUnit(
                itemId,
                TransferItemToUnitRequestDto(
                    targetUnitId = targetUnitId,
                    quantity = quantity,
                    notes = notes?.ifBlank { null }
                )
            ).onSuccess {
                _shareMessage.value = "Daiktas perduotas vienetui."
                loadItem(itemId)
            }.onFailure { error ->
                _actionError.value = error.message ?: "Nepavyko perduoti daikto"
            }
            _isTransferring.value = false
        }
    }

    fun returnToShared(itemId: String, quantity: Int, notes: String?) {
        viewModelScope.launch {
            if (_isTransferring.value) return@launch
            _isTransferring.value = true
            itemRepository.returnItemToShared(
                itemId,
                ReturnItemToSharedRequestDto(
                    quantity = quantity,
                    notes = notes?.ifBlank { null }
                )
            ).onSuccess {
                _shareMessage.value = "Daiktas grąžintas į bendrą inventorių."
                loadItem(itemId)
            }.onFailure { error ->
                _actionError.value = error.message ?: "Nepavyko grąžinti daikto"
            }
            _isTransferring.value = false
        }
    }

    fun restockItem(itemId: String, quantity: Int, purchaseDate: String?, purchasePrice: Double?, notes: String?) {
        viewModelScope.launch {
            if (_isTransferring.value) return@launch
            _isTransferring.value = true
            itemRepository.restockItem(
                itemId,
                RestockItemRequestDto(
                    quantity = quantity,
                    purchaseDate = purchaseDate?.ifBlank { null },
                    purchasePrice = purchasePrice,
                    notes = notes?.ifBlank { null }
                )
            ).onSuccess {
                _shareMessage.value = "Daiktas papildytas."
                loadItem(itemId)
            }.onFailure { error ->
                _actionError.value = error.message ?: "Nepavyko papildyti daikto"
            }
            _isTransferring.value = false
        }
    }

    fun requestSharedItemForActiveUnit(itemId: String, quantity: Int) {
        viewModelScope.launch {
            val activeUnitId = tokenManager.activeOrgUnitId.first()
            if (activeUnitId.isNullOrBlank()) {
                _actionError.value = "Pasirink aktyvų vienetą, kuriam nori gauti daiktą"
                return@launch
            }
            if (tokenManager.permissions.first().canManageSharedInventory()) {
                _actionError.value = "Bendro inventoriaus valdytojai daiktus vienetui perduoda tiesiogiai, be prašymo."
                return@launch
            }

            if (quantity < 1) {
                _actionError.value = "Kiekis turi būti teigiamas skaičius"
                return@launch
            }

            if (_isCreatingSharedRequest.value) return@launch

            _isCreatingSharedRequest.value = true
            requestRepository.createRequest(
                CreateBendrasRequestDto(
                    requestingUnitId = activeUnitId,
                    neededByDate = null,
                    notes = "Prašymas paimti daiktą iš bendro tunto inventoriaus",
                    items = listOf(CreateBendrasRequestItemDto(itemId = itemId, quantity = quantity))
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

    private fun loadItemHistory(itemId: String) {
        viewModelScope.launch {
            val assignments = itemRepository.getItemAssignments(itemId).getOrNull()
            val conditionLog = itemRepository.getItemConditionLog(itemId).getOrNull()
            val itemHistory = itemRepository.getItemHistory(itemId).getOrNull()
            val transfers = itemRepository.getItemTransfers(itemId).getOrNull()
            val current = _uiState.value as? InventoryDetailUiState.Success ?: return@launch
            _uiState.value = current.copy(
                assignments = assignments ?: current.assignments,
                conditionLog = conditionLog ?: current.conditionLog,
                itemHistory = itemHistory ?: current.itemHistory,
                transfers = transfers ?: current.transfers
            )
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
