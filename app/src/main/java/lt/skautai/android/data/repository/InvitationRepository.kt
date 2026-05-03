package lt.skautai.android.data.repository

import lt.skautai.android.util.userFacingException

import kotlinx.coroutines.flow.first
import lt.skautai.android.data.remote.AcceptInvitationRequestDto
import lt.skautai.android.data.remote.CreateInvitationRequestDto
import lt.skautai.android.data.remote.InvitationApiService
import lt.skautai.android.data.remote.InvitationResponseDto
import lt.skautai.android.util.TokenManager
import lt.skautai.android.util.errorMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvitationRepository @Inject constructor(
    private val invitationApiService: InvitationApiService,
    private val tokenManager: TokenManager
) {

    suspend fun createInvitation(
        roleId: String,
        organizationalUnitId: String?,
        expiresInHours: Int = 72
    ): Result<InvitationResponseDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = invitationApiService.createInvitation(
                token = "Bearer $token",
                tuntasId = tuntasId,
                request = CreateInvitationRequestDto(
                    roleId = roleId,
                    organizationalUnitId = organizationalUnitId,
                    expiresInHours = expiresInHours
                )
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida kuriant pakvietimą")))
            }
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    suspend fun acceptInvitation(code: String): Result<InvitationResponseDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val response = invitationApiService.acceptInvitation(
                token = "Bearer $token",
                request = AcceptInvitationRequestDto(code = code.trim())
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida priimant pakvietima")))
            }
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }
}
