package lt.skautai.android.ui.units

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.repository.OrganizationalUnitRepository
import javax.inject.Inject

data class UnitListUiState(
    val isLoading: Boolean = true,
    val units: List<OrganizationalUnitDto> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class UnitListViewModel @Inject constructor(
    private val orgUnitRepository: OrganizationalUnitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnitListUiState())
    val uiState: StateFlow<UnitListUiState> = _uiState.asStateFlow()

    init {
        observeCachedUnits()
        loadUnits()
    }

    private fun observeCachedUnits() {
        viewModelScope.launch {
            orgUnitRepository.observeUnits().collect { units ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    units = units,
                    error = if (units.isNotEmpty()) null else _uiState.value.error
                )
            }
        }
    }

    fun loadUnits() {
        viewModelScope.launch {
            orgUnitRepository.refreshUnits()
                .onFailure { error ->
                    if (_uiState.value.units.isEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Klaida gaunant vienetus"
                        )
                    }
                }
        }
    }
}
