package lt.skautai.android.data.repository

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
import lt.skautai.android.util.TokenManager

@Singleton
class ReservationRepository @Inject constructor(
    private val reservationApiService: ReservationApiService,
    private val tokenManager: TokenManager,
    private val reservationDao: ReservationDao
) {
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
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant rezervacijas"))
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
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant rezervacija"))
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
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida kuriant rezervacija"))
            }
        } catch (e: Exception) { Result.failure(Exception("Šis veiksmas galimas tik prisijungus", e)) }
    }

    suspend fun getAvailability(startDate: String, endDate: String): Result<ReservationAvailabilityDto> {
        return try {
            val response = reservationApiService.getAvailability("Bearer ${token()}", tuntasId(), startDate, endDate)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant prieinama kieki"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateReservationStatus(id: String, request: UpdateReservationStatusRequestDto): Result<ReservationDto> =
        updateOnlineOnly(id, "Klaida atnaujinant rezervacija") {
            reservationApiService.updateReservationStatus("Bearer ${token()}", tuntasId(), id, request)
        }

    suspend fun updateReservationPickupTime(id: String, request: UpdateReservationPickupRequestDto): Result<ReservationDto> =
        updateOnlineOnly(id, "Klaida atnaujinant atsiemimo laika") {
            reservationApiService.updateReservationPickupTime("Bearer ${token()}", tuntasId(), id, request)
        }

    suspend fun updateReservationReturnTime(id: String, request: UpdateReservationReturnTimeRequestDto): Result<ReservationDto> =
        updateOnlineOnly(id, "Klaida atnaujinant grazinimo laika") {
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
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida atšaukiant rezervacija"))
            }
        } catch (e: Exception) { Result.failure(Exception("Šis veiksmas galimas tik prisijungus", e)) }
    }

    suspend fun reviewUnitReservation(id: String, request: ReviewReservationRequestDto): Result<ReservationDto> =
        updateOnlineOnly(id, "Klaida tvirtinant vieneto dali") {
            reservationApiService.reviewUnitReservation("Bearer ${token()}", tuntasId(), id, request)
        }

    suspend fun reviewTopLevelReservation(id: String, request: ReviewReservationRequestDto): Result<ReservationDto> =
        updateOnlineOnly(id, "Klaida tvirtinant tunto dali") {
            reservationApiService.reviewTopLevelReservation("Bearer ${token()}", tuntasId(), id, request)
        }

    suspend fun getReservationMovements(id: String): Result<ReservationMovementListDto> {
        return try {
            val response = reservationApiService.getReservationMovements("Bearer ${token()}", tuntasId(), id)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant judejimus"))
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
    ): Result<ReservationDto> = updateOnlineOnly(id, "Klaida registruojant judejima") {
        when (movement) {
            "return" -> reservationApiService.returnReservationItems("Bearer ${token()}", tuntasId(), id, request)
            "mark_returned" -> reservationApiService.markReservationItemsReturned("Bearer ${token()}", tuntasId(), id, request)
            else -> reservationApiService.issueReservationItems("Bearer ${token()}", tuntasId(), id, request)
        }
    }

    private suspend fun updateOnlineOnly(
        id: String,
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
                Result.failure(Exception(response.errorBody()?.string() ?: fallbackMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Šis veiksmas galimas tik prisijungus", e))
        }
    }
}
