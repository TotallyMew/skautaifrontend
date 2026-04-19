package lt.skautai.android.data.remote

import retrofit2.Response
import retrofit2.http.*

interface RequestApiService {

    @GET("api/inventory-requests")
    suspend fun getRequests(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String
    ): Response<BendrasRequestListDto>

    @GET("api/inventory-requests/{id}")
    suspend fun getRequest(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String
    ): Response<BendrasRequestDto>

    @POST("api/inventory-requests")
    suspend fun createRequest(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Body request: CreateBendrasRequestDto
    ): Response<BendrasRequestDto>

    @DELETE("api/inventory-requests/{id}")
    suspend fun cancelRequest(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String
    ): Response<Unit>

    @POST("api/inventory-requests/{id}/draugininkas-review")
    suspend fun draugininkasReview(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Body request: ReviewRequestDto
    ): Response<BendrasRequestDto>

    @POST("api/inventory-requests/{id}/top-level-review")
    suspend fun topLevelReview(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Body request: ReviewRequestDto
    ): Response<BendrasRequestDto>
}