package lt.skautai.android.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.CreateItemRequestDto
import lt.skautai.android.data.remote.LocationDto
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.remote.UpdateItemRequestDto
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.data.repository.LocationRepository
import lt.skautai.android.data.repository.OrganizationalUnitRepository
import lt.skautai.android.util.TokenManager

data class InventoryAddEditUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isCreatingLocation: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val name: String = "",
    val description: String = "",
    val type: String = "COLLECTIVE",
    val category: String = "CAMPING",
    val condition: String = "GOOD",
    val custodianId: String? = null,
    val origin: String = "UNIT_ACQUIRED",
    val quantity: String = "1",
    val notes: String = "",
    val purchaseDate: String = "",
    val purchasePrice: String = "",
    val temporaryStorageLabel: String = "",
    val orgUnits: List<OrganizationalUnitDto> = emptyList(),
    val selectedOrgUnitId: String = "",
    val locations: List<LocationDto> = emptyList(),
    val selectedLocationId: String = "",
    val tuntasId: String = "",
    val mode: String = "SHARED"
)

@HiltViewModel
class InventoryAddEditViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val orgUnitRepository: OrganizationalUnitRepository,
    private val locationRepository: LocationRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryAddEditUiState())
    val uiState: StateFlow<InventoryAddEditUiState> = _uiState.asStateFlow()

    fun init(itemId: String?, mode: String?) {
        viewModelScope.launch {
            val tuntasId = tokenManager.activeTuntasId.first() ?: ""
            val activeOrgUnitId = tokenManager.activeOrgUnitId.first().orEmpty()
            val resolvedMode = mode ?: "SHARED"
            _uiState.value = _uiState.value.copy(
                tuntasId = tuntasId,
                isLoading = itemId != null,
                mode = resolvedMode,
                type = defaultTypeForMode(resolvedMode),
                origin = defaultOriginForMode(resolvedMode),
                selectedOrgUnitId = if (resolvedMode == "UNIT_OWN") activeOrgUnitId else "",
                custodianId = if (resolvedMode == "UNIT_OWN") activeOrgUnitId.ifBlank { null } else null
            )

            orgUnitRepository.getUnits()
                .onSuccess { units ->
                    _uiState.value = _uiState.value.copy(orgUnits = units)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message ?: "Nepavyko gauti vienetu")
                }

            locationRepository.getLocations()
                .onSuccess { locations ->
                    _uiState.value = _uiState.value.copy(locations = locations)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message ?: "Nepavyko gauti lokaciju")
                }

            if (itemId != null) {
                itemRepository.getItem(itemId)
                    .onSuccess { item ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            name = item.name,
                            description = item.description ?: "",
                            type = item.type,
                            category = item.category,
                            condition = item.condition,
                            custodianId = item.custodianId,
                            origin = item.origin,
                            quantity = item.quantity.toString(),
                            notes = item.notes ?: "",
                            purchaseDate = item.purchaseDate ?: "",
                            purchasePrice = item.purchasePrice?.toString() ?: "",
                            temporaryStorageLabel = item.temporaryStorageLabel ?: "",
                            selectedOrgUnitId = item.custodianId ?: "",
                            selectedLocationId = item.locationId ?: ""
                        )
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Nepavyko gauti daikto"
                        )
                    }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun onNameChange(value: String) { _uiState.value = _uiState.value.copy(name = value) }
    fun onDescriptionChange(value: String) { _uiState.value = _uiState.value.copy(description = value) }
    fun onNotesChange(value: String) { _uiState.value = _uiState.value.copy(notes = value) }
    fun onQuantityChange(value: String) { _uiState.value = _uiState.value.copy(quantity = value) }
    fun onPurchaseDateChange(value: String) { _uiState.value = _uiState.value.copy(purchaseDate = value) }
    fun onPurchasePriceChange(value: String) { _uiState.value = _uiState.value.copy(purchasePrice = value) }
    fun onTypeChange(value: String) { _uiState.value = _uiState.value.copy(type = value) }
    fun onCategoryChange(value: String) { _uiState.value = _uiState.value.copy(category = value) }
    fun onConditionChange(value: String) { _uiState.value = _uiState.value.copy(condition = value) }
    fun onOriginChange(value: String) { _uiState.value = _uiState.value.copy(origin = value) }
    fun onTemporaryStorageLabelChange(value: String) { _uiState.value = _uiState.value.copy(temporaryStorageLabel = value) }

    fun onOrgUnitChange(unitId: String?) {
        _uiState.value = _uiState.value.copy(
            selectedOrgUnitId = unitId ?: "",
            custodianId = unitId
        )
    }

    fun onLocationChange(locationId: String?) {
        _uiState.value = _uiState.value.copy(selectedLocationId = locationId ?: "")
    }

    fun showValidationError(message: String) {
        _uiState.value = _uiState.value.copy(error = message)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun save(itemId: String?) {
        val state = _uiState.value

        if (state.name.isBlank()) {
            _uiState.value = state.copy(error = "Pavadinimas privalomas")
            return
        }

        val qty = state.quantity.toIntOrNull()
        if (qty == null || qty < 1) {
            _uiState.value = state.copy(error = "Kiekis turi buti teigiamas skaicius")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)

            val price = state.purchasePrice.toDoubleOrNull()
            val locationId = state.selectedLocationId.ifBlank { null }
            val custodianId = when (state.mode) {
                "UNIT_OWN" -> state.selectedOrgUnitId.ifBlank { null }
                "SHARED" -> null
                else -> state.custodianId
            }

            if (itemId == null) {
                val request = CreateItemRequestDto(
                    name = state.name.trim(),
                    description = state.description.ifBlank { null },
                    type = state.type,
                    category = state.category,
                    custodianId = custodianId,
                    origin = state.origin,
                    quantity = qty,
                    locationId = locationId,
                    temporaryStorageLabel = state.temporaryStorageLabel.ifBlank { null },
                    notes = state.notes.ifBlank { null },
                    purchaseDate = state.purchaseDate.ifBlank { null },
                    purchasePrice = price
                )
                itemRepository.createItem(request)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(isSaving = false, isSuccess = true)
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            error = error.message ?: "Nepavyko issaugoti daikto"
                        )
                    }
            } else {
                val request = UpdateItemRequestDto(
                    name = state.name.trim(),
                    description = state.description.ifBlank { null },
                    type = state.type,
                    category = state.category,
                    condition = state.condition,
                    quantity = qty,
                    custodianId = custodianId,
                    locationId = locationId,
                    temporaryStorageLabel = state.temporaryStorageLabel.ifBlank { null },
                    notes = state.notes.ifBlank { null },
                    purchaseDate = state.purchaseDate.ifBlank { null },
                    purchasePrice = price
                )
                itemRepository.updateItem(itemId, request)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(isSaving = false, isSuccess = true)
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            error = error.message ?: "Nepavyko atnaujinti daikto"
                        )
                    }
            }
        }
    }

    private fun defaultTypeForMode(mode: String): String = when (mode) {
        "PERSONAL" -> "INDIVIDUAL"
        "UNIT_OWN" -> "COLLECTIVE"
        else -> "COLLECTIVE"
    }

    private fun defaultOriginForMode(mode: String): String = when (mode) {
        "UNIT_OWN" -> "UNIT_ACQUIRED"
        "PERSONAL" -> "UNIT_ACQUIRED"
        else -> "UNIT_ACQUIRED"
    }
}
