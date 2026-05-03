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
import lt.skautai.android.data.local.dao.BendrasRequestDao
import lt.skautai.android.data.local.mapper.toBendrasRequestDtos
import lt.skautai.android.data.local.mapper.toBendrasRequestEntities
import lt.skautai.android.data.local.mapper.toDto
import lt.skautai.android.data.local.mapper.toEntity
import lt.skautai.android.data.remote.BendrasRequestDto
import lt.skautai.android.data.remote.BendrasRequestListDto
import lt.skautai.android.data.remote.CreateBendrasRequestDto
import lt.skautai.android.data.remote.RequestApiService
import lt.skautai.android.data.remote.ReviewRequestDto
import lt.skautai.android.data.sync.PendingEntityType
import lt.skautai.android.data.sync.PendingOperationRepository
import lt.skautai.android.data.sync.PendingOperationType
import lt.skautai.android.data.sync.ReviewPayload
import lt.skautai.android.util.TokenManager
import lt.skautai.android.util.errorMessage

@Singleton
class RequestRepository @Inject constructor(
    private val requestApiService: RequestApiService,
    private val tokenManager: TokenManager,
    private val bendrasRequestDao: BendrasRequestDao,
    private val pendingOperationRepository: PendingOperationRepository
) {
    private suspend fun token() = tokenManager.token.first()
        ?: throw Exception("Nav prisijungta")

    private suspend fun tuntasId() = tokenManager.activeTuntasId.first()
        ?: throw Exception("Tuntas nepasirinktas")

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeRequests(): Flow<BendrasRequestListDto> {
        return tokenManager.activeTuntasId.flatMapLatest { currentTuntasId ->
            if (currentTuntasId == null) {
                flowOf(BendrasRequestListDto(emptyList(), 0))
            } else {
                bendrasRequestDao.observeRequests(currentTuntasId)
                    .map { requests -> BendrasRequestListDto(requests.toBendrasRequestDtos(), requests.size) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeRequest(id: String): Flow<BendrasRequestDto?> {
        return tokenManager.activeTuntasId.flatMapLatest { currentTuntasId ->
            if (currentTuntasId == null) flowOf(null)
            else bendrasRequestDao.observeRequest(id, currentTuntasId).map { it?.toDto() }
        }
    }

    suspend fun refreshRequests(): Result<Unit> {
        return try {
            val currentTuntasId = tuntasId()
            val response = requestApiService.getRequests("Bearer ${token()}", currentTuntasId)
            if (response.isSuccessful) {
                val requests = response.body()?.requests.orEmpty()
                val entities = requests.toBendrasRequestEntities()
                bendrasRequestDao.deleteForTuntas(currentTuntasId)
                bendrasRequestDao.upsertAll(entities)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida gaunant prašymus")))
            }
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    suspend fun refreshRequest(id: String): Result<Unit> {
        return try {
            val currentTuntasId = tuntasId()
            val response = requestApiService.getRequest("Bearer ${token()}", currentTuntasId, id)
            if (response.isSuccessful) {
                bendrasRequestDao.upsert(response.body()!!.toEntity())
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida gaunant prašymą")))
            }
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    suspend fun getRequests(): Result<BendrasRequestListDto> {
        refreshRequests()
        val currentTuntasId = tokenManager.activeTuntasId.first()
        val cachedRequests = currentTuntasId
            ?.let { bendrasRequestDao.getRequests(it).toBendrasRequestDtos() }
            .orEmpty()
        return Result.success(BendrasRequestListDto(cachedRequests, cachedRequests.size))
    }

    suspend fun getRequest(id: String): Result<BendrasRequestDto> {
        refreshRequest(id)
        val currentTuntasId = tokenManager.activeTuntasId.first()
        val cachedRequest = currentTuntasId?.let { bendrasRequestDao.getRequest(id, it)?.toDto() }
        return if (cachedRequest != null) {
            Result.success(cachedRequest)
        } else {
            Result.failure(Exception("Prasymo nepavyko atnaujinti. Prisijunkite prie interneto bent karta, kad jis butu issaugotas offline."))
        }
    }

    suspend fun createRequest(request: CreateBendrasRequestDto): Result<BendrasRequestDto> {
        return try {
            val response = requestApiService.createRequest("Bearer ${token()}", tuntasId(), request)
            if (response.isSuccessful) {
                val created = response.body()!!
                bendrasRequestDao.upsert(created.toEntity())
                Result.success(created)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida kuriant prašymą")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val now = Instant.now().toString()
            val local = BendrasRequestDto(
                id = "local-${UUID.randomUUID()}",
                tuntasId = currentTuntasId,
                requestedByUserId = tokenManager.userId.first().orEmpty(),
                requestedByUserName = tokenManager.userName.first(),
                itemId = null,
                itemName = request.itemDescription ?: request.items.firstOrNull()?.itemId ?: "Prašymas",
                itemDescription = request.itemDescription,
                quantity = request.quantity ?: request.items.sumOf { it.quantity },
                neededByDate = request.neededByDate,
                requestingUnitId = request.requestingUnitId,
                requestingUnitName = null,
                needsDraugininkasApproval = request.requestingUnitId != null,
                draugininkasStatus = if (request.requestingUnitId != null) "PENDING" else null,
                draugininkasReviewedByUserId = null,
                draugininkasRejectionReason = null,
                topLevelStatus = "PENDING",
                topLevelReviewedByUserId = null,
                topLevelRejectionReason = null,
                notes = request.notes,
                items = emptyList(),
                createdAt = now,
                updatedAt = now
            )
            bendrasRequestDao.upsert(local.toEntity())
            pendingOperationRepository.enqueue(currentTuntasId, PendingEntityType.BENDRAS_REQUEST, local.id, PendingOperationType.BENDRAS_REQUEST_CREATE, request)
            Result.success(local)
        } catch (e: Exception) { Result.failure(e.userFacingException()) }
    }

    suspend fun cancelRequest(id: String): Result<Unit> {
        return try {
            val currentTuntasId = tuntasId()
            val response = requestApiService.cancelRequest("Bearer ${token()}", currentTuntasId, id)
            if (response.isSuccessful) {
                bendrasRequestDao.deleteRequest(id, currentTuntasId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida atsaukiant prašymą")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            if (id.startsWith("local-") && pendingOperationRepository.hasCreateOperationInFlight(
                    entityType = PendingEntityType.BENDRAS_REQUEST,
                    entityId = id,
                    createOperationType = PendingOperationType.BENDRAS_REQUEST_CREATE
                )
            ) {
                return Result.failure(Exception("Prašymas dabar sinchronizuojamas. Pabandykite dar kartą vėliau."))
            }
            if (id.startsWith("local-") && pendingOperationRepository.deletePendingCreateIfExists(
                    entityType = PendingEntityType.BENDRAS_REQUEST,
                    entityId = id,
                    createOperationType = PendingOperationType.BENDRAS_REQUEST_CREATE
                )
            ) {
                bendrasRequestDao.deleteRequest(id, currentTuntasId)
                return Result.success(Unit)
            }
            bendrasRequestDao.getRequest(id, currentTuntasId)?.toDto()?.let {
                bendrasRequestDao.upsert(it.copy(topLevelStatus = "CANCELLED", updatedAt = Instant.now().toString()).toEntity())
            }
            pendingOperationRepository.enqueue(currentTuntasId, PendingEntityType.BENDRAS_REQUEST, id, PendingOperationType.BENDRAS_REQUEST_CANCEL, mapOf("id" to id))
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e.userFacingException()) }
    }

    suspend fun draugininkasReview(id: String, action: String, rejectionReason: String?): Result<BendrasRequestDto> =
        reviewOnlineOnly(id, action, rejectionReason, topLevel = false)

    suspend fun topLevelReview(id: String, action: String, rejectionReason: String?): Result<BendrasRequestDto> =
        reviewOnlineOnly(id, action, rejectionReason, topLevel = true)

    private suspend fun reviewOnlineOnly(
        id: String,
        action: String,
        rejectionReason: String?,
        topLevel: Boolean
    ): Result<BendrasRequestDto> {
        return try {
            val request = ReviewRequestDto(action = action, rejectionReason = rejectionReason)
            val response = if (topLevel) {
                requestApiService.topLevelReview("Bearer ${token()}", tuntasId(), id, request)
            } else {
                requestApiService.draugininkasReview("Bearer ${token()}", tuntasId(), id, request)
            }
            if (response.isSuccessful) {
                val updated = response.body()!!
                bendrasRequestDao.upsert(updated.toEntity())
                Result.success(updated)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida atliekant perziura")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val cached = bendrasRequestDao.getRequest(id, currentTuntasId)?.toDto()
                ?: return Result.failure(Exception("Prašymas nerastas offline cache"))
            val updated = if (topLevel) {
                cached.copy(topLevelStatus = action, topLevelRejectionReason = rejectionReason, updatedAt = Instant.now().toString())
            } else {
                cached.copy(draugininkasStatus = action, draugininkasRejectionReason = rejectionReason, updatedAt = Instant.now().toString())
            }
            bendrasRequestDao.upsert(updated.toEntity())
            pendingOperationRepository.enqueue(
                currentTuntasId,
                PendingEntityType.BENDRAS_REQUEST,
                id,
                if (topLevel) PendingOperationType.BENDRAS_REQUEST_REVIEW_TOP_LEVEL else PendingOperationType.BENDRAS_REQUEST_REVIEW_UNIT,
                ReviewPayload(id, action, rejectionReason)
            )
            Result.success(updated)
        } catch (e: Exception) { Result.failure(e.userFacingException()) }
    }
}
