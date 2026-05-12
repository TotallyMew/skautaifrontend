package lt.skautai.android.data.repository

import lt.skautai.android.util.userFacingException

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import lt.skautai.android.data.remote.ChangeMyPasswordRequestDto
import lt.skautai.android.data.remote.MyProfileDto
import lt.skautai.android.data.remote.PermissionsResponseDto
import lt.skautai.android.data.remote.UpdateMyProfileRequestDto
import lt.skautai.android.data.remote.UserApiService
import lt.skautai.android.data.remote.UserTuntasDto
import lt.skautai.android.util.SESSION_EXPIRED_MESSAGE
import lt.skautai.android.util.TokenManager
import lt.skautai.android.util.errorMessage

@Singleton
class UserRepository @Inject constructor(
    private val userApiService: UserApiService,
    private val tokenManager: TokenManager
) {

    suspend fun getMyProfile(): Result<MyProfileDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception(SESSION_EXPIRED_MESSAGE))
            val response = userApiService.getMyProfile("Bearer $token")
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorMessage("Nepavyko gauti profilio.")))
            }
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    suspend fun getMyTuntai(): Result<List<UserTuntasDto>> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception(SESSION_EXPIRED_MESSAGE))
            val response = userApiService.getMyTuntai("Bearer $token")
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorMessage("Nepavyko gauti tuntų sąrašo.")))
            }
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    suspend fun getMyPermissions(tuntasId: String): Result<PermissionsResponseDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception(SESSION_EXPIRED_MESSAGE))
            val response = userApiService.getMyPermissions("Bearer $token", tuntasId)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorMessage("Nepavyko gauti naudotojo teisių.")))
            }
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    suspend fun leaveTuntas(tuntasId: String): Result<Unit> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception(SESSION_EXPIRED_MESSAGE))
            val response = userApiService.leaveTuntas("Bearer $token", tuntasId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorMessage("Nepavyko palikti tunto.")))
            }
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    suspend fun updateMyProfile(
        name: String,
        surname: String,
        email: String,
        phone: String?
    ): Result<MyProfileDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception(SESSION_EXPIRED_MESSAGE))
            val response = userApiService.updateMyProfile(
                "Bearer $token",
                UpdateMyProfileRequestDto(
                    name = name,
                    surname = surname,
                    email = email,
                    phone = phone
                )
            )
            if (response.isSuccessful) {
                val body = response.body()!!
                tokenManager.updateUserIdentity(body.name, body.email)
                Result.success(body)
            } else {
                Result.failure(Exception(response.errorMessage("Nepavyko atnaujinti profilio.")))
            }
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    suspend fun changeMyPassword(
        currentPassword: String,
        newPassword: String
    ): Result<String> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception(SESSION_EXPIRED_MESSAGE))
            val response = userApiService.changeMyPassword(
                "Bearer $token",
                ChangeMyPasswordRequestDto(
                    currentPassword = currentPassword,
                    newPassword = newPassword
                )
            )
            if (response.isSuccessful) {
                Result.success(response.body()?.message ?: "Slaptažodis pakeistas.")
            } else {
                Result.failure(Exception(response.errorMessage("Nepavyko pakeisti slaptažodžio.")))
            }
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }
}
