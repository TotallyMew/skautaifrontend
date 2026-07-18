package lt.skautai.android.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.DirectItemLoanDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.LocationDto
import lt.skautai.android.data.remote.ReturnDirectItemLoanRequestDto
import lt.skautai.android.data.remote.UpdateItemRequestDto
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.data.repository.LocationRepository

@HiltViewModel
class InventoryQrScannerViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _isResolving = MutableStateFlow(false)
    val isResolving: StateFlow<Boolean> = _isResolving.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _scannedItem = MutableStateFlow<ItemDto?>(null)
    val scannedItem: StateFlow<ItemDto?> = _scannedItem.asStateFlow()

    private val _directLoans = MutableStateFlow<List<DirectItemLoanDto>>(emptyList())
    val directLoans: StateFlow<List<DirectItemLoanDto>> = _directLoans.asStateFlow()

    private val _locations = MutableStateFlow<List<LocationDto>>(emptyList())
    val locations: StateFlow<List<LocationDto>> = _locations.asStateFlow()

    init {
        viewModelScope.launch {
            locationRepository.getLocations()
                .onSuccess { locations ->
                    _locations.value = locations.filter { it.isLeafSelectable }
                }
        }
    }

    fun resolveToken(token: String, onResolved: (String) -> Unit) {
        if (_isResolving.value) return

        viewModelScope.launch {
            _isResolving.value = true
            itemRepository.resolveQrToken(token)
                .onSuccess { itemId ->
                    _message.value = null
                    itemRepository.getFreshItem(itemId)
                        .onSuccess { item ->
                            _scannedItem.value = item
                            loadDirectLoans(item.id)
                        }
                        .onFailure {
                            onResolved(itemId)
                        }
                }
                .onFailure {
                    _message.value = it.message ?: "Nepavyko atpazinti kodo"
                }
            _isResolving.value = false
        }
    }

    fun openScannedItem(onResolved: (String) -> Unit) {
        _scannedItem.value?.id?.let(onResolved)
    }

    fun updateCondition(condition: String) {
        val item = _scannedItem.value ?: return
        viewModelScope.launch {
            _isResolving.value = true
            itemRepository.updateItem(item.id, UpdateItemRequestDto(condition = condition))
                .onSuccess {
                    _scannedItem.value = it
                    _message.value = "Bukle atnaujinta."
                }
                .onFailure { _message.value = it.message ?: "Nepavyko atnaujinti bukles" }
            _isResolving.value = false
        }
    }

    fun updateLocation(locationId: String?, temporaryLabel: String?) {
        val item = _scannedItem.value ?: return
        viewModelScope.launch {
            _isResolving.value = true
            itemRepository.updateItem(
                item.id,
                UpdateItemRequestDto(
                    locationId = locationId,
                    temporaryStorageLabel = temporaryLabel?.ifBlank { null },
                    clearLocationId = locationId == null
                )
            ).onSuccess {
                _scannedItem.value = it
                _message.value = "Vieta atnaujinta."
            }.onFailure {
                _message.value = it.message ?: "Nepavyko pakeisti vietos"
            }
            _isResolving.value = false
        }
    }

    fun addIncidentNote(note: String) {
        val item = _scannedItem.value ?: return
        val normalized = note.trim()
        if (normalized.isBlank()) {
            _message.value = "Iveskite pastaba."
            return
        }
        val nextNotes = listOfNotNull(item.notes?.takeIf { it.isNotBlank() }, normalized)
            .joinToString("\n")
        viewModelScope.launch {
            _isResolving.value = true
            itemRepository.updateItem(item.id, UpdateItemRequestDto(notes = nextNotes))
                .onSuccess {
                    _scannedItem.value = it
                    _message.value = "Pastaba prideta."
                }
                .onFailure { _message.value = it.message ?: "Nepavyko prideti pastabos" }
            _isResolving.value = false
        }
    }

    fun writeOff(reason: String) {
        val item = _scannedItem.value ?: return
        viewModelScope.launch {
            _isResolving.value = true
            itemRepository.writeOffItem(item.id, reason.ifBlank { "Nurasymas po skenavimo" })
                .onSuccess {
                    _scannedItem.value = it
                    _message.value = "Daiktas nurasytas."
                }
                .onFailure { _message.value = it.message ?: "Nepavyko nurasyti daikto" }
            _isResolving.value = false
        }
    }

    fun returnDirectLoan(loan: DirectItemLoanDto, quantity: Int) {
        viewModelScope.launch {
            _isResolving.value = true
            itemRepository.returnDirectItemLoan(
                loan.itemId,
                loan.id,
                ReturnDirectItemLoanRequestDto(quantity = quantity)
            ).onSuccess {
                _message.value = "Grazinimas pazymetas."
                loadDirectLoans(loan.itemId)
            }.onFailure {
                _message.value = it.message ?: "Nepavyko grazinti daikto"
            }
            _isResolving.value = false
        }
    }

    fun clearScannedItem() {
        _scannedItem.value = null
        _directLoans.value = emptyList()
    }

    fun showMessage(message: String) {
        _message.value = message
    }

    fun clearMessage() {
        _message.value = null
    }

    private suspend fun loadDirectLoans(itemId: String) {
        itemRepository.getDirectItemLoans(itemId)
            .onSuccess { response ->
                _directLoans.value = response.loans.filter { it.outstandingQuantity > 0 }
            }
    }
}
