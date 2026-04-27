package lt.skautai.android.data.repository

import lt.skautai.android.data.remote.UserApiService
import lt.skautai.android.data.remote.UserTuntasDto
import lt.skautai.android.data.remote.PermissionsResponseDto
import lt.skautai.android.data.remote.MyProfileDto
import lt.skautai.android.data.remote.UpdateMyProfileRequestDto
import lt.skautai.android.data.remote.ChangeMyPasswordRequestDto
import lt.skautai.android.util.TokenManager
import lt.skautai.android.util.errorMessage
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userApiService: UserApiService,
    private val tokenManager: TokenManager
) {

    suspend fun getMyProfile(): Result<MyProfileDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val response = userApiService.getMyProfile("Bearer $token")
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida gaunant profili")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyTuntai(): Result<List<UserTuntasDto>> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val response = userApiService.getMyTuntai("Bearer $token")
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida gaunant tuntus")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyPermissions(tuntasId: String): Result<List<String>> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val response = userApiService.getMyPermissions("Bearer $token", tuntasId)
            if (response.isSuccessful) {
                Result.success(response.body()!!.permissions)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida gaunant teises")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveTuntas(tuntasId: String): Result<Unit> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val response = userApiService.leaveTuntas("Bearer $token", tuntasId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida paliekant tunta")))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
                ?: return Result.failure(Exception("Nav prisijungta"))
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
                Result.failure(Exception(response.errorMessage("Klaida atnaujinant profili")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changeMyPassword(
        currentPassword: String,
        newPassword: String
    ): Result<String> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val response = userApiService.changeMyPassword(
                "Bearer $token",
                ChangeMyPasswordRequestDto(
                    currentPassword = currentPassword,
                    newPassword = newPassword
                )
            )
            if (response.isSuccessful) {
                Result.success(response.body()?.message ?: "Slaptažodis pakeistas")
            } else {
                Result.failure(Exception(response.errorMessage("Klaida keičiant slaptažodį")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
