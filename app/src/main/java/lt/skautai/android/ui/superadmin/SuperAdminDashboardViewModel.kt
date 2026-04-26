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
import lt.skautai.android.data.remote.MemberRankDto
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.remote.RoleDto
import lt.skautai.android.data.remote.TuntasDto
import lt.skautai.android.data.remote.UpdateLeadershipRoleRequestDto
import lt.skautai.android.data.repository.SuperAdminRepository
import javax.inject.Inject

data class SuperAdminDashboardUiState(
    val tuntai: List<TuntasDto> = emptyList(),
    val selectedTuntasId: String? = null,
    val selectedMemberId: String? = null,
    val roles: List<RoleDto> = emptyList(),
    val units: List<OrganizationalUnitDto> = emptyList(),
    val members: List<MemberDto> = emptyList(),
    val selectedMember: MemberDto? = null,
    val isLoadingTuntai: Boolean = false,
    val isLoadingContext: Boolean = false,
    val isLoadingMember: Boolean = false,
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
class SuperAdminDashboardViewModel @Inject constructor(
    private val superAdminRepository: SuperAdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuperAdminDashboardUiState())
    val uiState: StateFlow<SuperAdminDashboardUiState> = _uiState.asStateFlow()

    init {
        loadTuntai()
    }

    fun loadTuntai() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingTuntai = true, error = null)
            superAdminRepository.getTuntai()
                .onSuccess { tuntai ->
                    val currentSelection = _uiState.value.selectedTuntasId
                    val nextSelection = when {
                        tuntai.any { it.id == currentSelection } -> currentSelection
                        tuntai.any { it.status == "PENDING" } -> tuntai.first { it.status == "PENDING" }.id
                        tuntai.isNotEmpty() -> tuntai.first().id
                        else -> null
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoadingTuntai = false,
                        tuntai = tuntai,
                        selectedTuntasId = nextSelection
                    )

                    if (nextSelection != null) {
                        loadTuntasContext(nextSelection, _uiState.value.selectedMemberId)
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingTuntai = false,
                        error = error.message ?: "Klaida gaunant tuntus"
                    )
                }
        }
    }

    fun selectTuntas(tuntasId: String) {
        if (tuntasId == _uiState.value.selectedTuntasId) return
        _uiState.value = _uiState.value.copy(
            selectedTuntasId = tuntasId,
            selectedMemberId = null,
            selectedMember = null
        )
        loadTuntasContext(tuntasId, null)
    }

    fun selectMember(userId: String) {
        val tuntasId = _uiState.value.selectedTuntasId ?: return
        _uiState.value = _uiState.value.copy(selectedMemberId = userId)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMember = true, error = null)
            superAdminRepository.getMember(tuntasId, userId)
                .onSuccess { member ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMember = false,
                        selectedMember = member
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMember = false,
                        error = error.message ?: "Klaida gaunant nario informacija"
                    )
                }
        }
    }

    fun approveTuntas(id: String) {
        performTuntasAction(
            action = { superAdminRepository.approveTuntas(id) },
            successFallback = "Tuntas patvirtintas"
        )
    }

    fun rejectTuntas(id: String) {
        performTuntasAction(
            action = { superAdminRepository.rejectTuntas(id) },
            successFallback = "Tuntas atmestas"
        )
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
        val tuntasId = state.selectedTuntasId ?: return
        val userId = state.selectedMemberId ?: return
        if (state.selectedRoleId.isBlank()) return

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            superAdminRepository.assignLeadershipRole(
                tuntasId = tuntasId,
                userId = userId,
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
                reloadSelectedMember()
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
        val tuntasId = state.selectedTuntasId ?: return
        val userId = state.selectedMemberId ?: return
        val assignmentId = state.editingAssignmentId ?: return

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            superAdminRepository.updateLeadershipRole(
                tuntasId = tuntasId,
                userId = userId,
                assignmentId = assignmentId,
                request = UpdateLeadershipRoleRequestDto(
                    startsAt = state.startsAt.ifBlank { null },
                    expiresAt = state.expiresAt.ifBlank { null },
                    termStatus = state.selectedTermStatus,
                    organizationalUnitId = state.selectedUnitId
                )
            ).onSuccess {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    actionSuccess = "Pareigos atnaujintos"
                )
                closeEditRoleDialog()
                reloadSelectedMember()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = error.message ?: "Klaida atnaujinant pareigas"
                )
            }
        }
    }

    fun removeLeadershipRole(assignmentId: String) {
        val tuntasId = _uiState.value.selectedTuntasId ?: return
        val userId = _uiState.value.selectedMemberId ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            superAdminRepository.removeLeadershipRole(tuntasId, userId, assignmentId)
                .onSuccess { message ->
                    _uiState.value = _uiState.value.copy(isSaving = false, actionSuccess = message)
                    reloadSelectedMember()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = error.message ?: "Klaida salinant pareigas"
                    )
                }
        }
    }

    fun assignRank() {
        val state = _uiState.value
        val tuntasId = state.selectedTuntasId ?: return
        val userId = state.selectedMemberId ?: return
        if (state.selectedRankRoleId.isBlank()) return

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            superAdminRepository.assignRank(
                tuntasId = tuntasId,
                userId = userId,
                request = AssignRankRequestDto(roleId = state.selectedRankRoleId)
            ).onSuccess {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    showAssignRankDialog = false,
                    selectedRankRoleId = "",
                    actionSuccess = "Laipsnis priskirtas"
                )
                reloadSelectedMember()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = error.message ?: "Klaida priskiriant laipsni"
                )
            }
        }
    }

    fun removeRank(rankId: String) {
        val tuntasId = _uiState.value.selectedTuntasId ?: return
        val userId = _uiState.value.selectedMemberId ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            superAdminRepository.removeRank(tuntasId, userId, rankId)
                .onSuccess { message ->
                    _uiState.value = _uiState.value.copy(isSaving = false, actionSuccess = message)
                    reloadSelectedMember()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = error.message ?: "Klaida salinant laipsni"
                    )
                }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, actionSuccess = null)
    }

    private fun performTuntasAction(
        action: suspend () -> Result<String>,
        successFallback: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            action().onSuccess { message ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    actionSuccess = message.ifBlank { successFallback }
                )
                loadTuntai()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = error.message ?: "Klaida atliekant veiksma"
                )
            }
        }
    }

    private fun loadTuntasContext(tuntasId: String, memberIdToReload: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingContext = true, error = null)

            val rolesDeferred = async { superAdminRepository.getRoles(tuntasId) }
            val unitsDeferred = async { superAdminRepository.getOrganizationalUnits(tuntasId) }
            val membersDeferred = async { superAdminRepository.getMembers(tuntasId) }

            val rolesResult = rolesDeferred.await()
            val unitsResult = unitsDeferred.await()
            val membersResult = membersDeferred.await()

            val roles = rolesResult.getOrElse {
                _uiState.value = _uiState.value.copy(isLoadingContext = false, error = it.message ?: "Klaida gaunant roles")
                return@launch
            }
            val units = unitsResult.getOrElse {
                _uiState.value = _uiState.value.copy(isLoadingContext = false, error = it.message ?: "Klaida gaunant vienetus")
                return@launch
            }
            val members = membersResult.getOrElse {
                _uiState.value = _uiState.value.copy(isLoadingContext = false, error = it.message ?: "Klaida gaunant narius")
                return@launch
            }.members

            val selectedMemberId = memberIdToReload?.takeIf { id -> members.any { it.userId == id } }

            _uiState.value = _uiState.value.copy(
                isLoadingContext = false,
                roles = roles,
                units = units,
                members = members,
                selectedMemberId = selectedMemberId,
                selectedMember = if (selectedMemberId == null) null else _uiState.value.selectedMember
            )

            if (selectedMemberId != null) {
                selectMember(selectedMemberId)
            }
        }
    }

    private fun reloadSelectedMember() {
        val state = _uiState.value
        val tuntasId = state.selectedTuntasId ?: return
        loadTuntasContext(tuntasId, state.selectedMemberId)
    }
}
