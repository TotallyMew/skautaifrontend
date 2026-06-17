package lt.skautai.android.data.repository

import lt.skautai.android.util.userFacingException

import kotlinx.coroutines.flow.first
import lt.skautai.android.data.remote.RoleApiService
import lt.skautai.android.data.remote.RoleDto
import lt.skautai.android.util.TokenManager
import lt.skautai.android.util.errorMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoleRepository @Inject constructor(
    private val roleApiService: RoleApiService,
    private val tokenManager: TokenManager
) {
    private var cachedTuntasId: String? = null
    private var cachedRoles: List<RoleDto> = emptyList()

    suspend fun getCachedRoles(): List<RoleDto> {
        val tuntasId = tokenManager.activeTuntasId.first() ?: return emptyList()
        return if (cachedTuntasId == tuntasId) cachedRoles else emptyList()
    }

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
                val roles = response.body()!!.roles
                cachedTuntasId = tuntasId
                cachedRoles = roles
                Result.success(roles)
            } else {
                if (cachedTuntasId == tuntasId && cachedRoles.isNotEmpty()) {
                    Result.success(cachedRoles)
                } else {
                    Result.failure(Exception(response.errorMessage("Klaida gaunant roles")))
                }
            }
        } catch (e: Exception) {
            val tuntasId = tokenManager.activeTuntasId.first()
            if (cachedTuntasId == tuntasId && cachedRoles.isNotEmpty()) {
                Result.success(cachedRoles)
            } else {
                Result.failure(e.userFacingException())
            }
        }
    }
}
