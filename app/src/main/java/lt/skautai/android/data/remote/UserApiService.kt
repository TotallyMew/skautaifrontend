package lt.skautai.android.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

data class PermissionsResponseDto(
    val permissions: List<String>,
    val leadershipUnitIds: List<String> = emptyList()
)

interface UserApiService {

    @GET("api/users/me")
    suspend fun getMyProfile(
        @Header("Authorization") token: String
    ): Response<MyProfileDto>

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

    @PUT("api/users/me/profile")
    suspend fun updateMyProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateMyProfileRequestDto
    ): Response<MyProfileDto>

    @PUT("api/users/me/password")
    suspend fun changeMyPassword(
        @Header("Authorization") token: String,
        @Body request: ChangeMyPasswordRequestDto
    ): Response<MessageResponseDto>
}
