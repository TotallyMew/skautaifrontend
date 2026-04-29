package lt.skautai.android.ui.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import lt.skautai.android.data.remote.*
import lt.skautai.android.data.repository.MemberRepository
import lt.skautai.android.data.repository.OrganizationalUnitRepository
import lt.skautai.android.data.repository.RoleRepository
import lt.skautai.android.data.repository.UserRepository
import lt.skautai.android.ui.common.isScoutReadOnlyMember
import lt.skautai.android.util.TokenManager
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
    val showEditRoleDialog: Boolean = false,
    val showAssignRankDialog: Boolean = false,
    val showMoveMemberDialog: Boolean = false,
    val showTransferTuntininkasDialog: Boolean = false,
    val leadershipRoles: List<RoleDto> = emptyList(),
    val rankRoles: List<RoleDto> = emptyList(),
    val availableUnits: List<OrganizationalUnitDto> = emptyList(),
    val availableSuccessors: List<MemberDto> = emptyList(),
    val selectedRoleId: String = "",
    val selectedUnitId: String? = null,
    val selectedSuccessorUserId: String = "",
    val selectedTermStatus: String = "ACTIVE",
    val startsAt: String = "",
    val expiresAt: String = "",
    val editingAssignmentId: String? = null,
    val transferAssignmentId: String? = null,
    val selectedMoveUnitId: String = "",
    val selectedRankRoleId: String = ""
)

