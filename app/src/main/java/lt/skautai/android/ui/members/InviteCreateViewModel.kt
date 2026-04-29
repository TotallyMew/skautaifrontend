package lt.skautai.android.ui.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.RoleDto
import lt.skautai.android.data.repository.InvitationRepository
import lt.skautai.android.data.repository.MemberRepository
import lt.skautai.android.data.repository.OrganizationalUnitRepository
import lt.skautai.android.data.repository.RoleRepository
import lt.skautai.android.util.TokenManager
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
    val lockedOrgUnitId: String? = null,
    val lockedOrgUnitName: String? = null,
    val canChooseOrgUnit: Boolean = true,
    val generatedCode: String? = null,
    val generatedRoleName: String? = null,
    val expiresAt: String? = null
)

private data class RestrictedInviteConfig(
    val orgUnitId: String,
    val orgUnitName: String,
    val allowedRoleIds: Set<String>
)

@HiltViewModel
class InviteCreateViewModel @Inject constructor(
    private val roleRepository: RoleRepository,
    private val orgUnitRepository: OrganizationalUnitRepository,
    private val invitationRepository: InvitationRepository,
    private val memberRepository: MemberRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(InviteCreateUiState())
    val uiState: StateFlow<InviteCreateUiState> = _uiState.asStateFlow()
    private var allOrgUnits: List<OrganizationalUnitDto> = emptyList()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingRoles = true, error = null)

            val rolesResult = roleRepository.getRoles()
            val orgUnitsResult = orgUnitRepository.getUnits()
            val currentUserId = tokenManager.userId.first()
            val permissions = tokenManager.permissions.first()
            val currentUserResult = currentUserId?.let { memberRepository.getMember(it) }

            rolesResult.onSuccess { roles ->
                val units = orgUnitsResult.getOrDefault(emptyList())
                allOrgUnits = units
                val assignedUnitId = currentUserId?.let { findAssignedUnitId(it, units) }
                val hasGlobalInviteScope = "invitations.create:ALL" in permissions
                val restrictedConfig = if (hasGlobalInviteScope) {
                    null
                } else {
                    currentUserResult
                        ?.getOrNull()
                        ?.let { deriveRestrictedInviteConfig(it, units, roles, assignedUnitId) }
                }

                val visibleRoles = restrictedConfig?.let { config ->
                    roles.filter { it.id in config.allowedRoleIds }
                } ?: roles.filterNot { it.name == "Tuntininkas" }
                val deduplicatedRoles = deduplicateDisplayRoles(visibleRoles)

                val filteredUnitsForSelection = if (restrictedConfig == null) {
                    sortOrgUnits(filterOrgUnitsForRole(units, null))
                } else {
                    emptyList()
                }

                _uiState.value = _uiState.value.copy(
                    roles = deduplicatedRoles,
                    orgUnits = filteredUnitsForSelection,
                    selectedOrgUnitId = restrictedConfig?.orgUnitId,
                    lockedOrgUnitId = restrictedConfig?.orgUnitId,
                    lockedOrgUnitName = restrictedConfig?.orgUnitName,
                    canChooseOrgUnit = restrictedConfig == null,
                    isLoadingRoles = false
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingRoles = false,
                    error = error.message ?: "Klaida gaunant roles"
                )
            }
        }
    }

    fun onRoleSelected(roleId: String) {
        val currentState = _uiState.value
        val role = currentState.roles.find { it.id == roleId }
        val filteredUnits = sortOrgUnits(filterOrgUnitsForRole(allOrgUnits, role))
        val selectedOrgUnitId = if (currentState.lockedOrgUnitId != null) {
            currentState.lockedOrgUnitId
        } else if (roleRequiresOrgUnit(role)) {
            currentState.selectedOrgUnitId
                ?.takeIf { selectedId -> filteredUnits.any { it.id == selectedId } }
        } else {
            null
        }

        _uiState.value = currentState.copy(
            selectedRoleId = roleId,
            selectedRoleType = role?.roleType ?: "",
            selectedOrgUnitId = selectedOrgUnitId,
            orgUnits = if (currentState.canChooseOrgUnit) filteredUnits else currentState.orgUnits
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
        val selectedRole = state.roles.find { it.id == state.selectedRoleId }

        if (state.selectedRoleId.isBlank()) {
            _uiState.value = state.copy(error = "Pasirinkite rolę")
            return
        }

        if (roleRequiresOrgUnit(selectedRole) && (state.lockedOrgUnitId ?: state.selectedOrgUnitId).isNullOrBlank()) {
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

    private fun deriveRestrictedInviteConfig(
        currentMember: MemberDto,
        orgUnits: List<OrganizationalUnitDto>,
        roles: List<RoleDto>,
        assignedUnitId: String?
    ): RestrictedInviteConfig? {
        val activeLeadershipRole = currentMember.leadershipRoles.firstOrNull { leadershipRole ->
            leadershipRole.termStatus == "ACTIVE" &&
                !leadershipRole.organizationalUnitId.isNullOrBlank() &&
                leadershipRole.roleName in ownUnitInviteRoleNames
        }

        if (activeLeadershipRole != null) {
            val orgUnitId = activeLeadershipRole.organizationalUnitId ?: return null
            val unit = orgUnits.find { it.id == orgUnitId } ?: return null
            val allowedRoleIds = resolveAllowedRoleIds(unit, activeLeadershipRole.roleName, roles)

            if (allowedRoleIds.isEmpty()) return null

            return RestrictedInviteConfig(
                orgUnitId = orgUnitId,
                orgUnitName = unit.name,
                allowedRoleIds = allowedRoleIds
            )
        }

        if (currentMember.ranks.none { it.roleName == advisorRankRoleName }) return null

        val assignedUnit = orgUnits.firstOrNull { unit ->
            unit.id == assignedUnitId && unit.type != "GILDIJA"
        } ?: return null

        val allowedRoleIds = resolveAdvisorAllowedRoleIds(assignedUnit, roles)

        if (allowedRoleIds.isEmpty()) return null

        return RestrictedInviteConfig(
            orgUnitId = assignedUnit.id,
            orgUnitName = assignedUnit.name,
            allowedRoleIds = allowedRoleIds
        )
    }

    private fun resolveAllowedRoleIds(
        unit: OrganizationalUnitDto,
        inviterRoleName: String,
        roles: List<RoleDto>
    ): Set<String> {
        val deputyRoleName = deputyRoleNameForInviter(inviterRoleName)
        val canInviteDeputy = inviterRoleName !in deputyInviterRoleNames

        return when (unit.type) {
            "GILDIJA" -> roles
                .filterNot { it.name in guildRestrictedRoleNames || (!canInviteDeputy && it.name == deputyRoleName) }
                .mapTo(linkedSetOf()) { it.id }

            "VYR_SKAUTU_VIENETAS", "VYR_SKAUCIU_VIENETAS" -> roles
                .filter { role ->
                    role.name in seniorScoutAllowedRoleNames ||
                        (canInviteDeputy && role.name == deputyRoleName)
                }
                .mapTo(linkedSetOf()) { it.id }

            else -> buildSet {
                resolveAllowedRankRoleId(unit, roles)?.let(::add)
                roles.firstOrNull { it.name == advisorRankRoleName }?.id?.let(::add)
                if (canInviteDeputy) deputyRoleName?.let { leadershipRoleName ->
                    roles.firstOrNull { it.name == leadershipRoleName }?.id?.let(::add)
                }
            }
        }
    }

    private fun resolveAdvisorAllowedRoleIds(
        unit: OrganizationalUnitDto,
        roles: List<RoleDto>
    ): Set<String> {
        if (unit.type == "GILDIJA") return emptySet()
        val childRoleId = resolveAllowedRankRoleId(unit, roles) ?: return emptySet()
        return setOf(childRoleId)
    }

    private suspend fun findAssignedUnitId(
        currentUserId: String,
        units: List<OrganizationalUnitDto>
    ): String? {
        units.forEach { unit ->
            val hasMember = orgUnitRepository.getUnitMembers(unit.id)
                .getOrDefault(emptyList())
                .any { it.userId == currentUserId && it.leftAt == null }
            if (hasMember) return unit.id
        }
        return null
    }

    private fun resolveAllowedRankRoleId(
        unit: OrganizationalUnitDto,
        roles: List<RoleDto>
    ): String? {
        unit.acceptedRankId?.let { return it }

        val fallbackRoleName = fallbackRankRoleNamesByUnitType[unit.type] ?: return null
        return roles.firstOrNull { it.name == fallbackRoleName }?.id
    }

    private fun filterOrgUnitsForRole(
        orgUnits: List<OrganizationalUnitDto>,
        role: RoleDto?
    ): List<OrganizationalUnitDto> {
        if (role == null || role.roleType != "LEADERSHIP") return orgUnits

        return when (role.name) {
            "Draugininkas", "Draugininko pavaduotojas" -> orgUnits.filter {
                it.type in standardDraugoveUnitTypes
            }

            "Gildijos pirmininkas", "Gildijos pirmininko pavaduotojas" -> orgUnits.filter {
                it.type == "GILDIJA"
            }

            "Vyr. skautu draugoves draugininkas", "Vyr. skautu draugoves draugininko pavaduotojas" -> orgUnits.filter {
                it.type == "VYR_SKAUTU_VIENETAS"
            }

            "Vyr. skautu burelio pirmininkas", "Vyr. skautu burelio pirmininko pavaduotojas" -> orgUnits.filter {
                it.type == "VYR_SKAUTU_VIENETAS"
            }

            "Vyr. skauciu draugoves draugininkas", "Vyr. skauciu draugoves draugininko pavaduotojas" -> orgUnits.filter {
                it.type == "VYR_SKAUCIU_VIENETAS"
            }

            "Vyr. skauciu burelio pirmininkas", "Vyr. skauciu burelio pirmininko pavaduotojas" -> orgUnits.filter {
                it.type == "VYR_SKAUCIU_VIENETAS"
            }

            else -> orgUnits
        }
    }

    private fun sortOrgUnits(orgUnits: List<OrganizationalUnitDto>): List<OrganizationalUnitDto> {
        return orgUnits.sortedWith(
            compareBy<OrganizationalUnitDto> { unitSortOrder(it) }
                .thenBy { it.name.lowercase() }
        )
    }

    private fun deduplicateDisplayRoles(roles: List<RoleDto>): List<RoleDto> {
        val seenKeys = linkedSetOf<Pair<String, String>>()
        return roles.filter { role ->
            seenKeys.add(role.roleType to displayRoleName(role.name))
        }
    }

    private fun roleRequiresOrgUnit(role: RoleDto?): Boolean {
        if (role == null || role.roleType != "LEADERSHIP") return false
        return role.name in unitScopedLeadershipRoleNames
    }

    private fun unitSortOrder(unit: OrganizationalUnitDto): Int = when {
        unit.type == "VYR_SKAUTU_VIENETAS" -> 0
        unit.type == "VYR_SKAUCIU_VIENETAS" -> 1
        unit.type == "GILDIJA" -> 2
        unit.type == "PATYRUSIU_SKAUTU_DRAUGOVE" -> 3
        unit.type == "SKAUTU_DRAUGOVE" -> 4
        unit.type == "VILKU_DRAUGOVE" -> 5
        else -> 6
    }

    private companion object {
        const val advisorRankRoleName = "Vadovas"
        val seniorScoutAllowedRoleNames = setOf(
            "Vyr. skautas",
            "Vyr. skautas kandidatas"
        )
        val guildRestrictedRoleNames = setOf(
            "Skautas",
            "Patyres skautas",
            "Tuntininkas",
            "Tuntininko pavaduotojas"
        )
        val fallbackRankRoleNamesByUnitType = mapOf(
            "SKAUTU_DRAUGOVE" to "Skautas",
            "PATYRUSIU_SKAUTU_DRAUGOVE" to "Patyres skautas"
        )
        val standardDraugoveUnitTypes = setOf(
            "VILKU_DRAUGOVE",
            "SKAUTU_DRAUGOVE",
            "PATYRUSIU_SKAUTU_DRAUGOVE"
        )
        val unitScopedLeadershipRoleNames = setOf(
            "Draugininkas",
            "Draugininko pavaduotojas",
            "Gildijos pirmininkas",
            "Gildijos pirmininko pavaduotojas",
            "Vyr. skautu draugoves draugininkas",
            "Vyr. skautu draugoves draugininko pavaduotojas",
            "Vyr. skautu burelio pirmininkas",
            "Vyr. skautu burelio pirmininko pavaduotojas",
            "Vyr. skauciu draugoves draugininkas",
            "Vyr. skauciu draugoves draugininko pavaduotojas",
            "Vyr. skauciu burelio pirmininkas",
            "Vyr. skauciu burelio pirmininko pavaduotojas"
        )

        val ownUnitInviteLeadershipTargets = mapOf(
            "Draugininkas" to "Draugininko pavaduotojas",
            "Draugininko pavaduotojas" to "Draugininko pavaduotojas",
            "Gildijos pirmininkas" to "Gildijos pirmininko pavaduotojas",
            "Gildijos pirmininko pavaduotojas" to "Gildijos pirmininko pavaduotojas",
            "Vyr. skautu draugoves draugininkas" to "Vyr. skautu draugoves draugininko pavaduotojas",
            "Vyr. skautu draugoves draugininko pavaduotojas" to "Vyr. skautu draugoves draugininko pavaduotojas",
            "Vyr. skautu burelio pirmininkas" to "Vyr. skautu burelio pirmininko pavaduotojas",
            "Vyr. skautu burelio pirmininko pavaduotojas" to "Vyr. skautu burelio pirmininko pavaduotojas",
            "Vyr. skauciu draugoves draugininkas" to "Vyr. skauciu draugoves draugininko pavaduotojas",
            "Vyr. skauciu draugoves draugininko pavaduotojas" to "Vyr. skauciu draugoves draugininko pavaduotojas",
            "Vyr. skauciu burelio pirmininkas" to "Vyr. skauciu burelio pirmininko pavaduotojas",
            "Vyr. skauciu burelio pirmininko pavaduotojas" to "Vyr. skauciu burelio pirmininko pavaduotojas"
        )

        val ownUnitInviteRoleNames = ownUnitInviteLeadershipTargets.keys
        val deputyInviterRoleNames = setOf(
            "Draugininko pavaduotojas",
            "Gildijos pirmininko pavaduotojas",
            "Vyr. skautu draugoves draugininko pavaduotojas",
            "Vyr. skautu burelio pirmininko pavaduotojas",
            "Vyr. skauciu draugoves draugininko pavaduotojas",
            "Vyr. skauciu burelio pirmininko pavaduotojas"
        )
    }

    private fun deputyRoleNameForInviter(inviterRoleName: String): String? =
        ownUnitInviteLeadershipTargets[inviterRoleName]
}
