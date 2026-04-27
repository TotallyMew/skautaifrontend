package lt.skautai.android.data.sync

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import java.io.IOException
import java.io.File
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
import lt.skautai.android.data.local.mapper.cachedInventoryPlan
import lt.skautai.android.data.local.mapper.cachedInventoryCustody
import lt.skautai.android.data.local.mapper.cachedInventoryMovements
import lt.skautai.android.data.local.mapper.cachedPastovykles
import lt.skautai.android.data.local.mapper.cachedPastovykleInventoryById
import lt.skautai.android.data.local.mapper.cachedPastovykleRequestsById
import lt.skautai.android.data.local.mapper.cachedPurchases
import lt.skautai.android.data.local.mapper.toEntity
import lt.skautai.android.data.local.mapper.withCachedInventoryPlan
import lt.skautai.android.data.local.mapper.withCachedInventoryCustody
import lt.skautai.android.data.local.mapper.withCachedInventoryMovements
import lt.skautai.android.data.local.mapper.withCachedPastovykles
import lt.skautai.android.data.local.mapper.withCachedPastovykleInventoryById
import lt.skautai.android.data.local.mapper.withCachedPastovykleRequestsById
import lt.skautai.android.data.local.mapper.withCachedPurchases
import lt.skautai.android.data.remote.AssignLeadershipRoleRequestDto
import lt.skautai.android.data.remote.AssignPastovykleInventoryRequestDto
import lt.skautai.android.data.remote.AssignRankRequestDto
import lt.skautai.android.data.remote.AssignEventRoleRequestDto
import lt.skautai.android.data.remote.AssignUnitInventoryToPastovykleRequestDto
import lt.skautai.android.data.remote.AssignUnitMemberRequestDto
import lt.skautai.android.data.remote.CreateBendrasRequestDto
import lt.skautai.android.data.remote.CreateEventInventoryAllocationRequestDto
import lt.skautai.android.data.remote.CreateEventInventoryBucketRequestDto
import lt.skautai.android.data.remote.CreateEventInventoryItemRequestDto
import lt.skautai.android.data.remote.CreateEventInventoryItemsBulkRequestDto
import lt.skautai.android.data.remote.CreateEventRequestDto
import lt.skautai.android.data.remote.CreateEventInventoryMovementRequestDto
import lt.skautai.android.data.remote.CreateEventPurchaseRequestDto
import lt.skautai.android.data.remote.CreatePastovykleInventoryRequestRequestDto
import lt.skautai.android.data.remote.CreatePastovykleRequestDto
import lt.skautai.android.data.remote.CreateItemRequestDto
import lt.skautai.android.data.remote.CreateLocationRequestDto
import lt.skautai.android.data.remote.UpdateLocationRequestDto
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
import lt.skautai.android.data.remote.UpdateEventInventoryAllocationRequestDto
import lt.skautai.android.data.remote.UpdateEventInventoryBucketRequestDto
import lt.skautai.android.data.remote.UpdateEventInventoryItemRequestDto
import lt.skautai.android.data.remote.UpdateItemRequestDto
import lt.skautai.android.data.remote.UpdateOrganizationalUnitRequestDto
import lt.skautai.android.data.remote.UpdatePastovykleInventoryRequestDto
import lt.skautai.android.data.remote.UpdatePastovykleRequestDto
import lt.skautai.android.data.remote.UpdateReservationPickupRequestDto
import lt.skautai.android.data.remote.UpdateReservationReturnTimeRequestDto
import lt.skautai.android.data.remote.UploadApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import lt.skautai.android.data.repository.UploadRepository
import lt.skautai.android.util.TokenManager
import lt.skautai.android.util.errorMessage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
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
    private val uploadApiService: UploadApiService,
    private val itemDao: ItemDao,
    private val reservationDao: ReservationDao,
    private val bendrasRequestDao: BendrasRequestDao,
    private val requisitionDao: RequisitionDao,
    private val locationDao: LocationDao,
    private val memberDao: MemberDao,
    private val organizationalUnitDao: OrganizationalUnitDao,
    private val eventDao: EventDao,
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    fun observePendingCount(): Flow<Int> = tokenManager.userId.flatMapLatest { userId ->
        userId?.let { pendingOperationDao.observePendingCount(it) } ?: flowOf(0)
    }

    fun observeFailedCount(): Flow<Int> = tokenManager.userId.flatMapLatest { userId ->
        userId?.let { pendingOperationDao.observeFailedCount(it) } ?: flowOf(0)
    }

    fun observeVisibleOperations(): Flow<List<PendingOperationEntity>> =
        tokenManager.userId.flatMapLatest { userId ->
            userId?.let { pendingOperationDao.observeVisibleOperations(it) } ?: flowOf(emptyList())
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

    suspend fun hasCreateOperationInFlight(
        entityType: String,
        entityId: String,
        createOperationType: String
    ): Boolean {
        val userId = tokenManager.userId.first() ?: return false
        val operation = pendingOperationDao.findOperationAnyStatus(userId, entityType, entityId, createOperationType)
            ?: return false
        return operation.status == "SYNCING"
    }

    suspend fun retryFailed() {
        val userId = tokenManager.userId.first()
            ?: throw Exception("Nav prisijungta")
        pendingOperationDao.retryFailed(userId)
        scheduler.schedule()
    }

    suspend fun dismissFailed() {
        val userId = tokenManager.userId.first() ?: return
        pendingOperationDao.deleteFailed(userId)
    }

    suspend fun dismissOperation(operationId: String) {
        pendingOperationDao.deleteOperation(operationId)
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
            PendingOperationType.LOCATION_UPDATE -> {
                val request = gson.fromJson(operation.payloadJson, UpdateLocationRequestDto::class.java)
                val updated = requireSuccessful(locationApiService.updateLocation(auth, operation.tuntasId, operation.entityId, request), "Klaida atnaujinant lokacija")
                locationDao.upsert(updated.toEntity())
            }
            PendingOperationType.LOCATION_DELETE -> {
                requireSuccessfulUnit(locationApiService.deleteLocation(auth, operation.tuntasId, operation.entityId), "Klaida trinant lokacija")
                locationDao.deleteLocation(operation.entityId, operation.tuntasId)
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
            PendingOperationType.EVENT_ASSIGN_ROLE -> {
                val request = gson.fromJson(operation.payloadJson, AssignEventRoleRequestDto::class.java)
                requireSuccessful(eventApiService.assignEventRole(auth, operation.tuntasId, operation.entityId, request), "Klaida priskiriant renginio pareiga")
                refreshEventAfterSync(auth, operation.tuntasId, operation.entityId)
            }
            PendingOperationType.EVENT_REMOVE_ROLE -> {
                val payload = gson.fromJson(operation.payloadJson, EventRoleRemovalPayload::class.java)
                requireSuccessfulEmpty(eventApiService.removeEventRole(auth, operation.tuntasId, payload.eventId, payload.roleId), "Klaida salinant renginio pareiga")
                refreshEventAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_CREATE_BUCKET -> {
                val payload = gson.fromJson(operation.payloadJson, EventBucketCreatePayload::class.java)
                requireSuccessful(eventApiService.createInventoryBucket(auth, operation.tuntasId, payload.eventId, payload.request), "Klaida kuriant plano bucket")
                refreshEventOfflineDetailsAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_UPDATE_BUCKET -> {
                val payload = gson.fromJson(operation.payloadJson, EventBucketUpdatePayload::class.java)
                requireSuccessful(eventApiService.updateInventoryBucket(auth, operation.tuntasId, payload.eventId, payload.bucketId, payload.request), "Klaida atnaujinant plano bucket")
                refreshEventOfflineDetailsAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_DELETE_BUCKET -> {
                val payload = gson.fromJson(operation.payloadJson, EventBucketDeletePayload::class.java)
                requireSuccessfulEmpty(eventApiService.deleteInventoryBucket(auth, operation.tuntasId, payload.eventId, payload.bucketId), "Klaida trinant plano bucket")
                refreshEventOfflineDetailsAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_CREATE_ITEM -> {
                val payload = gson.fromJson(operation.payloadJson, EventItemCreatePayload::class.java)
                requireSuccessful(eventApiService.createInventoryItem(auth, operation.tuntasId, payload.eventId, payload.request), "Klaida kuriant plano eilute")
                refreshEventOfflineDetailsAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_CREATE_ITEMS_BULK -> {
                val payload = gson.fromJson(operation.payloadJson, EventItemsBulkCreatePayload::class.java)
                requireSuccessful(eventApiService.createInventoryItemsBulk(auth, operation.tuntasId, payload.eventId, payload.request), "Klaida kuriant plano eilutes")
                refreshEventOfflineDetailsAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_UPDATE_ITEM -> {
                val payload = gson.fromJson(operation.payloadJson, EventItemUpdatePayload::class.java)
                requireSuccessful(eventApiService.updateInventoryItem(auth, operation.tuntasId, payload.eventId, payload.inventoryItemId, payload.request), "Klaida atnaujinant plano eilute")
                refreshEventOfflineDetailsAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_DELETE_ITEM -> {
                val payload = gson.fromJson(operation.payloadJson, EventItemDeletePayload::class.java)
                requireSuccessfulEmpty(eventApiService.deleteInventoryItem(auth, operation.tuntasId, payload.eventId, payload.inventoryItemId), "Klaida trinant plano eilute")
                refreshEventOfflineDetailsAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_CREATE_ALLOCATION -> {
                val payload = gson.fromJson(operation.payloadJson, EventAllocationCreatePayload::class.java)
                requireSuccessful(eventApiService.createInventoryAllocation(auth, operation.tuntasId, payload.eventId, payload.request), "Klaida kuriant paskirstyma")
                refreshEventOfflineDetailsAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_UPDATE_ALLOCATION -> {
                val payload = gson.fromJson(operation.payloadJson, EventAllocationUpdatePayload::class.java)
                requireSuccessful(eventApiService.updateInventoryAllocation(auth, operation.tuntasId, payload.eventId, payload.allocationId, payload.request), "Klaida atnaujinant paskirstyma")
                refreshEventOfflineDetailsAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_DELETE_ALLOCATION -> {
                val payload = gson.fromJson(operation.payloadJson, EventAllocationDeletePayload::class.java)
                requireSuccessfulEmpty(eventApiService.deleteInventoryAllocation(auth, operation.tuntasId, payload.eventId, payload.allocationId), "Klaida trinant paskirstyma")
                refreshEventOfflineDetailsAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_CREATE_PASTOVYKLE -> {
                val payload = gson.fromJson(operation.payloadJson, EventPastovykleUpsertPayload::class.java)
                requireSuccessful(
                    eventApiService.createPastovykle(auth, operation.tuntasId, payload.eventId, payload.request),
                    "Klaida kuriant pastovykle"
                )
                refreshPastovyklesAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_UPDATE_PASTOVYKLE -> {
                val payload = gson.fromJson(operation.payloadJson, EventPastovykleUpdatePayload::class.java)
                requireSuccessful(
                    eventApiService.updatePastovykle(auth, operation.tuntasId, payload.eventId, payload.pastovykleId, payload.request),
                    "Klaida atnaujinant pastovykle"
                )
                refreshPastovyklesAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_DELETE_PASTOVYKLE -> {
                val payload = gson.fromJson(operation.payloadJson, EventPastovykleDeletePayload::class.java)
                requireSuccessfulEmpty(
                    eventApiService.deletePastovykle(auth, operation.tuntasId, payload.eventId, payload.pastovykleId),
                    "Klaida trinant pastovykle"
                )
                refreshPastovyklesAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_ASSIGN_PASTOVYKLE_INVENTORY -> {
                val payload = gson.fromJson(operation.payloadJson, EventPastovykleInventoryAssignPayload::class.java)
                requireSuccessful(eventApiService.assignPastovykleInventory(auth, operation.tuntasId, payload.eventId, payload.pastovykleId, payload.request), "Klaida priskiriant pastovykles inventoriu")
                refreshEventOfflineDetailsAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_UPDATE_PASTOVYKLE_INVENTORY -> {
                val payload = gson.fromJson(operation.payloadJson, EventPastovykleInventoryUpdatePayload::class.java)
                requireSuccessful(eventApiService.updatePastovykleInventory(auth, operation.tuntasId, payload.eventId, payload.pastovykleId, payload.inventoryId, payload.request), "Klaida atnaujinant pastovykles inventoriu")
                refreshEventOfflineDetailsAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_DELETE_PASTOVYKLE_INVENTORY -> {
                val payload = gson.fromJson(operation.payloadJson, EventPastovykleInventoryDeletePayload::class.java)
                requireSuccessfulEmpty(eventApiService.deletePastovykleInventory(auth, operation.tuntasId, payload.eventId, payload.pastovykleId, payload.inventoryId), "Klaida trinant pastovykles inventoriu")
                refreshEventOfflineDetailsAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_CREATE_PASTOVYKLE_REQUEST -> {
                val payload = gson.fromJson(operation.payloadJson, EventPastovykleRequestCreatePayload::class.java)
                requireSuccessful(eventApiService.createPastovykleRequest(auth, operation.tuntasId, payload.eventId, payload.pastovykleId, payload.request), "Klaida kuriant pastovykles poreiki")
                refreshEventOfflineDetailsAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_APPROVE_PASTOVYKLE_REQUEST -> {
                val payload = gson.fromJson(operation.payloadJson, EventPastovykleRequestActionPayload::class.java)
                requireSuccessful(eventApiService.approvePastovykleRequest(auth, operation.tuntasId, payload.eventId, payload.pastovykleId, payload.requestId), "Klaida tvirtinant pastovykles poreiki")
                refreshEventOfflineDetailsAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_REJECT_PASTOVYKLE_REQUEST -> {
                val payload = gson.fromJson(operation.payloadJson, EventPastovykleRequestActionPayload::class.java)
                requireSuccessful(eventApiService.rejectPastovykleRequest(auth, operation.tuntasId, payload.eventId, payload.pastovykleId, payload.requestId), "Klaida atmetant pastovykles poreiki")
                refreshEventOfflineDetailsAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_SELF_PROVIDE_PASTOVYKLE_REQUEST -> {
                val payload = gson.fromJson(operation.payloadJson, EventPastovykleRequestActionPayload::class.java)
                requireSuccessful(
                    eventApiService.selfProvidePastovykleRequest(
                        auth,
                        operation.tuntasId,
                        payload.eventId,
                        payload.pastovykleId,
                        payload.requestId,
                        lt.skautai.android.data.remote.MarkPastovykleInventoryRequestSelfProvidedRequestDto(payload.notes)
                    ),
                    "Klaida pazymint, kad pasirupinta patiems"
                )
                refreshEventOfflineDetailsAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_FULFILL_PASTOVYKLE_REQUEST -> {
                val payload = gson.fromJson(operation.payloadJson, EventPastovykleRequestActionPayload::class.java)
                requireSuccessful(
                    eventApiService.fulfillPastovykleRequest(
                        auth,
                        operation.tuntasId,
                        payload.eventId,
                        payload.pastovykleId,
                        payload.requestId,
                        lt.skautai.android.data.remote.FulfillPastovykleInventoryRequestRequestDto(payload.quantity, payload.notes)
                    ),
                    "Klaida ivykdant pastovykles poreiki"
                )
                refreshEventOfflineDetailsAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_ASSIGN_FROM_UNIT_INVENTORY -> {
                val payload = gson.fromJson(operation.payloadJson, EventAssignFromUnitInventoryPayload::class.java)
                requireSuccessful(
                    eventApiService.assignFromUnitInventory(
                        auth,
                        operation.tuntasId,
                        payload.eventId,
                        payload.pastovykleId,
                        AssignUnitInventoryToPastovykleRequestDto(payload.itemId, payload.quantity, payload.notes)
                    ),
                    "Klaida priskiriant inventoriu is vieneto"
                )
                refreshEventOfflineDetailsAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_CREATE_PURCHASE -> {
                val payload = gson.fromJson(operation.payloadJson, EventPurchaseCreatePayload::class.java)
                requireSuccessful(
                    eventApiService.createPurchase(auth, operation.tuntasId, payload.eventId, payload.request),
                    "Klaida kuriant pirkima"
                )
                refreshPurchasesAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_ATTACH_PURCHASE_INVOICE -> {
                val payload = gson.fromJson(operation.payloadJson, EventAttachPurchaseInvoicePayload::class.java)
                val invoiceUrl = payload.invoiceFileUrl ?: uploadStagedDocument(auth, payload.stagedDocumentUrl ?: throw Exception("Truksta staged dokumento"))
                requireSuccessful(
                    eventApiService.attachPurchaseInvoice(
                        auth,
                        operation.tuntasId,
                        payload.eventId,
                        payload.purchaseId,
                        lt.skautai.android.data.remote.AttachEventPurchaseInvoiceRequestDto(invoiceUrl)
                    ),
                    "Klaida prisegant saskaita"
                )
                refreshPurchasesAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_COMPLETE_PURCHASE -> {
                val payload = gson.fromJson(operation.payloadJson, EventPurchasePayload::class.java)
                requireSuccessful(
                    eventApiService.completePurchase(auth, operation.tuntasId, payload.eventId, payload.purchaseId),
                    "Klaida uzbaigiant pirkima"
                )
                refreshPurchasesAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_ADD_PURCHASE_TO_INVENTORY -> {
                val payload = gson.fromJson(operation.payloadJson, EventPurchasePayload::class.java)
                requireSuccessful(
                    eventApiService.addPurchaseToInventory(auth, operation.tuntasId, payload.eventId, payload.purchaseId),
                    "Klaida pridedant pirkima i inventoriu"
                )
                refreshPurchasesAfterSync(auth, operation.tuntasId, payload.eventId)
            }
            PendingOperationType.EVENT_CREATE_INVENTORY_MOVEMENT -> {
                val payload = gson.fromJson(operation.payloadJson, EventInventoryMovementPayload::class.java)
                requireSuccessful(
                    eventApiService.createInventoryMovement(auth, operation.tuntasId, payload.eventId, payload.request),
                    "Klaida registruojant renginio judejima"
                )
                refreshInventoryMovementAfterSync(auth, operation.tuntasId, payload.eventId)
            }
        }
    }

    private suspend fun refreshMemberAfterSync(auth: String, tuntasId: String, userId: String) {
        val member = requireSuccessful(memberApiService.getMember(auth, tuntasId, userId), "Klaida atnaujinant nari")
        memberDao.upsert(member.toEntity(tuntasId))
    }

    private suspend fun refreshEventAfterSync(auth: String, tuntasId: String, eventId: String) {
        val event = requireSuccessful(eventApiService.getEvent(auth, tuntasId, eventId), "Klaida atnaujinant rengini")
        eventDao.upsert(event.toEntity())
    }

    private suspend fun refreshPastovyklesAfterSync(auth: String, tuntasId: String, eventId: String) {
        val event = eventDao.getEvent(eventId, tuntasId) ?: return
        val pastovykles = requireSuccessful(
            eventApiService.getPastovykles(auth, tuntasId, eventId),
            "Klaida atnaujinant pastovykles"
        ).pastovykles
        eventDao.upsert(event.withCachedPastovykles(pastovykles))
    }

    private suspend fun refreshPurchasesAfterSync(auth: String, tuntasId: String, eventId: String) {
        val event = eventDao.getEvent(eventId, tuntasId) ?: return
        val purchases = requireSuccessful(
            eventApiService.getPurchases(auth, tuntasId, eventId),
            "Klaida atnaujinant pirkimus"
        ).purchases
        eventDao.upsert(event.withCachedPurchases(purchases))
    }

    private suspend fun refreshInventoryMovementAfterSync(auth: String, tuntasId: String, eventId: String) {
        val event = eventDao.getEvent(eventId, tuntasId) ?: return
        val custody = requireSuccessful(
            eventApiService.getInventoryCustody(auth, tuntasId, eventId),
            "Klaida atnaujinant inventoriaus perdavimus"
        ).custody
        val movements = requireSuccessful(
            eventApiService.getInventoryMovements(auth, tuntasId, eventId),
            "Klaida atnaujinant inventoriaus istorija"
        ).movements
        eventDao.upsert(
            event
                .withCachedInventoryCustody(custody)
                .withCachedInventoryMovements(movements)
        )
    }

    private suspend fun refreshEventOfflineDetailsAfterSync(auth: String, tuntasId: String, eventId: String) {
        val current = eventDao.getEvent(eventId, tuntasId)
        val eventResponse = requireSuccessful(eventApiService.getEvent(auth, tuntasId, eventId), "Klaida atnaujinant rengini")
        val plan = requireSuccessful(
            eventApiService.getInventoryPlan(auth, tuntasId, eventId),
            "Klaida atnaujinant plana"
        )
        val purchases = requireSuccessful(
            eventApiService.getPurchases(auth, tuntasId, eventId),
            "Klaida atnaujinant pirkimus"
        ).purchases
        val pastovykles = requireSuccessful(
            eventApiService.getPastovykles(auth, tuntasId, eventId),
            "Klaida atnaujinant pastovykles"
        ).pastovykles
        val inventoryByPastovykleId = mutableMapOf<String, List<lt.skautai.android.data.remote.PastovykleInventoryDto>>()
        val requestsByPastovykleId = mutableMapOf<String, List<lt.skautai.android.data.remote.EventInventoryRequestDto>>()
        for (pastovykle in pastovykles) {
            inventoryByPastovykleId[pastovykle.id] = requireSuccessful(
                eventApiService.getPastovykleInventory(auth, tuntasId, eventId, pastovykle.id),
                "Klaida atnaujinant pastovykles inventoriu"
            ).inventory
            requestsByPastovykleId[pastovykle.id] = requireSuccessful(
                eventApiService.getPastovykleRequests(auth, tuntasId, eventId, pastovykle.id),
                "Klaida atnaujinant pastovykles poreikius"
            ).requests
        }
        val custody = requireSuccessful(
            eventApiService.getInventoryCustody(auth, tuntasId, eventId),
            "Klaida atnaujinant inventoriaus perdavimus"
        ).custody
        val movements = requireSuccessful(
            eventApiService.getInventoryMovements(auth, tuntasId, eventId),
            "Klaida atnaujinant inventoriaus istorija"
        ).movements
        val merged = eventResponse.toEntity()
            .withCachedInventoryPlan(plan)
            .withCachedPastovykles(pastovykles)
            .withCachedPurchases(purchases)
            .withCachedPastovykleInventoryById(inventoryByPastovykleId)
            .withCachedPastovykleRequestsById(requestsByPastovykleId)
            .withCachedInventoryCustody(custody)
            .withCachedInventoryMovements(movements)
        eventDao.upsert(
            current?.copy(
                id = merged.id,
                tuntasId = merged.tuntasId,
                name = merged.name,
                type = merged.type,
                startDate = merged.startDate,
                endDate = merged.endDate,
                locationId = merged.locationId,
                organizationalUnitId = merged.organizationalUnitId,
                createdByUserId = merged.createdByUserId,
                status = merged.status,
                notes = merged.notes,
                createdAt = merged.createdAt,
                eventRolesJson = merged.eventRolesJson,
                stovyklaDetailsJson = merged.stovyklaDetailsJson
            ) ?: merged
        )
    }

    private suspend fun uploadStagedDocument(auth: String, stagedDocumentUrl: String): String {
        if (!UploadRepository.isStagedDocumentUrl(stagedDocumentUrl)) return stagedDocumentUrl
        val uri = Uri.parse(stagedDocumentUrl)
        val absolutePath = Uri.decode(uri.encodedSchemeSpecificPart.substringBefore("?"))
        val displayName = uri.getQueryParameter("name") ?: File(absolutePath).name
        val mimeType = uri.getQueryParameter("mime") ?: "application/pdf"
        val stagedFile = File(absolutePath)
        if (!stagedFile.exists()) throw Exception("Staged dokumentas nerastas")
        val requestBody = stagedFile.asRequestBody(mimeType.toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", displayName, requestBody)
        val uploaded = requireSuccessful(uploadApiService.uploadDocument(auth, filePart), "Nepavyko ikelti dokumento")
        stagedFile.delete()
        return uploaded.url
    }

    private fun <T> requireSuccessful(response: Response<T>, fallback: String): T {
        if (response.isSuccessful) return response.body() ?: throw Exception(fallback)
        throw Exception(response.errorMessage(fallback))
    }

    private fun requireSuccessfulUnit(response: Response<Unit>, fallback: String) {
        if (!response.isSuccessful) throw Exception(response.errorMessage(fallback))
    }

    private fun requireSuccessfulEmpty(response: Response<*>, fallback: String) {
        if (!response.isSuccessful) throw Exception(response.errorMessage(fallback))
    }
}

data class ReservationMovementSyncPayload(
    val movement: String,
    val requestJson: String
)

data class EventPurchaseCreatePayload(
    val eventId: String,
    val request: CreateEventPurchaseRequestDto
)
