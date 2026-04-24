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

    @GET("api/reservations/availability")
    suspend fun getAvailability(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<ReservationAvailabilityDto>

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

    @PUT("api/reservations/{id}/pickup-time")
    suspend fun updateReservationPickupTime(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Body request: UpdateReservationPickupRequestDto
    ): Response<ReservationDto>

    @PUT("api/reservations/{id}/return-time")
    suspend fun updateReservationReturnTime(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Body request: UpdateReservationReturnTimeRequestDto
    ): Response<ReservationDto>

    @POST("api/reservations/{id}/unit-review")
    suspend fun reviewUnitReservation(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Body request: ReviewReservationRequestDto
    ): Response<ReservationDto>

    @POST("api/reservations/{id}/top-level-review")
    suspend fun reviewTopLevelReservation(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Body request: ReviewReservationRequestDto
    ): Response<ReservationDto>

    @GET("api/reservations/{id}/movements")
    suspend fun getReservationMovements(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String
    ): Response<ReservationMovementListDto>

    @POST("api/reservations/{id}/issue")
    suspend fun issueReservationItems(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Body request: ReservationMovementRequestDto
    ): Response<ReservationDto>

    @POST("api/reservations/{id}/return")
    suspend fun returnReservationItems(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Body request: ReservationMovementRequestDto
    ): Response<ReservationDto>

    @POST("api/reservations/{id}/mark-returned")
    suspend fun markReservationItemsReturned(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Body request: ReservationMovementRequestDto
    ): Response<ReservationDto>

    @DELETE("api/reservations/{id}")
    suspend fun cancelReservation(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String
    ): Response<Unit>
}
