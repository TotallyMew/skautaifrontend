package lt.skautai.android.ui.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.InvitationRoleOptionDto
import lt.skautai.android.data.remote.InvitationUnitOptionDto
import lt.skautai.android.data.remote.RoleDto
import lt.skautai.android.data.repository.InvitationRepository
import javax.inject.Inject

data class InviteCreateUiState(
    val isLoadingRoles: Boolean = true,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val roles: List<RoleDto> = emptyList(),
    val orgUnits: List<InvitationUnitOptionDto> = emptyList(),
    val selectedRoleId: String = "",
    val selectedOrgUnitId: String? = null,
    val selectedRoleRequiresOrgUnit: Boolean = false,
    val lockedOrgUnitId: String? = null,
    val lockedOrgUnitName: String? = null,
    val canChooseOrgUnit: Boolean = true,
    val generatedCode: String? = null,
    val generatedRoleName: String? = null,
    val expiresAt: String? = null
)

@HiltViewModel
class InviteCreateViewModel @Inject constructor(
    private val invitationRepository: InvitationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InviteCreateUiState())
    val uiState: StateFlow<InviteCreateUiState> = _uiState.asStateFlow()
    private var roleOptions: List<InvitationRoleOptionDto> = emptyList()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingRoles = true, error = null)

            invitationRepository.getInvitationOptions().onSuccess { options ->
                roleOptions = options.roles
                _uiState.value = _uiState.value.copy(
                    roles = options.roles.map { it.role },
                    orgUnits = emptyList(),
                    isLoadingRoles = false
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingRoles = false,
                    error = error.message ?: "Klaida gaunant pareigas ir laipsnius"
                )
            }
        }
    }

    fun onRoleSelected(roleId: String) {
        val currentState = _uiState.value
        val option = roleOptions.find { it.role.id == roleId }
        val units = option?.organizationalUnits.orEmpty()
        val requiresOrgUnit = option?.canInviteWithoutOrganizationalUnit == false
        val lockedUnit = units.singleOrNull()?.takeIf { requiresOrgUnit }
        val selectedOrgUnitId = lockedUnit?.id ?: currentState.selectedOrgUnitId
            ?.takeIf { selectedId -> units.any { it.id == selectedId } }

        _uiState.value = currentState.copy(
            selectedRoleId = roleId,
            selectedOrgUnitId = selectedOrgUnitId,
            selectedRoleRequiresOrgUnit = requiresOrgUnit,
            orgUnits = units,
            lockedOrgUnitId = lockedUnit?.id,
            lockedOrgUnitName = lockedUnit?.name,
            canChooseOrgUnit = lockedUnit == null
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
        if (state.isSaving) return
        if (state.selectedRoleId.isBlank()) {
            _uiState.value = state.copy(error = "Pasirinkite pareigas arba laipsnį")
            return
        }

        if (state.selectedRoleRequiresOrgUnit && (state.lockedOrgUnitId ?: state.selectedOrgUnitId).isNullOrBlank()) {
            _uiState.value = state.copy(error = "Pasirinkite vienetą")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)

            invitationRepository.createInvitation(
                roleId = state.selectedRoleId,
                organizationalUnitId = state.lockedOrgUnitId ?: state.selectedOrgUnitId
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
