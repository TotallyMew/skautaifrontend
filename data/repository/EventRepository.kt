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
import lt.skautai.android.data.local.mapper.toDto
import lt.skautai.android.data.local.mapper.toEntity
import lt.skautai.android.data.local.mapper.toEventDtos
import lt.skautai.android.data.local.mapper.toEventEntities
import lt.skautai.android.data.remote.CreateEventRequestDto
import lt.skautai.android.data.remote.AssignEventRoleRequestDto
import lt.skautai.android.data.remote.CreateEventInventoryAllocationRequestDto
import lt.skautai.android.data.remote.CreateEventInventoryBucketRequestDto
import lt.skautai.android.data.remote.CreateEventInventoryItemRequestDto
import lt.skautai.android.data.remote.CreateEventInventoryItemsBulkRequestDto
import lt.skautai.android.data.remote.CreateEventInventoryMovementRequestDto
import lt.skautai.android.data.remote.CreateEventPurchaseRequestDto
import lt.skautai.android.data.remote.AttachEventPurchaseInvoiceRequestDto
import lt.skautai.android.data.remote.CreatePastovykleRequestDto
import lt.skautai.android.data.remote.EventApiService
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.EventInventoryAllocationDto
import lt.skautai.android.data.remote.EventInventoryBucketDto
import lt.skautai.android.data.remote.EventInventoryCustodyListDto
import lt.skautai.android.data.remote.EventInventoryItemDto
import lt.skautai.android.data.remote.EventInventoryItemListDto
import lt.skautai.android.data.remote.EventInventoryMovementDto
import lt.skautai.android.data.remote.EventInventoryMovementListDto
import lt.skautai.android.data.remote.EventInventoryPlanDto
import lt.skautai.android.data.remote.EventListDto
import lt.skautai.android.data.remote.EventPurchaseDto
import lt.skautai.android.data.remote.EventPurchaseListDto
import lt.skautai.android.data.remote.PastovykleDto
import lt.skautai.android.data.remote.PastovykleListDto
import lt.skautai.android.data.remote.StovyklaDetailsDto
import lt.skautai.android.data.remote.UpdateEventRequestDto
import lt.skautai.android.data.remote.UpdateEventInventoryItemRequestDto
import lt.skautai.android.data.sync.PendingEntityType
import lt.skautai.android.data.sync.PendingOperationRepository
import lt.skautai.android.data.sync.PendingOperationType
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
        ?: throw Exception("Nav prisijungta")

    private suspend fun tuntasId() = tokenManager.activeTuntasId.first()
        ?: throw Exception("Tuntas nepasirinktas")

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
                Result.failure(Exception(response.errorMessage("Klaida")))
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
                Result.failure(Exception(response.errorMessage("Klaida")))
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
            Result.failure(refreshResult.exceptionOrNull() ?: Exception("Klaida"))
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
            Result.failure(refreshResult.exceptionOrNull() ?: Exception("Klaida"))
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
                Result.failure(Exception(response.errorMessage("Klaida")))
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
                stovyklaDetails = if (
                    request.registrationDeadline != null ||
                    request.expectedParticipants != null
                ) {
                    StovyklaDetailsDto(
                        id = "local-details-$eventId",
                        registrationDeadline = request.registrationDeadline,
                        expectedParticipants = request.expectedParticipants,
                        actualParticipants = null
                    )
                } else {
                    null
                },
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
                eventDao.deleteEvent(id, currentTuntasId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            if (id.startsWith("local-") && pendingOperationRepository.deletePendingCreateIfExists(
                    entityType = PendingEntityType.EVENT,
                    entityId = id,
                    createOperationType = PendingOperationType.EVENT_CREATE
                )
            ) {
                eventDao.deleteEvent(id, currentTuntasId)
                return Result.success(Unit)
            }
            eventDao.deleteEvent(id, currentTuntasId)
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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeEventRole(eventId: String, roleId: String): Result<Unit> {
        return try {
            val response = eventApiService.removeEventRole("Bearer ${token()}", tuntasId(), eventId, roleId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorMessage("Klaida")))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInventoryPlan(eventId: String): Result<EventInventoryPlanDto> {
        return try {
            val response = eventApiService.getInventoryPlan("Bearer ${token()}", tuntasId(), eventId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: EventInventoryPlanDto(emptyList(), emptyList(), emptyList()))
            } else {
                Result.failure(Exception(response.errorMessage("Klaida")))
            }
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
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorMessage("Klaida")))
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
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorMessage("Klaida")))
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
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorMessage("Klaida")))
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
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorMessage("Klaida")))
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
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorMessage("Klaida")))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPurchases(eventId: String): Result<EventPurchaseListDto> {
        return try {
            val response = eventApiService.getPurchases("Bearer ${token()}", tuntasId(), eventId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: EventPurchaseListDto(emptyList(), 0))
            } else {
                Result.failure(Exception(response.errorMessage("Klaida")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPurchase(eventId: String, request: CreateEventPurchaseRequestDto): Result<EventPurchaseDto> {
        return try {
            val response = eventApiService.createPurchase("Bearer ${token()}", tuntasId(), eventId, request)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorMessage("Klaida")))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun attachPurchaseInvoice(eventId: String, purchaseId: String, invoiceFileUrl: String): Result<EventPurchaseDto> {
        return try {
            val response = eventApiService.attachPurchaseInvoice(
                "Bearer ${token()}",
                tuntasId(),
                eventId,
                purchaseId,
                AttachEventPurchaseInvoiceRequestDto(invoiceFileUrl)
            )
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorMessage("Klaida")))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completePurchase(eventId: String, purchaseId: String): Result<EventPurchaseDto> {
        return try {
            val response = eventApiService.completePurchase("Bearer ${token()}", tuntasId(), eventId, purchaseId)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorMessage("Klaida")))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addPurchaseToInventory(eventId: String, purchaseId: String): Result<EventPurchaseDto> {
        return try {
            val response = eventApiService.addPurchaseToInventory("Bearer ${token()}", tuntasId(), eventId, purchaseId)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorMessage("Klaida")))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPastovykles(eventId: String): Result<PastovykleListDto> {
        return try {
            val response = eventApiService.getPastovykles("Bearer ${token()}", tuntasId(), eventId)
            if (response.isSuccessful) Result.success(response.body() ?: PastovykleListDto(emptyList(), 0))
            else Result.failure(Exception(response.errorMessage("Klaida gaunant pastovykles")))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPastovykle(eventId: String, request: CreatePastovykleRequestDto): Result<PastovykleDto> {
        return try {
            val response = eventApiService.createPastovykle("Bearer ${token()}", tuntasId(), eventId, request)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorMessage("Klaida kuriant pastovykle")))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInventoryCustody(eventId: String): Result<EventInventoryCustodyListDto> {
        return try {
            val response = eventApiService.getInventoryCustody("Bearer ${token()}", tuntasId(), eventId)
            if (response.isSuccessful) Result.success(response.body() ?: EventInventoryCustodyListDto(emptyList(), 0))
            else Result.failure(Exception(response.errorMessage("Klaida gaunant inventoriaus judejima")))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInventoryMovements(eventId: String): Result<EventInventoryMovementListDto> {
        return try {
            val response = eventApiService.getInventoryMovements("Bearer ${token()}", tuntasId(), eventId)
            if (response.isSuccessful) Result.success(response.body() ?: EventInventoryMovementListDto(emptyList(), 0))
            else Result.failure(Exception(response.errorMessage("Klaida gaunant inventoriaus istorija")))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createInventoryMovement(eventId: String, request: CreateEventInventoryMovementRequestDto): Result<EventInventoryMovementDto> {
        return try {
            val response = eventApiService.createInventoryMovement("Bearer ${token()}", tuntasId(), eventId, request)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorMessage("Klaida registruojant inventoriaus judejima")))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun EventDto.toCreateRequest(): CreateEventRequestDto = CreateEventRequestDto(
        name = name,
        type = type,
        startDate = startDate,
        endDate = endDate,
        notes = notes,
        registrationDeadline = stovyklaDetails?.registrationDeadline,
        expectedParticipants = stovyklaDetails?.expectedParticipants
    )
}
