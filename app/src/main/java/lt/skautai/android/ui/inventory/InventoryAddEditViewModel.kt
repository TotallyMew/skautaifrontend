package lt.skautai.android.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.CreateItemRequestDto
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.remote.UpdateItemRequestDto
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.data.repository.OrganizationalUnitRepository
import lt.skautai.android.util.TokenManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class InventoryAddEditUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val name: String = "",
    val description: String = "",
    val category: String = "COLLECTIVE",
    val condition: String = "GOOD",
    val custodianId: String? = null,
    val origin: String = "UNIT_ACQUIRED",
    val quantity: String = "1",
    val notes: String = "",
    val purchaseDate: String = "",
    val purchasePrice: String = "",
    val orgUnits: List<OrganizationalUnitDto> = emptyList(),
    val selectedOrgUnitId: String = "",
    val tuntasId: String = ""
)

@HiltViewModel
class InventoryAddEditViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val orgUnitRepository: OrganizationalUnitRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryAddEditUiState())
    val uiState: StateFlow<InventoryAddEditUiState> = _uiState.asStateFlow()

    fun init(itemId: String?) {
        viewModelScope.launch {
            val tuntasId = tokenManager.activeTuntasId.first() ?: ""
            _uiState.value = _uiState.value.copy(tuntasId = tuntasId)

            orgUnitRepository.getUnits()
                .onSuccess { units ->
                    _uiState.value = _uiState.value.copy(orgUnits = units)
                }

            if (itemId != null) {
                _uiState.value = _uiState.value.copy(isLoading = true)
                itemRepository.getItem(itemId)
                    .onSuccess { item ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            name = item.name,
                            description = item.description ?: "",
                            category = item.category,
                            condition = item.condition,
                            custodianId = item.custodianId,
                            origin = item.origin,
                            quantity = item.quantity.toString(),
                            notes = item.notes ?: "",
                            purchaseDate = item.purchaseDate ?: "",
                            purchasePrice = item.purchasePrice?.toString() ?: "",
                            selectedOrgUnitId = item.custodianId ?: ""
                        )
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Nepavyko gauti daikto"
                        )
                    }
            }
        }
    }

    fun onNameChange(value: String) { _uiState.value = _uiState.value.copy(name = value) }
    fun onDescriptionChange(value: String) { _uiState.value = _uiState.value.copy(description = value) }
    fun onNotesChange(value: String) { _uiState.value = _uiState.value.copy(notes = value) }
    fun onQuantityChange(value: String) { _uiState.value = _uiState.value.copy(quantity = value) }
    fun onPurchaseDateChange(value: String) { _uiState.value = _uiState.value.copy(purchaseDate = value) }
    fun onPurchasePriceChange(value: String) { _uiState.value = _uiState.value.copy(purchasePrice = value) }

    fun onCategoryChange(value: String) {
        _uiState.value = _uiState.value.copy(category = value)
    }

    fun onConditionChange(value: String) {
        _uiState.value = _uiState.value.copy(condition = value)
    }

    fun onOriginChange(value: String) {
        _uiState.value = _uiState.value.copy(origin = value)
    }

    fun onOrgUnitChange(unitId: String?) {
        _uiState.value = _uiState.value.copy(
            selectedOrgUnitId = unitId ?: "",
            custodianId = unitId
        )
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    fun save(itemId: String?) {
        val state = _uiState.value

        if (state.name.isBlank()) {
            _uiState.value = state.copy(error = "Pavadinimas privalomas")
            return
        }

        val qty = state.quantity.toIntOrNull()
        if (qty == null || qty < 1) {
            _uiState.value = state.copy(error = "Kiekis turi būti teigiamas skaičius")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)

            val price = state.purchasePrice.toDoubleOrNull()

            if (itemId == null) {
                val request = CreateItemRequestDto(
                    name = state.name,
                    description = state.description.ifBlank { null },
                    category = state.category,
                    custodianId = state.custodianId,
                    origin = state.origin,
                    quantity = qty,
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
                            error = error.message ?: "Nepavyko išsaugoti daikto"
                        )
                    }
            } else {
                val request = UpdateItemRequestDto(
                    name = state.name,
                    description = state.description.ifBlank { null },
                    category = state.category,
                    condition = state.condition,
                    quantity = qty,
                    custodianId = state.custodianId,
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
}