package lt.skautai.android.data.repository

import com.google.gson.Gson
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
import lt.skautai.android.data.local.dao.ReservationDao
import lt.skautai.android.data.local.mapper.toDto
import lt.skautai.android.data.local.mapper.toEntity
import lt.skautai.android.data.local.mapper.toReservationDtos
import lt.skautai.android.data.local.mapper.toReservationEntities
import lt.skautai.android.data.remote.CreateReservationRequestDto
import lt.skautai.android.data.remote.ReservationAvailabilityDto
import lt.skautai.android.data.remote.ReservationApiService
import lt.skautai.android.data.remote.ReservationDto
import lt.skautai.android.data.remote.ReservationListDto
import lt.skautai.android.data.remote.ReservationMovementListDto
import lt.skautai.android.data.remote.ReservationMovementRequestDto
import lt.skautai.android.data.remote.ReviewReservationRequestDto
import lt.skautai.android.data.remote.UpdateReservationPickupRequestDto
import lt.skautai.android.data.remote.UpdateReservationReturnTimeRequestDto
import lt.skautai.android.data.remote.UpdateReservationStatusRequestDto
import lt.skautai.android.data.sync.PendingEntityType
import lt.skautai.android.data.sync.PendingOperationRepository
import lt.skautai.android.data.sync.PendingOperationType
import lt.skautai.android.data.sync.ReservationMovementSyncPayload
import lt.skautai.android.util.TokenManager
import lt.skautai.android.util.errorMessage

