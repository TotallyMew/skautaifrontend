package lt.skautai.android.ui.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.CreateBendrasRequestDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.data.repository.OrganizationalUnitRepository
import lt.skautai.android.data.repository.RequestRepository
import javax.inject.Inject

data class RequestCreateUiState(
    val isLoadingItems: Boolean = true,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val items: List<ItemDto> = emptyList(),
    val orgUnits: List<OrganizationalUnitDto> = emptyList(),
    val selectedItemId: String = "",
    val selectedOrgUnitId: String? = null,
    val quantity: String = "1",
    val startDate: String = "",
    val endDate: String = "",
    val notes: String = ""
)

@HiltViewModel
class RequestCreateViewModel @Inject constructor(
    private val requestRepository: RequestRepository,
    private val itemRepository: ItemRepository,
    private val orgUnitRepository: OrganizationalUnitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RequestCreateUiState())
    val uiState: StateFlow<RequestCreateUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingItems = true, error = null)

            itemRepository.getItems(status = "ACTIVE")
                .onSuccess { items ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingItems = false,
                        items = items
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingItems = false,
                        error = error.message ?: "Klaida gaunant daiktus"
                    )
                }

            orgUnitRepository.getUnits()
                .onSuccess { units ->
                    _uiState.value = _uiState.value.copy(orgUnits = units)
                }
        }
    }

    fun onItemSelected(itemId: String) {
        _uiState.value = _uiState.value.copy(selectedItemId = itemId)
    }

    fun onOrgUnitSelected(orgUnitId: String?) {
        _uiState.value = _uiState.value.copy(selectedOrgUnitId = orgUnitId)
    }

    fun onQuantityChange(value: String) {
        _uiState.value = _uiState.value.copy(quantity = value)
    }

    fun onStartDateChange(value: String) {
        _uiState.value = _uiState.value.copy(startDate = value)
    }

    fun onEndDateChange(value: String) {
        _uiState.value = _uiState.value.copy(endDate = value)
    }

    fun onNotesChange(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun createRequest() {
        val state = _uiState.value

        if (state.selectedItemId.isBlank()) {
            _uiState.value = state.copy(error = "Pasirinkite daiktą")
            return
        }
        val qty = state.quantity.toIntOrNull()
        if (qty == null || qty < 1) {
            _uiState.value = state.copy(error = "Kiekis turi būti teigiamas skaičius")
            return
        }
        if (state.startDate.isBlank()) {
            _uiState.value = state.copy(error = "Įveskite pradžios datą")
            return
        }
        if (state.endDate.isBlank()) {
            _uiState.value = state.copy(error = "Įveskite pabaigos datą")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            requestRepository.createRequest(
                CreateBendrasRequestDto(
                    itemId = state.selectedItemId,
                    quantity = qty,
                    startDate = state.startDate,
                    endDate = state.endDate,
                    requestingUnitId = state.selectedOrgUnitId,
                    eventId = null,
                    notes = state.notes.ifBlank { null }
                )
            ).onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, isSuccess = true)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = error.message ?: "Klaida kuriant prašymą"
                )
            }
        }
    }
}