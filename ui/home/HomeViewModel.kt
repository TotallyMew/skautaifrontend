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
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.data.repository.MemberRepository
import lt.skautai.android.data.repository.OrganizationalUnitRepository
import lt.skautai.android.data.repository.RequestRepository
import lt.skautai.android.data.repository.RequisitionRepository
import lt.skautai.android.data.repository.ReservationRepository
import lt.skautai.android.util.TokenManager

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
    private val tokenManager: TokenManager
) : ViewModel() {
    private companion object {
        const val REFRESH_COOLDOWN_MS = 30_000L
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var lastRefreshAtMillis = 0L

    val userName: StateFlow<String?> = tokenManager.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        refresh(force = true)
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val hasLoadedData = _uiState.value.availableUnits.isNotEmpty() ||
                _uiState.value.activeUnitItemCount > 0 ||
                _uiState.value.sharedInventoryCount > 0 ||
                _uiState.value.personalLendingCount > 0
            if (!force && hasLoadedData && now - lastRefreshAtMillis < REFRESH_COOLDOWN_MS) {
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isLoading = !hasLoadedData,
                error = null
            )

            val userId = tokenManager.userId.first()
            val permissions = tokenManager.permissions.first()
            val unitsResult = orgUnitRepository.getUnits()
            val currentMember = userId?.let { memberRepository.getMember(it).getOrNull() }
            val ownUnits = unitsResult.getOrDefault(emptyList()).filter { unit ->
                currentMember?.leadershipRoles?.any {
                    it.termStatus == "ACTIVE" && it.organizationalUnitId == unit.id
                } == true || orgUnitRepository.getUnitMembers(unit.id)
                    .getOrDefault(emptyList())
                    .any { it.userId == userId && it.leftAt == null }
            }

            val persistedUnitId = tokenManager.activeOrgUnitId.first()
            val resolvedUnit = ownUnits.firstOrNull { it.id == persistedUnitId } ?: ownUnits.firstOrNull()
            if (resolvedUnit?.id != persistedUnitId) {
                tokenManager.setActiveOrgUnit(resolvedUnit?.id)
            }

            val itemsResult = itemRepository.getItems()
            val pendingItemsResult = if ("items.transfer" in permissions) {
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
            val openRequisitions = requisitions.filter { it.status in listOf("SUBMITTED", "PARTIALLY_APPROVED") }
            val myRequisitions = userId?.let { currentUserId ->
                requisitions.count {
                    it.createdByUserId == currentUserId &&
                        it.status == "APPROVED" &&
                        it.topLevelReviewStatus == "APPROVED"
                }
            } ?: 0
            val assignedRequisitions = openRequisitions.count { request ->
                val waitsForActiveUnit = request.createdByUserId != userId &&
                    request.requestingUnitId == resolvedUnit?.id &&
                    request.unitReviewStatus == "PENDING"
                val waitsForTopLevel = request.createdByUserId != userId &&
                    "requisitions.approve" in permissions &&
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
            val personalItems = items.filter { it.type == "INDIVIDUAL" }

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
                requisitionCount = openRequisitions.size,
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
                ).firstOrNull()?.message
            )
            lastRefreshAtMillis = now
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
