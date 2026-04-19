package lt.skautai.android.data.repository

import kotlinx.coroutines.flow.first
import lt.skautai.android.data.remote.BendrasRequestDto
import lt.skautai.android.data.remote.BendrasRequestListDto
import lt.skautai.android.data.remote.CreateBendrasRequestDto
import lt.skautai.android.data.remote.RequestApiService
import lt.skautai.android.data.remote.ReviewRequestDto
import lt.skautai.android.util.TokenManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RequestRepository @Inject constructor(
    private val requestApiService: RequestApiService,
    private val tokenManager: TokenManager
) {

    suspend fun getRequests(): Result<BendrasRequestListDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = requestApiService.getRequests(
                token = "Bearer $token",
                tuntasId = tuntasId
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant prašymus"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRequest(id: String): Result<BendrasRequestDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = requestApiService.getRequest(
                token = "Bearer $token",
                tuntasId = tuntasId,
                id = id
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant prašymą"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createRequest(request: CreateBendrasRequestDto): Result<BendrasRequestDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = requestApiService.createRequest(
                token = "Bearer $token",
                tuntasId = tuntasId,
                request = request
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida kuriant prašymą"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelRequest(id: String): Result<Unit> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = requestApiService.cancelRequest(
                token = "Bearer $token",
                tuntasId = tuntasId,
                id = id
            )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida atšaukiant prašymą"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun draugininkasReview(
        id: String,
        action: String,
        rejectionReason: String?
    ): Result<BendrasRequestDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = requestApiService.draugininkasReview(
                token = "Bearer $token",
                tuntasId = tuntasId,
                id = id,
                request = ReviewRequestDto(action = action, rejectionReason = rejectionReason)
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida atliekant peržiūrą"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun topLevelReview(
        id: String,
        action: String,
        rejectionReason: String?
    ): Result<BendrasRequestDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = requestApiService.topLevelReview(
                token = "Bearer $token",
                tuntasId = tuntasId,
                id = id,
                request = ReviewRequestDto(action = action, rejectionReason = rejectionReason)
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida atliekant peržiūrą"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}