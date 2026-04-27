package lt.skautai.android.data.repository

import java.io.IOException
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import lt.skautai.android.data.local.dao.EventDao
import lt.skautai.android.data.local.mapper.cachedInventoryCustody
import lt.skautai.android.data.local.mapper.cachedInventoryPlan
import lt.skautai.android.data.local.mapper.cachedInventoryMovements
import lt.skautai.android.data.local.mapper.cachedPastovykles
import lt.skautai.android.data.local.mapper.cachedPastovykleInventoryById
import lt.skautai.android.data.local.mapper.cachedPastovykleRequestsById
import lt.skautai.android.data.local.mapper.cachedPurchases
import lt.skautai.android.data.local.mapper.toDto
import lt.skautai.android.data.local.mapper.toEntity
import lt.skautai.android.data.local.mapper.toEventDtos
import lt.skautai.android.data.local.mapper.toEventEntities
import lt.skautai.android.data.local.mapper.withCachedInventoryCustody
import lt.skautai.android.data.local.mapper.withCachedInventoryPlan
import lt.skautai.android.data.local.mapper.withCachedInventoryMovements
import lt.skautai.android.data.local.mapper.withCachedPastovykles
import lt.skautai.android.data.local.mapper.withCachedPastovykleInventoryById
import lt.skautai.android.data.local.mapper.withCachedPastovykleRequestsById
import lt.skautai.android.data.local.mapper.withCachedPurchases
import lt.skautai.android.data.remote.CreateEventRequestDto
import lt.skautai.android.data.remote.AssignEventRoleRequestDto
import lt.skautai.android.data.remote.CreateEventInventoryAllocationRequestDto
import lt.skautai.android.data.remote.CreateEventInventoryBucketRequestDto
import lt.skautai.android.data.remote.CreateEventInventoryItemRequestDto
import lt.skautai.android.data.remote.CreateEventInventoryItemsBulkRequestDto
import lt.skautai.android.data.remote.CreateEventInventoryMovementRequestDto
import lt.skautai.android.data.remote.CreatePastovykleInventoryRequestRequestDto
import lt.skautai.android.data.remote.CreateEventPurchaseRequestDto
import lt.skautai.android.data.remote.AttachEventPurchaseInvoiceRequestDto
import lt.skautai.android.data.remote.CreatePastovykleRequestDto
import lt.skautai.android.data.remote.AssignPastovykleInventoryRequestDto
import lt.skautai.android.data.remote.AssignUnitInventoryToPastovykleRequestDto
import lt.skautai.android.data.remote.EventApiService
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.EventRoleDto
import lt.skautai.android.data.remote.EventInventoryAllocationDto
import lt.skautai.android.data.remote.EventInventoryBucketDto
import lt.skautai.android.data.remote.EventInventoryCustodyDto
import lt.skautai.android.data.remote.EventInventoryCustodyListDto
import lt.skautai.android.data.remote.EventInventoryItemDto
import lt.skautai.android.data.remote.EventInventoryItemListDto
import lt.skautai.android.data.remote.EventInventoryMovementDto
import lt.skautai.android.data.remote.EventInventoryMovementListDto
import lt.skautai.android.data.remote.EventInventoryPlanDto
import lt.skautai.android.data.remote.EventInventoryRequestDto
import lt.skautai.android.data.remote.EventInventoryRequestListDto
import lt.skautai.android.data.remote.EventListDto
import lt.skautai.android.data.remote.EventPurchaseItemDto
import lt.skautai.android.data.remote.EventPurchaseDto
import lt.skautai.android.data.remote.EventPurchaseListDto
import lt.skautai.android.data.remote.PastovykleDto
import lt.skautai.android.data.remote.PastovykleInventoryDto
import lt.skautai.android.data.remote.PastovykleInventoryListDto
import lt.skautai.android.data.remote.PastovykleListDto
import lt.skautai.android.data.remote.FulfillPastovykleInventoryRequestRequestDto
import lt.skautai.android.data.remote.MarkPastovykleInventoryRequestSelfProvidedRequestDto
import lt.skautai.android.data.remote.UpdateEventRequestDto
import lt.skautai.android.data.remote.UpdateEventInventoryAllocationRequestDto
import lt.skautai.android.data.remote.UpdateEventInventoryBucketRequestDto
import lt.skautai.android.data.remote.UpdateEventInventoryItemRequestDto
import lt.skautai.android.data.remote.UpdateEventPurchaseRequestDto
import lt.skautai.android.data.remote.UpdatePastovykleInventoryRequestDto
import lt.skautai.android.data.remote.UpdatePastovykleRequestDto
import lt.skautai.android.data.sync.EventRoleRemovalPayload
import lt.skautai.android.data.sync.EventPastovykleDeletePayload
import lt.skautai.android.data.sync.EventPastovykleInventoryAssignPayload
import lt.skautai.android.data.sync.EventPastovykleInventoryDeletePayload
import lt.skautai.android.data.sync.EventPastovykleInventoryUpdatePayload
import lt.skautai.android.data.sync.EventPastovykleRequestActionPayload
import lt.skautai.android.data.sync.EventPastovykleRequestCreatePayload
import lt.skautai.android.data.sync.EventPastovykleUpdatePayload
import lt.skautai.android.data.sync.EventPastovykleUpsertPayload
import lt.skautai.android.data.sync.EventBucketCreatePayload
import lt.skautai.android.data.sync.EventBucketDeletePayload
import lt.skautai.android.data.sync.EventBucketUpdatePayload
import lt.skautai.android.data.sync.EventItemCreatePayload
import lt.skautai.android.data.sync.EventItemDeletePayload
import lt.skautai.android.data.sync.EventItemUpdatePayload
import lt.skautai.android.data.sync.EventItemsBulkCreatePayload
import lt.skautai.android.data.sync.EventAllocationCreatePayload
import lt.skautai.android.data.sync.EventAllocationDeletePayload
import lt.skautai.android.data.sync.EventAllocationUpdatePayload
import lt.skautai.android.data.sync.EventAssignFromUnitInventoryPayload
import lt.skautai.android.data.sync.EventAttachPurchaseInvoicePayload
import lt.skautai.android.data.sync.EventPurchasePayload
import lt.skautai.android.data.sync.EventInventoryMovementPayload
import lt.skautai.android.data.sync.PendingEntityType
import lt.skautai.android.data.sync.PendingOperationRepository
import lt.skautai.android.data.sync.PendingOperationType
import lt.skautai.android.data.sync.EventPurchaseCreatePayload
import lt.skautai.android.util.SESSION_EXPIRED_MESSAGE
import lt.skautai.android.util.TUNTAS_SELECTION_REQUIRED_MESSAGE
import lt.skautai.android.util.TokenManager
import lt.skautai.android.util.errorMessage

