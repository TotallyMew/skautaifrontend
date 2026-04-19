package lt.skautai.android.ui.units

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.CreateOrganizationalUnitRequestDto
import lt.skautai.android.data.repository.OrganizationalUnitRepository
import javax.inject.Inject

data class UnitCreateUiState(
    val name: String = "",
    val type: String = "",
    val subType: String? = null,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class UnitCreateViewModel @Inject constructor(
    private val orgUnitRepository: OrganizationalUnitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnitCreateUiState())
    val uiState: StateFlow<UnitCreateUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun onTypeChange(type: String) {
        val subType = if (type.startsWith("VYR_")) _uiState.value.subType else null
        _uiState.value = _uiState.value.copy(type = type, subType = subType)
    }

    fun onSubTypeChange(subType: String?) {
        _uiState.value = _uiState.value.copy(subType = subType)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun createUnit() {
        val state = _uiState.value
        if (state.name.isBlank() || state.type.isBlank()) {
            _uiState.value = state.copy(error = "Pavadinimas ir tipas yra privalomi")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            orgUnitRepository.createUnit(
                CreateOrganizationalUnitRequestDto(
                    name = state.name.trim(),
                    type = state.type,
                    subType = state.subType
                )
            )
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSaving = false, isSuccess = true)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = error.message ?: "Klaida kuriant vienetą"
                    )
                }
        }
    }
}
