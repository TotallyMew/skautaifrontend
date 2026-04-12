package lt.skautai.android.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<TokenResponseDto>

    @POST("api/auth/register")
    suspend fun registerTuntininkas(@Body request: RegisterTuntininkasRequestDto): Response<TokenResponseDto>

    @POST("api/auth/register/invite")
    suspend fun registerWithInvite(@Body request: RegisterWithInviteRequestDto): Response<TokenResponseDto>
}