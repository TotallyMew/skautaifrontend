package lt.skautai.android.data.repository

import kotlinx.coroutines.flow.first
import lt.skautai.android.data.remote.RoleApiService
import lt.skautai.android.data.remote.RoleDto
import lt.skautai.android.util.TokenManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoleRepository @Inject constructor(
    private val roleApiService: RoleApiService,
    private val tokenManager: TokenManager
) {

    suspend fun getRoles(): Result<List<RoleDto>> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = roleApiService.getRoles(
                token = "Bearer $token",
                tuntasId = tuntasId
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!.roles)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant roles"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}