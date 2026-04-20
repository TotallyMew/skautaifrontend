package lt.skautai.android.data.repository

import lt.skautai.android.data.remote.UserApiService
import lt.skautai.android.data.remote.UserTuntasDto
import lt.skautai.android.data.remote.PermissionsResponseDto
import lt.skautai.android.util.TokenManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userApiService: UserApiService,
    private val tokenManager: TokenManager
) {

    suspend fun getMyTuntai(): Result<List<UserTuntasDto>> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val response = userApiService.getMyTuntai("Bearer $token")
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant tuntus"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyPermissions(tuntasId: String): Result<List<String>> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val response = userApiService.getMyPermissions("Bearer $token", tuntasId)
            if (response.isSuccessful) {
                Result.success(response.body()!!.permissions)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant teises"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}