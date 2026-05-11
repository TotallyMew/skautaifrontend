package lt.skautai.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.remote.ReservationDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.data.repository.MemberRepository
import lt.skautai.android.data.repository.OrganizationalUnitRepository
import lt.skautai.android.data.repository.RequestRepository
import lt.skautai.android.data.repository.RequisitionRepository
import lt.skautai.android.data.repository.ReservationRepository
import lt.skautai.android.data.repository.UserRepository
import lt.skautai.android.util.TokenManager
import lt.skautai.android.util.canForwardUnitRequests
import lt.skautai.android.util.canManageSharedInventory
import lt.skautai.android.util.canReviewTopLevelRequisitions
import lt.skautai.android.util.hasPermission

data class HomeUiState(
    val isLoading: Boolean = true,
    val activeUnitId: String? = null,
    val activeUnitName: String? = null,
    val availableUnits: List<OrganizationalUnitDto> = emptyList(),
    val activeUnitItemCount: Int = 0,
    val activeUnitFromSharedCount: Int = 0,
    val sharedInventoryCount: Int = 0,
    val sharedPendingApprovalCount: Int = 0,
    val personalLendingCount: Int = 0,
    val requisitionCount: Int = 0,
    val myRequisitionCount: Int = 0,
    val assignedRequisitionCount: Int = 0,
    val sharedRequestCount: Int = 0,
    val myReservationCount: Int = 0,
    val assignedReservationCount: Int = 0,
    val trackedReservationCount: Int = 0,
    val activeReservations: List<ReservationDto> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val reservationRepository: ReservationRepository,
    private val requestRepository: RequestRepository,
    private val requisitionRepository: RequisitionRepository,
    private val memberRepository: MemberRepository,
    private val orgUnitRepository: OrganizationalUnitRepository,
    private val userRepository: UserRepository,
    private val tokenManager: TokenManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val userName: StateFlow<String?> = tokenManager.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        refresh(force = true)
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            val hasLoadedData = _uiState.value.availableUnits.isNotEmpty() ||
                _uiState.value.activeUnitItemCount > 0 ||
                _uiState.value.sharedInventoryCount > 0 ||
                _uiState.value.personalLendingCount > 0

            _uiState.value = _uiState.value.copy(
                isLoading = !hasLoadedData,
                error = null
            )

            val userId = tokenManager.userId.first()
            val activeTuntasId = tokenManager.activeTuntasId.first()
            val permissions = tokenManager.permissions.first()
            activeTuntasId?.let { tuntasId ->
                userRepository.getMyPermissions(tuntasId)
                    .onSuccess { tokenManager.savePermissions(it) }
            }
            val unitsResult = orgUnitRepository.getUnits()
            val currentMember = userId?.let { memberRepository.getMember(it).getOrNull() }
            val currentMemberUnitIds = currentMember?.activeUnitIds().orEmpty()
            val knownUnits = (
                unitsResult.getOrDefault(emptyList()) +
                    currentMember.offlineUnitDtos(activeTuntasId)
                ).distinctBy { it.id }
            val ownUnits = if (currentMemberUnitIds.isNotEmpty()) {
                knownUnits.filter { it.id in currentMemberUnitIds }
            } else {
                knownUnits.filter { unit ->
                    orgUnitRepository.getUnitMembers(unit.id)
                        .getOrDefault(emptyList())
                        .any { it.userId == userId && it.leftAt == null }
                }
            }

            val persistedUnitId = tokenManager.activeOrgUnitId.first()
            val persistedUnit = persistedUnitId?.let { unitId ->
                orgUnitRepository.getUnit(unitId).getOrNull()
            }
            val resolvedUnit = ownUnits.firstOrNull { it.id == persistedUnitId }
                ?: ownUnits.firstOrNull()
                ?: persistedUnit
            if (resolvedUnit?.id != persistedUnitId) {
                tokenManager.setActiveOrgUnit(resolvedUnit?.id)
            }

            val itemsResult = itemRepository.getItems()
            val pendingItemsResult = if (permissions.canManageSharedInventory()) {
                itemRepository.getItems(status = "PENDING_APPROVAL")
            } else {
                Result.success(emptyList())
            }
            val reservationsResult = reservationRepository.getReservations()
            val sharedRequestsResult = requestRepository.getRequests()
            val requisitionsResult = requisitionRepository.getRequests()

            val items = itemsResult.getOrDefault(emptyList())
            val pendingItems = pendingItemsResult.getOrDefault(emptyList())
            val activeReservations = reservationsResult.getOrNull()
                ?.reservations
                ?.filter { it.status in listOf("APPROVED", "ACTIVE") }
                .orEmpty()
            val allReservations = reservationsResult.getOrNull()?.reservations.orEmpty()
            val sharedRequests = sharedRequestsResult.getOrNull()?.requests.orEmpty()
            val requisitions = requisitionsResult.getOrNull()?.requests.orEmpty()
            val myRequisitions = userId?.let { currentUserId ->
                requisitions.count {
                    it.createdByUserId == currentUserId
                }
            } ?: 0
            val assignedRequisitions = requisitions.count { request ->
                val waitsForActiveUnit = request.createdByUserId != userId &&
                    request.requestingUnitId == resolvedUnit?.id &&
                    request.unitReviewStatus == "PENDING" &&
                    (
                        permissions.hasPermission("items.request.approve.unit") ||
                            permissions.canForwardUnitRequests()
                        )
                val waitsForTopLevel = request.createdByUserId != userId &&
                    permissions.canReviewTopLevelRequisitions() &&
                    request.topLevelReviewStatus == "PENDING"
                waitsForActiveUnit || waitsForTopLevel
            }

            val activeUnitItems = resolvedUnit?.id?.let { activeUnitId ->
                items.filter { it.custodianId == activeUnitId && it.type != "INDIVIDUAL" }
            }.orEmpty()
            val myReservations = userId?.let { currentUserId ->
                allReservations.count { it.reservedByUserId == currentUserId && it.status in listOf("APPROVED", "ACTIVE") }
            } ?: 0
            val assignedReservations = allReservations.count { reservation ->
                reservation.status == "PENDING" &&
                    (
                        ("reservations.approve:ALL" in permissions &&
                            reservation.topLevelReviewStatus == "PENDING" &&
                            reservation.items.any { it.custodianId == null }) ||
                            (("reservations.approve:OWN_UNIT" in permissions || "reservations.approve:ALL" in permissions) &&
                                reservation.unitReviewStatus == "PENDING" &&
                                reservation.items.any { it.custodianId != null && it.custodianId == resolvedUnit?.id })
                        )
            }
            val trackedReservations = allReservations.count { reservation ->
                reservation.status in listOf("APPROVED", "ACTIVE") &&
                    reservation.canBeManagedBy(permissions, resolvedUnit?.id) &&
                    (
                        reservation.items.any { it.remainingToIssue > 0 } ||
                            reservation.items.any { it.remainingToReceive > 0 } ||
                            reservation.items.any { it.remainingToReturn > 0 }
                        )
            }
            val sharedItems = items.filter { it.custodianId == null && it.type != "INDIVIDUAL" }
            val personalItems = items.filter { it.type == "INDIVIDUAL" && it.createdByUserId == userId }

            _uiState.value = HomeUiState(
                isLoading = false,
                activeUnitId = resolvedUnit?.id,
                activeUnitName = resolvedUnit?.name,
                availableUnits = ownUnits,
                activeUnitItemCount = activeUnitItems.size,
                activeUnitFromSharedCount = activeUnitItems.count { it.origin == "TRANSFERRED_FROM_TUNTAS" },
                sharedInventoryCount = sharedItems.size,
                sharedPendingApprovalCount = pendingItems.count { it.custodianId == null && it.type != "INDIVIDUAL" },
                personalLendingCount = personalItems.size,
                requisitionCount = requisitions.size,
                myRequisitionCount = myRequisitions,
                assignedRequisitionCount = assignedRequisitions,
                sharedRequestCount = sharedRequests.count { it.topLevelStatus == "PENDING" },
                myReservationCount = myReservations,
                assignedReservationCount = assignedReservations,
                trackedReservationCount = trackedReservations,
                activeReservations = activeReservations.take(5),
                error = listOf(
                    unitsResult.exceptionOrNull(),
                    itemsResult.exceptionOrNull(),
                    pendingItemsResult.exceptionOrNull(),
                    reservationsResult.exceptionOrNull(),
                    sharedRequestsResult.exceptionOrNull(),
                    requisitionsResult.exceptionOrNull()
                ).mapNotNull { it?.message }
                    .distinct()
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString("\n")
            )
        }
    }

    fun selectActiveUnit(unitId: String) {
        viewModelScope.launch {
            tokenManager.setActiveOrgUnit(unitId)
            refresh(force = true)
        }
    }
}

