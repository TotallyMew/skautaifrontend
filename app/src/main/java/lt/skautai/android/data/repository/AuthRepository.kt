package lt.skautai.android.data.repository

import lt.skautai.android.util.userFacingException

import lt.skautai.android.data.remote.AuthApiService
import lt.skautai.android.data.remote.LoginRequestDto
import lt.skautai.android.data.remote.RefreshTokenRequestDto
import lt.skautai.android.data.remote.ForgotPasswordRequestDto
import lt.skautai.android.data.remote.ResetPasswordRequestDto
import lt.skautai.android.data.remote.RegisterTuntininkasRequestDto
import lt.skautai.android.data.remote.RegisterWithInviteRequestDto
import lt.skautai.android.data.remote.TokenResponseDto
import lt.skautai.android.util.TokenManager
import lt.skautai.android.util.errorMessage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager,
    private val userRepository: UserRepository
) {

    private suspend fun persistSession(
        body: TokenResponseDto,
        resetTuntasContext: Boolean
    ) {
        tokenManager.saveToken(
            token = body.token,
            refreshToken = body.refreshToken,
            userId = body.userId,
            name = body.name,
            email = body.email,
            type = body.type
        )

        if (resetTuntasContext) {
            tokenManager.clearActiveTuntas()
        }

        val tuntai = body.tuntai.orEmpty()
        tokenManager.cacheMyTuntai(tuntai)
        val activeTuntai = tuntai.filter { it.status == "ACTIVE" }
        val currentActiveTuntasId = tokenManager.activeTuntasId.first()
        if (
            currentActiveTuntasId != null &&
            activeTuntai.none { it.id == currentActiveTuntasId }
        ) {
            tokenManager.clearActiveTuntas()
        }
        if (activeTuntai.size == 1 && tokenManager.activeTuntasId.first().isNullOrBlank()) {
            val tuntas = activeTuntai.first()
            val tuntasId = tuntas.id
            tokenManager.setActiveTuntas(tuntasId, tuntas.name)
            userRepository.getMyPermissions(tuntasId)
                .onSuccess {
                    tokenManager.savePermissions(it.permissions)
                    tokenManager.saveLeadershipUnitIds(it.leadershipUnitIds)
                    tokenManager.cacheTuntasContext(tuntasId, it.permissions, it.leadershipUnitIds)
                }
        }
    }

    suspend fun login(email: String, password: String): Result<TokenResponseDto> {
        return try {
            val response = authApiService.login(LoginRequestDto(email, password))
            if (response.isSuccessful) {
                val body = response.body()!!
                persistSession(body, resetTuntasContext = true)
                Result.success(body)

            } else {
                Result.failure(Exception(response.errorMessage("Prisijungimas nepavyko")))
            }
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    suspend fun registerTuntininkas(
        name: String,
        surname: String,
        email: String,
        password: String,
        phone: String?,
        tuntasName: String,
        tuntasKrastas: String?
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
                    tuntasKrastas = tuntasKrastas
                )
            )
            if (response.isSuccessful) {
                val body = response.body()!!
                persistSession(body, resetTuntasContext = true)
                Result.success(body)
            } else {
                Result.failure(Exception(response.errorMessage("Registracija nepavyko")))
            }
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
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
                persistSession(body, resetTuntasContext = true)
                Result.success(body)
            } else {
                Result.failure(Exception(response.errorMessage("Registracija nepavyko")))
            }
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    suspend fun logout() {
        val refreshToken = tokenManager.refreshToken.first()
        try {
            if (!refreshToken.isNullOrBlank()) {
                authApiService.logout(RefreshTokenRequestDto(refreshToken))
            }
        } catch (_: Exception) {
            // Local logout must still succeed when the device is offline.
        } finally {
            tokenManager.clearAll()
        }
    }

    suspend fun refreshSession(refreshToken: String): Result<TokenResponseDto> {
        return try {
            val response = authApiService.refresh(RefreshTokenRequestDto(refreshToken))
            if (response.isSuccessful) {
                val body = response.body()!!
                persistSession(body, resetTuntasContext = false)
                Result.success(body)
            } else {
                Result.failure(Exception(response.errorMessage("Perkrovimas nepavyko")))
            }
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    suspend fun requestPasswordReset(email: String): Result<String> = try {
        val response = authApiService.forgotPassword(ForgotPasswordRequestDto(email.trim()))
        if (response.isSuccessful) {
            Result.success(response.body()?.message ?: "Patikrinkite savo el. paštą.")
        } else {
            Result.failure(Exception(response.errorMessage("Prašymo išsiųsti nepavyko")))
        }
    } catch (e: Exception) {
        Result.failure(e.userFacingException())
    }

    suspend fun resetPassword(token: String, newPassword: String): Result<String> = try {
        val response = authApiService.resetPassword(
            ResetPasswordRequestDto(token = token, newPassword = newPassword)
        )
        if (response.isSuccessful) {
            Result.success(response.body()?.message ?: "Slaptažodis pakeistas.")
        } else {
            Result.failure(Exception(response.errorMessage("Slaptažodžio pakeisti nepavyko")))
        }
    } catch (e: Exception) {
        Result.failure(e.userFacingException())
    }

    fun getToken() = tokenManager.token
    fun getUserId() = tokenManager.userId
    fun getUserName() = tokenManager.userName
    fun getActiveTuntasId() = tokenManager.activeTuntasId
}
