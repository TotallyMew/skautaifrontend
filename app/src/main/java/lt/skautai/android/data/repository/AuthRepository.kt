package lt.skautai.android.data.repository

import lt.skautai.android.data.remote.AuthApiService
import lt.skautai.android.data.remote.LoginRequestDto
import lt.skautai.android.data.remote.RegisterTuntininkasRequestDto
import lt.skautai.android.data.remote.RegisterWithInviteRequestDto
import lt.skautai.android.data.remote.TokenResponseDto
import lt.skautai.android.util.TokenManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) {

    suspend fun login(email: String, password: String): Result<TokenResponseDto> {
        return try {
            val response = authApiService.login(LoginRequestDto(email, password))
            if (response.isSuccessful) {
                val body = response.body()!!
                tokenManager.saveToken(
                    token = body.token,
                    userId = body.userId,
                    name = body.name,
                    email = body.email,
                    type = body.type
                )
                if (body.tuntai.size == 1) {
                    tokenManager.setActiveTuntas(body.tuntai.first().id)
                }
                Result.success(body)

            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerTuntininkas(
        name: String,
        surname: String,
        email: String,
        password: String,
        phone: String?,
        tuntasName: String,
        tuntasKrastas: String?,
        tuntasContactEmail: String?
    ): Result<TokenResponseDto> {
        return try {
            val response = authApiService.registerTuntininkas(
                RegisterTuntininkasRequestDto(
                    name = name,
                    surname = surname,
                    email = email,
                    password = password,
                    phone = phone,
                    tuntasName = tuntasName,
                    tuntasKrastas = tuntasKrastas,
                    tuntasContactEmail = tuntasContactEmail
                )
            )
            if (response.isSuccessful) {
                val body = response.body()!!
                tokenManager.saveToken(
                    token = body.token,
                    userId = body.userId,
                    name = body.name,
                    email = body.email,
                    type = body.type
                )
                Result.success(body)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Registration failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerWithInvite(
        name: String,
        surname: String,
        email: String,
        password: String,
        phone: String?,
        inviteCode: String
    ): Result<TokenResponseDto> {
        return try {
            val response = authApiService.registerWithInvite(
                RegisterWithInviteRequestDto(
                    name = name,
                    surname = surname,
                    email = email,
                    password = password,
                    phone = phone,
                    inviteCode = inviteCode
                )
            )
            if (response.isSuccessful) {
                val body = response.body()!!
                tokenManager.saveToken(
                    token = body.token,
                    userId = body.userId,
                    name = body.name,
                    email = body.email,
                    type = body.type
                )
                if (body.tuntai.size == 1) {
                    tokenManager.setActiveTuntas(body.tuntai.first().id)
                }
                Result.success(body)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Registration failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        tokenManager.clearAll()
    }

    fun getToken() = tokenManager.token
    fun getUserId() = tokenManager.userId
    fun getUserName() = tokenManager.userName
    fun getActiveTuntasId() = tokenManager.activeTuntasId
}