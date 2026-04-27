package lt.skautai.android.data.repository

import java.io.IOException
import java.time.Instant
import java.util.UUID
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
import lt.skautai.android.data.remote.TransferTuntininkasRequestDto
import lt.skautai.android.data.remote.UpdateLeadershipRoleRequestDto
import lt.skautai.android.data.sync.MemberAssignmentPayload
import lt.skautai.android.data.sync.MemberRankPayload
import lt.skautai.android.data.sync.PendingEntityType
import lt.skautai.android.data.sync.PendingOperationRepository
import lt.skautai.android.data.sync.PendingOperationType
import lt.skautai.android.util.TokenManager
import lt.skautai.android.util.errorMessage

@Singleton
class MemberRepository @Inject constructor(
    private val memberApiService: MemberApiService,
    private val tokenManager: TokenManager,
    private val memberDao: MemberDao,
    private val pendingOperationRepository: PendingOperationRepository
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
                Result.failure(Exception(response.errorMessage("Klaida gaunant narius")))
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
                Result.failure(Exception(response.errorMessage("Klaida gaunant nari")))
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

    suspend fun assignLeadershipRole(
        userId: String,
        request: AssignLeadershipRoleRequestDto
    ): Result<MemberLeadershipRoleDto> {
        return try {
            val response = memberApiService.assignLeadershipRole("Bearer ${token()}", tuntasId(), userId, request)
            if (response.isSuccessful) {
                val role = response.body()!!
                refreshMember(userId)
                Result.success(role)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida priskiriant pareigas")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val role = MemberLeadershipRoleDto(
                id = "local-${UUID.randomUUID()}",
                roleId = request.roleId,
                roleName = request.roleId,
                organizationalUnitId = request.organizationalUnitId,
                organizationalUnitName = null,
                assignedByUserId = null,
                assignedAt = Instant.now().toString(),
                startsAt = request.startsAt,
                expiresAt = request.expiresAt,
                leftAt = null,
                termNumber = request.termNumber,
                termStatus = "ACTIVE"
            )
            updateCachedMember(currentTuntasId, userId) { member ->
                member.copy(leadershipRoles = member.leadershipRoles + role)
            }
            pendingOperationRepository.enqueue(
                tuntasId = currentTuntasId,
                entityType = PendingEntityType.MEMBER,
                entityId = userId,
                operationType = PendingOperationType.MEMBER_ASSIGN_LEADERSHIP_ROLE,
                payload = request
            )
            Result.success(role)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeLeadershipRole(userId: String, assignmentId: String): Result<Unit> {
        return try {
            val response = memberApiService.removeLeadershipRole("Bearer ${token()}", tuntasId(), userId, assignmentId)
            if (response.isSuccessful) {
                refreshMember(userId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida salinant pareigas")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            updateCachedMember(currentTuntasId, userId) { member ->
                member.copy(leadershipRoles = member.leadershipRoles.filterNot { it.id == assignmentId })
            }
            pendingOperationRepository.enqueue(
                tuntasId = currentTuntasId,
                entityType = PendingEntityType.MEMBER,
                entityId = userId,
                operationType = PendingOperationType.MEMBER_REMOVE_LEADERSHIP_ROLE,
                payload = MemberAssignmentPayload(userId, assignmentId)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateLeadershipRole(
        userId: String,
        assignmentId: String,
        request: UpdateLeadershipRoleRequestDto
    ): Result<MemberLeadershipRoleDto> {
        return try {
            val response = memberApiService.updateLeadershipRole(
                "Bearer ${token()}",
                tuntasId(),
                userId,
                assignmentId,
                request
            )
            if (response.isSuccessful) {
                val role = response.body()!!
                refreshMember(userId)
                Result.success(role)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida atnaujinant pareigas")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun stepDownLeadershipRole(assignmentId: String): Result<Unit> {
        return try {
            val response = memberApiService.stepDownLeadershipRole("Bearer ${token()}", tuntasId(), assignmentId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorMessage("Klaida atsistatydinant")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val userId = findMemberByLeadershipAssignment(currentTuntasId, assignmentId)
                ?: return Result.failure(Exception("Nario pareigos nerastos offline cache"))
            updateCachedMember(currentTuntasId, userId) { member ->
                member.copy(leadershipRoles = member.leadershipRoles.filterNot { it.id == assignmentId })
            }
            pendingOperationRepository.enqueue(
                tuntasId = currentTuntasId,
                entityType = PendingEntityType.MEMBER,
                entityId = userId,
                operationType = PendingOperationType.MEMBER_STEP_DOWN_LEADERSHIP_ROLE,
                payload = MemberAssignmentPayload(userId, assignmentId)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun transferTuntininkas(successorUserId: String): Result<Unit> {
        return try {
            val response = memberApiService.transferTuntininkas(
                "Bearer ${token()}",
                tuntasId(),
                TransferTuntininkasRequestDto(successorUserId)
            )
            if (response.isSuccessful) {
                refreshMembers()
                refreshMember(successorUserId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida perleidziant tuntininko pareigas")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun assignRank(userId: String, request: AssignRankRequestDto): Result<MemberRankDto> {
        return try {
            val response = memberApiService.assignRank("Bearer ${token()}", tuntasId(), userId, request)
            if (response.isSuccessful) {
                val rank = response.body()!!
                refreshMember(userId)
                Result.success(rank)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida priskiriant laipsni")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val rank = MemberRankDto(
                id = "local-${UUID.randomUUID()}",
                roleId = request.roleId,
                roleName = request.roleId,
                assignedByUserId = null,
                assignedAt = Instant.now().toString()
            )
            updateCachedMember(currentTuntasId, userId) { member ->
                member.copy(ranks = member.ranks + rank)
            }
            pendingOperationRepository.enqueue(
                tuntasId = currentTuntasId,
                entityType = PendingEntityType.MEMBER,
                entityId = userId,
                operationType = PendingOperationType.MEMBER_ASSIGN_RANK,
                payload = request
            )
            Result.success(rank)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeRank(userId: String, rankId: String): Result<Unit> {
        return try {
            val response = memberApiService.removeRank("Bearer ${token()}", tuntasId(), userId, rankId)
            if (response.isSuccessful) {
                refreshMember(userId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida salinant laipsni")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            updateCachedMember(currentTuntasId, userId) { member ->
                member.copy(ranks = member.ranks.filterNot { it.id == rankId })
            }
            pendingOperationRepository.enqueue(
                tuntasId = currentTuntasId,
                entityType = PendingEntityType.MEMBER,
                entityId = userId,
                operationType = PendingOperationType.MEMBER_REMOVE_RANK,
                payload = MemberRankPayload(userId, rankId)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeMember(userId: String): Result<Unit> {
        return try {
            val currentTuntasId = tuntasId()
            val response = memberApiService.removeMember("Bearer ${token()}", currentTuntasId, userId)
            if (response.isSuccessful) {
                memberDao.deleteMember(userId, currentTuntasId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida salinant nari")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            memberDao.deleteMember(userId, currentTuntasId)
            pendingOperationRepository.enqueue(
                tuntasId = currentTuntasId,
                entityType = PendingEntityType.MEMBER,
                entityId = userId,
                operationType = PendingOperationType.MEMBER_REMOVE,
                payload = mapOf("id" to userId)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateCachedMember(
        currentTuntasId: String,
        userId: String,
        update: (MemberDto) -> MemberDto
    ) {
        val cached = memberDao.getMember(userId, currentTuntasId)?.toDto() ?: return
        memberDao.upsert(update(cached).toEntity(currentTuntasId))
    }

    private suspend fun findMemberByLeadershipAssignment(
        currentTuntasId: String,
        assignmentId: String
    ): String? {
        return memberDao.getMembers(currentTuntasId)
            .map { it.toDto() }
            .firstOrNull { member -> member.leadershipRoles.any { it.id == assignmentId } }
            ?.userId
    }
}
