package lt.skautai.android.data.sync

import com.google.gson.Gson
import java.io.IOException
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import lt.skautai.android.data.local.dao.BendrasRequestDao
import lt.skautai.android.data.local.dao.EventDao
import lt.skautai.android.data.local.dao.ItemDao
import lt.skautai.android.data.local.dao.LocationDao
import lt.skautai.android.data.local.dao.MemberDao
import lt.skautai.android.data.local.dao.OrganizationalUnitDao
import lt.skautai.android.data.local.dao.PendingOperationDao
import lt.skautai.android.data.local.dao.RequisitionDao
import lt.skautai.android.data.local.dao.ReservationDao
import lt.skautai.android.data.local.entity.PendingOperationEntity
import lt.skautai.android.data.local.mapper.toEntity
import lt.skautai.android.data.remote.AssignLeadershipRoleRequestDto
import lt.skautai.android.data.remote.AssignRankRequestDto
import lt.skautai.android.data.remote.AssignUnitMemberRequestDto
import lt.skautai.android.data.remote.CreateBendrasRequestDto
import lt.skautai.android.data.remote.CreateEventRequestDto
import lt.skautai.android.data.remote.CreateItemRequestDto
import lt.skautai.android.data.remote.CreateLocationRequestDto
import lt.skautai.android.data.remote.CreateOrganizationalUnitRequestDto
import lt.skautai.android.data.remote.CreateRequisitionDto
import lt.skautai.android.data.remote.CreateReservationRequestDto
import lt.skautai.android.data.remote.EventApiService
import lt.skautai.android.data.remote.ItemApiService
import lt.skautai.android.data.remote.LocationApiService
import lt.skautai.android.data.remote.MemberApiService
import lt.skautai.android.data.remote.OrganizationalUnitApiService
import lt.skautai.android.data.remote.RequestApiService
import lt.skautai.android.data.remote.RequisitionApiService
import lt.skautai.android.data.remote.RequisitionReviewDto
import lt.skautai.android.data.remote.ReservationApiService
import lt.skautai.android.data.remote.ReservationMovementRequestDto
import lt.skautai.android.data.remote.ReviewReservationRequestDto
import lt.skautai.android.data.remote.ReviewRequestDto
import lt.skautai.android.data.remote.UpdateEventRequestDto
import lt.skautai.android.data.remote.UpdateItemRequestDto
import lt.skautai.android.data.remote.UpdateOrganizationalUnitRequestDto
import lt.skautai.android.data.remote.UpdateReservationPickupRequestDto
import lt.skautai.android.data.remote.UpdateReservationReturnTimeRequestDto
import lt.skautai.android.util.TokenManager
import retrofit2.Response

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class PendingOperationRepository @Inject constructor(
    private val pendingOperationDao: PendingOperationDao,
    private val scheduler: PendingSyncScheduler,
    private val tokenManager: TokenManager,
    private val itemApiService: ItemApiService,
    private val reservationApiService: ReservationApiService,
    private val requestApiService: RequestApiService,
    private val requisitionApiService: RequisitionApiService,
    private val locationApiService: LocationApiService,
    private val memberApiService: MemberApiService,
    private val orgUnitApiService: OrganizationalUnitApiService,
    private val eventApiService: EventApiService,
    private val itemDao: ItemDao,
    private val reservationDao: ReservationDao,
    private val bendrasRequestDao: BendrasRequestDao,
    private val requisitionDao: RequisitionDao,
    private val locationDao: LocationDao,
    private val memberDao: MemberDao,
    private val organizationalUnitDao: OrganizationalUnitDao,
    private val eventDao: EventDao
) {
    private val gson = Gson()

    fun observePendingCount(): Flow<Int> = tokenManager.userId.flatMapLatest { userId ->
        userId?.let { pendingOperationDao.observePendingCount(it) } ?: flowOf(0)
    }

    fun observeFailedCount(): Flow<Int> = tokenManager.userId.flatMapLatest { userId ->
        userId?.let { pendingOperationDao.observeFailedCount(it) } ?: flowOf(0)
    }

    fun observePendingCountForEntity(entityType: String, entityId: String): Flow<Int> =
        tokenManager.userId.flatMapLatest { userId ->
            userId?.let { pendingOperationDao.observePendingCountForEntity(it, entityType, entityId) } ?: flowOf(0)
        }

    fun observeFailedCountForEntity(entityType: String, entityId: String): Flow<Int> =
        tokenManager.userId.flatMapLatest { userId ->
            userId?.let { pendingOperationDao.observeFailedCountForEntity(it, entityType, entityId) } ?: flowOf(0)
        }

    suspend fun enqueue(
        tuntasId: String,
        entityType: String,
        entityId: String,
        operationType: String,
        payload: Any
    ) {
        val userId = tokenManager.userId.first()
            ?: throw Exception("Nav prisijungta")
        pendingOperationDao.upsert(
            PendingOperationEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                tuntasId = tuntasId,
                entityType = entityType,
                entityId = entityId,
                operationType = operationType,
                payloadJson = gson.toJson(payload),
                createdAt = Instant.now().toString()
            )
        )
        scheduler.schedule()
    }

    suspend fun replaceCreatePayloadIfPending(
        entityType: String,
        entityId: String,
        createOperationType: String,
        payload: Any
    ): Boolean {
        val userId = tokenManager.userId.first() ?: return false
        val operation = pendingOperationDao.findOperation(userId, entityType, entityId, createOperationType)
            ?: return false
        pendingOperationDao.updatePayload(operation.id, gson.toJson(payload))
        scheduler.schedule()
        return true
    }

    suspend fun deletePendingCreateIfExists(
        entityType: String,
        entityId: String,
        createOperationType: String
    ): Boolean {
        val userId = tokenManager.userId.first() ?: return false
        val operation = pendingOperationDao.findOperation(userId, entityType, entityId, createOperationType)
            ?: return false
        pendingOperationDao.deleteOperation(operation.id)
        return true
    }

    suspend fun retryFailed() {
        val userId = tokenManager.userId.first()
            ?: throw Exception("Nav prisijungta")
        pendingOperationDao.retryFailed(userId)
        scheduler.schedule()
    }

    suspend fun syncPending(): Result<Unit> {
        val token = tokenManager.token.first()
            ?: return Result.failure(Exception("Nav prisijungta"))
        val userId = tokenManager.userId.first()
            ?: return Result.failure(Exception("Nav prisijungta"))
        val auth = "Bearer $token"
        pendingOperationDao.resetSyncingToPending(userId)
        pendingOperationDao.getPendingOperations(userId).forEach { operation ->
            pendingOperationDao.markSyncing(operation.id)
            try {
                syncOperation(auth, operation)
                pendingOperationDao.markSynced(operation.id)
            } catch (e: IOException) {
                pendingOperationDao.markPendingError(operation.id, e.message ?: "Tinklo klaida")
                return Result.failure(e)
            } catch (e: Exception) {
                pendingOperationDao.markFailed(operation.id, e.message ?: "Sync klaida")
            }
        }
        pendingOperationDao.deleteSynced(userId)
        return Result.success(Unit)
    }

    private suspend fun syncOperation(auth: String, operation: PendingOperationEntity) {
        when (operation.operationType) {
            PendingOperationType.ITEM_CREATE -> {
                val request = gson.fromJson(operation.payloadJson, CreateItemRequestDto::class.java)
                val item = requireSuccessful(itemApiService.createItem(auth, operation.tuntasId, request), "Klaida kuriant daikta")
                itemDao.deleteItem(operation.entityId, operation.tuntasId)
                itemDao.upsert(item.toEntity())
            }
            PendingOperationType.ITEM_UPDATE -> {
                val request = gson.fromJson(operation.payloadJson, UpdateItemRequestDto::class.java)
                val item = requireSuccessful(itemApiService.updateItem(auth, operation.tuntasId, operation.entityId, request), "Klaida atnaujinant daikta")
                itemDao.upsert(item.toEntity())
            }
            PendingOperationType.ITEM_DELETE -> {
                requireSuccessfulUnit(itemApiService.deleteItem(auth, operation.tuntasId, operation.entityId), "Klaida trinant daikta")
                itemDao.deleteItem(operation.entityId, operation.tuntasId)
            }
            PendingOperationType.RESERVATION_CREATE -> {
                val request = gson.fromJson(operation.payloadJson, CreateReservationRequestDto::class.java)
                val reservation = requireSuccessful(reservationApiService.createReservation(auth, operation.tuntasId, request), "Klaida kuriant rezervacija")
                reservationDao.deleteReservation(operation.entityId, operation.tuntasId)
                reservationDao.upsert(reservation.toEntity())
            }
            PendingOperationType.RESERVATION_CANCEL -> {
                requireSuccessfulUnit(reservationApiService.cancelReservation(auth, operation.tuntasId, operation.entityId), "Klaida atsaukiant rezervacija")
                reservationDao.deleteReservation(operation.entityId, operation.tuntasId)
            }
            PendingOperationType.RESERVATION_REVIEW_UNIT -> {
                val request = gson.fromJson(operation.payloadJson, ReviewReservationRequestDto::class.java)
                val reservation = requireSuccessful(reservationApiService.reviewUnitReservation(auth, operation.tuntasId, operation.entityId, request), "Klaida tvirtinant rezervacija")
                reservationDao.upsert(reservation.toEntity())
            }
            PendingOperationType.RESERVATION_REVIEW_TOP_LEVEL -> {
                val request = gson.fromJson(operation.payloadJson, ReviewReservationRequestDto::class.java)
                val reservation = requireSuccessful(reservationApiService.reviewTopLevelReservation(auth, operation.tuntasId, operation.entityId, request), "Klaida tvirtinant rezervacija")
                reservationDao.upsert(reservation.toEntity())
            }
            PendingOperationType.RESERVATION_UPDATE_STATUS -> {
                val request = gson.fromJson(operation.payloadJson, lt.skautai.android.data.remote.UpdateReservationStatusRequestDto::class.java)
                val reservation = requireSuccessful(reservationApiService.updateReservationStatus(auth, operation.tuntasId, operation.entityId, request), "Klaida atnaujinant rezervacija")
                reservationDao.upsert(reservation.toEntity())
            }
            PendingOperationType.RESERVATION_UPDATE_PICKUP -> {
                val request = gson.fromJson(operation.payloadJson, UpdateReservationPickupRequestDto::class.java)
                val reservation = requireSuccessful(reservationApiService.updateReservationPickupTime(auth, operation.tuntasId, operation.entityId, request), "Klaida atnaujinant laika")
                reservationDao.upsert(reservation.toEntity())
            }
            PendingOperationType.RESERVATION_UPDATE_RETURN -> {
                val request = gson.fromJson(operation.payloadJson, UpdateReservationReturnTimeRequestDto::class.java)
                val reservation = requireSuccessful(reservationApiService.updateReservationReturnTime(auth, operation.tuntasId, operation.entityId, request), "Klaida atnaujinant laika")
                reservationDao.upsert(reservation.toEntity())
            }
            PendingOperationType.RESERVATION_MOVEMENT -> {
                val payload = gson.fromJson(operation.payloadJson, ReservationMovementSyncPayload::class.java)
                val request = gson.fromJson(payload.requestJson, ReservationMovementRequestDto::class.java)
                val response = when (payload.movement) {
                    "return" -> reservationApiService.returnReservationItems(auth, operation.tuntasId, operation.entityId, request)
                    "mark_returned" -> reservationApiService.markReservationItemsReturned(auth, operation.tuntasId, operation.entityId, request)
                    else -> reservationApiService.issueReservationItems(auth, operation.tuntasId, operation.entityId, request)
                }
                reservationDao.upsert(requireSuccessful(response, "Klaida registruojant judejima").toEntity())
            }
            PendingOperationType.BENDRAS_REQUEST_CREATE -> {
                val request = gson.fromJson(operation.payloadJson, CreateBendrasRequestDto::class.java)
                val created = requireSuccessful(requestApiService.createRequest(auth, operation.tuntasId, request), "Klaida kuriant prasyma")
                bendrasRequestDao.deleteRequest(operation.entityId, operation.tuntasId)
                bendrasRequestDao.upsert(created.toEntity())
            }
            PendingOperationType.BENDRAS_REQUEST_CANCEL -> {
                requireSuccessfulUnit(requestApiService.cancelRequest(auth, operation.tuntasId, operation.entityId), "Klaida atsaukiant prasyma")
                bendrasRequestDao.deleteRequest(operation.entityId, operation.tuntasId)
            }
            PendingOperationType.BENDRAS_REQUEST_REVIEW_UNIT -> {
                val payload = gson.fromJson(operation.payloadJson, ReviewPayload::class.java)
                val request = ReviewRequestDto(payload.action, payload.rejectionReason)
                val updated = requireSuccessful(requestApiService.draugininkasReview(auth, operation.tuntasId, operation.entityId, request), "Klaida atliekant perziura")
                bendrasRequestDao.upsert(updated.toEntity())
            }
            PendingOperationType.BENDRAS_REQUEST_REVIEW_TOP_LEVEL -> {
                val payload = gson.fromJson(operation.payloadJson, ReviewPayload::class.java)
                val request = ReviewRequestDto(payload.action, payload.rejectionReason)
                val updated = requireSuccessful(requestApiService.topLevelReview(auth, operation.tuntasId, operation.entityId, request), "Klaida atliekant perziura")
                bendrasRequestDao.upsert(updated.toEntity())
            }
            PendingOperationType.REQUISITION_CREATE -> {
                val request = gson.fromJson(operation.payloadJson, CreateRequisitionDto::class.java)
                val created = requireSuccessful(requisitionApiService.createRequest(auth, operation.tuntasId, request), "Klaida kuriant prasyma")
                requisitionDao.deleteRequest(operation.entityId, operation.tuntasId)
                requisitionDao.upsert(created.toEntity())
            }
            PendingOperationType.REQUISITION_REVIEW_UNIT -> {
                val payload = gson.fromJson(operation.payloadJson, ReviewPayload::class.java)
                val request = RequisitionReviewDto(payload.action, payload.rejectionReason)
                val updated = requireSuccessful(requisitionApiService.unitReview(auth, operation.tuntasId, operation.entityId, request), "Klaida atliekant perziura")
                requisitionDao.upsert(updated.toEntity())
            }
            PendingOperationType.REQUISITION_REVIEW_TOP_LEVEL -> {
                val payload = gson.fromJson(operation.payloadJson, ReviewPayload::class.java)
                val request = RequisitionReviewDto(payload.action, payload.rejectionReason)
                val updated = requireSuccessful(requisitionApiService.topLevelReview(auth, operation.tuntasId, operation.entityId, request), "Klaida atliekant perziura")
                requisitionDao.upsert(updated.toEntity())
            }
            PendingOperationType.LOCATION_CREATE -> {
                val request = gson.fromJson(operation.payloadJson, CreateLocationRequestDto::class.java)
                val created = requireSuccessful(locationApiService.createLocation(auth, operation.tuntasId, request), "Klaida kuriant lokacija")
                locationDao.deleteLocation(operation.entityId, operation.tuntasId)
                locationDao.upsert(created.toEntity())
            }
            PendingOperationType.MEMBER_ASSIGN_LEADERSHIP_ROLE -> {
                val request = gson.fromJson(operation.payloadJson, AssignLeadershipRoleRequestDto::class.java)
                requireSuccessful(memberApiService.assignLeadershipRole(auth, operation.tuntasId, operation.entityId, request), "Klaida priskiriant pareigas")
                refreshMemberAfterSync(auth, operation.tuntasId, operation.entityId)
            }
            PendingOperationType.MEMBER_REMOVE_LEADERSHIP_ROLE -> {
                val payload = gson.fromJson(operation.payloadJson, MemberAssignmentPayload::class.java)
                requireSuccessfulEmpty(memberApiService.removeLeadershipRole(auth, operation.tuntasId, payload.userId, payload.assignmentId), "Klaida salinant pareigas")
                refreshMemberAfterSync(auth, operation.tuntasId, payload.userId)
            }
            PendingOperationType.MEMBER_STEP_DOWN_LEADERSHIP_ROLE -> {
                val payload = gson.fromJson(operation.payloadJson, MemberAssignmentPayload::class.java)
                requireSuccessfulEmpty(memberApiService.stepDownLeadershipRole(auth, operation.tuntasId, payload.assignmentId), "Klaida atsistatydinant")
                refreshMemberAfterSync(auth, operation.tuntasId, payload.userId)
            }
            PendingOperationType.MEMBER_ASSIGN_RANK -> {
                val request = gson.fromJson(operation.payloadJson, AssignRankRequestDto::class.java)
                requireSuccessful(memberApiService.assignRank(auth, operation.tuntasId, operation.entityId, request), "Klaida priskiriant laipsni")
                refreshMemberAfterSync(auth, operation.tuntasId, operation.entityId)
            }
            PendingOperationType.MEMBER_REMOVE_RANK -> {
                val payload = gson.fromJson(operation.payloadJson, MemberRankPayload::class.java)
                requireSuccessfulEmpty(memberApiService.removeRank(auth, operation.tuntasId, payload.userId, payload.rankId), "Klaida salinant laipsni")
                refreshMemberAfterSync(auth, operation.tuntasId, payload.userId)
            }
            PendingOperationType.MEMBER_REMOVE -> {
                requireSuccessfulEmpty(memberApiService.removeMember(auth, operation.tuntasId, operation.entityId), "Klaida salinant nari")
                memberDao.deleteMember(operation.entityId, operation.tuntasId)
            }
            PendingOperationType.UNIT_CREATE -> {
                val request = gson.fromJson(operation.payloadJson, CreateOrganizationalUnitRequestDto::class.java)
                val created = requireSuccessful(orgUnitApiService.createUnit(auth, operation.tuntasId, request), "Klaida kuriant vieneta")
                organizationalUnitDao.deleteUnit(operation.entityId, operation.tuntasId)
                organizationalUnitDao.upsert(created.toEntity())
            }
            PendingOperationType.UNIT_UPDATE -> {
                val request = gson.fromJson(operation.payloadJson, UpdateOrganizationalUnitRequestDto::class.java)
                val updated = requireSuccessful(orgUnitApiService.updateUnit(auth, operation.tuntasId, operation.entityId, request), "Klaida atnaujinant vieneta")
                organizationalUnitDao.upsert(updated.toEntity())
            }
            PendingOperationType.UNIT_DELETE -> {
                requireSuccessfulEmpty(orgUnitApiService.deleteUnit(auth, operation.tuntasId, operation.entityId), "Klaida trinant vieneta")
                organizationalUnitDao.deleteUnit(operation.entityId, operation.tuntasId)
            }
            PendingOperationType.UNIT_ASSIGN_MEMBER -> {
                val request = gson.fromJson(operation.payloadJson, AssignUnitMemberRequestDto::class.java)
                requireSuccessful(orgUnitApiService.assignUnitMember(auth, operation.tuntasId, operation.entityId, request), "Klaida priskiriant nari")
                refreshMemberAfterSync(auth, operation.tuntasId, request.userId)
            }
            PendingOperationType.UNIT_REMOVE_MEMBER -> {
                val payload = gson.fromJson(operation.payloadJson, UnitMemberPayload::class.java)
                requireSuccessfulEmpty(orgUnitApiService.removeUnitMember(auth, operation.tuntasId, payload.unitId, payload.userId), "Klaida salinant nari")
                refreshMemberAfterSync(auth, operation.tuntasId, payload.userId)
            }
            PendingOperationType.UNIT_LEAVE -> {
                requireSuccessfulEmpty(orgUnitApiService.leaveUnit(auth, operation.tuntasId, operation.entityId), "Klaida paliekant vieneta")
            }
            PendingOperationType.UNIT_MOVE_MEMBER -> {
                val payload = gson.fromJson(operation.payloadJson, UnitMemberPayload::class.java)
                requireSuccessful(orgUnitApiService.moveUnitMember(auth, operation.tuntasId, payload.unitId, payload.userId), "Klaida perkeliant nari")
                refreshMemberAfterSync(auth, operation.tuntasId, payload.userId)
            }
            PendingOperationType.EVENT_CREATE -> {
                val request = gson.fromJson(operation.payloadJson, CreateEventRequestDto::class.java)
                val created = requireSuccessful(eventApiService.createEvent(auth, operation.tuntasId, request), "Klaida kuriant rengini")
                eventDao.deleteEvent(operation.entityId, operation.tuntasId)
                eventDao.upsert(created.toEntity())
            }
            PendingOperationType.EVENT_UPDATE -> {
                val request = gson.fromJson(operation.payloadJson, UpdateEventRequestDto::class.java)
                val updated = requireSuccessful(eventApiService.updateEvent(auth, operation.tuntasId, operation.entityId, request), "Klaida atnaujinant rengini")
                eventDao.upsert(updated.toEntity())
            }
            PendingOperationType.EVENT_CANCEL -> {
                requireSuccessfulUnit(eventApiService.cancelEvent(auth, operation.tuntasId, operation.entityId), "Klaida atsaukiant rengini")
                eventDao.deleteEvent(operation.entityId, operation.tuntasId)
            }
        }
    }

    private suspend fun refreshMemberAfterSync(auth: String, tuntasId: String, userId: String) {
        val member = requireSuccessful(memberApiService.getMember(auth, tuntasId, userId), "Klaida atnaujinant nari")
        memberDao.upsert(member.toEntity(tuntasId))
    }

    private fun <T> requireSuccessful(response: Response<T>, fallback: String): T {
        if (response.isSuccessful) return response.body() ?: throw Exception(fallback)
        throw Exception(response.errorBody()?.string() ?: fallback)
    }

    private fun requireSuccessfulUnit(response: Response<Unit>, fallback: String) {
        if (!response.isSuccessful) throw Exception(response.errorBody()?.string() ?: fallback)
    }

    private fun requireSuccessfulEmpty(response: Response<*>, fallback: String) {
        if (!response.isSuccessful) throw Exception(response.errorBody()?.string() ?: fallback)
    }
}

data class ReservationMovementSyncPayload(
    val movement: String,
    val requestJson: String
)
