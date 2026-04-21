package lt.skautai.android.data.repository

import kotlinx.coroutines.flow.first
import lt.skautai.android.data.remote.CreateLocationRequestDto
import lt.skautai.android.data.remote.LocationApiService
import lt.skautai.android.data.remote.LocationDto
import lt.skautai.android.util.TokenManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val locationApiService: LocationApiService,
    private val tokenManager: TokenManager
) {

    private suspend fun token() = tokenManager.token.first()
        ?: throw Exception("Nav prisijungta")

    private suspend fun tuntasId() = tokenManager.activeTuntasId.first()
        ?: throw Exception("Tuntas nepasirinktas")

    suspend fun getLocations(): Result<List<LocationDto>> {
        return try {
            val response = locationApiService.getLocations("Bearer ${token()}", tuntasId())
            if (response.isSuccessful) Result.success(response.body()!!.locations)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant lokacijas"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createLocation(request: CreateLocationRequestDto): Result<LocationDto> {
        return try {
            val response = locationApiService.createLocation("Bearer ${token()}", tuntasId(), request)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida kuriant lokacija"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
