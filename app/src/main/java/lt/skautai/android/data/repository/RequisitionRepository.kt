package lt.skautai.android.data.repository

import lt.skautai.android.util.userFacingException

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
import lt.skautai.android.data.local.dao.RequisitionDao
import lt.skautai.android.data.local.mapper.toDto
import lt.skautai.android.data.local.mapper.toEntity
import lt.skautai.android.data.local.mapper.toRequisitionDtos
import lt.skautai.android.data.local.mapper.toRequisitionEntities
import lt.skautai.android.data.remote.CreateRequisitionDto
import lt.skautai.android.data.remote.RequisitionApiService
import lt.skautai.android.data.remote.RequisitionDto
import lt.skautai.android.data.remote.RequisitionListDto
import lt.skautai.android.data.remote.RequisitionReviewDto
import lt.skautai.android.data.remote.RequisitionItemDto
import lt.skautai.android.data.sync.PendingEntityType
import lt.skautai.android.data.sync.PendingOperationRepository
import lt.skautai.android.data.sync.PendingOperationType
import lt.skautai.android.data.sync.ReviewPayload
import lt.skautai.android.util.TokenManager
import lt.skautai.android.util.errorMessage

@Singleton
class RequisitionRepository @Inject constructor(
    private val requisitionApiService: RequisitionApiService,
    private val tokenManager: TokenManager,
    private val requisitionDao: RequisitionDao,
    private val pendingOperationRepository: PendingOperationRepository
) {
    private suspend fun token() = tokenManager.token.first()
        ?: throw Exception("Nav prisijungta")

    private suspend fun tuntasId() = tokenManager.activeTuntasId.first()
        ?: throw Exception("Tuntas nepasirinktas")

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeRequests(): Flow<RequisitionListDto> {
        return tokenManager.activeTuntasId.flatMapLatest { currentTuntasId ->
            if (currentTuntasId == null) {
                flowOf(RequisitionListDto(emptyList(), 0))
            } else {
                requisitionDao.observeRequests(currentTuntasId)
                    .map { requests -> RequisitionListDto(requests.toRequisitionDtos(), requests.size) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeRequest(id: String): Flow<RequisitionDto?> {
        return tokenManager.activeTuntasId.flatMapLatest { currentTuntasId ->
            if (currentTuntasId == null) flowOf(null)
            else requisitionDao.observeRequest(id, currentTuntasId).map { it?.toDto() }
        }
    }

    suspend fun refreshRequests(): Result<Unit> {
        return try {
            val currentTuntasId = tuntasId()
            val response = requisitionApiService.getRequests("Bearer ${token()}", currentTuntasId)
            if (response.isSuccessful) {
                val requests = response.body()?.requests.orEmpty()
                val entities = requests.toRequisitionEntities()
                requisitionDao.deleteForTuntas(currentTuntasId)
                requisitionDao.upsertAll(entities)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida gaunant prašymą")))
            }
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    suspend fun refreshRequest(id: String): Result<Unit> {
        return try {
            val currentTuntasId = tuntasId()
            val response = requisitionApiService.getRequest("Bearer ${token()}", currentTuntasId, id)
            if (response.isSuccessful) {
                requisitionDao.upsert(response.body()!!.toEntity())
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida gaunant prašymą")))
            }
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    suspend fun getRequests(): Result<RequisitionListDto> {
        refreshRequests()
        val currentTuntasId = tokenManager.activeTuntasId.first()
        val cachedRequests = currentTuntasId
            ?.let { requisitionDao.getRequests(it).toRequisitionDtos() }
            .orEmpty()
        return Result.success(RequisitionListDto(cachedRequests, cachedRequests.size))
    }

    suspend fun getRequest(id: String): Result<RequisitionDto> {
        refreshRequest(id)
        val currentTuntasId = tokenManager.activeTuntasId.first()
        val cachedRequest = currentTuntasId?.let { requisitionDao.getRequest(id, it)?.toDto() }
        return if (cachedRequest != null) {
            Result.success(cachedRequest)
        } else {
            Result.failure(Exception("Prasymo nepavyko atnaujinti. Prisijunkite prie interneto bent karta, kad jis butu issaugotas offline."))
        }
    }

    suspend fun createRequest(request: CreateRequisitionDto): Result<RequisitionDto> {
        return try {
            val response = requisitionApiService.createRequest("Bearer ${token()}", tuntasId(), request)
            if (response.isSuccessful) {
                val created = response.body()!!
                requisitionDao.upsert(created.toEntity())
                Result.success(created)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida kuriant prašymą")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val now = Instant.now().toString()
            val local = RequisitionDto(
                id = "local-${UUID.randomUUID()}",
                tuntasId = currentTuntasId,
                createdByUserId = tokenManager.userId.first().orEmpty(),
                requestingUnitId = request.requestingUnitId,
                requestingUnitName = null,
                status = "PENDING",
                unitReviewStatus = "PENDING",
                unitReviewedByUserId = null,
                unitReviewedAt = null,
                topLevelReviewStatus = "PENDING",
                topLevelReviewedByUserId = null,
                topLevelReviewedAt = null,
                reviewLevel = "UNIT",
                lastAction = "CREATED",
                neededByDate = request.neededByDate,
                notes = request.notes,
                items = request.items.map {
                    RequisitionItemDto(
                        id = "local-${UUID.randomUUID()}",
                        itemId = null,
                        itemName = it.itemName,
                        itemDescription = it.itemDescription,
                        quantityRequested = it.quantity,
                        quantityApproved = null,
                        rejectionReason = null,
                        notes = it.notes
                    )
                },
                createdAt = now,
                updatedAt = now
            )
            requisitionDao.upsert(local.toEntity())
            pendingOperationRepository.enqueue(currentTuntasId, PendingEntityType.REQUISITION, local.id, PendingOperationType.REQUISITION_CREATE, request)
            Result.success(local)
        } catch (e: Exception) { Result.failure(e.userFacingException()) }
    }

    suspend fun unitReview(id: String, action: String, rejectionReason: String? = null): Result<RequisitionDto> =
        reviewOnlineOnly(id, action, rejectionReason, topLevel = false)

    suspend fun topLevelReview(id: String, action: String, rejectionReason: String? = null): Result<RequisitionDto> =
        reviewOnlineOnly(id, action, rejectionReason, topLevel = true)

    suspend fun cancelRequest(id: String): Result<Unit> {
        return try {
            val currentTuntasId = tuntasId()
            val response = requisitionApiService.cancelRequest("Bearer ${token()}", currentTuntasId, id)
            if (response.isSuccessful) {
                requisitionDao.getRequest(id, currentTuntasId)?.toDto()?.let {
                    requisitionDao.upsert(it.cancelledCopy().toEntity())
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida atsaukiant prasyma")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            if (id.startsWith("local-") && pendingOperationRepository.hasCreateOperationInFlight(
                    entityType = PendingEntityType.REQUISITION,
                    entityId = id,
                    createOperationType = PendingOperationType.REQUISITION_CREATE
                )
            ) {
                return Result.failure(Exception("Prasymas dabar sinchronizuojamas. Pabandykite dar karta veliau."))
            }
            if (id.startsWith("local-") && pendingOperationRepository.deletePendingCreateIfExists(
                    entityType = PendingEntityType.REQUISITION,
                    entityId = id,
                    createOperationType = PendingOperationType.REQUISITION_CREATE
                )
            ) {
                requisitionDao.deleteRequest(id, currentTuntasId)
                return Result.success(Unit)
            }
            requisitionDao.getRequest(id, currentTuntasId)?.toDto()?.let {
                requisitionDao.upsert(it.cancelledCopy().toEntity())
            }
            pendingOperationRepository.enqueue(
                currentTuntasId,
                PendingEntityType.REQUISITION,
                id,
                PendingOperationType.REQUISITION_CANCEL,
                mapOf("id" to id)
            )
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e.userFacingException()) }
    }

    private suspend fun reviewOnlineOnly(
        id: String,
        action: String,
        rejectionReason: String?,
        topLevel: Boolean
    ): Result<RequisitionDto> {
        return try {
            val request = RequisitionReviewDto(action = action, rejectionReason = rejectionReason)
            val response = if (topLevel) {
                requisitionApiService.topLevelReview("Bearer ${token()}", tuntasId(), id, request)
            } else {
                requisitionApiService.unitReview("Bearer ${token()}", tuntasId(), id, request)
            }
            if (response.isSuccessful) {
                val updated = response.body()!!
                requisitionDao.upsert(updated.toEntity())
                Result.success(updated)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida atliekant perziura")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val cached = requisitionDao.getRequest(id, currentTuntasId)?.toDto()
                ?: return Result.failure(Exception("Prašymas nerastas offline cache"))
            val updated = if (topLevel) {
                cached.copy(topLevelReviewStatus = action, lastAction = action, updatedAt = Instant.now().toString())
            } else {
                cached.copy(unitReviewStatus = action, lastAction = action, updatedAt = Instant.now().toString())
            }
            requisitionDao.upsert(updated.toEntity())
            pendingOperationRepository.enqueue(
                currentTuntasId,
                PendingEntityType.REQUISITION,
                id,
                if (topLevel) PendingOperationType.REQUISITION_REVIEW_TOP_LEVEL else PendingOperationType.REQUISITION_REVIEW_UNIT,
                ReviewPayload(id, action, rejectionReason)
            )
            Result.success(updated)
        } catch (e: Exception) { Result.failure(e.userFacingException()) }
    }

    private fun RequisitionDto.cancelledCopy(): RequisitionDto {
        val now = Instant.now().toString()
        return copy(
            status = "CANCELLED",
            unitReviewStatus = if (unitReviewStatus == "PENDING") "CANCELLED" else unitReviewStatus,
            topLevelReviewStatus = if (topLevelReviewStatus == "PENDING") "CANCELLED" else topLevelReviewStatus,
            lastAction = "CANCELLED",
            updatedAt = now
        )
    }
}
