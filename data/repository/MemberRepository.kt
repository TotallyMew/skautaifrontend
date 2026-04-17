package lt.skautai.android.data.repository

import kotlinx.coroutines.flow.first
import lt.skautai.android.data.remote.MemberApiService
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.MemberListDto
import lt.skautai.android.util.TokenManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemberRepository @Inject constructor(
    private val memberApiService: MemberApiService,
    private val tokenManager: TokenManager
) {

    suspend fun getMembers(): Result<MemberListDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = memberApiService.getMembers(
                token = "Bearer $token",
                tuntasId = tuntasId
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant narius"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMember(userId: String): Result<MemberDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = memberApiService.getMember(
                token = "Bearer $token",
                tuntasId = tuntasId,
                userId = userId
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant narį"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}