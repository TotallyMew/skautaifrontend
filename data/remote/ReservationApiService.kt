package lt.skautai.android.data.remote

import retrofit2.Response
import retrofit2.http.*

interface ReservationApiService {

    @GET("api/reservations")
    suspend fun getReservations(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Query("itemId") itemId: String? = null,
        @Query("status") status: String? = null
    ): Response<ReservationListDto>

    @GET("api/reservations/{id}")
    suspend fun getReservation(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String
    ): Response<ReservationDto>

    @POST("api/reservations")
    suspend fun createReservation(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Body request: CreateReservationRequestDto
    ): Response<ReservationDto>

    @PUT("api/reservations/{id}/status")
    suspend fun updateReservationStatus(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Body request: UpdateReservationStatusRequestDto
    ): Response<ReservationDto>

    @DELETE("api/reservations/{id}")
    suspend fun cancelReservation(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String
    ): Response<Unit>
}