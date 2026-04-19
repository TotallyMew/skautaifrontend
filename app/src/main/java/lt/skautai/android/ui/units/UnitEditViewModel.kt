package lt.skautai.android.ui.units

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.UpdateOrganizationalUnitRequestDto
import lt.skautai.android.data.repository.OrganizationalUnitRepository
import javax.inject.Inject

data class UnitEditUiState(
    val isLoading: Boolean = true,
    val name: String = "",
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class UnitEditViewModel @Inject constructor(
    private val orgUnitRepository: OrganizationalUnitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnitEditUiState())
    val uiState: StateFlow<UnitEditUiState> = _uiState.asStateFlow()

    fun loadUnit(unitId: String) {
        viewModelScope.launch {
            orgUnitRepository.getUnit(unitId)
                .onSuccess { unit ->
                    _uiState.value = _uiState.value.copy(isLoading = false, name = unit.name)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false,
                        error = e.message ?: "Klaida gaunant vienetą")
                }
        }
    }

    fun onNameChange(name: String) { _uiState.value = _uiState.value.copy(name = name) }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    fun saveUnit(unitId: String) {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = state.copy(error = "Pavadinimas negali būti tuščias")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            orgUnitRepository.updateUnit(unitId, UpdateOrganizationalUnitRequestDto(name = state.name.trim()))
                .onSuccess { _uiState.value = _uiState.value.copy(isSaving = false, isSuccess = true) }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isSaving = false,
                        error = e.message ?: "Klaida atnaujinant vienetą")
                }
        }
    }
}