private fun ReservationDto.canBeManagedBy(permissions: Set<String>, activeUnitId: String?): Boolean =
    items.any { item ->
        when {
            item.custodianId == null -> "reservations.approve:ALL" in permissions
            "reservations.approve:ALL" in permissions -> true
            else -> "reservations.approve:OWN_UNIT" in permissions && item.custodianId == activeUnitId
        }
    }

private fun MemberDto.activeUnitIds(): Set<String> =
    (
        unitAssignments.orEmpty().map { it.organizationalUnitId } +
            leadershipRoles
                .filter { it.termStatus == "ACTIVE" && it.organizationalUnitId != null }
                .mapNotNull { it.organizationalUnitId }
        ).toSet()

private fun MemberDto?.offlineUnitDtos(activeTuntasId: String?): List<OrganizationalUnitDto> {
    if (this == null || activeTuntasId == null) return emptyList()
    val assignmentUnits = unitAssignments.orEmpty().map { assignment ->
        OrganizationalUnitDto(
            id = assignment.organizationalUnitId,
            tuntasId = activeTuntasId,
            name = assignment.organizationalUnitName,
            type = "UNKNOWN",
            subtype = null,
            acceptedRankId = null,
            acceptedRankName = null,
            memberCount = 0,
            itemCount = 0,
            createdAt = assignment.joinedAt
        )
    }
    val leadershipUnits = leadershipRoles
        .filter { it.termStatus == "ACTIVE" && it.organizationalUnitId != null && !it.organizationalUnitName.isNullOrBlank() }
        .map { role ->
            OrganizationalUnitDto(
                id = role.organizationalUnitId!!,
                tuntasId = activeTuntasId,
                name = role.organizationalUnitName!!,
                type = "UNKNOWN",
                subtype = null,
                acceptedRankId = null,
                acceptedRankName = null,
                memberCount = 0,
                itemCount = 0,
                createdAt = role.assignedAt
            )
        }
    return (assignmentUnits + leadershipUnits).distinctBy { it.id }
}
