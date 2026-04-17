package lt.skautai.android.ui.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.remote.RoleDto
import lt.skautai.android.data.repository.InvitationRepository
import lt.skautai.android.data.repository.OrganizationalUnitRepository
import lt.skautai.android.data.repository.RoleRepository
import javax.inject.Inject

data class InviteCreateUiState(
    val isLoadingRoles: Boolean = true,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val roles: List<RoleDto> = emptyList(),
    val orgUnits: List<OrganizationalUnitDto> = emptyList(),
    val selectedRoleId: String = "",
    val selectedRoleType: String = "",
    val selectedOrgUnitId: String? = null,
    val generatedCode: String? = null,
    val generatedRoleName: String? = null,
    val expiresAt: String? = null
)

@HiltViewModel
class InviteCreateViewModel @Inject constructor(
    private val roleRepository: RoleRepository,
    private val orgUnitRepository: OrganizationalUnitRepository,
    private val invitationRepository: InvitationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InviteCreateUiState())
    val uiState: StateFlow<InviteCreateUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingRoles = true, error = null)

            val rolesResult = roleRepository.getRoles()
            val orgUnitsResult = orgUnitRepository.getUnits()

            rolesResult.onSuccess { roles ->
                _uiState.value = _uiState.value.copy(
                    roles = roles,
                    isLoadingRoles = false
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingRoles = false,
                    error = error.message ?: "Klaida gaunant roles"
                )
            }

            orgUnitsResult.onSuccess { units ->
                _uiState.value = _uiState.value.copy(orgUnits = units)
            }
        }
    }

    fun onRoleSelected(roleId: String) {
        val role = _uiState.value.roles.find { it.id == roleId }
        _uiState.value = _uiState.value.copy(
            selectedRoleId = roleId,
            selectedRoleType = role?.roleType ?: "",
            selectedOrgUnitId = null
        )
    }

    fun onOrgUnitSelected(orgUnitId: String?) {
        _uiState.value = _uiState.value.copy(selectedOrgUnitId = orgUnitId)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun createInvitation() {
        val state = _uiState.value

        if (state.selectedRoleId.isBlank()) {
            _uiState.value = state.copy(error = "Pasirinkite rolę")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)

            invitationRepository.createInvitation(
                roleId = state.selectedRoleId,
                organizationalUnitId = state.selectedOrgUnitId
            ).onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    isSuccess = true,
                    generatedCode = response.code,
                    generatedRoleName = response.roleName,
                    expiresAt = response.expiresAt
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = error.message ?: "Klaida kuriant pakvietimą"
                )
            }
        }
    }
}