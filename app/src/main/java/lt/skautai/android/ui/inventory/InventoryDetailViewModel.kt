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

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    private val _sharedRequestCreated = MutableStateFlow(false)
    val sharedRequestCreated: StateFlow<Boolean> = _sharedRequestCreated.asStateFlow()

    private val _isCreatingSharedRequest = MutableStateFlow(false)
    val isCreatingSharedRequest: StateFlow<Boolean> = _isCreatingSharedRequest.asStateFlow()
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
            itemRepository.deleteItem(itemId)
                .onSuccess {
                    _deleted.value = true
                }
                .onFailure { error ->
                    _deleteError.value = error.message ?: "Nepavyko ištrinti daikto"
                }
        }
    }

    fun updateStatus(itemId: String, status: String) {
        viewModelScope.launch {
            itemRepository.updateItem(itemId, UpdateItemRequestDto(status = status))
                .onSuccess { item ->
                    _uiState.value = InventoryDetailUiState.Success(item)
                    loadItemReservations(itemId)
                }
                .onFailure { error ->
                    _deleteError.value = error.message ?: "Nepavyko pakeisti busenos"
                }
        }
    }

    fun requestSharedItemForActiveUnit(itemId: String) {
        viewModelScope.launch {
            val activeUnitId = tokenManager.activeOrgUnitId.first()
            if (activeUnitId.isNullOrBlank()) {
                _deleteError.value = "Pasirink aktyvu vieneta, kuriam nori gauti daikta"
                return@launch
            }

            _isCreatingSharedRequest.value = true
            requestRepository.createRequest(
                CreateBendrasRequestDto(
                    requestingUnitId = activeUnitId,
                    neededByDate = null,
                    notes = "Prasymas paimti daikta is bendro tunto inventoriaus",
                    items = listOf(CreateBendrasRequestItemDto(itemId = itemId, quantity = 1))
                )
            ).onSuccess {
                _sharedRequestCreated.value = true
            }.onFailure { error ->
                _deleteError.value = error.message ?: "Nepavyko sukurti paemimo prasymo"
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

    fun onDeleteErrorShown() {
        _deleteError.value = null
    }

    fun onSharedRequestMessageShown() {
        _sharedRequestCreated.value = false
    }
}
