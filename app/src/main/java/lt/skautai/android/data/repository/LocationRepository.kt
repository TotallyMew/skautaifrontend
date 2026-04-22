package lt.skautai.android.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import lt.skautai.android.data.local.dao.LocationDao
import lt.skautai.android.data.local.mapper.toDto
import lt.skautai.android.data.local.mapper.toEntity
import lt.skautai.android.data.local.mapper.toLocationDtos
import lt.skautai.android.data.local.mapper.toLocationEntities
import lt.skautai.android.data.remote.CreateLocationRequestDto
import lt.skautai.android.data.remote.LocationApiService
import lt.skautai.android.data.remote.LocationDto
import lt.skautai.android.util.TokenManager

@Singleton
class LocationRepository @Inject constructor(
    private val locationApiService: LocationApiService,
    private val tokenManager: TokenManager,
    private val locationDao: LocationDao
) {

    private suspend fun token() = tokenManager.token.first()
        ?: throw Exception("Nav prisijungta")

    private suspend fun tuntasId() = tokenManager.activeTuntasId.first()
        ?: throw Exception("Tuntas nepasirinktas")

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeLocations(): Flow<List<LocationDto>> {
        return tokenManager.activeTuntasId.flatMapLatest { currentTuntasId ->
            if (currentTuntasId == null) flowOf(emptyList())
            else locationDao.observeLocations(currentTuntasId).map { it.toLocationDtos() }
        }
    }

    suspend fun refreshLocations(): Result<Unit> {
        return try {
            val currentTuntasId = tuntasId()
            val response = locationApiService.getLocations("Bearer ${token()}", currentTuntasId)
            if (response.isSuccessful) {
                val locations = response.body()?.locations.orEmpty()
                locationDao.deleteForTuntas(currentTuntasId)
                locationDao.upsertAll(locations.toLocationEntities())
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant lokacijas"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLocations(): Result<List<LocationDto>> {
        val refreshResult = refreshLocations()
        val currentTuntasId = tokenManager.activeTuntasId.first()
        val cachedLocations = currentTuntasId
            ?.let { locationDao.getLocations(it).toLocationDtos() }
            .orEmpty()
        return if (refreshResult.isSuccess || cachedLocations.isNotEmpty()) {
            Result.success(cachedLocations)
        } else {
            Result.failure(refreshResult.exceptionOrNull() ?: Exception("Klaida gaunant lokacijas"))
        }
    }

    suspend fun createLocation(request: CreateLocationRequestDto): Result<LocationDto> {
        return try {
            val response = locationApiService.createLocation("Bearer ${token()}", tuntasId(), request)
            if (response.isSuccessful) {
                val location = response.body()!!
                locationDao.upsert(location.toEntity())
                Result.success(location)
            }
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida kuriant lokacija"))
        } catch (e: Exception) {
            Result.failure(Exception("Šis veiksmas galimas tik prisijungus", e))
        }
    }
}
