package lt.skautai.android.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<TokenResponseDto>

    @POST("api/auth/register")
    suspend fun registerTuntininkas(@Body request: RegisterTuntininkasRequestDto): Response<TokenResponseDto>

    @POST("api/auth/register/invite")
    suspend fun registerWithInvite(@Body request: RegisterWithInviteRequestDto): Response<TokenResponseDto>

    @POST("api/super-admin/login")
    suspend fun loginSuperAdmin(@Body request: LoginRequestDto): Response<TokenResponseDto>

    @GET("api/super-admin/tuntai")
    suspend fun getTuntai(
        @Header("Authorization") token: String
    ): Response<List<TuntasDto>>

    @POST("api/super-admin/tuntai/{id}/approve")
    suspend fun approveTuntas(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<MessageResponseDto>

    @POST("api/super-admin/tuntai/{id}/reject")
    suspend fun rejectTuntas(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<MessageResponseDto>


}