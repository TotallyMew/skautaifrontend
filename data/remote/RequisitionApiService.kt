package lt.skautai.android.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface RequisitionApiService {

    @GET("api/requisitions")
    suspend fun getRequests(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String
    ): Response<RequisitionListDto>

    @GET("api/requisitions/{id}")
    suspend fun getRequest(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String
    ): Response<RequisitionDto>

    @POST("api/requisitions")
    suspend fun createRequest(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Body request: CreateRequisitionDto
    ): Response<RequisitionDto>

    @POST("api/requisitions/{id}/unit-review")
    suspend fun unitReview(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Body request: RequisitionReviewDto
    ): Response<RequisitionDto>

    @POST("api/requisitions/{id}/top-level-review")
    suspend fun topLevelReview(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Body request: RequisitionReviewDto
    ): Response<RequisitionDto>
}
