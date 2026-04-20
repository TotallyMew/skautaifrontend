package lt.skautai.android.data.repository

import kotlinx.coroutines.flow.first
import lt.skautai.android.data.remote.*
import lt.skautai.android.util.TokenManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val eventApiService: EventApiService,
    private val tokenManager: TokenManager
) {

    suspend fun getEvents(type: String? = null, status: String? = null): Result<EventListDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = eventApiService.getEvents("Bearer $token", tuntasId, type, status)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEvent(id: String): Result<EventDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = eventApiService.getEvent("Bearer $token", tuntasId, id)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createEvent(request: CreateEventRequestDto): Result<EventDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = eventApiService.createEvent("Bearer $token", tuntasId, request)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEvent(id: String, request: UpdateEventRequestDto): Result<EventDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = eventApiService.updateEvent("Bearer $token", tuntasId, id, request)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelEvent(id: String): Result<Unit> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = eventApiService.cancelEvent("Bearer $token", tuntasId, id)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
