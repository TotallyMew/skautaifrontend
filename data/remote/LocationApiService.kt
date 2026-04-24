package lt.skautai.android.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface LocationApiService {

    @GET("api/locations")
    suspend fun getLocations(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String
    ): Response<LocationListResponseDto>

    @POST("api/locations")
    suspend fun createLocation(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Body request: CreateLocationRequestDto
    ): Response<LocationDto>
}
