package lt.skautai.android.ui.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import lt.skautai.android.data.remote.*
import lt.skautai.android.data.repository.MemberRepository
import lt.skautai.android.data.repository.OrganizationalUnitRepository
import lt.skautai.android.data.repository.RoleRepository
import javax.inject.Inject

data class MemberDetailUiState(
    val isLoading: Boolean = true,
    val member: MemberDto? = null,
    val error: String? = null,
    val isSaving: Boolean = false,
    val actionError: String? = null,
    val isDone: Boolean = false,
    val showRemoveMemberDialog: Boolean = false,
    val showAssignRoleDialog: Boolean = false,
    val showAssignRankDialog: Boolean = false,
    val leadershipRoles: List<RoleDto> = emptyList(),
    val rankRoles: List<RoleDto> = emptyList(),
    val availableUnits: List<OrganizationalUnitDto> = emptyList(),
    val selectedRoleId: String = "",
    val selectedUnitId: String? = null,
    val selectedRankRoleId: String = ""
)

@HiltViewModel
class MemberDetailViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val roleRepository: RoleRepository,
    private val orgUnitRepository: OrganizationalUnitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemberDetailUiState())
    val uiState: StateFlow<MemberDetailUiState> = _uiState.asStateFlow()

    fun loadMember(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            memberRepository.getMember(userId)
                .onSuccess { member ->
                    _uiState.value = _uiState.value.copy(isLoading = false, member = member)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false,
                        error = e.message ?: "Klaida gaunant nario informaciją")
                }
        }
    }

    // Remove member from tuntas
    fun showRemoveMemberDialog() { _uiState.value = _uiState.value.copy(showRemoveMemberDialog = true) }
    fun hideRemoveMemberDialog() { _uiState.value = _uiState.value.copy(showRemoveMemberDialog = false) }

    fun removeMember(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, actionError = null)
            memberRepository.removeMember(userId)
                .onSuccess { _uiState.value = _uiState.value.copy(isSaving = false, isDone = true) }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isSaving = false, showRemoveMemberDialog = false,
                        actionError = e.message ?: "Klaida šalinant narį")
                }
        }
    }

    // Leadership roles
    fun openAssignRoleDialog() {
        viewModelScope.launch {
            val rolesResult = roleRepository.getRoles()
            val unitsResult = orgUnitRepository.getUnits()
            _uiState.value = _uiState.value.copy(
                leadershipRoles = rolesResult.getOrDefault(emptyList())
                    .filter { it.roleType == "LEADERSHIP" },
                availableUnits = unitsResult.getOrDefault(emptyList()),
                showAssignRoleDialog = true
            )
        }
    }

    fun hideAssignRoleDialog() {
        _uiState.value = _uiState.value.copy(showAssignRoleDialog = false,
            selectedRoleId = "", selectedUnitId = null)
    }

    fun onRoleSelected(roleId: String) { _uiState.value = _uiState.value.copy(selectedRoleId = roleId) }
    fun onRoleUnitSelected(unitId: String?) { _uiState.value = _uiState.value.copy(selectedUnitId = unitId) }

    fun assignLeadershipRole(userId: String) {
        val state = _uiState.value
        if (state.selectedRoleId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, actionError = null)
            memberRepository.assignLeadershipRole(
                userId,
                AssignLeadershipRoleRequestDto(
                    roleId = state.selectedRoleId,
                    organizationalUnitId = state.selectedUnitId
                )
            )
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSaving = false, showAssignRoleDialog = false,
                        selectedRoleId = "", selectedUnitId = null)
                    loadMember(userId)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isSaving = false,
                        actionError = e.message ?: "Klaida priskiriant pareigas")
                }
        }
    }

    fun removeLeadershipRole(userId: String, assignmentId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, actionError = null)
            memberRepository.removeLeadershipRole(userId, assignmentId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    loadMember(userId)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isSaving = false,
                        actionError = e.message ?: "Klaida šalinant pareigas")
                }
        }
    }

    // Ranks
    fun openAssignRankDialog() {
        viewModelScope.launch {
            roleRepository.getRoles()
                .onSuccess { roles ->
                    _uiState.value = _uiState.value.copy(
                        rankRoles = roles.filter { it.roleType == "RANK" },
                        showAssignRankDialog = true
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(actionError = e.message ?: "Klaida gaunant laipsnius")
                }
        }
    }

    fun hideAssignRankDialog() {
        _uiState.value = _uiState.value.copy(showAssignRankDialog = false, selectedRankRoleId = "")
    }

    fun onRankRoleSelected(roleId: String) { _uiState.value = _uiState.value.copy(selectedRankRoleId = roleId) }

    fun assignRank(userId: String) {
        val state = _uiState.value
        if (state.selectedRankRoleId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, actionError = null)
            memberRepository.assignRank(userId, AssignRankRequestDto(roleId = state.selectedRankRoleId))
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSaving = false, showAssignRankDialog = false,
                        selectedRankRoleId = "")
                    loadMember(userId)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isSaving = false,
                        actionError = e.message ?: "Klaida priskiriant laipsnį")
                }
        }
    }

    fun removeRank(userId: String, rankId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, actionError = null)
            memberRepository.removeRank(userId, rankId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    loadMember(userId)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isSaving = false,
                        actionError = e.message ?: "Klaida šalinant laipsnį")
                }
        }
    }

    fun clearActionError() { _uiState.value = _uiState.value.copy(actionError = null) }
}
