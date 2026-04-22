package lt.skautai.android.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import lt.skautai.android.data.local.dao.MemberDao
import lt.skautai.android.data.local.mapper.toDto
import lt.skautai.android.data.local.mapper.toEntity
import lt.skautai.android.data.local.mapper.toMemberDtos
import lt.skautai.android.data.local.mapper.toMemberEntities
import lt.skautai.android.data.remote.AssignLeadershipRoleRequestDto
import lt.skautai.android.data.remote.AssignRankRequestDto
import lt.skautai.android.data.remote.MemberApiService
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.MemberLeadershipRoleDto
import lt.skautai.android.data.remote.MemberListDto
import lt.skautai.android.data.remote.MemberRankDto
import lt.skautai.android.util.TokenManager

@Singleton
class MemberRepository @Inject constructor(
    private val memberApiService: MemberApiService,
    private val tokenManager: TokenManager,
    private val memberDao: MemberDao
) {
    private suspend fun token() = tokenManager.token.first()
        ?: throw Exception("Nav prisijungta")

    private suspend fun tuntasId() = tokenManager.activeTuntasId.first()
        ?: throw Exception("Tuntas nepasirinktas")

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeMembers(): Flow<MemberListDto> {
        return tokenManager.activeTuntasId.flatMapLatest { currentTuntasId ->
            if (currentTuntasId == null) {
                flowOf(MemberListDto(emptyList(), 0))
            } else {
                memberDao.observeMembers(currentTuntasId)
                    .map { members -> MemberListDto(members.toMemberDtos(), members.size) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeMember(userId: String): Flow<MemberDto?> {
        return tokenManager.activeTuntasId.flatMapLatest { currentTuntasId ->
            if (currentTuntasId == null) flowOf(null)
            else memberDao.observeMember(userId, currentTuntasId).map { it?.toDto() }
        }
    }

    suspend fun refreshMembers(): Result<Unit> {
        return try {
            val currentTuntasId = tuntasId()
            val response = memberApiService.getMembers("Bearer ${token()}", currentTuntasId)
            if (response.isSuccessful) {
                val members = response.body()?.members.orEmpty()
                memberDao.deleteForTuntas(currentTuntasId)
                memberDao.upsertAll(members.toMemberEntities(currentTuntasId))
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant narius"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshMember(userId: String): Result<Unit> {
        return try {
            val currentTuntasId = tuntasId()
            val response = memberApiService.getMember("Bearer ${token()}", currentTuntasId, userId)
            if (response.isSuccessful) {
                memberDao.upsert(response.body()!!.toEntity(currentTuntasId))
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant nari"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMembers(): Result<MemberListDto> {
        val refreshResult = refreshMembers()
        val currentTuntasId = tokenManager.activeTuntasId.first()
        val cachedMembers = currentTuntasId
            ?.let { memberDao.getMembers(it).toMemberDtos() }
            .orEmpty()
        return if (refreshResult.isSuccess || cachedMembers.isNotEmpty()) {
            Result.success(MemberListDto(cachedMembers, cachedMembers.size))
        } else {
            Result.failure(refreshResult.exceptionOrNull() ?: Exception("Klaida gaunant narius"))
        }
    }

    suspend fun getMember(userId: String): Result<MemberDto> {
        val refreshResult = refreshMember(userId)
        val currentTuntasId = tokenManager.activeTuntasId.first()
        val cachedMember = currentTuntasId?.let { memberDao.getMember(userId, it)?.toDto() }
        return if (cachedMember != null) {
            Result.success(cachedMember)
        } else {
            Result.failure(refreshResult.exceptionOrNull() ?: Exception("Klaida gaunant nari"))
        }
    }

    suspend fun assignLeadershipRole(userId: String, request: AssignLeadershipRoleRequestDto): Result<MemberLeadershipRoleDto> {
        return try {
            val response = memberApiService.assignLeadershipRole("Bearer ${token()}", tuntasId(), userId, request)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida priskiriant pareigas"))
        } catch (e: Exception) { Result.failure(Exception("Šis veiksmas galimas tik prisijungus", e)) }
    }

    suspend fun removeLeadershipRole(userId: String, assignmentId: String): Result<Unit> {
        return try {
            val response = memberApiService.removeLeadershipRole("Bearer ${token()}", tuntasId(), userId, assignmentId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida šalinant pareigas"))
        } catch (e: Exception) { Result.failure(Exception("Šis veiksmas galimas tik prisijungus", e)) }
    }

    suspend fun stepDownLeadershipRole(assignmentId: String): Result<Unit> {
        return try {
            val response = memberApiService.stepDownLeadershipRole("Bearer ${token()}", tuntasId(), assignmentId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida atsistatydinant"))
        } catch (e: Exception) { Result.failure(Exception("Šis veiksmas galimas tik prisijungus", e)) }
    }

    suspend fun assignRank(userId: String, request: AssignRankRequestDto): Result<MemberRankDto> {
        return try {
            val response = memberApiService.assignRank("Bearer ${token()}", tuntasId(), userId, request)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida priskiriant laipsni"))
        } catch (e: Exception) { Result.failure(Exception("Šis veiksmas galimas tik prisijungus", e)) }
    }

    suspend fun removeRank(userId: String, rankId: String): Result<Unit> {
        return try {
            val response = memberApiService.removeRank("Bearer ${token()}", tuntasId(), userId, rankId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida šalinant laipsni"))
        } catch (e: Exception) { Result.failure(Exception("Šis veiksmas galimas tik prisijungus", e)) }
    }

    suspend fun removeMember(userId: String): Result<Unit> {
        return try {
            val currentTuntasId = tuntasId()
            val response = memberApiService.removeMember("Bearer ${token()}", currentTuntasId, userId)
            if (response.isSuccessful) {
                memberDao.deleteMember(userId, currentTuntasId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida šalinant nari"))
            }
        } catch (e: Exception) { Result.failure(Exception("Šis veiksmas galimas tik prisijungus", e)) }
    }
}
