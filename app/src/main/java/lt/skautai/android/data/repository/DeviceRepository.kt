package lt.skautai.android.data.repository

import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import lt.skautai.android.data.remote.DeviceApiService
import lt.skautai.android.data.remote.RegisterDeviceRequestDto
import lt.skautai.android.util.TokenManager
import lt.skautai.android.util.errorMessage
import lt.skautai.android.util.userFacingException

@Singleton
class DeviceRepository @Inject constructor(
    private val deviceApiService: DeviceApiService,
    private val tokenManager: TokenManager
) {
    suspend fun registerDevice(deviceToken: String): Result<Unit> = try {
        val token = tokenManager.token.first() ?: return Result.failure(Exception("Nav prisijungta"))
        val response = deviceApiService.registerDevice(
            token = "Bearer $token",
            request = RegisterDeviceRequestDto(
                deviceToken = deviceToken,
                deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
            )
        )
        if (response.isSuccessful) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(response.errorMessage("Nepavyko užregistruoti įrenginio pranešimams")))
        }
    } catch (e: Exception) {
        Result.failure(e.userFacingException())
    }

    suspend fun unregisterDevice(deviceToken: String): Result<Unit> = try {
        val token = tokenManager.token.first() ?: return Result.failure(Exception("Nav prisijungta"))
        val response = deviceApiService.unregisterDevice(
            token = "Bearer $token",
            request = RegisterDeviceRequestDto(
                deviceToken = deviceToken,
                deviceName = null
            )
        )
        if (response.isSuccessful) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(response.errorMessage("Nepavyko išregistruoti įrenginio pranešimams")))
        }
    } catch (e: Exception) {
        Result.failure(e.userFacingException())
    }
}
