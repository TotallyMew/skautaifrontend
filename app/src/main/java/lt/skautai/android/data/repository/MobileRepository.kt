package lt.skautai.android.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import lt.skautai.android.data.remote.MobileApiService
import lt.skautai.android.data.remote.MobileCacheStateDto
import lt.skautai.android.data.remote.MobileHomeSummaryDto
import lt.skautai.android.util.TokenManager
import lt.skautai.android.util.errorMessage
import lt.skautai.android.util.userFacingException

@Singleton
class MobileRepository @Inject constructor(
    private val mobileApiService: MobileApiService,
    private val tokenManager: TokenManager,
    private val refreshCoordinator: RefreshCoordinator
) {
    suspend fun getHomeSummary(forceRefresh: Boolean = false): Result<MobileHomeSummaryDto> = try {
        val token = tokenManager.token.first() ?: return Result.failure(Exception("Nav prisijungta"))
        val tuntasId = tokenManager.activeTuntasId.first() ?: return Result.failure(Exception("Tuntas nepasirinktas"))
        val response = mobileApiService.getHomeSummary(
            token = "Bearer $token",
            tuntasId = tuntasId,
            activeOrgUnitId = tokenManager.activeOrgUnitId.first()
        )
        if (response.isSuccessful) {
            refreshCoordinator.recordAttempt(HOME_SUMMARY_RESOURCE, success = true)
            Result.success(response.body()!!)
        } else {
            val error = response.errorMessage("Nepavyko gauti pradžios suvestinės")
            refreshCoordinator.recordAttempt(HOME_SUMMARY_RESOURCE, success = false, error = error)
            Result.failure(Exception(error))
        }
    } catch (e: Exception) {
        refreshCoordinator.recordAttempt(HOME_SUMMARY_RESOURCE, success = false, error = e.message)
        Result.failure(e.userFacingException())
    }

    suspend fun getCacheState(): Result<MobileCacheStateDto> = try {
        val token = tokenManager.token.first() ?: return Result.failure(Exception("Nav prisijungta"))
        val tuntasId = tokenManager.activeTuntasId.first() ?: return Result.failure(Exception("Tuntas nepasirinktas"))
        val response = mobileApiService.getCacheState("Bearer $token", tuntasId)
        if (response.isSuccessful) Result.success(response.body()!!)
        else Result.failure(Exception(response.errorMessage("Nepavyko gauti cache būsenos")))
    } catch (e: Exception) {
        Result.failure(e.userFacingException())
    }

    companion object {
        private const val HOME_SUMMARY_RESOURCE = "home_summary"
    }
}
