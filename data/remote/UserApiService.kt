package lt.skautai.android.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

data class PermissionsResponseDto(val permissions: List<String>)

interface UserApiService {

    @GET("api/users/me/tuntai")
    suspend fun getMyTuntai(
        @Header("Authorization") token: String
    ): Response<List<UserTuntasDto>>

    @GET("api/users/me/permissions")
    suspend fun getMyPermissions(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String
    ): Response<PermissionsResponseDto>

    @POST("api/users/me/tuntai/{tuntasId}/leave")
    suspend fun leaveTuntas(
        @Header("Authorization") token: String,
        @Path("tuntasId") tuntasId: String
    ): Response<Unit>
}
