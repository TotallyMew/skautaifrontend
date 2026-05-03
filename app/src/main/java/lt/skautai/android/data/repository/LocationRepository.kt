package lt.skautai.android.data.repository

import lt.skautai.android.util.userFacingException

import java.io.IOException
import java.time.Instant
import java.util.UUID
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
import lt.skautai.android.data.remote.UpdateLocationRequestDto
import lt.skautai.android.data.sync.IdPayload
import lt.skautai.android.data.sync.PendingEntityType
import lt.skautai.android.data.sync.PendingOperationRepository
import lt.skautai.android.data.sync.PendingOperationType
import lt.skautai.android.util.SESSION_EXPIRED_MESSAGE
import lt.skautai.android.util.TUNTAS_SELECTION_REQUIRED_MESSAGE
import lt.skautai.android.util.TokenManager
import lt.skautai.android.util.errorMessage

@Singleton
class LocationRepository @Inject constructor(
    private val locationApiService: LocationApiService,
    private val tokenManager: TokenManager,
    private val locationDao: LocationDao,
    private val pendingOperationRepository: PendingOperationRepository
) {

    private suspend fun token() = tokenManager.token.first()
        ?: throw Exception(SESSION_EXPIRED_MESSAGE)

    private suspend fun tuntasId() = tokenManager.activeTuntasId.first()
        ?: throw Exception(TUNTAS_SELECTION_REQUIRED_MESSAGE)

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
                locationDao.upsertAll(locations.toLocationEntities())
                locationDao.deleteStaleForTuntas(currentTuntasId, locations.map { it.id })
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorMessage("Nepavyko gauti lokacijų.")))
            }
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
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
            Result.failure(refreshResult.exceptionOrNull() ?: Exception("Nepavyko gauti lokacijų."))
        }
    }

    suspend fun getLocation(locationId: String): Result<LocationDto> {
        return try {
            val currentTuntasId = tuntasId()
            val response = locationApiService.getLocation("Bearer ${token()}", currentTuntasId, locationId)
            if (response.isSuccessful) {
                val location = response.body()!!
                locationDao.upsert(location.toEntity())
                Result.success(location)
            } else {
                val cached = locationDao.getLocation(locationId, currentTuntasId)?.toDto()
                cached?.let { Result.success(it) }
                    ?: Result.failure(Exception(response.errorMessage("Nepavyko gauti lokacijos.")))
            }
        } catch (e: Exception) {
            val currentTuntasId = tokenManager.activeTuntasId.first()
            val cached = currentTuntasId?.let { locationDao.getLocation(locationId, it)?.toDto() }
            cached?.let { Result.success(it) } ?: Result.failure(e.userFacingException())
        }
    }

    suspend fun createLocation(request: CreateLocationRequestDto): Result<LocationDto> {
        return try {
            val response = locationApiService.createLocation("Bearer ${token()}", tuntasId(), request)
            if (response.isSuccessful) {
                val location = response.body()!!
                locationDao.upsert(location.toEntity())
                Result.success(location)
            } else {
                Result.failure(Exception(response.errorMessage("Nepavyko sukurti lokacijos.")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val location = LocationDto(
                id = "local-${UUID.randomUUID()}",
                tuntasId = currentTuntasId,
                name = request.name,
                visibility = request.visibility,
                parentLocationId = request.parentLocationId,
                ownerUserId = null,
                ownerUnitId = request.ownerUnitId,
                ownerUnitName = null,
                fullPath = request.name,
                hasChildren = false,
                isLeafSelectable = true,
                isEditable = true,
                address = request.address,
                description = request.description,
                latitude = request.latitude,
                longitude = request.longitude,
                createdAt = Instant.now().toString()
            )
            locationDao.upsert(location.toEntity())
            pendingOperationRepository.enqueue(
                tuntasId = currentTuntasId,
                entityType = PendingEntityType.LOCATION,
                entityId = location.id,
                operationType = PendingOperationType.LOCATION_CREATE,
                payload = request
            )
            Result.success(location)
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    suspend fun updateLocation(locationId: String, request: UpdateLocationRequestDto): Result<LocationDto> {
        return try {
            val currentTuntasId = tuntasId()
            val response = locationApiService.updateLocation("Bearer ${token()}", currentTuntasId, locationId, request)
            if (response.isSuccessful) {
                val location = response.body()!!
                locationDao.upsert(location.toEntity())
                Result.success(location)
            } else {
                Result.failure(Exception(response.errorMessage("Nepavyko atnaujinti lokacijos.")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val cached = locationDao.getLocation(locationId, currentTuntasId)?.toDto()
                ?: return Result.failure(Exception("Lokacija nerasta"))
            val merged = cached.copy(
                name = request.name ?: cached.name,
                visibility = request.visibility ?: cached.visibility,
                address = request.address ?: cached.address,
                description = request.description ?: cached.description,
                latitude = request.latitude ?: cached.latitude,
                longitude = request.longitude ?: cached.longitude
            )
            locationDao.upsert(merged.toEntity())
            val replaced = pendingOperationRepository.replaceCreatePayloadIfPending(
                entityType = PendingEntityType.LOCATION,
                entityId = locationId,
                createOperationType = PendingOperationType.LOCATION_CREATE,
                payload = request
            )
            if (!replaced) {
                pendingOperationRepository.enqueue(
                    tuntasId = currentTuntasId,
                    entityType = PendingEntityType.LOCATION,
                    entityId = locationId,
                    operationType = PendingOperationType.LOCATION_UPDATE,
                    payload = request
                )
            }
            Result.success(merged)
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    suspend fun deleteLocation(locationId: String): Result<Unit> {
        return try {
            val currentTuntasId = tuntasId()
            val response = locationApiService.deleteLocation("Bearer ${token()}", currentTuntasId, locationId)
            if (response.isSuccessful) {
                locationDao.deleteLocation(locationId, currentTuntasId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorMessage("Nepavyko ištrinti lokacijos.")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val deletedCreate = pendingOperationRepository.deletePendingCreateIfExists(
                entityType = PendingEntityType.LOCATION,
                entityId = locationId,
                createOperationType = PendingOperationType.LOCATION_CREATE
            )
            locationDao.deleteLocation(locationId, currentTuntasId)
            if (!deletedCreate) {
                pendingOperationRepository.enqueue(
                    tuntasId = currentTuntasId,
                    entityType = PendingEntityType.LOCATION,
                    entityId = locationId,
                    operationType = PendingOperationType.LOCATION_DELETE,
                    payload = IdPayload(locationId)
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }
}
