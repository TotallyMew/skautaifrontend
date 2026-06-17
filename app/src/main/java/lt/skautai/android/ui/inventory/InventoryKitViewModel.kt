package lt.skautai.android.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.CreateInventoryKitRequestDto
import lt.skautai.android.data.remote.InventoryKitDto
import lt.skautai.android.data.remote.InventoryKitItemRequestDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.LocationDto
import lt.skautai.android.data.repository.InventoryKitRepository
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.data.repository.LocationRepository

sealed interface InventoryKitUiState {
    object Loading : InventoryKitUiState
    data class Success(
        val kits: List<InventoryKitDto>,
        val selectedKit: InventoryKitDto?,
        val availableItems: List<ItemDto> = emptyList(),
        val locations: List<LocationDto> = emptyList(),
        val isCreating: Boolean = false,
        val actionError: String? = null
    ) : InventoryKitUiState
    data class Error(val message: String) : InventoryKitUiState
}

@HiltViewModel
class InventoryKitViewModel @Inject constructor(
    private val repository: InventoryKitRepository,
    private val itemRepository: ItemRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<InventoryKitUiState>(InventoryKitUiState.Loading)
    val uiState: StateFlow<InventoryKitUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val existing = _uiState.value as? InventoryKitUiState.Success
            if (existing == null) {
                _uiState.value = InventoryKitUiState.Loading
            }
            repository.getKits()
                .onSuccess { list ->
                    val assignedItemIds = list.kits
                        .filter { it.status == "ACTIVE" }
                        .flatMap { kit -> kit.items.map { it.itemId } }
                        .toSet()
                    val cachedItems = itemRepository.getCachedItems(status = "ACTIVE")
                    val cachedLocations = locationRepository.getCachedLocations()
                    if (existing != null && (cachedItems.isNotEmpty() || cachedLocations.isNotEmpty())) {
                        _uiState.value = existing.copy(
                            kits = list.kits,
                            selectedKit = existing.selectedKit?.let { selected -> list.kits.firstOrNull { it.id == selected.id } } ?: list.kits.firstOrNull(),
                            availableItems = cachedItems.filter { item -> item.id !in assignedItemIds && item.kitId == null },
                            locations = if (cachedLocations.isNotEmpty()) cachedLocations else existing.locations,
                            isCreating = false
                        )
                    }
                    val items = itemRepository.getItems(status = "ACTIVE").getOrNull().orEmpty().ifEmpty { cachedItems }
                    val locations = locationRepository.getLocations().getOrNull().orEmpty().ifEmpty { cachedLocations }
                    _uiState.value = InventoryKitUiState.Success(
                        kits = list.kits,
                        selectedKit = list.kits.firstOrNull(),
                        availableItems = items.filter { item -> item.id !in assignedItemIds && item.kitId == null },
                        locations = locations
                    )
                }
                .onFailure { error ->
                    _uiState.value = InventoryKitUiState.Error(error.message ?: "Nepavyko gauti komplektų")
                }
        }
    }

    fun selectKit(id: String) {
        val current = _uiState.value as? InventoryKitUiState.Success ?: return
        _uiState.value = current.copy(selectedKit = current.kits.firstOrNull { it.id == id })
    }

    fun createKit(
        name: String,
        description: String?,
        locationId: String?,
        temporaryStorageLabel: String?,
        selectedItems: Map<String, Int>
    ) {
        val current = _uiState.value as? InventoryKitUiState.Success ?: return
        if (current.isCreating) return
        if (name.isBlank()) {
            _uiState.value = current.copy(actionError = "Įvesk komplekto pavadinimą")
            return
        }

        val selectedItemDtos = current.availableItems.filter { it.id in selectedItems.keys }
        val custodians = selectedItemDtos.map { it.custodianId }.distinct()
        if (custodians.size > 1) {
            _uiState.value = current.copy(actionError = "Komplekte gali būti tik vienos saugojimo apimties daiktai")
            return
        }

        viewModelScope.launch {
            _uiState.value = current.copy(isCreating = true, actionError = null)
            val request = CreateInventoryKitRequestDto(
                name = name.trim(),
                description = description?.trim()?.ifBlank { null },
                custodianId = custodians.firstOrNull(),
                locationId = locationId,
                temporaryStorageLabel = temporaryStorageLabel?.trim()?.ifBlank { null },
                items = selectedItems.map { (itemId, quantity) ->
                    InventoryKitItemRequestDto(itemId = itemId, quantity = quantity.coerceAtLeast(1))
                }
            )
            repository.createKit(request)
                .onSuccess { created ->
                    repository.getKits()
                        .onSuccess { list ->
                            _uiState.value = InventoryKitUiState.Success(
                                kits = list.kits,
                                selectedKit = list.kits.firstOrNull { it.id == created.id } ?: created,
                                availableItems = current.availableItems.filter { it.id !in selectedItems.keys },
                                locations = current.locations
                            )
                        }
                        .onFailure { error ->
                            _uiState.value = current.copy(
                                isCreating = false,
                                actionError = error.message ?: "Komplektas sukurtas, bet sąrašo atnaujinti nepavyko"
                            )
                        }
                }
                .onFailure { error ->
                    _uiState.value = current.copy(isCreating = false, actionError = error.message ?: "Nepavyko sukurti komplekto")
                }
        }
    }

    fun clearActionError() {
        val current = _uiState.value as? InventoryKitUiState.Success ?: return
        _uiState.value = current.copy(actionError = null)
    }
}
