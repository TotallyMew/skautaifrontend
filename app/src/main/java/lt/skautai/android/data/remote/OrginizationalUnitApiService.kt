package lt.skautai.android.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface OrganizationalUnitApiService {

    @GET("api/organizational-units")
    suspend fun getUnits(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Query("type") type: String? = null
    ): Response<OrganizationalUnitListResponseDto>
}