@Singleton
class EventRepository @Inject constructor(
    private val eventApiService: EventApiService,
    private val tokenManager: TokenManager,
    private val eventDao: EventDao,
    private val pendingOperationRepository: PendingOperationRepository
) {
    private suspend fun token() = tokenManager.token.first()
        ?: throw Exception(SESSION_EXPIRED_MESSAGE)

    private suspend fun tuntasId() = tokenManager.activeTuntasId.first()
        ?: throw Exception(TUNTAS_SELECTION_REQUIRED_MESSAGE)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeEvents(type: String? = null, status: String? = null): Flow<EventListDto> {
        return tokenManager.activeTuntasId.flatMapLatest { currentTuntasId ->
            if (currentTuntasId == null) {
                flowOf(EventListDto(emptyList(), 0))
            } else {
                eventDao.observeEvents(currentTuntasId, type, status)
                    .map { events -> EventListDto(events.toEventDtos(), events.size) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeEvent(id: String): Flow<EventDto?> {
        return tokenManager.activeTuntasId.flatMapLatest { currentTuntasId ->
            if (currentTuntasId == null) flowOf(null)
            else eventDao.observeEvent(id, currentTuntasId).map { it?.toDto() }
        }
    }

    suspend fun refreshEvents(type: String? = null, status: String? = null): Result<Unit> {
        return try {
            val currentTuntasId = tuntasId()
            val response = eventApiService.getEvents("Bearer ${token()}", currentTuntasId, type, status)
            if (response.isSuccessful) {
                val events = response.body()?.events.orEmpty()
                eventDao.deleteForQuery(currentTuntasId, type, status)
                eventDao.upsertAll(events.toEventEntities())
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorMessage("Nepavyko gauti renginių.")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshEvent(id: String): Result<Unit> {
        if (id.startsWith("local-")) return Result.success(Unit)
        return try {
            val currentTuntasId = tuntasId()
            val response = eventApiService.getEvent("Bearer ${token()}", currentTuntasId, id)
            if (response.isSuccessful) {
                eventDao.upsert(response.body()!!.toEntity())
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorMessage("Nepavyko gauti renginio.")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEvents(type: String? = null, status: String? = null): Result<EventListDto> {
        val refreshResult = refreshEvents(type, status)
        val currentTuntasId = tokenManager.activeTuntasId.first()
        val cachedEvents = currentTuntasId
            ?.let { eventDao.getEvents(it, type, status).toEventDtos() }
            .orEmpty()
        return if (refreshResult.isSuccess || cachedEvents.isNotEmpty()) {
            Result.success(EventListDto(cachedEvents, cachedEvents.size))
        } else {
            Result.failure(refreshResult.exceptionOrNull() ?: Exception("Nepavyko gauti renginių."))
        }
    }

    suspend fun getEvent(id: String): Result<EventDto> {
        if (id.startsWith("local-")) {
            val currentTuntasId = tokenManager.activeTuntasId.first()
            val cachedEvent = currentTuntasId?.let { eventDao.getEvent(id, it)?.toDto() }
            return if (cachedEvent != null) {
                Result.success(cachedEvent)
            } else {
                Result.failure(Exception("Renginys nerastas"))
            }
        }
        val refreshResult = refreshEvent(id)
        val currentTuntasId = tokenManager.activeTuntasId.first()
        val cachedEvent = currentTuntasId?.let { eventDao.getEvent(id, it)?.toDto() }
        return if (cachedEvent != null) {
            Result.success(cachedEvent)
        } else {
            Result.failure(refreshResult.exceptionOrNull() ?: Exception("Nepavyko gauti renginio."))
        }
    }

    suspend fun createEvent(request: CreateEventRequestDto): Result<EventDto> {
        return try {
            val response = eventApiService.createEvent("Bearer ${token()}", tuntasId(), request)
            if (response.isSuccessful) {
                val event = response.body()!!
                eventDao.upsert(event.toEntity())
                Result.success(event)
            } else {
                Result.failure(Exception(response.errorMessage("Nepavyko sukurti renginio.")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val eventId = "local-${UUID.randomUUID()}"
            val event = EventDto(
                id = eventId,
                tuntasId = currentTuntasId,
                name = request.name,
                type = request.type,
                startDate = request.startDate,
                endDate = request.endDate,
                locationId = null,
                organizationalUnitId = null,
                createdByUserId = null,
                status = "ACTIVE",
                notes = request.notes,
                createdAt = Instant.now().toString(),
                eventRoles = emptyList(),
                inventorySummary = null
            )
            eventDao.upsert(event.toEntity())
            pendingOperationRepository.enqueue(
                tuntasId = currentTuntasId,
                entityType = PendingEntityType.EVENT,
                entityId = event.id,
                operationType = PendingOperationType.EVENT_CREATE,
                payload = request
            )
            Result.success(event)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEvent(id: String, request: UpdateEventRequestDto): Result<EventDto> {
        return try {
            val response = eventApiService.updateEvent("Bearer ${token()}", tuntasId(), id, request)
            if (response.isSuccessful) {
                val event = response.body()!!
                eventDao.upsert(event.toEntity())
                Result.success(event)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val cached = eventDao.getEvent(id, currentTuntasId)?.toDto()
                ?: return Result.failure(Exception("Renginys nerastas offline cache"))
            val updated = cached.copy(
                name = request.name ?: cached.name,
                status = request.status ?: cached.status,
                notes = request.notes ?: cached.notes
            )
            eventDao.upsert(updated.toEntity())
            val mergedIntoCreate = if (id.startsWith("local-")) {
                pendingOperationRepository.replaceCreatePayloadIfPending(
                    entityType = PendingEntityType.EVENT,
                    entityId = id,
                    createOperationType = PendingOperationType.EVENT_CREATE,
                    payload = updated.toCreateRequest()
                )
            } else {
                false
            }
            if (!mergedIntoCreate) {
                pendingOperationRepository.enqueue(
                    tuntasId = currentTuntasId,
                    entityType = PendingEntityType.EVENT,
                    entityId = id,
                    operationType = PendingOperationType.EVENT_UPDATE,
                    payload = request
                )
            }
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelEvent(id: String): Result<Unit> {
        return try {
            val currentTuntasId = tuntasId()
            val response = eventApiService.cancelEvent("Bearer ${token()}", currentTuntasId, id)
            if (response.isSuccessful) {
                eventDao.getEvent(id, currentTuntasId)?.let { existing ->
                    eventDao.upsert(existing.copy(status = "CANCELLED"))
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            if (id.startsWith("local-") && pendingOperationRepository.hasCreateOperationInFlight(
                    entityType = PendingEntityType.EVENT,
                    entityId = id,
                    createOperationType = PendingOperationType.EVENT_CREATE
                )
            ) {
                return Result.failure(Exception("Renginys dabar sinchronizuojamas. Pabandykite dar kartą vėliau."))
            }
            if (id.startsWith("local-") && pendingOperationRepository.deletePendingCreateIfExists(
                    entityType = PendingEntityType.EVENT,
                    entityId = id,
                    createOperationType = PendingOperationType.EVENT_CREATE
                )
            ) {
                eventDao.deleteEvent(id, currentTuntasId)
                return Result.success(Unit)
            }
            val cached = eventDao.getEvent(id, currentTuntasId)
                ?: return Result.failure(Exception("Renginys nerastas offline cache"))
            eventDao.upsert(cached.copy(status = "CANCELLED"))
            pendingOperationRepository.enqueue(
                tuntasId = currentTuntasId,
                entityType = PendingEntityType.EVENT,
                entityId = id,
                operationType = PendingOperationType.EVENT_CANCEL,
                payload = mapOf("id" to id)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun assignEventRole(eventId: String, request: AssignEventRoleRequestDto): Result<Unit> {
        return try {
            val response = eventApiService.assignEventRole("Bearer ${token()}", tuntasId(), eventId, request)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorMessage("Klaida")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val cached = eventDao.getEvent(eventId, currentTuntasId)?.toDto()
                ?: return Result.failure(Exception("Renginys nerastas offline cache"))
            val optimisticRole = EventRoleDto(
                id = "local-${UUID.randomUUID()}",
                userId = request.userId,
                userName = null,
                role = request.role,
                targetGroup = request.targetGroup,
                assignedByUserId = tokenManager.userId.first(),
                assignedAt = Instant.now().toString()
            )
            eventDao.upsert(cached.copy(eventRoles = cached.eventRoles + optimisticRole).toEntity())
            pendingOperationRepository.enqueue(
                tuntasId = currentTuntasId,
                entityType = PendingEntityType.EVENT,
                entityId = eventId,
                operationType = PendingOperationType.EVENT_ASSIGN_ROLE,
                payload = request
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeEventRole(eventId: String, roleId: String): Result<Unit> {
        return try {
            val response = eventApiService.removeEventRole("Bearer ${token()}", tuntasId(), eventId, roleId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorMessage("Klaida")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val cached = eventDao.getEvent(eventId, currentTuntasId)?.toDto()
                ?: return Result.failure(Exception("Renginys nerastas offline cache"))
            eventDao.upsert(cached.copy(eventRoles = cached.eventRoles.filterNot { it.id == roleId }).toEntity())
            pendingOperationRepository.enqueue(
                tuntasId = currentTuntasId,
                entityType = PendingEntityType.EVENT,
                entityId = eventId,
                operationType = PendingOperationType.EVENT_REMOVE_ROLE,
                payload = EventRoleRemovalPayload(eventId, roleId)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInventoryPlan(eventId: String): Result<EventInventoryPlanDto> {
        return try {
            val response = eventApiService.getInventoryPlan("Bearer ${token()}", tuntasId(), eventId)
            if (response.isSuccessful) {
                val plan = response.body() ?: EventInventoryPlanDto(emptyList(), emptyList(), emptyList())
                cacheInventoryPlan(eventId, plan)
                Result.success(plan)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida")))
            }
        } catch (e: IOException) {
            cachedInventoryPlan(eventId)?.let { Result.success(it) } ?: Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createInventoryBucket(
        eventId: String,
        request: CreateEventInventoryBucketRequestDto
    ): Result<EventInventoryBucketDto> {
        return try {
            val response = eventApiService.createInventoryBucket("Bearer ${token()}", tuntasId(), eventId, request)
            if (response.isSuccessful) {
                val bucket = response.body()!!
                upsertCachedBucket(eventId, bucket)
                Result.success(bucket)
            } else Result.failure(Exception(response.errorMessage("Klaida")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val bucket = EventInventoryBucketDto(
                id = "local-${UUID.randomUUID()}",
                eventId = eventId,
                name = request.name,
                type = request.type,
                pastovykleId = request.pastovykleId,
                pastovykleName = request.pastovykleId?.let { cachedPastovykles(eventId)?.firstOrNull { pastovykle -> pastovykle.id == it }?.name },
                locationId = request.locationId,
                locationPath = null,
                notes = request.notes
            )
            upsertCachedBucket(eventId, bucket)
            pendingOperationRepository.enqueue(currentTuntasId, PendingEntityType.EVENT, bucket.id, PendingOperationType.EVENT_CREATE_BUCKET, EventBucketCreatePayload(eventId, request))
            Result.success(bucket)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateInventoryBucket(
        eventId: String,
        bucketId: String,
        request: UpdateEventInventoryBucketRequestDto
    ): Result<EventInventoryBucketDto> {
        return try {
            val response = eventApiService.updateInventoryBucket("Bearer ${token()}", tuntasId(), eventId, bucketId, request)
            if (response.isSuccessful) {
                val bucket = response.body()!!
                upsertCachedBucket(eventId, bucket)
                Result.success(bucket)
            } else Result.failure(Exception(response.errorMessage("Klaida")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val cached = cachedInventoryPlan(eventId)?.buckets?.firstOrNull { it.id == bucketId }
                ?: return Result.failure(Exception("Bucket nerastas offline cache"))
            val updated = cached.copy(
                name = request.name ?: cached.name,
                type = request.type ?: cached.type,
                pastovykleId = request.pastovykleId ?: cached.pastovykleId,
                pastovykleName = request.pastovykleId?.let { cachedPastovykles(eventId)?.firstOrNull { pastovykle -> pastovykle.id == it }?.name }
                    ?: cached.pastovykleName,
                locationId = request.locationId ?: cached.locationId,
                notes = request.notes ?: cached.notes
            )
            upsertCachedBucket(eventId, updated)
            pendingOperationRepository.enqueue(currentTuntasId, PendingEntityType.EVENT, bucketId, PendingOperationType.EVENT_UPDATE_BUCKET, EventBucketUpdatePayload(eventId, bucketId, request))
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteInventoryBucket(eventId: String, bucketId: String): Result<Unit> {
        return try {
            val response = eventApiService.deleteInventoryBucket("Bearer ${token()}", tuntasId(), eventId, bucketId)
            if (response.isSuccessful) {
                deleteCachedBucket(eventId, bucketId)
                Result.success(Unit)
            } else Result.failure(Exception(response.errorMessage("Klaida")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            deleteCachedBucket(eventId, bucketId)
            pendingOperationRepository.enqueue(currentTuntasId, PendingEntityType.EVENT, bucketId, PendingOperationType.EVENT_DELETE_BUCKET, EventBucketDeletePayload(eventId, bucketId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createInventoryItem(
        eventId: String,
        request: CreateEventInventoryItemRequestDto
    ): Result<EventInventoryItemDto> {
        return try {
            val response = eventApiService.createInventoryItem("Bearer ${token()}", tuntasId(), eventId, request)
            if (response.isSuccessful) {
                val item = response.body()!!
                upsertCachedInventoryItem(eventId, item)
                Result.success(item)
            } else Result.failure(Exception(response.errorMessage("Klaida")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val now = Instant.now().toString()
            val item = EventInventoryItemDto(
                id = "local-${UUID.randomUUID()}",
                eventId = eventId,
                itemId = request.itemId,
                bucketId = request.bucketId,
                bucketName = request.bucketId?.let { cachedInventoryPlan(eventId)?.buckets?.firstOrNull { bucket -> bucket.id == it }?.name },
                reservationGroupId = null,
                name = request.name,
                plannedQuantity = request.plannedQuantity,
                availableQuantity = 0,
                shortageQuantity = request.plannedQuantity,
                allocatedQuantity = 0,
                unallocatedQuantity = 0,
                needsPurchase = true,
                notes = request.notes,
                responsibleUserId = request.responsibleUserId,
                responsibleUserName = null,
                createdByUserId = tokenManager.userId.first(),
                createdAt = now
            )
            upsertCachedInventoryItem(eventId, item)
            pendingOperationRepository.enqueue(currentTuntasId, PendingEntityType.EVENT, item.id, PendingOperationType.EVENT_CREATE_ITEM, EventItemCreatePayload(eventId, request))
            Result.success(item)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createInventoryItemsBulk(
        eventId: String,
        request: CreateEventInventoryItemsBulkRequestDto
    ): Result<EventInventoryItemListDto> {
        return try {
            val response = eventApiService.createInventoryItemsBulk("Bearer ${token()}", tuntasId(), eventId, request)
            if (response.isSuccessful) {
                val created = response.body()!!
                mergeCachedInventoryItems(eventId, created.items)
                Result.success(created)
            } else Result.failure(Exception(response.errorMessage("Klaida")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val now = Instant.now().toString()
            val items = request.items.map { row ->
                EventInventoryItemDto(
                    id = "local-${UUID.randomUUID()}",
                    eventId = eventId,
                    itemId = row.itemId,
                    bucketId = row.bucketId,
                    bucketName = row.bucketId?.let { cachedInventoryPlan(eventId)?.buckets?.firstOrNull { bucket -> bucket.id == it }?.name },
                    reservationGroupId = null,
                    name = row.name,
                    plannedQuantity = row.plannedQuantity,
                    availableQuantity = 0,
                    shortageQuantity = row.plannedQuantity,
                    allocatedQuantity = 0,
                    unallocatedQuantity = 0,
                    needsPurchase = true,
                    notes = row.notes,
                    responsibleUserId = row.responsibleUserId,
                    responsibleUserName = null,
                    createdByUserId = tokenManager.userId.first(),
                    createdAt = now
                )
            }
            mergeCachedInventoryItems(eventId, items)
            pendingOperationRepository.enqueue(currentTuntasId, PendingEntityType.EVENT, "bulk-${UUID.randomUUID()}", PendingOperationType.EVENT_CREATE_ITEMS_BULK, EventItemsBulkCreatePayload(eventId, request))
            Result.success(EventInventoryItemListDto(items, items.size))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateInventoryItem(
        eventId: String,
        inventoryItemId: String,
        request: UpdateEventInventoryItemRequestDto
    ): Result<EventInventoryItemDto> {
        return try {
            val response = eventApiService.updateInventoryItem("Bearer ${token()}", tuntasId(), eventId, inventoryItemId, request)
            if (response.isSuccessful) {
                val item = response.body()!!
                upsertCachedInventoryItem(eventId, item)
                Result.success(item)
            } else Result.failure(Exception(response.errorMessage("Klaida")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val cached = cachedInventoryPlan(eventId)?.items?.firstOrNull { it.id == inventoryItemId }
                ?: return Result.failure(Exception("Plano eilute nerasta offline cache"))
            val planned = request.plannedQuantity ?: cached.plannedQuantity
            val updated = cached.copy(
                name = request.name ?: cached.name,
                plannedQuantity = planned,
                bucketId = request.bucketId ?: cached.bucketId,
                bucketName = request.bucketId?.let { cachedInventoryPlan(eventId)?.buckets?.firstOrNull { bucket -> bucket.id == it }?.name } ?: cached.bucketName,
                shortageQuantity = (planned - cached.availableQuantity).coerceAtLeast(0),
                needsPurchase = planned > cached.availableQuantity,
                notes = request.notes ?: cached.notes,
                responsibleUserId = request.responsibleUserId ?: cached.responsibleUserId
            )
            upsertCachedInventoryItem(eventId, updated)
            pendingOperationRepository.enqueue(currentTuntasId, PendingEntityType.EVENT, inventoryItemId, PendingOperationType.EVENT_UPDATE_ITEM, EventItemUpdatePayload(eventId, inventoryItemId, request))
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteInventoryItem(eventId: String, inventoryItemId: String): Result<Unit> {
        return try {
            val response = eventApiService.deleteInventoryItem("Bearer ${token()}", tuntasId(), eventId, inventoryItemId)
            if (response.isSuccessful) {
                deleteCachedInventoryItem(eventId, inventoryItemId)
                Result.success(Unit)
            } else Result.failure(Exception(response.errorMessage("Klaida")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            deleteCachedInventoryItem(eventId, inventoryItemId)
            pendingOperationRepository.enqueue(currentTuntasId, PendingEntityType.EVENT, inventoryItemId, PendingOperationType.EVENT_DELETE_ITEM, EventItemDeletePayload(eventId, inventoryItemId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createInventoryAllocation(
        eventId: String,
        request: CreateEventInventoryAllocationRequestDto
    ): Result<EventInventoryAllocationDto> {
        return try {
            val response = eventApiService.createInventoryAllocation("Bearer ${token()}", tuntasId(), eventId, request)
            if (response.isSuccessful) {
                val allocation = response.body()!!
                upsertCachedAllocation(eventId, allocation)
                Result.success(allocation)
            } else Result.failure(Exception(response.errorMessage("Klaida")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val allocation = EventInventoryAllocationDto(
                id = "local-${UUID.randomUUID()}",
                eventInventoryItemId = request.eventInventoryItemId,
                bucketId = request.bucketId,
                bucketName = cachedInventoryPlan(eventId)?.buckets?.firstOrNull { it.id == request.bucketId }?.name ?: "Bucket",
                quantity = request.quantity,
                notes = request.notes
            )
            upsertCachedAllocation(eventId, allocation)
            pendingOperationRepository.enqueue(currentTuntasId, PendingEntityType.EVENT, allocation.id, PendingOperationType.EVENT_CREATE_ALLOCATION, EventAllocationCreatePayload(eventId, request))
            Result.success(allocation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateInventoryAllocation(
        eventId: String,
        allocationId: String,
        request: UpdateEventInventoryAllocationRequestDto
    ): Result<EventInventoryAllocationDto> {
        return try {
            val response = eventApiService.updateInventoryAllocation("Bearer ${token()}", tuntasId(), eventId, allocationId, request)
            if (response.isSuccessful) {
                val allocation = response.body()!!
                upsertCachedAllocation(eventId, allocation)
                Result.success(allocation)
            } else Result.failure(Exception(response.errorMessage("Klaida")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val cached = cachedInventoryPlan(eventId)?.allocations?.firstOrNull { it.id == allocationId }
                ?: return Result.failure(Exception("Paskirstymas nerastas offline cache"))
            val updated = cached.copy(
                quantity = request.quantity ?: cached.quantity,
                notes = request.notes ?: cached.notes
            )
            upsertCachedAllocation(eventId, updated)
            pendingOperationRepository.enqueue(currentTuntasId, PendingEntityType.EVENT, allocationId, PendingOperationType.EVENT_UPDATE_ALLOCATION, EventAllocationUpdatePayload(eventId, allocationId, request))
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteInventoryAllocation(eventId: String, allocationId: String): Result<Unit> {
        return try {
            val response = eventApiService.deleteInventoryAllocation("Bearer ${token()}", tuntasId(), eventId, allocationId)
            if (response.isSuccessful) {
                deleteCachedAllocation(eventId, allocationId)
                Result.success(Unit)
            } else Result.failure(Exception(response.errorMessage("Klaida")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            deleteCachedAllocation(eventId, allocationId)
            pendingOperationRepository.enqueue(currentTuntasId, PendingEntityType.EVENT, allocationId, PendingOperationType.EVENT_DELETE_ALLOCATION, EventAllocationDeletePayload(eventId, allocationId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPurchases(eventId: String): Result<EventPurchaseListDto> {
        return try {
            val response = eventApiService.getPurchases("Bearer ${token()}", tuntasId(), eventId)
            if (response.isSuccessful) {
                val purchases = response.body()?.purchases.orEmpty()
                cachePurchases(eventId, purchases)
                Result.success(EventPurchaseListDto(purchases, purchases.size))
            } else {
                Result.failure(Exception(response.errorMessage("Klaida")))
            }
        } catch (e: IOException) {
            cachedPurchases(eventId)?.let { Result.success(EventPurchaseListDto(it, it.size)) }
                ?: Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPurchase(eventId: String, request: CreateEventPurchaseRequestDto): Result<EventPurchaseDto> {
        return try {
            val response = eventApiService.createPurchase("Bearer ${token()}", tuntasId(), eventId, request)
            if (response.isSuccessful) {
                val purchase = response.body()!!
                upsertCachedPurchase(eventId, purchase)
                Result.success(purchase)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            ensureEventExists(eventId, currentTuntasId)
                ?: return Result.failure(Exception("Renginys nerastas offline cache"))
            val now = Instant.now().toString()
            val purchaseId = "local-${UUID.randomUUID()}"
            val offlineItems = mutableListOf<EventPurchaseItemDto>()
            for (requestItem in request.items) {
                offlineItems += EventPurchaseItemDto(
                    id = "local-${UUID.randomUUID()}",
                    purchaseId = purchaseId,
                    eventInventoryItemId = requestItem.eventInventoryItemId,
                    itemName = cachedInventoryItemName(eventId, requestItem.eventInventoryItemId) ?: "Inventoriaus eilute",
                    purchasedQuantity = requestItem.purchasedQuantity,
                    unitPrice = requestItem.unitPrice,
                    lineTotal = (requestItem.unitPrice ?: 0.0) * requestItem.purchasedQuantity,
                    addedToInventory = false,
                    addedToInventoryItemId = null,
                    notes = requestItem.notes
                )
            }
            val purchase = EventPurchaseDto(
                id = purchaseId,
                eventId = eventId,
                purchasedByUserId = tokenManager.userId.first(),
                purchasedByName = null,
                status = "DRAFT",
                purchaseDate = request.purchaseDate,
                totalAmount = request.items.sumOf { (it.unitPrice ?: 0.0) * it.purchasedQuantity },
                invoiceFileUrl = null,
                notes = request.notes,
                createdAt = now,
                updatedAt = now,
                items = offlineItems
            )
            upsertCachedPurchase(eventId, purchase)
            pendingOperationRepository.enqueue(
                tuntasId = currentTuntasId,
                entityType = PendingEntityType.EVENT,
                entityId = purchase.id,
                operationType = PendingOperationType.EVENT_CREATE_PURCHASE,
                payload = EventPurchaseCreatePayload(eventId, request)
            )
            Result.success(purchase)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePurchase(
        eventId: String,
        purchaseId: String,
        request: UpdateEventPurchaseRequestDto
    ): Result<EventPurchaseDto> {
        return try {
            val response = eventApiService.updatePurchase(
                "Bearer ${token()}",
                tuntasId(),
                eventId,
                purchaseId,
                request
            )
            if (response.isSuccessful) {
                val purchase = response.body()!!
                upsertCachedPurchase(eventId, purchase)
                Result.success(purchase)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida atnaujinant pirkima")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun attachPurchaseInvoice(eventId: String, purchaseId: String, invoiceFileUrl: String): Result<EventPurchaseDto> {
        if (UploadRepository.isStagedDocumentUrl(invoiceFileUrl)) {
            val currentTuntasId = tuntasId()
            val cached = cachedPurchases(eventId)?.firstOrNull { it.id == purchaseId }
                ?: return Result.failure(Exception("Pirkimas nerastas offline cache"))
            val updated = cached.copy(invoiceFileUrl = invoiceFileUrl, updatedAt = Instant.now().toString())
            upsertCachedPurchase(eventId, updated)
            pendingOperationRepository.enqueue(
                currentTuntasId,
                PendingEntityType.EVENT,
                purchaseId,
                PendingOperationType.EVENT_ATTACH_PURCHASE_INVOICE,
                EventAttachPurchaseInvoicePayload(eventId, purchaseId, stagedDocumentUrl = invoiceFileUrl)
            )
            return Result.success(updated)
        }
        return try {
            val response = eventApiService.attachPurchaseInvoice(
                "Bearer ${token()}",
                tuntasId(),
                eventId,
                purchaseId,
                AttachEventPurchaseInvoiceRequestDto(invoiceFileUrl)
            )
            if (response.isSuccessful) {
                val purchase = response.body()!!
                upsertCachedPurchase(eventId, purchase)
                Result.success(purchase)
            } else Result.failure(Exception(response.errorMessage("Klaida")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val cached = cachedPurchases(eventId)?.firstOrNull { it.id == purchaseId }
                ?: return Result.failure(Exception("Pirkimas nerastas offline cache"))
            val updated = cached.copy(invoiceFileUrl = invoiceFileUrl, updatedAt = Instant.now().toString())
            upsertCachedPurchase(eventId, updated)
            pendingOperationRepository.enqueue(
                currentTuntasId,
                PendingEntityType.EVENT,
                purchaseId,
                PendingOperationType.EVENT_ATTACH_PURCHASE_INVOICE,
                EventAttachPurchaseInvoicePayload(eventId, purchaseId, invoiceFileUrl = invoiceFileUrl)
            )
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completePurchase(eventId: String, purchaseId: String): Result<EventPurchaseDto> {
        return try {
            val response = eventApiService.completePurchase("Bearer ${token()}", tuntasId(), eventId, purchaseId)
            if (response.isSuccessful) {
                val purchase = response.body()!!
                upsertCachedPurchase(eventId, purchase)
                Result.success(purchase)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val cached = cachedPurchases(eventId)?.firstOrNull { it.id == purchaseId }
                ?: return Result.failure(Exception("Pirkimas nerastas offline cache"))
            val updated = cached.copy(
                status = "PURCHASED",
                purchaseDate = cached.purchaseDate ?: Instant.now().toString().take(10),
                updatedAt = Instant.now().toString()
            )
            upsertCachedPurchase(eventId, updated)
            pendingOperationRepository.enqueue(
                tuntasId = currentTuntasId,
                entityType = PendingEntityType.EVENT,
                entityId = purchaseId,
                operationType = PendingOperationType.EVENT_COMPLETE_PURCHASE,
                payload = EventPurchasePayload(eventId, purchaseId)
            )
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addPurchaseToInventory(eventId: String, purchaseId: String): Result<EventPurchaseDto> {
        return try {
            val response = eventApiService.addPurchaseToInventory("Bearer ${token()}", tuntasId(), eventId, purchaseId)
            if (response.isSuccessful) {
                val purchase = response.body()!!
                upsertCachedPurchase(eventId, purchase)
                Result.success(purchase)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val cached = cachedPurchases(eventId)?.firstOrNull { it.id == purchaseId }
                ?: return Result.failure(Exception("Pirkimas nerastas offline cache"))
            val updated = cached.copy(
                status = "ADDED_TO_INVENTORY",
                updatedAt = Instant.now().toString(),
                items = cached.items.map { it.copy(addedToInventory = true) }
            )
            upsertCachedPurchase(eventId, updated)
            pendingOperationRepository.enqueue(
                tuntasId = currentTuntasId,
                entityType = PendingEntityType.EVENT,
                entityId = purchaseId,
                operationType = PendingOperationType.EVENT_ADD_PURCHASE_TO_INVENTORY,
                payload = EventPurchasePayload(eventId, purchaseId)
            )
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPastovykles(eventId: String): Result<PastovykleListDto> {
        return try {
            val response = eventApiService.getPastovykles("Bearer ${token()}", tuntasId(), eventId)
            if (response.isSuccessful) {
                val pastovykles = response.body()?.pastovykles.orEmpty()
                cachePastovykles(eventId, pastovykles)
                Result.success(PastovykleListDto(pastovykles, pastovykles.size))
            } else {
                Result.failure(Exception(response.errorMessage("Klaida gaunant pastovykles")))
            }
        } catch (e: IOException) {
            cachedPastovykles(eventId)?.let { Result.success(PastovykleListDto(it, it.size)) }
                ?: Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPastovykle(eventId: String, request: CreatePastovykleRequestDto): Result<PastovykleDto> {
        return try {
            val response = eventApiService.createPastovykle("Bearer ${token()}", tuntasId(), eventId, request)
            if (response.isSuccessful) {
                val pastovykle = response.body()!!
                upsertCachedPastovykle(eventId, pastovykle)
                Result.success(pastovykle)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida kuriant pastovykle")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            ensureEventExists(eventId, currentTuntasId)
                ?: return Result.failure(Exception("Renginys nerastas offline cache"))
            val pastovykle = PastovykleDto(
                id = "local-${UUID.randomUUID()}",
                eventId = eventId,
                name = request.name,
                responsibleUserId = request.responsibleUserId,
                ageGroup = request.ageGroup,
                notes = request.notes
            )
            upsertCachedPastovykle(eventId, pastovykle)
            pendingOperationRepository.enqueue(
                tuntasId = currentTuntasId,
                entityType = PendingEntityType.EVENT,
                entityId = pastovykle.id,
                operationType = PendingOperationType.EVENT_CREATE_PASTOVYKLE,
                payload = EventPastovykleUpsertPayload(eventId, request)
            )
            Result.success(pastovykle)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPastovykle(eventId: String, pastovykleId: String): Result<PastovykleDto> {
        if (pastovykleId.startsWith("local-")) {
            val cached = cachedPastovykles(eventId)?.firstOrNull { it.id == pastovykleId }
            return if (cached != null) Result.success(cached) else Result.failure(Exception("Pastovykle nerasta"))
        }
        return try {
            val response = eventApiService.getPastovykle("Bearer ${token()}", tuntasId(), eventId, pastovykleId)
            if (response.isSuccessful) {
                val pastovykle = response.body()!!
                upsertCachedPastovykle(eventId, pastovykle)
                Result.success(pastovykle)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida gaunant pastovykle")))
            }
        } catch (e: IOException) {
            cachedPastovykles(eventId)
                ?.firstOrNull { it.id == pastovykleId }
                ?.let { Result.success(it) }
                ?: Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePastovykle(eventId: String, pastovykleId: String, request: UpdatePastovykleRequestDto): Result<PastovykleDto> {
        return try {
            val response = eventApiService.updatePastovykle("Bearer ${token()}", tuntasId(), eventId, pastovykleId, request)
            if (response.isSuccessful) {
                val updated = response.body()!!
                upsertCachedPastovykle(eventId, updated)
                Result.success(updated)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida atnaujinant pastovykle")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val cached = cachedPastovykles(eventId)?.firstOrNull { it.id == pastovykleId }
                ?: return Result.failure(Exception("Pastovykle nerasta offline cache"))
            val updated = cached.copy(
                name = request.name ?: cached.name,
                responsibleUserId = request.responsibleUserId ?: cached.responsibleUserId,
                ageGroup = request.ageGroup ?: cached.ageGroup,
                notes = request.notes ?: cached.notes
            )
            upsertCachedPastovykle(eventId, updated)
            val mergedIntoCreate = if (pastovykleId.startsWith("local-")) {
                pendingOperationRepository.replaceCreatePayloadIfPending(
                    entityType = PendingEntityType.EVENT,
                    entityId = pastovykleId,
                    createOperationType = PendingOperationType.EVENT_CREATE_PASTOVYKLE,
                    payload = updated.toCreateRequest()
                )
            } else {
                false
            }
            if (!mergedIntoCreate) {
                pendingOperationRepository.enqueue(
                    tuntasId = currentTuntasId,
                    entityType = PendingEntityType.EVENT,
                    entityId = pastovykleId,
                    operationType = PendingOperationType.EVENT_UPDATE_PASTOVYKLE,
                    payload = EventPastovykleUpdatePayload(eventId, pastovykleId, request)
                )
            }
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePastovykle(eventId: String, pastovykleId: String): Result<Unit> {
        return try {
            val response = eventApiService.deletePastovykle("Bearer ${token()}", tuntasId(), eventId, pastovykleId)
            if (response.isSuccessful) {
                deleteCachedPastovykle(eventId, pastovykleId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida trinant pastovykle")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            if (pastovykleId.startsWith("local-") && pendingOperationRepository.hasCreateOperationInFlight(
                    entityType = PendingEntityType.EVENT,
                    entityId = pastovykleId,
                    createOperationType = PendingOperationType.EVENT_CREATE_PASTOVYKLE
                )
            ) {
                return Result.failure(Exception("Pastovykle dabar sinchronizuojama. Pabandykite dar karta veliau."))
            }
            if (pastovykleId.startsWith("local-") && pendingOperationRepository.deletePendingCreateIfExists(
                    entityType = PendingEntityType.EVENT,
                    entityId = pastovykleId,
                    createOperationType = PendingOperationType.EVENT_CREATE_PASTOVYKLE
                )
            ) {
                deleteCachedPastovykle(eventId, pastovykleId)
                return Result.success(Unit)
            }
            val cached = cachedPastovykles(eventId)?.firstOrNull { it.id == pastovykleId }
                ?: return Result.failure(Exception("Pastovykle nerasta offline cache"))
            deleteCachedPastovykle(eventId, cached.id)
            pendingOperationRepository.enqueue(
                tuntasId = currentTuntasId,
                entityType = PendingEntityType.EVENT,
                entityId = pastovykleId,
                operationType = PendingOperationType.EVENT_DELETE_PASTOVYKLE,
                payload = EventPastovykleDeletePayload(eventId, pastovykleId)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPastovykleInventory(eventId: String, pastovykleId: String): Result<PastovykleInventoryListDto> {
        return try {
            val response = eventApiService.getPastovykleInventory("Bearer ${token()}", tuntasId(), eventId, pastovykleId)
            if (response.isSuccessful) {
                val inventory = response.body()?.inventory.orEmpty()
                cachePastovykleInventory(eventId, pastovykleId, inventory)
                Result.success(PastovykleInventoryListDto(inventory, inventory.size))
            } else Result.failure(Exception(response.errorMessage("Klaida gaunant pastovykles inventoriu")))
        } catch (e: IOException) {
            cachedPastovykleInventory(eventId, pastovykleId)?.let { Result.success(PastovykleInventoryListDto(it, it.size)) }
                ?: Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun assignPastovykleInventory(
        eventId: String,
        pastovykleId: String,
        request: AssignPastovykleInventoryRequestDto
    ): Result<PastovykleInventoryDto> {
        return try {
            val response = eventApiService.assignPastovykleInventory("Bearer ${token()}", tuntasId(), eventId, pastovykleId, request)
            if (response.isSuccessful) {
                val inventory = response.body()!!
                upsertCachedPastovykleInventory(eventId, pastovykleId, inventory)
                Result.success(inventory)
            } else Result.failure(Exception(response.errorMessage("Klaida priskiriant pastovykles inventoriu")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val row = PastovykleInventoryDto(
                id = "local-${UUID.randomUUID()}",
                pastovykleId = pastovykleId,
                itemId = request.itemId,
                itemName = cachedItemNameForPastovykle(eventId, request.itemId),
                distributedByUserId = tokenManager.userId.first(),
                recipientUserId = request.recipientUserId,
                recipientType = request.recipientType,
                quantityAssigned = request.quantity,
                quantityReturned = 0,
                assignedAt = Instant.now().toString(),
                returnedAt = null,
                notes = request.notes
            )
            upsertCachedPastovykleInventory(eventId, pastovykleId, row)
            pendingOperationRepository.enqueue(currentTuntasId, PendingEntityType.EVENT, row.id, PendingOperationType.EVENT_ASSIGN_PASTOVYKLE_INVENTORY, EventPastovykleInventoryAssignPayload(eventId, pastovykleId, request))
            Result.success(row)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePastovykleInventory(
        eventId: String,
        pastovykleId: String,
        inventoryId: String,
        request: UpdatePastovykleInventoryRequestDto
    ): Result<PastovykleInventoryDto> {
        return try {
            val response = eventApiService.updatePastovykleInventory("Bearer ${token()}", tuntasId(), eventId, pastovykleId, inventoryId, request)
            if (response.isSuccessful) {
                val updated = response.body()!!
                upsertCachedPastovykleInventory(eventId, pastovykleId, updated)
                Result.success(updated)
            } else Result.failure(Exception(response.errorMessage("Klaida atnaujinant pastovykles inventoriu")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val cached = cachedPastovykleInventory(eventId, pastovykleId)?.firstOrNull { it.id == inventoryId }
                ?: return Result.failure(Exception("Pastovykles inventorius nerastas offline cache"))
            val updated = cached.copy(
                quantityReturned = request.quantityReturned ?: cached.quantityReturned,
                returnedAt = request.returnedAt ?: cached.returnedAt,
                notes = request.notes ?: cached.notes
            )
            upsertCachedPastovykleInventory(eventId, pastovykleId, updated)
            pendingOperationRepository.enqueue(currentTuntasId, PendingEntityType.EVENT, inventoryId, PendingOperationType.EVENT_UPDATE_PASTOVYKLE_INVENTORY, EventPastovykleInventoryUpdatePayload(eventId, pastovykleId, inventoryId, request))
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePastovykleInventory(eventId: String, pastovykleId: String, inventoryId: String): Result<Unit> {
        return try {
            val response = eventApiService.deletePastovykleInventory("Bearer ${token()}", tuntasId(), eventId, pastovykleId, inventoryId)
            if (response.isSuccessful) {
                deleteCachedPastovykleInventory(eventId, pastovykleId, inventoryId)
                Result.success(Unit)
            } else Result.failure(Exception(response.errorMessage("Klaida trinant pastovykles inventoriu")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            deleteCachedPastovykleInventory(eventId, pastovykleId, inventoryId)
            pendingOperationRepository.enqueue(currentTuntasId, PendingEntityType.EVENT, inventoryId, PendingOperationType.EVENT_DELETE_PASTOVYKLE_INVENTORY, EventPastovykleInventoryDeletePayload(eventId, pastovykleId, inventoryId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPastovykleRequests(eventId: String, pastovykleId: String): Result<EventInventoryRequestListDto> {
        return try {
            val response = eventApiService.getPastovykleRequests("Bearer ${token()}", tuntasId(), eventId, pastovykleId)
            if (response.isSuccessful) {
                val requests = response.body()?.requests.orEmpty()
                cachePastovykleRequests(eventId, pastovykleId, requests)
                Result.success(EventInventoryRequestListDto(requests, requests.size))
            } else Result.failure(Exception(response.errorMessage("Klaida gaunant pastovykles poreikius")))
        } catch (e: IOException) {
            cachedPastovykleRequests(eventId, pastovykleId)?.let { Result.success(EventInventoryRequestListDto(it, it.size)) }
                ?: Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPastovykleRequest(
        eventId: String,
        pastovykleId: String,
        request: CreatePastovykleInventoryRequestRequestDto
    ): Result<EventInventoryRequestDto> {
        return try {
            val response = eventApiService.createPastovykleRequest("Bearer ${token()}", tuntasId(), eventId, pastovykleId, request)
            if (response.isSuccessful) {
                val created = response.body()!!
                upsertCachedPastovykleRequest(eventId, pastovykleId, created)
                Result.success(created)
            } else Result.failure(Exception(response.errorMessage("Klaida kuriant pastovykles poreiki")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val row = EventInventoryRequestDto(
                id = "local-${UUID.randomUUID()}",
                eventId = eventId,
                eventInventoryItemId = request.eventInventoryItemId,
                itemId = cachedInventoryPlan(eventId)?.items?.firstOrNull { it.id == request.eventInventoryItemId }?.itemId,
                itemName = cachedInventoryPlan(eventId)?.items?.firstOrNull { it.id == request.eventInventoryItemId }?.name ?: "Inventoriaus eilute",
                pastovykleId = pastovykleId,
                pastovykleName = cachedPastovykles(eventId)?.firstOrNull { it.id == pastovykleId }?.name ?: "Pastovykle",
                requestedByUserId = tokenManager.userId.first().orEmpty(),
                requestedByName = null,
                quantity = request.quantity,
                status = "PENDING",
                notes = request.notes,
                createdAt = Instant.now().toString(),
                reviewedAt = null,
                reviewedByUserId = null,
                reviewedByUserName = null,
                fulfilledAt = null,
                resolvedByUserId = null,
                resolvedByUserName = null
            )
            upsertCachedPastovykleRequest(eventId, pastovykleId, row)
            pendingOperationRepository.enqueue(currentTuntasId, PendingEntityType.EVENT, row.id, PendingOperationType.EVENT_CREATE_PASTOVYKLE_REQUEST, EventPastovykleRequestCreatePayload(eventId, pastovykleId, request))
            Result.success(row)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun approvePastovykleRequest(eventId: String, pastovykleId: String, requestId: String): Result<EventInventoryRequestDto> {
        return try {
            val response = eventApiService.approvePastovykleRequest("Bearer ${token()}", tuntasId(), eventId, pastovykleId, requestId)
            if (response.isSuccessful) {
                val updated = response.body()!!
                upsertCachedPastovykleRequest(eventId, pastovykleId, updated)
                Result.success(updated)
            } else Result.failure(Exception(response.errorMessage("Klaida tvirtinant poreiki")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val updated = updateCachedPastovykleRequestStatus(eventId, pastovykleId, requestId, "APPROVED")
            pendingOperationRepository.enqueue(currentTuntasId, PendingEntityType.EVENT, requestId, PendingOperationType.EVENT_APPROVE_PASTOVYKLE_REQUEST, EventPastovykleRequestActionPayload(eventId, pastovykleId, requestId))
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectPastovykleRequest(eventId: String, pastovykleId: String, requestId: String): Result<EventInventoryRequestDto> {
        return try {
            val response = eventApiService.rejectPastovykleRequest("Bearer ${token()}", tuntasId(), eventId, pastovykleId, requestId)
            if (response.isSuccessful) {
                val updated = response.body()!!
                upsertCachedPastovykleRequest(eventId, pastovykleId, updated)
                Result.success(updated)
            } else Result.failure(Exception(response.errorMessage("Klaida atmetant poreiki")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val updated = updateCachedPastovykleRequestStatus(eventId, pastovykleId, requestId, "REJECTED")
            pendingOperationRepository.enqueue(currentTuntasId, PendingEntityType.EVENT, requestId, PendingOperationType.EVENT_REJECT_PASTOVYKLE_REQUEST, EventPastovykleRequestActionPayload(eventId, pastovykleId, requestId))
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun selfProvidePastovykleRequest(
        eventId: String,
        pastovykleId: String,
        requestId: String,
        notes: String?
    ): Result<EventInventoryRequestDto> {
        return try {
            val response = eventApiService.selfProvidePastovykleRequest(
                "Bearer ${token()}",
                tuntasId(),
                eventId,
                pastovykleId,
                requestId,
                MarkPastovykleInventoryRequestSelfProvidedRequestDto(notes)
            )
            if (response.isSuccessful) {
                val updated = response.body()!!
                upsertCachedPastovykleRequest(eventId, pastovykleId, updated)
                Result.success(updated)
            } else Result.failure(Exception(response.errorMessage("Klaida pazymint, kad pasirupinta patiems")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val updated = updateCachedPastovykleRequestStatus(eventId, pastovykleId, requestId, "SELF_PROVIDED", notes = notes)
            pendingOperationRepository.enqueue(currentTuntasId, PendingEntityType.EVENT, requestId, PendingOperationType.EVENT_SELF_PROVIDE_PASTOVYKLE_REQUEST, EventPastovykleRequestActionPayload(eventId, pastovykleId, requestId, notes = notes))
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fulfillPastovykleRequest(
        eventId: String,
        pastovykleId: String,
        requestId: String,
        quantity: Int? = null,
        notes: String? = null
    ): Result<EventInventoryRequestDto> {
        return try {
            val response = eventApiService.fulfillPastovykleRequest(
                "Bearer ${token()}",
                tuntasId(),
                eventId,
                pastovykleId,
                requestId,
                FulfillPastovykleInventoryRequestRequestDto(quantity, notes)
            )
            if (response.isSuccessful) {
                val updated = response.body()!!
                upsertCachedPastovykleRequest(eventId, pastovykleId, updated)
                Result.success(updated)
            } else Result.failure(Exception(response.errorMessage("Klaida ivykdant pastovykles poreiki")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val updated = updateCachedPastovykleRequestStatus(eventId, pastovykleId, requestId, "FULFILLED", quantity, notes)
            pendingOperationRepository.enqueue(currentTuntasId, PendingEntityType.EVENT, requestId, PendingOperationType.EVENT_FULFILL_PASTOVYKLE_REQUEST, EventPastovykleRequestActionPayload(eventId, pastovykleId, requestId, quantity, notes))
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun assignFromUnitInventory(
        eventId: String,
        pastovykleId: String,
        itemId: String,
        quantity: Int,
        notes: String? = null
    ): Result<PastovykleInventoryDto> {
        return try {
            val response = eventApiService.assignFromUnitInventory(
                "Bearer ${token()}",
                tuntasId(),
                eventId,
                pastovykleId,
                AssignUnitInventoryToPastovykleRequestDto(itemId, quantity, notes)
            )
            if (response.isSuccessful) {
                val inventory = response.body()!!
                upsertCachedPastovykleInventory(eventId, pastovykleId, inventory)
                Result.success(inventory)
            } else Result.failure(Exception(response.errorMessage("Klaida priskiriant inventoriu is vieneto")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val row = PastovykleInventoryDto(
                id = "local-${UUID.randomUUID()}",
                pastovykleId = pastovykleId,
                itemId = itemId,
                itemName = cachedItemNameForPastovykle(eventId, itemId),
                distributedByUserId = tokenManager.userId.first(),
                recipientUserId = null,
                recipientType = "UNIT",
                quantityAssigned = quantity,
                quantityReturned = 0,
                assignedAt = Instant.now().toString(),
                returnedAt = null,
                notes = notes
            )
            upsertCachedPastovykleInventory(eventId, pastovykleId, row)
            pendingOperationRepository.enqueue(currentTuntasId, PendingEntityType.EVENT, row.id, PendingOperationType.EVENT_ASSIGN_FROM_UNIT_INVENTORY, EventAssignFromUnitInventoryPayload(eventId, pastovykleId, itemId, quantity, notes))
            Result.success(row)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInventoryCustody(eventId: String): Result<EventInventoryCustodyListDto> {
        return try {
            val response = eventApiService.getInventoryCustody("Bearer ${token()}", tuntasId(), eventId)
            if (response.isSuccessful) {
                val custody = response.body()?.custody.orEmpty()
                cacheInventoryCustody(eventId, custody)
                Result.success(EventInventoryCustodyListDto(custody, custody.size))
            } else {
                Result.failure(Exception(response.errorMessage("Klaida gaunant inventoriaus judejima")))
            }
        } catch (e: IOException) {
            cachedInventoryCustody(eventId)?.let { Result.success(EventInventoryCustodyListDto(it, it.size)) }
                ?: Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInventoryMovements(eventId: String): Result<EventInventoryMovementListDto> {
        return try {
            val response = eventApiService.getInventoryMovements("Bearer ${token()}", tuntasId(), eventId)
            if (response.isSuccessful) {
                val movements = response.body()?.movements.orEmpty()
                cacheInventoryMovements(eventId, movements)
                Result.success(EventInventoryMovementListDto(movements, movements.size))
            } else {
                Result.failure(Exception(response.errorMessage("Klaida gaunant inventoriaus istorija")))
            }
        } catch (e: IOException) {
            cachedInventoryMovements(eventId)?.let { Result.success(EventInventoryMovementListDto(it, it.size)) }
                ?: Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createInventoryMovement(eventId: String, request: CreateEventInventoryMovementRequestDto): Result<EventInventoryMovementDto> {
        return try {
            val response = eventApiService.createInventoryMovement("Bearer ${token()}", tuntasId(), eventId, request)
            if (response.isSuccessful) {
                val movement = response.body()!!
                appendCachedMovement(eventId, movement)
                Result.success(movement)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida registruojant inventoriaus judejima")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            ensureEventExists(eventId, currentTuntasId)
                ?: return Result.failure(Exception("Renginys nerastas offline cache"))
            val now = Instant.now().toString()
            val movement = EventInventoryMovementDto(
                id = "local-${UUID.randomUUID()}",
                eventId = eventId,
                eventInventoryItemId = request.eventInventoryItemId,
                itemName = cachedInventoryItemName(eventId, request.eventInventoryItemId) ?: "Inventoriaus eilute",
                custodyId = request.fromCustodyId,
                movementType = request.movementType,
                quantity = request.quantity,
                fromPastovykleId = currentFromPastovykleId(eventId, request),
                fromPastovykleName = currentFromPastovykleName(eventId, request),
                toPastovykleId = currentToPastovykleId(request),
                toPastovykleName = currentToPastovykleName(eventId, request),
                fromUserId = currentFromUserId(eventId, request),
                fromUserName = currentFromUserName(eventId, request),
                toUserId = currentToUserId(request),
                toUserName = currentToUserName(request),
                performedByUserId = tokenManager.userId.first().orEmpty(),
                performedByUserName = null,
                notes = request.notes,
                createdAt = now
            )
            appendCachedMovement(eventId, movement)
            applyOptimisticCustodyUpdate(eventId, movement, request)
            pendingOperationRepository.enqueue(
                tuntasId = currentTuntasId,
                entityType = PendingEntityType.EVENT,
                entityId = movement.id,
                operationType = PendingOperationType.EVENT_CREATE_INVENTORY_MOVEMENT,
                payload = EventInventoryMovementPayload(eventId, request)
            )
            Result.success(movement)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun EventDto.toCreateRequest(): CreateEventRequestDto = CreateEventRequestDto(
        name = name,
        type = type,
        startDate = startDate,
        endDate = endDate,
        notes = notes
    )

    private fun PastovykleDto.toCreateRequest(): CreatePastovykleRequestDto = CreatePastovykleRequestDto(
        name = name,
        responsibleUserId = responsibleUserId,
        ageGroup = ageGroup,
        notes = notes
    )

    private suspend fun cachedPastovykles(eventId: String): List<PastovykleDto>? =
        getCachedEventEntity(eventId)?.cachedPastovykles()

    private suspend fun cachedInventoryPlan(eventId: String): EventInventoryPlanDto? =
        getCachedEventEntity(eventId)?.cachedInventoryPlan()

    private suspend fun cachedPurchases(eventId: String): List<EventPurchaseDto>? =
        getCachedEventEntity(eventId)?.cachedPurchases()

    private suspend fun cachedPastovykleInventory(eventId: String, pastovykleId: String): List<PastovykleInventoryDto>? =
        getCachedEventEntity(eventId)?.cachedPastovykleInventoryById()?.get(pastovykleId)

    private suspend fun cachedPastovykleRequests(eventId: String, pastovykleId: String): List<EventInventoryRequestDto>? =
        getCachedEventEntity(eventId)?.cachedPastovykleRequestsById()?.get(pastovykleId)

    private suspend fun cachedInventoryCustody(eventId: String): List<EventInventoryCustodyDto>? =
        getCachedEventEntity(eventId)?.cachedInventoryCustody()

    private suspend fun cachedInventoryMovements(eventId: String): List<EventInventoryMovementDto>? =
        getCachedEventEntity(eventId)?.cachedInventoryMovements()

    private suspend fun ensureEventExists(eventId: String, tuntasId: String) =
        eventDao.getEvent(eventId, tuntasId)

    private suspend fun getCachedEventEntity(eventId: String) =
        tokenManager.activeTuntasId.first()?.let { eventDao.getEvent(eventId, it) }

    private suspend fun cachePastovykles(eventId: String, pastovykles: List<PastovykleDto>) {
        updateCachedEvent(eventId) { it.withCachedPastovykles(pastovykles) }
    }

    private suspend fun cacheInventoryPlan(eventId: String, plan: EventInventoryPlanDto?) {
        updateCachedEvent(eventId) { it.withCachedInventoryPlan(plan) }
    }

    private suspend fun cachePurchases(eventId: String, purchases: List<EventPurchaseDto>) {
        updateCachedEvent(eventId) { it.withCachedPurchases(purchases) }
    }

    private suspend fun cachePastovykleInventory(eventId: String, pastovykleId: String, inventory: List<PastovykleInventoryDto>) {
        updateCachedEvent(eventId) { entity ->
            val current = entity.cachedPastovykleInventoryById().toMutableMap()
            current[pastovykleId] = inventory
            entity.withCachedPastovykleInventoryById(current)
        }
    }

    private suspend fun cachePastovykleRequests(eventId: String, pastovykleId: String, requests: List<EventInventoryRequestDto>) {
        updateCachedEvent(eventId) { entity ->
            val current = entity.cachedPastovykleRequestsById().toMutableMap()
            current[pastovykleId] = requests
            entity.withCachedPastovykleRequestsById(current)
        }
    }

    private suspend fun cacheInventoryCustody(eventId: String, custody: List<EventInventoryCustodyDto>) {
        updateCachedEvent(eventId) { it.withCachedInventoryCustody(custody) }
    }

    private suspend fun cacheInventoryMovements(eventId: String, movements: List<EventInventoryMovementDto>) {
        updateCachedEvent(eventId) { it.withCachedInventoryMovements(movements) }
    }

    private suspend fun upsertCachedPastovykle(eventId: String, pastovykle: PastovykleDto) {
        val current = cachedPastovykles(eventId).orEmpty()
        cachePastovykles(eventId, current.filterNot { it.id == pastovykle.id } + pastovykle)
    }

    private suspend fun upsertCachedBucket(eventId: String, bucket: EventInventoryBucketDto) {
        val plan = cachedInventoryPlan(eventId) ?: EventInventoryPlanDto(emptyList(), emptyList(), emptyList())
        val updated = plan.copy(
            buckets = plan.buckets.filterNot { it.id == bucket.id } + bucket
        )
        cacheInventoryPlan(eventId, updated)
    }

    private suspend fun deleteCachedBucket(eventId: String, bucketId: String) {
        val plan = cachedInventoryPlan(eventId) ?: return
        val itemIdsToRemove = plan.items.filter { it.bucketId == bucketId }.map { it.id }.toSet()
        cacheInventoryPlan(
            eventId,
            plan.copy(
                buckets = plan.buckets.filterNot { it.id == bucketId },
                items = plan.items.filterNot { it.id in itemIdsToRemove || it.bucketId == bucketId },
                allocations = plan.allocations.filterNot { it.bucketId == bucketId || it.eventInventoryItemId in itemIdsToRemove }
            )
        )
    }

    private suspend fun upsertCachedInventoryItem(eventId: String, item: EventInventoryItemDto) {
        val plan = cachedInventoryPlan(eventId) ?: EventInventoryPlanDto(emptyList(), emptyList(), emptyList())
        val updated = plan.copy(
            items = plan.items.filterNot { it.id == item.id } + recalculateItemAllocation(plan.allocations, item)
        )
        cacheInventoryPlan(eventId, updated)
    }

    private suspend fun mergeCachedInventoryItems(eventId: String, items: List<EventInventoryItemDto>) {
        var plan = cachedInventoryPlan(eventId) ?: EventInventoryPlanDto(emptyList(), emptyList(), emptyList())
        for (item in items) {
            plan = plan.copy(items = plan.items.filterNot { it.id == item.id } + recalculateItemAllocation(plan.allocations, item))
        }
        cacheInventoryPlan(eventId, plan)
    }

    private suspend fun deleteCachedInventoryItem(eventId: String, inventoryItemId: String) {
        val plan = cachedInventoryPlan(eventId) ?: return
        cacheInventoryPlan(
            eventId,
            plan.copy(
                items = plan.items.filterNot { it.id == inventoryItemId },
                allocations = plan.allocations.filterNot { it.eventInventoryItemId == inventoryItemId }
            )
        )
    }

    private suspend fun upsertCachedAllocation(eventId: String, allocation: EventInventoryAllocationDto) {
        val plan = cachedInventoryPlan(eventId) ?: EventInventoryPlanDto(emptyList(), emptyList(), emptyList())
        val allocations = plan.allocations.filterNot { it.id == allocation.id } + allocation
        val items = plan.items.map { item ->
            if (item.id == allocation.eventInventoryItemId) recalculateItemAllocation(allocations, item) else item
        }
        cacheInventoryPlan(eventId, plan.copy(items = items, allocations = allocations))
    }

    private suspend fun deleteCachedAllocation(eventId: String, allocationId: String) {
        val plan = cachedInventoryPlan(eventId) ?: return
        val removed = plan.allocations.firstOrNull { it.id == allocationId }
        val allocations = plan.allocations.filterNot { it.id == allocationId }
        val items = plan.items.map { item ->
            if (item.id == removed?.eventInventoryItemId) recalculateItemAllocation(allocations, item) else item
        }
        cacheInventoryPlan(eventId, plan.copy(items = items, allocations = allocations))
    }

    private suspend fun upsertCachedPurchase(eventId: String, purchase: EventPurchaseDto) {
        val current = cachedPurchases(eventId).orEmpty()
        cachePurchases(eventId, current.filterNot { it.id == purchase.id } + purchase)
        if (purchase.status == "PURCHASED" || purchase.status == "ADDED_TO_INVENTORY") {
            applyPurchaseToInventoryPlan(eventId, purchase)
        }
    }

    private suspend fun upsertCachedPastovykleInventory(eventId: String, pastovykleId: String, inventory: PastovykleInventoryDto) {
        val current = cachedPastovykleInventory(eventId, pastovykleId).orEmpty()
        cachePastovykleInventory(eventId, pastovykleId, current.filterNot { it.id == inventory.id } + inventory)
    }

    private suspend fun deleteCachedPastovykleInventory(eventId: String, pastovykleId: String, inventoryId: String) {
        val current = cachedPastovykleInventory(eventId, pastovykleId).orEmpty()
        cachePastovykleInventory(eventId, pastovykleId, current.filterNot { it.id == inventoryId })
    }

    private suspend fun upsertCachedPastovykleRequest(eventId: String, pastovykleId: String, request: EventInventoryRequestDto) {
        val current = cachedPastovykleRequests(eventId, pastovykleId).orEmpty()
        cachePastovykleRequests(eventId, pastovykleId, current.filterNot { it.id == request.id } + request)
    }

    private suspend fun updateCachedPastovykleRequestStatus(
        eventId: String,
        pastovykleId: String,
        requestId: String,
        status: String,
        quantity: Int? = null,
        notes: String? = null
    ): EventInventoryRequestDto {
        val current = cachedPastovykleRequests(eventId, pastovykleId).orEmpty()
        val cached = current.firstOrNull { it.id == requestId }
            ?: throw Exception("Pastovykles poreikis nerastas offline cache")
        val now = Instant.now().toString()
        val updated = cached.copy(
            status = status,
            notes = notes ?: cached.notes,
            reviewedAt = if (status in listOf("APPROVED", "REJECTED")) now else cached.reviewedAt,
            reviewedByUserId = if (status in listOf("APPROVED", "REJECTED")) tokenManager.userId.first() else cached.reviewedByUserId,
            fulfilledAt = if (status == "FULFILLED") now else cached.fulfilledAt,
            resolvedByUserId = if (status in listOf("FULFILLED", "SELF_PROVIDED")) tokenManager.userId.first() else cached.resolvedByUserId
        )
        upsertCachedPastovykleRequest(eventId, pastovykleId, updated)
        return updated
    }

    private suspend fun appendCachedMovement(eventId: String, movement: EventInventoryMovementDto) {
        val current = cachedInventoryMovements(eventId).orEmpty()
        cacheInventoryMovements(eventId, listOf(movement) + current.filterNot { it.id == movement.id })
    }

    private suspend fun deleteCachedPastovykle(eventId: String, pastovykleId: String) {
        val current = cachedPastovykles(eventId) ?: return
        cachePastovykles(eventId, current.filterNot { it.id == pastovykleId })
    }

    private suspend fun cachedInventoryItemName(eventId: String, eventInventoryItemId: String): String? =
        cachedInventoryPlan(eventId)?.items?.firstOrNull { it.id == eventInventoryItemId }?.name
            ?: cachedInventoryMovements(eventId)?.firstOrNull { it.eventInventoryItemId == eventInventoryItemId }?.itemName
            ?: cachedInventoryCustody(eventId)?.firstOrNull { it.eventInventoryItemId == eventInventoryItemId }?.itemName

    private suspend fun cachedItemNameForPastovykle(eventId: String, itemId: String): String =
        cachedInventoryPlan(eventId)?.items?.firstOrNull { it.itemId == itemId }?.name ?: "Inventoriaus daiktas"

    private suspend fun applyPurchaseToInventoryPlan(eventId: String, purchase: EventPurchaseDto) {
        val plan = cachedInventoryPlan(eventId) ?: return
        val itemsById = plan.items.associateBy { it.id }.toMutableMap()
        for (line in purchase.items) {
            val currentItem = itemsById[line.eventInventoryItemId] ?: continue
            val nextAvailable = if (purchase.status == "PURCHASED" || purchase.status == "ADDED_TO_INVENTORY") {
                currentItem.availableQuantity.coerceAtLeast(0) + line.purchasedQuantity
            } else currentItem.availableQuantity
            itemsById[line.eventInventoryItemId] = recalculateItemAllocation(
                plan.allocations,
                currentItem.copy(
                    availableQuantity = nextAvailable,
                    needsPurchase = currentItem.plannedQuantity > nextAvailable,
                    shortageQuantity = (currentItem.plannedQuantity - nextAvailable).coerceAtLeast(0)
                )
            )
        }
        cacheInventoryPlan(eventId, plan.copy(items = itemsById.values.toList()))
    }

    private fun recalculateItemAllocation(
        allocations: List<EventInventoryAllocationDto>,
        item: EventInventoryItemDto
    ): EventInventoryItemDto {
        val allocated = allocations.filter { it.eventInventoryItemId == item.id }.sumOf { it.quantity }
        return item.copy(
            allocatedQuantity = allocated,
            unallocatedQuantity = (item.availableQuantity - allocated).coerceAtLeast(0),
            shortageQuantity = (item.plannedQuantity - item.availableQuantity).coerceAtLeast(0),
            needsPurchase = item.plannedQuantity > item.availableQuantity
        )
    }

    private suspend fun updateCachedEvent(eventId: String, block: (lt.skautai.android.data.local.entity.EventEntity) -> lt.skautai.android.data.local.entity.EventEntity) {
        val currentTuntasId = tokenManager.activeTuntasId.first() ?: return
        val cachedEvent = eventDao.getEvent(eventId, currentTuntasId) ?: return
        eventDao.upsert(block(cachedEvent))
    }

    private suspend fun applyOptimisticCustodyUpdate(
        eventId: String,
        movement: EventInventoryMovementDto,
        request: CreateEventInventoryMovementRequestDto
    ) {
        val current = cachedInventoryCustody(eventId).orEmpty().toMutableList()
        val now = movement.createdAt
        when (movement.movementType) {
            "ASSIGN_TO_PASTOVYKLE", "CHECKOUT_TO_PERSON" -> {
                current += EventInventoryCustodyDto(
                    id = movement.custodyId ?: "local-${UUID.randomUUID()}",
                    eventInventoryItemId = movement.eventInventoryItemId,
                    itemName = movement.itemName,
                    pastovykleId = movement.toPastovykleId,
                    pastovykleName = movement.toPastovykleName,
                    holderUserId = movement.toUserId,
                    holderUserName = movement.toUserName,
                    quantity = movement.quantity,
                    returnedQuantity = 0,
                    remainingQuantity = movement.quantity,
                    status = "OPEN",
                    createdByUserId = movement.performedByUserId,
                    createdByUserName = movement.performedByUserName,
                    createdAt = now,
                    closedAt = null,
                    notes = movement.notes
                )
            }
            "RETURN_TO_PASTOVYKLE", "RETURN_TO_EVENT_STORAGE" -> {
                val index = current.indexOfFirst { it.id == request.fromCustodyId }
                if (index >= 0) {
                    val row = current[index]
                    val returned = (row.returnedQuantity + movement.quantity).coerceAtMost(row.quantity)
                    current[index] = row.copy(
                        returnedQuantity = returned,
                        remainingQuantity = (row.quantity - returned).coerceAtLeast(0),
                        status = if (returned >= row.quantity) "RETURNED" else row.status,
                        closedAt = if (returned >= row.quantity) now else row.closedAt
                    )
                }
            }
        }
        cacheInventoryCustody(eventId, current)
    }

    private suspend fun currentFromPastovykleId(eventId: String, request: CreateEventInventoryMovementRequestDto): String? =
        when (request.movementType) {
            "RETURN_TO_PASTOVYKLE", "RETURN_TO_EVENT_STORAGE" ->
                cachedInventoryCustody(eventId)?.firstOrNull { it.id == request.fromCustodyId }?.pastovykleId
            else -> request.pastovykleId
        }

    private suspend fun currentFromPastovykleName(eventId: String, request: CreateEventInventoryMovementRequestDto): String? =
        when (request.movementType) {
            "RETURN_TO_PASTOVYKLE", "RETURN_TO_EVENT_STORAGE" ->
                cachedInventoryCustody(eventId)?.firstOrNull { it.id == request.fromCustodyId }?.pastovykleName
            else -> cachedPastovykles(eventId)?.firstOrNull { it.id == request.pastovykleId }?.name
        }

    private suspend fun currentToPastovykleId(request: CreateEventInventoryMovementRequestDto): String? =
        when (request.movementType) {
            "RETURN_TO_EVENT_STORAGE" -> null
            else -> request.pastovykleId
        }

    private suspend fun currentToPastovykleName(eventId: String, request: CreateEventInventoryMovementRequestDto): String? =
        currentToPastovykleId(request)?.let { targetId ->
            cachedPastovykles(eventId)?.firstOrNull { it.id == targetId }?.name
        }

    private suspend fun currentFromUserId(eventId: String, request: CreateEventInventoryMovementRequestDto): String? =
        if (request.movementType == "RETURN_TO_PASTOVYKLE" || request.movementType == "RETURN_TO_EVENT_STORAGE") {
            cachedInventoryCustody(eventId)?.firstOrNull { it.id == request.fromCustodyId }?.holderUserId
        } else {
            null
        }

    private suspend fun currentFromUserName(eventId: String, request: CreateEventInventoryMovementRequestDto): String? =
        if (request.movementType == "RETURN_TO_PASTOVYKLE" || request.movementType == "RETURN_TO_EVENT_STORAGE") {
            cachedInventoryCustody(eventId)?.firstOrNull { it.id == request.fromCustodyId }?.holderUserName
        } else {
            null
        }

    private suspend fun currentToUserId(request: CreateEventInventoryMovementRequestDto): String? =
        if (request.movementType == "CHECKOUT_TO_PERSON") request.toUserId else null

    private suspend fun currentToUserName(request: CreateEventInventoryMovementRequestDto): String? = null
}
