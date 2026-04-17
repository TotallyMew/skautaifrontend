package lt.skautai.android.data.repository

import kotlinx.coroutines.flow.first
import lt.skautai.android.data.remote.CreateReservationRequestDto
import lt.skautai.android.data.remote.ReservationApiService
import lt.skautai.android.data.remote.ReservationDto
import lt.skautai.android.data.remote.ReservationListDto
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
}