@Singleton
class ReservationRepository @Inject constructor(
    private val reservationApiService: ReservationApiService,
    private val tokenManager: TokenManager,
    private val reservationDao: ReservationDao,
    private val pendingOperationRepository: PendingOperationRepository
) {
    private val gson = Gson()

    private suspend fun token() = tokenManager.token.first()
        ?: throw Exception("Nav prisijungta")

    private suspend fun tuntasId() = tokenManager.activeTuntasId.first()
        ?: throw Exception("Tuntas nepasirinktas")

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeReservations(itemId: String? = null, status: String? = null): Flow<ReservationListDto> {
        return tokenManager.activeTuntasId.flatMapLatest { currentTuntasId ->
            if (currentTuntasId == null) {
                flowOf(ReservationListDto(emptyList(), 0))
            } else {
                reservationDao.observeReservations(currentTuntasId, itemId, status)
                    .map { reservations -> ReservationListDto(reservations.toReservationDtos(), reservations.size) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeReservation(id: String): Flow<ReservationDto?> {
        return tokenManager.activeTuntasId.flatMapLatest { currentTuntasId ->
            if (currentTuntasId == null) flowOf(null)
            else reservationDao.observeReservation(id, currentTuntasId).map { it?.toDto() }
        }
    }

    suspend fun refreshReservations(itemId: String? = null, status: String? = null): Result<Unit> {
        return try {
            val currentTuntasId = tuntasId()
            val response = reservationApiService.getReservations(
                token = "Bearer ${token()}",
                tuntasId = currentTuntasId,
                itemId = itemId,
                status = status
            )
            if (response.isSuccessful) {
                val reservations = response.body()?.reservations.orEmpty()
                if (itemId == null) {
                    reservationDao.deleteForQuery(currentTuntasId, status)
                }
                reservationDao.upsertAll(reservations.toReservationEntities())
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida gaunant rezervacijas")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshReservation(id: String): Result<Unit> {
        return try {
            val currentTuntasId = tuntasId()
            val response = reservationApiService.getReservation(
                token = "Bearer ${token()}",
                tuntasId = currentTuntasId,
                id = id
            )
            if (response.isSuccessful) {
                reservationDao.upsert(response.body()!!.toEntity())
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida gaunant rezervacija")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReservations(itemId: String? = null, status: String? = null): Result<ReservationListDto> {
        val refreshResult = refreshReservations(itemId, status)
        val currentTuntasId = tokenManager.activeTuntasId.first()
        val cachedReservations = currentTuntasId
            ?.let { reservationDao.getReservations(it, itemId, status).toReservationDtos() }
            .orEmpty()
        return if (refreshResult.isSuccess || cachedReservations.isNotEmpty()) {
            Result.success(ReservationListDto(cachedReservations, cachedReservations.size))
        } else {
            Result.failure(Exception("Rezervaciju nepavyko atnaujinti. Prisijunkite prie interneto bent karta, kad jos butu issaugotos offline."))
        }
    }

    suspend fun getReservation(id: String): Result<ReservationDto> {
        val refreshResult = refreshReservation(id)
        val currentTuntasId = tokenManager.activeTuntasId.first()
        val cachedReservation = currentTuntasId?.let { reservationDao.getReservation(id, it)?.toDto() }
        return if (cachedReservation != null) {
            Result.success(cachedReservation)
        } else {
            Result.failure(Exception("Rezervacijos nepavyko atnaujinti. Prisijunkite prie interneto bent karta, kad ji butu issaugota offline."))
        }
    }

    suspend fun createReservation(request: CreateReservationRequestDto): Result<ReservationDto> {
        return try {
            val response = reservationApiService.createReservation("Bearer ${token()}", tuntasId(), request)
            if (response.isSuccessful) {
                val reservation = response.body()!!
                reservationDao.upsert(reservation.toEntity())
                Result.success(reservation)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida kuriant rezervacija")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val userId = tokenManager.userId.first().orEmpty()
            val now = Instant.now().toString()
            val reservation = ReservationDto(
                id = "local-${UUID.randomUUID()}",
                title = request.title,
                tuntasId = currentTuntasId,
                reservedByUserId = userId,
                reservedByName = tokenManager.userName.first(),
                approvedByUserId = null,
                requestingUnitId = request.requestingUnitId,
                requestingUnitName = null,
                eventId = null,
                totalItems = request.items.size,
                totalQuantity = request.items.sumOf { it.quantity },
                startDate = request.startDate,
                endDate = request.endDate,
                status = "PENDING",
                pickupLocationId = request.pickupLocationId,
                pickupLocationPath = null,
                returnLocationId = request.returnLocationId,
                returnLocationPath = null,
                notes = request.notes,
                createdAt = now,
                updatedAt = now,
                items = request.items.map {
                    lt.skautai.android.data.remote.ReservationItemDto(
                        itemId = it.itemId,
                        itemName = it.itemId,
                        quantity = it.quantity,
                        remainingAfterReservation = null
                    )
                }
            )
            reservationDao.upsert(reservation.toEntity())
            pendingOperationRepository.enqueue(
                currentTuntasId,
                PendingEntityType.RESERVATION,
                reservation.id,
                PendingOperationType.RESERVATION_CREATE,
                request
            )
            Result.success(reservation)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getAvailability(startDate: String, endDate: String): Result<ReservationAvailabilityDto> {
        return try {
            val response = reservationApiService.getAvailability("Bearer ${token()}", tuntasId(), startDate, endDate)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorMessage("Klaida gaunant prieinama kieki")))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateReservationStatus(id: String, request: UpdateReservationStatusRequestDto): Result<ReservationDto> =
        updateOnlineOnly(id, PendingOperationType.RESERVATION_UPDATE_STATUS, request, "Klaida atnaujinant rezervacija") {
            reservationApiService.updateReservationStatus("Bearer ${token()}", tuntasId(), id, request)
        }

    suspend fun updateReservationPickupTime(id: String, request: UpdateReservationPickupRequestDto): Result<ReservationDto> =
        updateOnlineOnly(id, PendingOperationType.RESERVATION_UPDATE_PICKUP, request, "Klaida atnaujinant atsiemimo laika") {
            reservationApiService.updateReservationPickupTime("Bearer ${token()}", tuntasId(), id, request)
        }

    suspend fun updateReservationReturnTime(id: String, request: UpdateReservationReturnTimeRequestDto): Result<ReservationDto> =
        updateOnlineOnly(id, PendingOperationType.RESERVATION_UPDATE_RETURN, request, "Klaida atnaujinant grazinimo laika") {
            reservationApiService.updateReservationReturnTime("Bearer ${token()}", tuntasId(), id, request)
        }

    suspend fun cancelReservation(id: String): Result<Unit> {
        return try {
            val currentTuntasId = tuntasId()
            val response = reservationApiService.cancelReservation("Bearer ${token()}", currentTuntasId, id)
            if (response.isSuccessful) {
                reservationDao.deleteReservation(id, currentTuntasId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida atšaukiant rezervacija")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            if (id.startsWith("local-") && pendingOperationRepository.hasCreateOperationInFlight(
                    entityType = PendingEntityType.RESERVATION,
                    entityId = id,
                    createOperationType = PendingOperationType.RESERVATION_CREATE
                )
            ) {
                return Result.failure(Exception("Rezervacija dabar sinchronizuojama. Pabandykite dar kartą vėliau."))
            }
            if (id.startsWith("local-") && pendingOperationRepository.deletePendingCreateIfExists(
                    entityType = PendingEntityType.RESERVATION,
                    entityId = id,
                    createOperationType = PendingOperationType.RESERVATION_CREATE
                )
            ) {
                reservationDao.deleteReservation(id, currentTuntasId)
                return Result.success(Unit)
            }
            reservationDao.getReservation(id, currentTuntasId)?.toDto()?.let {
                reservationDao.upsert(it.copy(status = "CANCELLED", updatedAt = Instant.now().toString()).toEntity())
            }
            pendingOperationRepository.enqueue(
                currentTuntasId,
                PendingEntityType.RESERVATION,
                id,
                PendingOperationType.RESERVATION_CANCEL,
                mapOf("id" to id)
            )
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun reviewUnitReservation(id: String, request: ReviewReservationRequestDto): Result<ReservationDto> =
        updateOnlineOnly(id, PendingOperationType.RESERVATION_REVIEW_UNIT, request, "Klaida tvirtinant vieneto dali") {
            reservationApiService.reviewUnitReservation("Bearer ${token()}", tuntasId(), id, request)
        }

    suspend fun reviewTopLevelReservation(id: String, request: ReviewReservationRequestDto): Result<ReservationDto> =
        updateOnlineOnly(id, PendingOperationType.RESERVATION_REVIEW_TOP_LEVEL, request, "Klaida tvirtinant tunto dali") {
            reservationApiService.reviewTopLevelReservation("Bearer ${token()}", tuntasId(), id, request)
        }

    suspend fun getReservationMovements(id: String): Result<ReservationMovementListDto> {
        return try {
            val response = reservationApiService.getReservationMovements("Bearer ${token()}", tuntasId(), id)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorMessage("Klaida gaunant judejimus")))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun issueReservationItems(id: String, request: ReservationMovementRequestDto): Result<ReservationDto> =
        recordReservationMovement(id, request, movement = "issue")

    suspend fun returnReservationItems(id: String, request: ReservationMovementRequestDto): Result<ReservationDto> =
        recordReservationMovement(id, request, movement = "return")

    suspend fun markReservationItemsReturned(id: String, request: ReservationMovementRequestDto): Result<ReservationDto> =
        recordReservationMovement(id, request, movement = "mark_returned")

    private suspend fun recordReservationMovement(
        id: String,
        request: ReservationMovementRequestDto,
        movement: String
    ): Result<ReservationDto> = updateOnlineOnly(
        id,
        PendingOperationType.RESERVATION_MOVEMENT,
        ReservationMovementSyncPayload(movement, gson.toJson(request)),
        "Klaida registruojant judejima"
    ) {
        when (movement) {
            "return" -> reservationApiService.returnReservationItems("Bearer ${token()}", tuntasId(), id, request)
            "mark_returned" -> reservationApiService.markReservationItemsReturned("Bearer ${token()}", tuntasId(), id, request)
            else -> reservationApiService.issueReservationItems("Bearer ${token()}", tuntasId(), id, request)
        }
    }

    private suspend fun updateOnlineOnly(
        id: String,
        operationType: String,
        payload: Any,
        fallbackMessage: String,
        call: suspend () -> retrofit2.Response<ReservationDto>
    ): Result<ReservationDto> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val reservation = response.body()!!
                reservationDao.upsert(reservation.toEntity())
                Result.success(reservation)
            } else {
                Result.failure(Exception(response.errorMessage(fallbackMessage)))
            }
        } catch (e: IOException) {
            val currentTuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val cached = reservationDao.getReservation(id, currentTuntasId)?.toDto()
                ?: return Result.failure(Exception("Rezervacija nerasta offline cache"))
            val updated = when (payload) {
                is ReviewReservationRequestDto -> cached.copy(
                    unitReviewStatus = if (operationType == PendingOperationType.RESERVATION_REVIEW_UNIT) payload.status else cached.unitReviewStatus,
                    topLevelReviewStatus = if (operationType == PendingOperationType.RESERVATION_REVIEW_TOP_LEVEL) payload.status else cached.topLevelReviewStatus,
                    status = if (payload.status == "REJECTED") "REJECTED" else cached.status,
                    updatedAt = Instant.now().toString()
                )
                is UpdateReservationStatusRequestDto -> cached.copy(status = payload.status, notes = payload.notes ?: cached.notes, updatedAt = Instant.now().toString())
                is UpdateReservationPickupRequestDto -> cached.copy(
                    pickupAt = payload.pickupAt,
                    pickupLocationId = payload.pickupLocationId ?: cached.pickupLocationId,
                    pickupProposalStatus = payload.response ?: "PENDING",
                    updatedAt = Instant.now().toString()
                )
                is UpdateReservationReturnTimeRequestDto -> cached.copy(
                    returnAt = payload.returnAt,
                    returnLocationId = payload.returnLocationId ?: cached.returnLocationId,
                    returnProposalStatus = payload.response ?: "PENDING",
                    updatedAt = Instant.now().toString()
                )
                else -> cached.copy(updatedAt = Instant.now().toString())
            }
            reservationDao.upsert(updated.toEntity())
            pendingOperationRepository.enqueue(currentTuntasId, PendingEntityType.RESERVATION, id, operationType, payload)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
