package lt.skautai.android.data.repository

import kotlinx.coroutines.flow.first
import lt.skautai.android.data.remote.*
import lt.skautai.android.util.TokenManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemberRepository @Inject constructor(
    private val memberApiService: MemberApiService,
    private val tokenManager: TokenManager
) {

    private suspend fun token() = tokenManager.token.first()
        ?: throw Exception("Nav prisijungta")

    private suspend fun tuntasId() = tokenManager.activeTuntasId.first()
        ?: throw Exception("Tuntas nepasirinktas")

    suspend fun getMembers(): Result<MemberListDto> {
        return try {
            val response = memberApiService.getMembers("Bearer ${token()}", tuntasId())
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant narius"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getMember(userId: String): Result<MemberDto> {
        return try {
            val response = memberApiService.getMember("Bearer ${token()}", tuntasId(), userId)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant narį"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun assignLeadershipRole(userId: String, request: AssignLeadershipRoleRequestDto): Result<MemberLeadershipRoleDto> {
        return try {
            val response = memberApiService.assignLeadershipRole("Bearer ${token()}", tuntasId(), userId, request)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida priskiriant pareigas"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun removeLeadershipRole(userId: String, assignmentId: String): Result<Unit> {
        return try {
            val response = memberApiService.removeLeadershipRole("Bearer ${token()}", tuntasId(), userId, assignmentId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida šalinant pareigas"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun assignRank(userId: String, request: AssignRankRequestDto): Result<MemberRankDto> {
        return try {
            val response = memberApiService.assignRank("Bearer ${token()}", tuntasId(), userId, request)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida priskiriant laipsnį"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun removeRank(userId: String, rankId: String): Result<Unit> {
        return try {
            val response = memberApiService.removeRank("Bearer ${token()}", tuntasId(), userId, rankId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida šalinant laipsnį"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun removeMember(userId: String): Result<Unit> {
        return try {
            val response = memberApiService.removeMember("Bearer ${token()}", tuntasId(), userId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida šalinant narį"))
        } catch (e: Exception) { Result.failure(e) }
    }
}
