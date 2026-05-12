package lt.skautai.android.ui.units

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.*
import lt.skautai.android.data.repository.MemberRepository
import lt.skautai.android.data.repository.OrganizationalUnitRepository
import lt.skautai.android.data.repository.UserRepository
import lt.skautai.android.ui.common.isScoutReadOnlyMember
import lt.skautai.android.util.TokenManager
import javax.inject.Inject

data class UnitDetailUiState(
    val isLoading: Boolean = true,
    val unit: OrganizationalUnitDto? = null,
    val members: List<UnitMembershipDto> = emptyList(),
    val memberDetails: Map<String, MemberDto> = emptyMap(),
    val canCurrentUserManageThisUnit: Boolean = false,
    val canCurrentUserLeaveThisUnit: Boolean = false,
    val accessDenied: Boolean = false,
    val error: String? = null,
    val showDeleteDialog: Boolean = false,
    val showLeaveDialog: Boolean = false,
    val showAssignMemberDialog: Boolean = false,
    val availableTuntasMembers: List<MemberDto> = emptyList(),
    val selectedMemberId: String = "",
    val selectedAssignmentType: String = "MEMBER",
    val isSaving: Boolean = false,
    val actionError: String? = null,
    val isDone: Boolean = false
)

@HiltViewModel
class UnitDetailViewModel @Inject constructor(
    private val orgUnitRepository: OrganizationalUnitRepository,
    private val memberRepository: MemberRepository,
    private val userRepository: UserRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnitDetailUiState())
    val uiState: StateFlow<UnitDetailUiState> = _uiState.asStateFlow()

    val permissions: StateFlow<Set<String>> = tokenManager.permissions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun loadUnit(unitId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val currentUserId = tokenManager.userId.first()
            val currentUserMember = currentUserId?.let { memberRepository.getMember(it).getOrNull() }
            if (currentUserMember != null && isScoutReadOnlyMember(currentUserMember)) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    accessDenied = true,
                    error = "Vieneto informacija prieinama tik vadovams."
                )
                return@launch
            }
            val unitDeferred = async { orgUnitRepository.getUnit(unitId) }
            val membersDeferred = async { orgUnitRepository.getUnitMembers(unitId) }
            val allMembersDeferred = async { memberRepository.getMembers() }
            val currentMemberDeferred = currentUserId?.let { async { memberRepository.getMember(it) } }
            val unitResult = unitDeferred.await()
            val membersResult = membersDeferred.await()
            val allMembersResult = allMembersDeferred.await()
            val currentMember = currentMemberDeferred?.await()?.getOrNull()
            val hasCurrentUserAssignment = currentMember?.unitAssignments.orEmpty()
                .any { it.organizationalUnitId == unitId }
            val hasCurrentUserLeadership = currentMember?.leadershipRoles.orEmpty()
                .any { it.termStatus == "ACTIVE" && it.organizationalUnitId == unitId }
            val canManageThisUnit = hasCurrentUserLeadership
            val memberDetails = allMembersResult.getOrNull()
                ?.members
                ?.associateBy { it.userId }
                .orEmpty()
            unitResult
                .onSuccess { unit ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        unit = unit,
                        members = membersResult.getOrDefault(emptyList()),
                        memberDetails = memberDetails,
                        canCurrentUserManageThisUnit = canManageThisUnit,
                        canCurrentUserLeaveThisUnit = hasCurrentUserAssignment && !hasCurrentUserLeadership
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Klaida gaunant vienetą"
                    )
                }
        }
    }

    fun showDeleteDialog() { _uiState.value = _uiState.value.copy(showDeleteDialog = true) }
    fun hideDeleteDialog() { _uiState.value = _uiState.value.copy(showDeleteDialog = false) }
    fun showLeaveDialog() { _uiState.value = _uiState.value.copy(showLeaveDialog = true) }
    fun hideLeaveDialog() { _uiState.value = _uiState.value.copy(showLeaveDialog = false) }

    fun deleteUnit(unitId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, actionError = null)
            orgUnitRepository.deleteUnit(unitId)
                .onSuccess { _uiState.value = _uiState.value.copy(isSaving = false, isDone = true) }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        showDeleteDialog = false,
                        actionError = e.message ?: "Klaida trinant vienetą"
                    )
                }
        }
    }

    fun openAssignMemberDialog() {
        viewModelScope.launch {
            memberRepository.getMembers()
                .onSuccess { list ->
                    val existingMemberIds = _uiState.value.members.map { it.userId }.toSet()
                    _uiState.value = _uiState.value.copy(
                        availableTuntasMembers = list.members.filterNot { it.userId in existingMemberIds },
                        showAssignMemberDialog = true
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(actionError = e.message ?: "Klaida gaunant narius")
                }
        }
    }

    fun hideAssignMemberDialog() {
        _uiState.value = _uiState.value.copy(
            showAssignMemberDialog = false,
            selectedMemberId = "",
            selectedAssignmentType = "MEMBER"
        )
    }

    fun onMemberSelected(userId: String) { _uiState.value = _uiState.value.copy(selectedMemberId = userId) }
    fun onAssignmentTypeSelected(type: String) { _uiState.value = _uiState.value.copy(selectedAssignmentType = type) }

    fun assignMember(unitId: String) {
        val state = _uiState.value
        if (state.selectedMemberId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, actionError = null)
            orgUnitRepository.assignUnitMember(
                unitId,
                AssignUnitMemberRequestDto(state.selectedMemberId, state.selectedAssignmentType)
            )
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSaving = false, showAssignMemberDialog = false,
                        selectedMemberId = "", selectedAssignmentType = "MEMBER")
                    loadUnit(unitId)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isSaving = false,
                        actionError = e.message ?: "Klaida priskiriant narį")
                }
        }
    }

    fun removeUnitMember(unitId: String, userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, actionError = null)
            orgUnitRepository.removeUnitMember(unitId, userId)
                .onSuccess {
                    if (tokenManager.userId.first() == userId) {
                        refreshPermissions()
                        clearActiveUnitIfNeeded(unitId)
                    }
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    loadUnit(unitId)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isSaving = false,
                        actionError = e.message ?: "Klaida šalinant narį")
                }
        }
    }

    fun leaveUnit(unitId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, actionError = null)
            orgUnitRepository.leaveUnit(unitId)
                .onSuccess {
                    refreshPermissions()
                    clearActiveUnitIfNeeded(unitId)
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        showLeaveDialog = false,
                        isDone = true
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        showLeaveDialog = false,
                        actionError = e.message ?: "Klaida paliekant vienetą"
                    )
                }
        }
    }

    fun clearActionError() { _uiState.value = _uiState.value.copy(actionError = null) }

    private suspend fun refreshPermissions() {
        val tuntasId = tokenManager.activeTuntasId.first() ?: return
        userRepository.getMyPermissions(tuntasId)
            .onSuccess {
                tokenManager.savePermissions(it.permissions)
                tokenManager.saveLeadershipUnitIds(it.leadershipUnitIds)
            }
    }

    private suspend fun clearActiveUnitIfNeeded(unitId: String) {
        if (tokenManager.activeOrgUnitId.first() == unitId) {
            tokenManager.setActiveOrgUnit(null)
        }
    }
}
