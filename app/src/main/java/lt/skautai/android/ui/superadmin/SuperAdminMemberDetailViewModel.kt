package lt.skautai.android.ui.superadmin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.AssignLeadershipRoleRequestDto
import lt.skautai.android.data.remote.AssignRankRequestDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.MemberLeadershipRoleDto
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.remote.RoleDto
import lt.skautai.android.data.remote.UpdateLeadershipRoleRequestDto
import lt.skautai.android.data.repository.SuperAdminRepository
import javax.inject.Inject

data class SuperAdminMemberDetailUiState(
    val tuntasId: String = "",
    val userId: String = "",
    val member: MemberDto? = null,
    val roles: List<RoleDto> = emptyList(),
    val units: List<OrganizationalUnitDto> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val actionSuccess: String? = null,
    val showAssignRoleDialog: Boolean = false,
    val showEditRoleDialog: Boolean = false,
    val showAssignRankDialog: Boolean = false,
    val selectedRoleId: String = "",
    val selectedUnitId: String? = null,
    val selectedRankRoleId: String = "",
    val selectedTermStatus: String = "ACTIVE",
    val startsAt: String = "",
    val expiresAt: String = "",
    val editingAssignmentId: String? = null
)

@HiltViewModel
class SuperAdminMemberDetailViewModel @Inject constructor(
    private val superAdminRepository: SuperAdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuperAdminMemberDetailUiState())
    val uiState: StateFlow<SuperAdminMemberDetailUiState> = _uiState.asStateFlow()

    fun load(tuntasId: String, userId: String) {
        val current = _uiState.value
        if (current.tuntasId == tuntasId && current.userId == userId && current.member != null) return
        _uiState.value = current.copy(tuntasId = tuntasId, userId = userId)
        reloadAll()
    }

    fun openAssignRoleDialog() {
        _uiState.value = _uiState.value.copy(
            showAssignRoleDialog = true,
            selectedRoleId = "",
            selectedUnitId = null
        )
    }

    fun closeAssignRoleDialog() {
        _uiState.value = _uiState.value.copy(showAssignRoleDialog = false, selectedRoleId = "", selectedUnitId = null)
    }

    fun openEditRoleDialog(role: MemberLeadershipRoleDto) {
        _uiState.value = _uiState.value.copy(
            showEditRoleDialog = true,
            editingAssignmentId = role.id,
            selectedUnitId = role.organizationalUnitId,
            selectedTermStatus = role.termStatus,
            startsAt = role.startsAt.orEmpty(),
            expiresAt = role.expiresAt.orEmpty()
        )
    }

    fun closeEditRoleDialog() {
        _uiState.value = _uiState.value.copy(
            showEditRoleDialog = false,
            editingAssignmentId = null,
            selectedUnitId = null,
            selectedTermStatus = "ACTIVE",
            startsAt = "",
            expiresAt = ""
        )
    }

    fun openAssignRankDialog() {
        _uiState.value = _uiState.value.copy(showAssignRankDialog = true, selectedRankRoleId = "")
    }

    fun closeAssignRankDialog() {
        _uiState.value = _uiState.value.copy(showAssignRankDialog = false, selectedRankRoleId = "")
    }

    fun onRoleSelected(roleId: String) {
        _uiState.value = _uiState.value.copy(selectedRoleId = roleId)
    }

    fun onUnitSelected(unitId: String?) {
        _uiState.value = _uiState.value.copy(selectedUnitId = unitId)
    }

    fun onRankSelected(roleId: String) {
        _uiState.value = _uiState.value.copy(selectedRankRoleId = roleId)
    }

    fun onTermStatusSelected(status: String) {
        _uiState.value = _uiState.value.copy(selectedTermStatus = status)
    }

    fun onStartsAtChanged(value: String) {
        _uiState.value = _uiState.value.copy(startsAt = value)
    }

    fun onExpiresAtChanged(value: String) {
        _uiState.value = _uiState.value.copy(expiresAt = value)
    }

    fun assignLeadershipRole() {
        val state = _uiState.value
        if (state.selectedRoleId.isBlank()) return

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            superAdminRepository.assignLeadershipRole(
                tuntasId = state.tuntasId,
                userId = state.userId,
                request = AssignLeadershipRoleRequestDto(
                    roleId = state.selectedRoleId,
                    organizationalUnitId = state.selectedUnitId
                )
            ).onSuccess {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    showAssignRoleDialog = false,
                    selectedRoleId = "",
                    selectedUnitId = null,
                    actionSuccess = "Pareigos priskirtos"
                )
                reloadMember()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = error.message ?: "Klaida priskiriant pareigas"
                )
            }
        }
    }

    fun updateLeadershipRole() {
        val state = _uiState.value
        val assignmentId = state.editingAssignmentId ?: return

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            superAdminRepository.updateLeadershipRole(
                tuntasId = state.tuntasId,
                userId = state.userId,
                assignmentId = assignmentId,
                request = UpdateLeadershipRoleRequestDto(
                    startsAt = state.startsAt.ifBlank { null },
                    expiresAt = state.expiresAt.ifBlank { null },
                    termStatus = state.selectedTermStatus,
                    organizationalUnitId = state.selectedUnitId
                )
            ).onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, actionSuccess = "Pareigos atnaujintos")
                closeEditRoleDialog()
                reloadMember()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = error.message ?: "Klaida atnaujinant pareigas"
                )
            }
        }
    }

    fun removeLeadershipRole(assignmentId: String) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            superAdminRepository.removeLeadershipRole(state.tuntasId, state.userId, assignmentId)
                .onSuccess { message ->
                    _uiState.value = _uiState.value.copy(isSaving = false, actionSuccess = message)
                    reloadMember()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = error.message ?: "Klaida šalinant pareigas"
                    )
                }
        }
    }

    fun assignRank() {
        val state = _uiState.value
        if (state.selectedRankRoleId.isBlank()) return

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            superAdminRepository.assignRank(
                tuntasId = state.tuntasId,
                userId = state.userId,
                request = AssignRankRequestDto(roleId = state.selectedRankRoleId)
            ).onSuccess {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    showAssignRankDialog = false,
                    selectedRankRoleId = "",
                    actionSuccess = "Laipsnis priskirtas"
                )
                reloadMember()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = error.message ?: "Klaida priskiriant laipsnį"
                )
            }
        }
    }

    fun removeRank(rankId: String) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            superAdminRepository.removeRank(state.tuntasId, state.userId, rankId)
                .onSuccess { message ->
                    _uiState.value = _uiState.value.copy(isSaving = false, actionSuccess = message)
                    reloadMember()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = error.message ?: "Klaida šalinant laipsnį"
                    )
                }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, actionSuccess = null)
    }

    private fun reloadAll() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.value = state.copy(isLoading = true, error = null)

            val rolesDeferred = async { superAdminRepository.getRoles(state.tuntasId) }
            val unitsDeferred = async { superAdminRepository.getOrganizationalUnits(state.tuntasId) }
            val memberDeferred = async { superAdminRepository.getMember(state.tuntasId, state.userId) }

            val roles = rolesDeferred.await().getOrElse {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message ?: "Klaida gaunant pareigas ir laipsnius")
                return@launch
            }
            val units = unitsDeferred.await().getOrElse {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message ?: "Klaida gaunant vienetus")
                return@launch
            }
            val member = memberDeferred.await().getOrElse {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message ?: "Klaida gaunant nario informaciją")
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                roles = roles,
                units = units,
                member = member
            )
        }
    }

    private fun reloadMember() {
        val state = _uiState.value
        viewModelScope.launch {
            superAdminRepository.getMember(state.tuntasId, state.userId)
                .onSuccess { member -> _uiState.value = _uiState.value.copy(member = member) }
                .onFailure { error -> _uiState.value = _uiState.value.copy(error = error.message ?: "Klaida gaunant nario informaciją") }
        }
    }
}
