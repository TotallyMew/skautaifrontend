package lt.skautai.android.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface LocationApiService {

    @GET("api/locations")
    suspend fun getLocations(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String
    ): Response<LocationListResponseDto>

    @GET("api/locations/{id}")
    suspend fun getLocation(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String
    ): Response<LocationDto>

    @POST("api/locations")
    suspend fun createLocation(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Body request: CreateLocationRequestDto
    ): Response<LocationDto>

    @PUT("api/locations/{id}")
    suspend fun updateLocation(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String,
        @Body request: UpdateLocationRequestDto
    ): Response<LocationDto>

    @DELETE("api/locations/{id}")
    suspend fun deleteLocation(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("id") id: String
    ): Response<Unit>
}
