package lt.skautai.android.ui.reservations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.CreateReservationItemRequestDto
import lt.skautai.android.data.remote.CreateReservationRequestDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.LocationDto
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.data.repository.LocationRepository
import lt.skautai.android.data.repository.ReservationRepository
import lt.skautai.android.util.TokenManager
import javax.inject.Inject

data class ReservationDraftItem(
    val itemId: String,
    val itemName: String,
    val quantity: Int
)

data class ReservationCreateUiState(
    val isLoadingItems: Boolean = true,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val isLoadingAvailability: Boolean = false,
    val error: String? = null,
    val items: List<ItemDto> = emptyList(),
    val selectedItems: List<ReservationDraftItem> = emptyList(),
    val title: String = "",
    val searchQuery: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val notes: String = "",
    val availabilityByItemId: Map<String, Int> = emptyMap(),
    val activeOrgUnitId: String? = null,
    val locations: List<LocationDto> = emptyList(),
    val pickupLocationId: String? = null,
    val returnLocationId: String? = null
)

@HiltViewModel
class ReservationCreateViewModel @Inject constructor(
    private val reservationRepository: ReservationRepository,
    private val itemRepository: ItemRepository,
    private val locationRepository: LocationRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReservationCreateUiState())
    val uiState: StateFlow<ReservationCreateUiState> = _uiState.asStateFlow()

    init {
        loadItems()
    }

    private fun loadItems() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingItems = true, error = null)
            val activeOrgUnitId = tokenManager.activeOrgUnitId.first()
            itemRepository.getItems(status = "ACTIVE")
                .onSuccess { items ->
                    val locations = locationRepository.getLocations().getOrDefault(emptyList())
                    _uiState.value = _uiState.value.copy(
                        isLoadingItems = false,
                        activeOrgUnitId = activeOrgUnitId,
                        locations = locations,
                        items = items
                            .filter { item -> item.custodianId == null || item.custodianId == activeOrgUnitId }
                            .sortedBy { it.name.lowercase() }
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingItems = false,
                        error = error.message ?: "Klaida gaunant daiktus"
                    )
                }
        }
    }

    fun onSearchQueryChange(value: String) {
        _uiState.value = _uiState.value.copy(searchQuery = value)
    }

    fun onTitleChange(value: String) {
        _uiState.value = _uiState.value.copy(title = value)
    }

    fun onStartDateChange(value: String) {
        _uiState.value = _uiState.value.copy(startDate = value)
        refreshAvailabilityIfPossible()
    }

    fun onEndDateChange(value: String) {
        _uiState.value = _uiState.value.copy(endDate = value)
        refreshAvailabilityIfPossible()
    }

    fun onNotesChange(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }

    fun onPickupLocationChange(value: String?) {
        _uiState.value = _uiState.value.copy(pickupLocationId = value)
    }

    fun onReturnLocationChange(value: String?) {
        _uiState.value = _uiState.value.copy(returnLocationId = value)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun addItem(itemId: String) {
        val state = _uiState.value
        if (!datesAreReady(state)) {
            _uiState.value = state.copy(error = "Pirma pasirinkite rezervacijos datas")
            return
        }

        val item = state.items.find { it.id == itemId }
            ?: run {
                _uiState.value = state.copy(error = "Daiktas nerastas")
                return
            }

        val remaining = remainingAvailability(itemId)
        if (remaining < 1) {
            _uiState.value = state.copy(error = "Siam laikotarpiui daugiau vienetu nebera")
            return
        }

        val updatedItems = state.selectedItems.toMutableList()
        val existingIndex = updatedItems.indexOfFirst { it.itemId == itemId }
        if (existingIndex >= 0) {
            val existing = updatedItems[existingIndex]
            updatedItems[existingIndex] = existing.copy(quantity = existing.quantity + 1)
        } else {
            updatedItems += ReservationDraftItem(
                itemId = item.id,
                itemName = item.name,
                quantity = 1
            )
        }

        _uiState.value = state.copy(selectedItems = updatedItems, error = null)
    }

    fun increaseItem(itemId: String) {
        addItem(itemId)
    }

    fun decreaseItem(itemId: String) {
        val updatedItems = _uiState.value.selectedItems.toMutableList()
        val existingIndex = updatedItems.indexOfFirst { it.itemId == itemId }
        if (existingIndex == -1) return

        val existing = updatedItems[existingIndex]
        if (existing.quantity <= 1) {
            updatedItems.removeAt(existingIndex)
        } else {
            updatedItems[existingIndex] = existing.copy(quantity = existing.quantity - 1)
        }

        _uiState.value = _uiState.value.copy(selectedItems = updatedItems)
    }

    fun createReservation() {
        val state = _uiState.value

        if (state.selectedItems.isEmpty()) {
            _uiState.value = state.copy(error = "Pridekite bent viena daikta")
            return
        }
        if (state.title.isBlank()) {
            _uiState.value = state.copy(error = "Iveskite rezervacijos pavadinima")
            return
        }
        if (state.startDate.isBlank()) {
            _uiState.value = state.copy(error = "Pasirinkite pradzios data")
            return
        }
        if (state.endDate.isBlank()) {
            _uiState.value = state.copy(error = "Pasirinkite pabaigos data")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)

            val result = reservationRepository.createReservation(
                CreateReservationRequestDto(
                    title = state.title.trim(),
                    items = state.selectedItems.map { draftItem ->
                        CreateReservationItemRequestDto(
                            itemId = draftItem.itemId,
                            quantity = draftItem.quantity
                        )
                    },
                    startDate = state.startDate,
                    endDate = state.endDate,
                    requestingUnitId = selectedRequestingUnitId(state),
                    pickupLocationId = state.pickupLocationId,
                    returnLocationId = state.returnLocationId,
                    notes = state.notes.ifBlank { null }
                )
            )

            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = result.exceptionOrNull()?.message ?: "Klaida kuriant rezervacija"
                )
                refreshAvailabilityIfPossible()
                return@launch
            }

            _uiState.value = _uiState.value.copy(isSaving = false, isSuccess = true)
        }
    }

    fun selectedQuantityFor(itemId: String): Int =
        _uiState.value.selectedItems.firstOrNull { it.itemId == itemId }?.quantity ?: 0

    fun remainingAvailability(itemId: String): Int {
        val baseAvailable = _uiState.value.availabilityByItemId[itemId]
            ?: _uiState.value.items.find { it.id == itemId }?.quantity
            ?: 0
        return (baseAvailable - selectedQuantityFor(itemId)).coerceAtLeast(0)
    }

    fun totalAvailability(itemId: String): Int =
        _uiState.value.availabilityByItemId[itemId]
            ?: _uiState.value.items.find { it.id == itemId }?.quantity
            ?: 0

    private fun refreshAvailabilityIfPossible() {
        val state = _uiState.value
        if (!datesAreReady(state)) {
            _uiState.value = state.copy(availabilityByItemId = emptyMap())
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAvailability = true, error = null)
            reservationRepository.getAvailability(
                startDate = _uiState.value.startDate,
                endDate = _uiState.value.endDate
            ).onSuccess { availability ->
                val availabilityMap = availability.items.associate { it.itemId to it.availableQuantity }
                val stillValidSelections = _uiState.value.selectedItems.mapNotNull { draftItem ->
                    val allowed = availabilityMap[draftItem.itemId]
                        ?: _uiState.value.items.find { it.id == draftItem.itemId }?.quantity
                        ?: 0
                    if (allowed <= 0) null else draftItem.copy(quantity = minOf(draftItem.quantity, allowed))
                }

                _uiState.value = _uiState.value.copy(
                    isLoadingAvailability = false,
                    availabilityByItemId = availabilityMap,
                    selectedItems = stillValidSelections
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingAvailability = false,
                    error = error.message ?: "Klaida gaunant prieinama kieki"
                )
            }
        }
    }

    private fun datesAreReady(state: ReservationCreateUiState): Boolean =
        state.startDate.isNotBlank() && state.endDate.isNotBlank()

    private fun selectedRequestingUnitId(state: ReservationCreateUiState): String? {
        val selectedIds = state.selectedItems.map { it.itemId }.toSet()
        return state.items
            .firstOrNull { it.id in selectedIds && it.custodianId != null }
            ?.custodianId
            ?: state.activeOrgUnitId
    }
}
