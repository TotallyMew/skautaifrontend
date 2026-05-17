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
import lt.skautai.android.data.repository.EventRepository
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
import lt.skautai.android.util.canReviewItemAdditions
import lt.skautai.android.util.canReviewTopLevelRequisitions
import lt.skautai.android.util.canSubmitItemAddition
import lt.skautai.android.util.hasPermission
import lt.skautai.android.util.NavRoutes

data class HomeTaskUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val count: Int? = null,
    val route: String,
    val kind: HomeTaskKind,
    val priority: Int
)

enum class HomeTaskKind {
    APPROVAL,
    TRACKING,
    RETURN,
    EVENT
}

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
    val tasks: List<HomeTaskUiModel> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val reservationRepository: ReservationRepository,
    private val requestRepository: RequestRepository,
    private val requisitionRepository: RequisitionRepository,
    private val eventRepository: EventRepository,
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
                    .onSuccess {
                        tokenManager.savePermissions(it.permissions)
                        tokenManager.saveLeadershipUnitIds(it.leadershipUnitIds)
                    }
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
            val pendingItemsResult = if (permissions.canReviewItemAdditions() || permissions.canSubmitItemAddition()) {
                itemRepository.getItems(status = "PENDING_APPROVAL")
            } else {
                Result.success(emptyList())
            }
            val reservationsResult = reservationRepository.getReservations()
            val sharedRequestsResult = requestRepository.getRequests()
            val requisitionsResult = requisitionRepository.getRequests()
            val eventsResult = eventRepository.getEvents()

            val items = itemsResult.getOrDefault(emptyList())
            val pendingItems = pendingItemsResult.getOrDefault(emptyList())
            val activeReservations = reservationsResult.getOrNull()
                ?.reservations
                ?.filter { it.status in listOf("APPROVED", "ACTIVE") }
                .orEmpty()
            val allReservations = reservationsResult.getOrNull()?.reservations.orEmpty()
            val sharedRequests = sharedRequestsResult.getOrNull()?.requests.orEmpty()
            val requisitions = requisitionsResult.getOrNull()?.requests.orEmpty()
            val events = eventsResult.getOrNull()?.events.orEmpty()
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
            val myPendingReturns = userId?.let { currentUserId ->
                allReservations.count { reservation ->
                    reservation.reservedByUserId == currentUserId &&
                        reservation.status in listOf("APPROVED", "ACTIVE") &&
                        reservation.items.any { it.remainingToReturn > 0 }
                }
            } ?: 0
            val sharedPickupReviews = sharedRequests.count { request ->
                request.requestedByUserId != userId &&
                    request.topLevelStatus == "PENDING" &&
                    permissions.canManageSharedInventory()
            }
            val eventsNeedingLogistics = events.filter { event ->
                event.status in listOf("PLANNING", "ACTIVE") &&
                    event.inventorySummary?.let { summary ->
                        summary.totalShortageQuantity > 0 || summary.itemsNeedingPurchase > 0
                    } == true
            }
            val sharedItems = items.filter { it.custodianId == null && it.type != "INDIVIDUAL" }
            val personalItems = items.filter { it.type == "INDIVIDUAL" && it.createdByUserId == userId }
            val sharedPendingApprovalCount = pendingItems.count { it.custodianId == null && it.type != "INDIVIDUAL" }
            val homeTasks = buildHomeTasks(
                sharedPendingApprovalCount = sharedPendingApprovalCount,
                assignedReservationCount = assignedReservations,
                trackedReservationCount = trackedReservations,
                myPendingReturnCount = myPendingReturns,
                assignedRequisitionCount = assignedRequisitions,
                sharedPickupReviewCount = sharedPickupReviews,
                eventsNeedingLogistics = eventsNeedingLogistics
            )

            _uiState.value = HomeUiState(
                isLoading = false,
                activeUnitId = resolvedUnit?.id,
                activeUnitName = resolvedUnit?.name,
                availableUnits = ownUnits,
                activeUnitItemCount = activeUnitItems.size,
                activeUnitFromSharedCount = activeUnitItems.count { it.origin == "TRANSFERRED_FROM_TUNTAS" },
                sharedInventoryCount = sharedItems.size,
                sharedPendingApprovalCount = sharedPendingApprovalCount,
                personalLendingCount = personalItems.size,
                requisitionCount = requisitions.size,
                myRequisitionCount = myRequisitions,
                assignedRequisitionCount = assignedRequisitions,
                sharedRequestCount = sharedRequests.count { it.topLevelStatus == "PENDING" },
                myReservationCount = myReservations,
                assignedReservationCount = assignedReservations,
                trackedReservationCount = trackedReservations,
                activeReservations = activeReservations.take(5),
                tasks = homeTasks,
                error = listOf(
                    unitsResult.exceptionOrNull(),
                    itemsResult.exceptionOrNull(),
                    pendingItemsResult.exceptionOrNull(),
                    reservationsResult.exceptionOrNull(),
                    sharedRequestsResult.exceptionOrNull(),
                    requisitionsResult.exceptionOrNull(),
                    eventsResult.exceptionOrNull()
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

private fun buildHomeTasks(
    sharedPendingApprovalCount: Int,
    assignedReservationCount: Int,
    trackedReservationCount: Int,
    myPendingReturnCount: Int,
    assignedRequisitionCount: Int,
    sharedPickupReviewCount: Int,
    eventsNeedingLogistics: List<lt.skautai.android.data.remote.EventDto>
): List<HomeTaskUiModel> {
    val tasks = buildList {
        if (sharedPendingApprovalCount > 0) {
            add(
                HomeTaskUiModel(
                    id = "inventory-approvals",
                    title = "Patvirtink naujus daiktus",
                    subtitle = "Laukia bendro inventoriaus įrašų peržiūra.",
                    count = sharedPendingApprovalCount,
                    route = NavRoutes.InventoryList.createRoute(),
                    kind = HomeTaskKind.APPROVAL,
                    priority = 10
                )
            )
        }
        if (assignedReservationCount > 0) {
            add(
                HomeTaskUiModel(
                    id = "reservation-approvals",
                    title = "Peržiūrėk rezervacijas",
                    subtitle = "Rezervacijos laukia tavo sprendimo.",
                    count = assignedReservationCount,
                    route = NavRoutes.ReservationList.createRoute(mode = "assigned"),
                    kind = HomeTaskKind.APPROVAL,
                    priority = 20
                )
            )
        }
        if (myPendingReturnCount > 0) {
            add(
                HomeTaskUiModel(
                    id = "my-returns",
                    title = "Grąžink paimtus daiktus",
                    subtitle = "Tavo rezervacijose dar liko negrąžintų kiekių.",
                    count = myPendingReturnCount,
                    route = NavRoutes.ReservationList.createRoute(mode = "my_active"),
                    kind = HomeTaskKind.RETURN,
                    priority = 30
                )
            )
        }
        if (trackedReservationCount > 0) {
            add(
                HomeTaskUiModel(
                    id = "tracked-reservations",
                    title = "Užbaik išdavimą ir grąžinimą",
                    subtitle = "Sekamose rezervacijose dar yra neišduotų ar nepriimtų kiekių.",
                    count = trackedReservationCount,
                    route = NavRoutes.ReservationList.createRoute(mode = "tracked"),
                    kind = HomeTaskKind.TRACKING,
                    priority = 40
                )
            )
        }
        if (assignedRequisitionCount > 0) {
            add(
                HomeTaskUiModel(
                    id = "requisition-approvals",
                    title = "Atsakyk į pirkimo prašymus",
                    subtitle = "Vienetų prašymai laukia tavo peržiūros.",
                    count = assignedRequisitionCount,
                    route = NavRoutes.RequestList.createRoute(mode = "assigned"),
                    kind = HomeTaskKind.APPROVAL,
                    priority = 50
                )
            )
        }
        if (sharedPickupReviewCount > 0) {
            add(
                HomeTaskUiModel(
                    id = "shared-pickup-approvals",
                    title = "Peržiūrėk paėmimo prašymus",
                    subtitle = "Vienetai laukia sprendimo dėl bendro inventoriaus paėmimo.",
                    count = sharedPickupReviewCount,
                    route = NavRoutes.SharedRequestList.route,
                    kind = HomeTaskKind.APPROVAL,
                    priority = 60
                )
            )
        }
        if (eventsNeedingLogistics.isNotEmpty()) {
            val totalShortage = eventsNeedingLogistics.sumOf { it.inventorySummary?.totalShortageQuantity ?: 0 }
            val totalPurchases = eventsNeedingLogistics.sumOf { it.inventorySummary?.itemsNeedingPurchase ?: 0 }
            val singleEvent = eventsNeedingLogistics.singleOrNull()
            add(
                HomeTaskUiModel(
                    id = "event-logistics",
                    title = if (singleEvent != null) {
                        "Sutvarkyk renginio logistiką"
                    } else {
                        "Peržiūrėk renginių logistiką"
                    },
                    subtitle = buildString {
                        append(
                            if (singleEvent != null) {
                                singleEvent.name
                            } else {
                                "${eventsNeedingLogistics.size} renginiai turi neužbaigtų logistinių darbų"
                            }
                        )
                        if (totalShortage > 0 || totalPurchases > 0) {
                            append(". ")
                            append("Trūksta $totalShortage vnt.")
                            if (totalPurchases > 0) {
                                append(", pirkimų eilučių: $totalPurchases")
                            }
                            append(".")
                        }
                    },
                    count = eventsNeedingLogistics.size,
                    route = singleEvent?.let { NavRoutes.EventPlan.createRoute(it.id) } ?: NavRoutes.EventList.route,
                    kind = HomeTaskKind.EVENT,
                    priority = 70
                )
            )
        }
    }
    return tasks.sortedBy { it.priority }.take(6)
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