@HiltViewModel
class MemberDetailViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val roleRepository: RoleRepository,
    private val orgUnitRepository: OrganizationalUnitRepository,
    private val userRepository: UserRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemberDetailUiState())
    val uiState: StateFlow<MemberDetailUiState> = _uiState.asStateFlow()

    val permissions: StateFlow<Set<String>> = tokenManager.permissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val currentUserId: StateFlow<String?> = tokenManager.userId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun loadMember(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val currentUser = currentUserId.value?.let { memberRepository.getMember(it).getOrNull() }
            if (currentUser != null && isScoutReadOnlyMember(currentUser) && currentUser.userId != userId) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Nario detale prieinama tik vadovams."
                )
                return@launch
            }
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

    fun openMoveMemberDialog() {
        viewModelScope.launch {
            orgUnitRepository.getUnits()
                .onSuccess { units ->
                    _uiState.value = _uiState.value.copy(
                        availableUnits = units,
                        selectedMoveUnitId = "",
                        showMoveMemberDialog = true
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(actionError = e.message ?: "Klaida gaunant vienetus")
                }
        }
    }

    fun hideMoveMemberDialog() {
        _uiState.value = _uiState.value.copy(
            showMoveMemberDialog = false,
            selectedMoveUnitId = ""
        )
    }

    fun onMoveUnitSelected(unitId: String) {
        _uiState.value = _uiState.value.copy(selectedMoveUnitId = unitId)
    }

    fun moveMember(userId: String) {
        val state = _uiState.value
        if (state.selectedMoveUnitId.isBlank()) return

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, actionError = null)
            orgUnitRepository.moveUnitMember(state.selectedMoveUnitId, userId)
                .onSuccess {
                    refreshPermissionsIfCurrentUser(userId)
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        showMoveMemberDialog = false,
                        selectedMoveUnitId = ""
                    )
                    loadMember(userId)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        actionError = e.message ?: "Klaida perkeliant nari"
                    )
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
                    .filter { it.roleType == "LEADERSHIP" && it.name != "Tuntininkas" },
                availableUnits = unitsResult.getOrDefault(emptyList()),
                showAssignRoleDialog = true
            )
        }
    }

    fun hideAssignRoleDialog() {
        _uiState.value = _uiState.value.copy(showAssignRoleDialog = false,
            selectedRoleId = "", selectedUnitId = null)
    }

    fun openEditRoleDialog(role: MemberLeadershipRoleDto) {
        viewModelScope.launch {
            val unitsResult = orgUnitRepository.getUnits()
            _uiState.value = _uiState.value.copy(
                availableUnits = unitsResult.getOrDefault(emptyList()),
                showEditRoleDialog = true,
                editingAssignmentId = role.id,
                selectedUnitId = role.organizationalUnitId,
                selectedTermStatus = role.termStatus,
                startsAt = role.startsAt?.take(10).orEmpty(),
                expiresAt = role.expiresAt?.take(10).orEmpty()
            )
        }
    }

    fun hideEditRoleDialog() {
        _uiState.value = _uiState.value.copy(
            showEditRoleDialog = false,
            editingAssignmentId = null,
            selectedUnitId = null,
            selectedTermStatus = "ACTIVE",
            startsAt = "",
            expiresAt = ""
        )
    }

    fun onRoleSelected(roleId: String) { _uiState.value = _uiState.value.copy(selectedRoleId = roleId) }
    fun onRoleUnitSelected(unitId: String?) { _uiState.value = _uiState.value.copy(selectedUnitId = unitId) }
    fun onSuccessorSelected(userId: String) { _uiState.value = _uiState.value.copy(selectedSuccessorUserId = userId) }
    fun onTermStatusSelected(value: String) { _uiState.value = _uiState.value.copy(selectedTermStatus = value) }
    fun onStartsAtChanged(value: String) { _uiState.value = _uiState.value.copy(startsAt = value) }
    fun onExpiresAtChanged(value: String) { _uiState.value = _uiState.value.copy(expiresAt = value) }

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
                    refreshPermissionsIfCurrentUser(userId)
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
                    refreshPermissionsIfCurrentUser(userId)
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    loadMember(userId)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isSaving = false,
                        actionError = e.message ?: "Klaida šalinant pareigas")
                }
        }
    }

    fun updateLeadershipRole(userId: String) {
        val state = _uiState.value
        val assignmentId = state.editingAssignmentId ?: return
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, actionError = null)
            memberRepository.updateLeadershipRole(
                userId = userId,
                assignmentId = assignmentId,
                request = UpdateLeadershipRoleRequestDto(
                    startsAt = state.startsAt.ifBlank { null },
                    expiresAt = state.expiresAt.ifBlank { null },
                    termStatus = state.selectedTermStatus,
                    organizationalUnitId = state.selectedUnitId
                )
            ).onSuccess {
                refreshPermissionsIfCurrentUser(userId)
                _uiState.value = _uiState.value.copy(isSaving = false)
                hideEditRoleDialog()
                loadMember(userId)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    actionError = e.message ?: "Klaida atnaujinant pareigas"
                )
            }
        }
    }

    fun stepDownLeadershipRole(userId: String, assignmentId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, actionError = null)
            memberRepository.stepDownLeadershipRole(assignmentId)
                .onSuccess {
                    refreshPermissions()
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    loadMember(userId)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        actionError = e.message ?: "Klaida atsistatydinant"
                    )
                }
        }
    }

    fun openTransferTuntininkasDialog(currentUserId: String, assignmentId: String) {
        viewModelScope.launch {
            memberRepository.getMembers()
                .onSuccess { memberList ->
                    val successors = memberList.members.filter { it.userId != currentUserId }
                    _uiState.value = _uiState.value.copy(
                        availableSuccessors = successors,
                        selectedSuccessorUserId = successors.firstOrNull()?.userId.orEmpty(),
                        transferAssignmentId = assignmentId,
                        showTransferTuntininkasDialog = true,
                        actionError = null
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        actionError = e.message ?: "Klaida gaunant nariųs"
                    )
                }
        }
    }

    fun hideTransferTuntininkasDialog() {
        _uiState.value = _uiState.value.copy(
            showTransferTuntininkasDialog = false,
            availableSuccessors = emptyList(),
            selectedSuccessorUserId = "",
            transferAssignmentId = null
        )
    }

    fun transferTuntininkas(currentUserId: String) {
        val state = _uiState.value
        if (state.selectedSuccessorUserId.isBlank()) {
            _uiState.value = state.copy(actionError = "Pasirinkite, kam perleisti pareigas")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, actionError = null)
            memberRepository.transferTuntininkas(state.selectedSuccessorUserId)
                .onSuccess {
                    refreshPermissions()
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    hideTransferTuntininkasDialog()
                    loadMember(currentUserId)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        actionError = e.message ?: "Klaida perleidziant tuntininko pareigas"
                    )
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

    private suspend fun refreshPermissions() {
        val tuntasId = tokenManager.activeTuntasId.first() ?: return
        userRepository.getMyPermissions(tuntasId)
            .onSuccess { tokenManager.savePermissions(it) }
    }

    private suspend fun refreshPermissionsIfCurrentUser(userId: String) {
        if (tokenManager.userId.first() == userId) refreshPermissions()
    }
}
