package lt.skautai.android.ui.units

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.repository.MemberRepository
import lt.skautai.android.data.repository.OrganizationalUnitRepository
import lt.skautai.android.ui.common.isScoutReadOnlyMember
import lt.skautai.android.util.TokenManager
import javax.inject.Inject

data class UnitListUiState(
    val isLoading: Boolean = true,
    val units: List<OrganizationalUnitDto> = emptyList(),
    val error: String? = null,
    val isReadOnly: Boolean = false
)

@HiltViewModel
class UnitListViewModel @Inject constructor(
    private val orgUnitRepository: OrganizationalUnitRepository,
    private val memberRepository: MemberRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnitListUiState())
    val uiState: StateFlow<UnitListUiState> = _uiState.asStateFlow()
    private var isReadOnlyForCurrentUser: Boolean = false

    init {
        loadCurrentUserAccessMode()
        observeCachedUnits()
        loadUnits()
    }

    private fun loadCurrentUserAccessMode() {
        viewModelScope.launch {
            val currentUserId = tokenManager.userId.first()
            isReadOnlyForCurrentUser = currentUserId
                ?.let { memberRepository.getMember(it).getOrNull() }
                ?.let(::isScoutReadOnlyMember)
                ?: false
            _uiState.value = _uiState.value.copy(isReadOnly = isReadOnlyForCurrentUser)
        }
    }

    private fun observeCachedUnits() {
        viewModelScope.launch {
            orgUnitRepository.observeUnits().collect { units ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    units = units,
                    isReadOnly = isReadOnlyForCurrentUser,
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
