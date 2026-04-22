package lt.skautai.android.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import lt.skautai.android.data.local.dao.EventDao
import lt.skautai.android.data.local.mapper.toDto
import lt.skautai.android.data.local.mapper.toEntity
import lt.skautai.android.data.local.mapper.toEventDtos
import lt.skautai.android.data.local.mapper.toEventEntities
import lt.skautai.android.data.remote.CreateEventRequestDto
import lt.skautai.android.data.remote.EventApiService
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.EventListDto
import lt.skautai.android.data.remote.UpdateEventRequestDto
import lt.skautai.android.util.TokenManager

@Singleton
class EventRepository @Inject constructor(
    private val eventApiService: EventApiService,
    private val tokenManager: TokenManager,
    private val eventDao: EventDao
) {
    private suspend fun token() = tokenManager.token.first()
        ?: throw Exception("Nav prisijungta")

    private suspend fun tuntasId() = tokenManager.activeTuntasId.first()
        ?: throw Exception("Tuntas nepasirinktas")

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeEvents(type: String? = null, status: String? = null): Flow<EventListDto> {
        return tokenManager.activeTuntasId.flatMapLatest { currentTuntasId ->
            if (currentTuntasId == null) {
                flowOf(EventListDto(emptyList(), 0))
            } else {
                eventDao.observeEvents(currentTuntasId, type, status)
                    .map { events -> EventListDto(events.toEventDtos(), events.size) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeEvent(id: String): Flow<EventDto?> {
        return tokenManager.activeTuntasId.flatMapLatest { currentTuntasId ->
            if (currentTuntasId == null) flowOf(null)
            else eventDao.observeEvent(id, currentTuntasId).map { it?.toDto() }
        }
    }

    suspend fun refreshEvents(type: String? = null, status: String? = null): Result<Unit> {
        return try {
            val currentTuntasId = tuntasId()
            val response = eventApiService.getEvents("Bearer ${token()}", currentTuntasId, type, status)
            if (response.isSuccessful) {
                val events = response.body()?.events.orEmpty()
                eventDao.deleteForQuery(currentTuntasId, type, status)
                eventDao.upsertAll(events.toEventEntities())
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshEvent(id: String): Result<Unit> {
        return try {
            val currentTuntasId = tuntasId()
            val response = eventApiService.getEvent("Bearer ${token()}", currentTuntasId, id)
            if (response.isSuccessful) {
                eventDao.upsert(response.body()!!.toEntity())
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEvents(type: String? = null, status: String? = null): Result<EventListDto> {
        val refreshResult = refreshEvents(type, status)
        val currentTuntasId = tokenManager.activeTuntasId.first()
        val cachedEvents = currentTuntasId
            ?.let { eventDao.getEvents(it, type, status).toEventDtos() }
            .orEmpty()
        return if (refreshResult.isSuccess || cachedEvents.isNotEmpty()) {
            Result.success(EventListDto(cachedEvents, cachedEvents.size))
        } else {
            Result.failure(refreshResult.exceptionOrNull() ?: Exception("Klaida"))
        }
    }

    suspend fun getEvent(id: String): Result<EventDto> {
        val refreshResult = refreshEvent(id)
        val currentTuntasId = tokenManager.activeTuntasId.first()
        val cachedEvent = currentTuntasId?.let { eventDao.getEvent(id, it)?.toDto() }
        return if (cachedEvent != null) {
            Result.success(cachedEvent)
        } else {
            Result.failure(refreshResult.exceptionOrNull() ?: Exception("Klaida"))
        }
    }

    suspend fun createEvent(request: CreateEventRequestDto): Result<EventDto> {
        return try {
            val response = eventApiService.createEvent("Bearer ${token()}", tuntasId(), request)
            if (response.isSuccessful) {
                val event = response.body()!!
                eventDao.upsert(event.toEntity())
                Result.success(event)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida"))
            }
        } catch (e: Exception) { Result.failure(Exception("Šis veiksmas galimas tik prisijungus", e)) }
    }

    suspend fun updateEvent(id: String, request: UpdateEventRequestDto): Result<EventDto> {
        return try {
            val response = eventApiService.updateEvent("Bearer ${token()}", tuntasId(), id, request)
            if (response.isSuccessful) {
                val event = response.body()!!
                eventDao.upsert(event.toEntity())
                Result.success(event)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida"))
            }
        } catch (e: Exception) { Result.failure(Exception("Šis veiksmas galimas tik prisijungus", e)) }
    }

    suspend fun cancelEvent(id: String): Result<Unit> {
        return try {
            val currentTuntasId = tuntasId()
            val response = eventApiService.cancelEvent("Bearer ${token()}", currentTuntasId, id)
            if (response.isSuccessful) {
                eventDao.deleteEvent(id, currentTuntasId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida"))
            }
        } catch (e: Exception) { Result.failure(Exception("Šis veiksmas galimas tik prisijungus", e)) }
    }
}
