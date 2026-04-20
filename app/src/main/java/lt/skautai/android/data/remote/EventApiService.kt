package lt.skautai.android.data.remote

import retrofit2.Response
import retrofit2.http.*

interface EventApiService {

    @GET("api/events")
    suspend fun getEvents(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Query("type") type: String? = null,
        @Query("status") status: String? = null
    ): Response<EventListDto>

    @GET("api/events/{id}")
    suspend fun getEvent(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String
    ): Response<EventDto>

    @POST("api/events")
    suspend fun createEvent(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Body request: CreateEventRequestDto
    ): Response<EventDto>

    @PUT("api/events/{id}")
    suspend fun updateEvent(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Body request: UpdateEventRequestDto
    ): Response<EventDto>

    @DELETE("api/events/{id}")
    suspend fun cancelEvent(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String
    ): Response<Unit>
}
