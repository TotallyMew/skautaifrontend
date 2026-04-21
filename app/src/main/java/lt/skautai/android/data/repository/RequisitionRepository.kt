package lt.skautai.android.data.repository

import kotlinx.coroutines.flow.first
import lt.skautai.android.data.remote.CreateRequisitionDto
import lt.skautai.android.data.remote.RequisitionApiService
import lt.skautai.android.data.remote.RequisitionDto
import lt.skautai.android.data.remote.RequisitionListDto
import lt.skautai.android.data.remote.RequisitionReviewDto
import lt.skautai.android.util.TokenManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RequisitionRepository @Inject constructor(
    private val requisitionApiService: RequisitionApiService,
    private val tokenManager: TokenManager
) {

    suspend fun getRequests(): Result<RequisitionListDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = requisitionApiService.getRequests("Bearer $token", tuntasId)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant prasyma"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRequest(id: String): Result<RequisitionDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = requisitionApiService.getRequest("Bearer $token", tuntasId, id)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant prasyma"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createRequest(request: CreateRequisitionDto): Result<RequisitionDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = requisitionApiService.createRequest("Bearer $token", tuntasId, request)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida kuriant prasyma"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unitReview(id: String, action: String, rejectionReason: String? = null): Result<RequisitionDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = requisitionApiService.unitReview(
                "Bearer $token",
                tuntasId,
                id,
                RequisitionReviewDto(action = action, rejectionReason = rejectionReason)
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida atliekant vieneto perziura"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun topLevelReview(id: String, action: String, rejectionReason: String? = null): Result<RequisitionDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = requisitionApiService.topLevelReview(
                "Bearer $token",
                tuntasId,
                id,
                RequisitionReviewDto(action = action, rejectionReason = rejectionReason)
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida atliekant perziura"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
