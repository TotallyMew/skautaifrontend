package lt.skautai.android.data.repository

import lt.skautai.android.data.remote.OrganizationalUnitApiService
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.util.TokenManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrganizationalUnitRepository @Inject constructor(
    private val orgUnitApiService: OrganizationalUnitApiService,
    private val tokenManager: TokenManager
) {

    suspend fun getUnits(type: String? = null): Result<List<OrganizationalUnitDto>> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = orgUnitApiService.getUnits(
                token = "Bearer $token",
                tuntasId = tuntasId,
                type = type
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!.units)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant padalinius"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}