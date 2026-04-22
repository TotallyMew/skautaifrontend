package lt.skautai.android.data.repository

import kotlinx.coroutines.flow.first
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReservationRepository @Inject constructor(
    private val reservationApiService: ReservationApiService,
    private val tokenManager: TokenManager
) {

    suspend fun getReservations(
        itemId: String? = null,
        status: String? = null
    ): Result<ReservationListDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = reservationApiService.getReservations(
                token = "Bearer $token",
                tuntasId = tuntasId,
                itemId = itemId,
                status = status
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant rezervacijas"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReservation(id: String): Result<ReservationDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = reservationApiService.getReservation(
                token = "Bearer $token",
                tuntasId = tuntasId,
                id = id
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant rezervaciją"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createReservation(request: CreateReservationRequestDto): Result<ReservationDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = reservationApiService.createReservation(
                token = "Bearer $token",
                tuntasId = tuntasId,
                request = request
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida kuriant rezervaciją"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAvailability(startDate: String, endDate: String): Result<ReservationAvailabilityDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = reservationApiService.getAvailability(
                token = "Bearer $token",
                tuntasId = tuntasId,
                startDate = startDate,
                endDate = endDate
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant prieinama kieki"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateReservationStatus(
        id: String,
        request: UpdateReservationStatusRequestDto
    ): Result<ReservationDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = reservationApiService.updateReservationStatus(
                token = "Bearer $token",
                tuntasId = tuntasId,
                id = id,
                request = request
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida atnaujinant rezervaciją"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateReservationPickupTime(
        id: String,
        request: UpdateReservationPickupRequestDto
    ): Result<ReservationDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = reservationApiService.updateReservationPickupTime(
                token = "Bearer $token",
                tuntasId = tuntasId,
                id = id,
                request = request
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida atnaujinant atsiemimo laika"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateReservationReturnTime(
        id: String,
        request: UpdateReservationReturnTimeRequestDto
    ): Result<ReservationDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = reservationApiService.updateReservationReturnTime(
                token = "Bearer $token",
                tuntasId = tuntasId,
                id = id,
                request = request
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida atnaujinant grazinimo laika"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelReservation(id: String): Result<Unit> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = reservationApiService.cancelReservation(
                token = "Bearer $token",
                tuntasId = tuntasId,
                id = id
            )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida atšaukiant rezervaciją"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reviewUnitReservation(
        id: String,
        request: ReviewReservationRequestDto
    ): Result<ReservationDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = reservationApiService.reviewUnitReservation(
                token = "Bearer $token",
                tuntasId = tuntasId,
                id = id,
                request = request
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida tvirtinant vieneto dalį"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reviewTopLevelReservation(
        id: String,
        request: ReviewReservationRequestDto
    ): Result<ReservationDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = reservationApiService.reviewTopLevelReservation(
                token = "Bearer $token",
                tuntasId = tuntasId,
                id = id,
                request = request
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida tvirtinant tunto dalį"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReservationMovements(id: String): Result<ReservationMovementListDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = reservationApiService.getReservationMovements(
                token = "Bearer $token",
                tuntasId = tuntasId,
                id = id
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant judėjimus"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun issueReservationItems(
        id: String,
        request: ReservationMovementRequestDto
    ): Result<ReservationDto> = recordReservationMovement(id, request, movement = "issue")

    suspend fun returnReservationItems(
        id: String,
        request: ReservationMovementRequestDto
    ): Result<ReservationDto> = recordReservationMovement(id, request, movement = "return")

    suspend fun markReservationItemsReturned(
        id: String,
        request: ReservationMovementRequestDto
    ): Result<ReservationDto> = recordReservationMovement(id, request, movement = "mark_returned")

    private suspend fun recordReservationMovement(
        id: String,
        request: ReservationMovementRequestDto,
        movement: String
    ): Result<ReservationDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = when (movement) {
                "return" -> reservationApiService.returnReservationItems(
                    token = "Bearer $token",
                    tuntasId = tuntasId,
                    id = id,
                    request = request
                )
                "mark_returned" -> reservationApiService.markReservationItemsReturned(
                    token = "Bearer $token",
                    tuntasId = tuntasId,
                    id = id,
                    request = request
                )
                else -> reservationApiService.issueReservationItems(
                    token = "Bearer $token",
                    tuntasId = tuntasId,
                    id = id,
                    request = request
                )
            }
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida registruojant judėjimą"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
