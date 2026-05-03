package lt.skautai.android.data.repository

import lt.skautai.android.util.userFacingException

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
import lt.skautai.android.data.local.dao.OrganizationalUnitDao
import lt.skautai.android.data.local.mapper.toDto
import lt.skautai.android.data.local.mapper.toEntity
import lt.skautai.android.data.local.mapper.toOrganizationalUnitDtos
import lt.skautai.android.data.local.mapper.toOrganizationalUnitEntities
import lt.skautai.android.data.remote.AssignUnitMemberRequestDto
import lt.skautai.android.data.remote.CreateOrganizationalUnitRequestDto
import lt.skautai.android.data.remote.OrganizationalUnitApiService
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.remote.UnitMembershipDto
import lt.skautai.android.data.remote.UpdateOrganizationalUnitRequestDto
import lt.skautai.android.data.sync.PendingEntityType
import lt.skautai.android.data.sync.PendingOperationRepository
import lt.skautai.android.data.sync.PendingOperationType
import lt.skautai.android.data.sync.UnitMemberPayload
import lt.skautai.android.util.TokenManager
import lt.skautai.android.util.errorMessage

@Singleton
class OrganizationalUnitRepository @Inject constructor(
    private val orgUnitApiService: OrganizationalUnitApiService,
    private val tokenManager: TokenManager,
    private val organizationalUnitDao: OrganizationalUnitDao,
    private val memberDao: MemberDao,
    private val pendingOperationRepository: PendingOperationRepository
) {
    private suspend fun token() = tokenManager.token.first()
        ?: throw Exception("Nav prisijungta")

    private suspend fun tuntasId() = tokenManager.activeTuntasId.first()
        ?: throw Exception("Tuntas nepasirinktas")

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeUnits(type: String? = null): Flow<List<OrganizationalUnitDto>> {
        return tokenManager.activeTuntasId.flatMapLatest { tuntasId ->
            if (tuntasId == null) {
                flowOf(emptyList())
            } else {
                organizationalUnitDao.observeUnits(tuntasId, type)
                    .map { it.toOrganizationalUnitDtos() }
            }
        }
    }

    suspend fun refreshUnits(type: String? = null): Result<Unit> {
        return try {
            val currentTuntasId = tuntasId()
            val response = orgUnitApiService.getUnits("Bearer ${token()}", currentTuntasId, type)
            if (response.isSuccessful) {
                val units = response.body()?.units.orEmpty()
                organizationalUnitDao.deleteForQuery(currentTuntasId, type)
                organizationalUnitDao.upsertAll(units.toOrganizationalUnitEntities())
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida gaunant vienetus")))
            }
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    suspend fun getUnits(type: String? = null): Result<List<OrganizationalUnitDto>> {
        val refreshResult = refreshUnits(type)
        val currentTuntasId = tokenManager.activeTuntasId.first()
        val cachedUnits = currentTuntasId
            ?.let { organizationalUnitDao.getUnits(it, type).toOrganizationalUnitDtos() }
            .orEmpty()
        return if (refreshResult.isSuccess || cachedUnits.isNotEmpty()) {
            Result.success(cachedUnits)
        } else {
            Result.failure(refreshResult.exceptionOrNull() ?: Exception("Klaida gaunant vienetus"))
        }
    }

    suspend fun getUnit(unitId: String): Result<OrganizationalUnitDto> {
        return try {
            val currentTuntasId = tuntasId()
            val response = orgUnitApiService.getUnit("Bearer ${token()}", currentTuntasId, unitId)
            if (response.isSuccessful) {
                val unit = response.body()!!
                organizationalUnitDao.upsert(unit.toEntity())
                Result.success(unit)
            } else {
                val cachedUnit = organizationalUnitDao.getUnit(unitId, currentTuntasId)?.toDto()
                if (cachedUnit != null) {
                    Result.success(cachedUnit)
                } else {
                    Result.failure(Exception(response.errorMessage("Vienetas nerastas")))
                }
            }
        } catch (e: Exception) {
            val currentTuntasId = tokenManager.activeTuntasId.first()
            val cachedUnit = currentTuntasId?.let {
                organizationalUnitDao.getUnit(unitId, it)?.toDto()
            }
            if (cachedUnit != null) Result.success(cachedUnit) else Result.failure(e.userFacingException())
        }
    }

    suspend fun createUnit(request: CreateOrganizationalUnitRequestDto): Result<OrganizationalUnitDto> {
        return try {
            val response = orgUnitApiService.createUnit("Bearer ${token()}", tuntasId(), request)
            if (response.isSuccessful) {
                val unit = response.body()!!
                organizationalUnitDao.upsert(unit.toEntity())
                Result.success(unit)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida kuriant vieneta")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val unit = OrganizationalUnitDto(
                id = "local-${UUID.randomUUID()}",
                tuntasId = currentTuntasId,
                name = request.name,
                type = request.type,
                subtype = request.subType,
                acceptedRankId = request.acceptedRankId,
                acceptedRankName = null,
                memberCount = 0,
                itemCount = 0,
                createdAt = Instant.now().toString()
            )
            organizationalUnitDao.upsert(unit.toEntity())
            pendingOperationRepository.enqueue(
                tuntasId = currentTuntasId,
                entityType = PendingEntityType.ORGANIZATIONAL_UNIT,
                entityId = unit.id,
                operationType = PendingOperationType.UNIT_CREATE,
                payload = request
            )
            Result.success(unit)
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    suspend fun updateUnit(unitId: String, request: UpdateOrganizationalUnitRequestDto): Result<OrganizationalUnitDto> {
        return try {
            val response = orgUnitApiService.updateUnit("Bearer ${token()}", tuntasId(), unitId, request)
            if (response.isSuccessful) {
                val unit = response.body()!!
                organizationalUnitDao.upsert(unit.toEntity())
                Result.success(unit)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida atnaujinant vieneta")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val cached = organizationalUnitDao.getUnit(unitId, currentTuntasId)?.toDto()
                ?: return Result.failure(Exception("Vienetas nerastas offline cache"))
            val updated = cached.copy(
                name = request.name ?: cached.name,
                acceptedRankId = request.acceptedRankId ?: cached.acceptedRankId
            )
            organizationalUnitDao.upsert(updated.toEntity())
            pendingOperationRepository.enqueue(
                tuntasId = currentTuntasId,
                entityType = PendingEntityType.ORGANIZATIONAL_UNIT,
                entityId = unitId,
                operationType = PendingOperationType.UNIT_UPDATE,
                payload = request
            )
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    suspend fun deleteUnit(unitId: String): Result<Unit> {
        return try {
            val currentTuntasId = tuntasId()
            val response = orgUnitApiService.deleteUnit("Bearer ${token()}", currentTuntasId, unitId)
            if (response.isSuccessful) {
                organizationalUnitDao.deleteUnit(unitId, currentTuntasId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida trinant vieneta")))
            }
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            organizationalUnitDao.deleteUnit(unitId, currentTuntasId)
            pendingOperationRepository.enqueue(
                tuntasId = currentTuntasId,
                entityType = PendingEntityType.ORGANIZATIONAL_UNIT,
                entityId = unitId,
                operationType = PendingOperationType.UNIT_DELETE,
                payload = mapOf("id" to unitId)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    suspend fun getUnitMembers(unitId: String): Result<List<UnitMembershipDto>> {
        return try {
            val response = orgUnitApiService.getUnitMembers("Bearer ${token()}", tuntasId(), unitId)
            if (response.isSuccessful) Result.success(response.body()!!.members)
            else Result.failure(Exception(response.errorMessage("Klaida gaunant nariųs")))
        } catch (e: Exception) {
            val currentTuntasId = tokenManager.activeTuntasId.first()
            val cachedMembers = currentTuntasId
                ?.let { cachedUnitMembers(it, unitId) }
                .orEmpty()
            if (cachedMembers.isNotEmpty()) Result.success(cachedMembers) else Result.failure(e.userFacingException())
        }
    }

    suspend fun assignUnitMember(unitId: String, request: AssignUnitMemberRequestDto): Result<UnitMembershipDto> {
        return try {
            val response = orgUnitApiService.assignUnitMember("Bearer ${token()}", tuntasId(), unitId, request)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorMessage("Klaida priskiriant nari")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            addUnitAssignmentToCachedMember(currentTuntasId, unitId, request.userId, request.assignmentType)
            pendingOperationRepository.enqueue(
                tuntasId = currentTuntasId,
                entityType = PendingEntityType.ORGANIZATIONAL_UNIT,
                entityId = unitId,
                operationType = PendingOperationType.UNIT_ASSIGN_MEMBER,
                payload = request
            )
            Result.success(localMembership(unitId, request.userId, request.assignmentType, currentTuntasId))
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    suspend fun removeUnitMember(unitId: String, userId: String): Result<Unit> {
        return try {
            val response = orgUnitApiService.removeUnitMember("Bearer ${token()}", tuntasId(), unitId, userId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorMessage("Klaida salinant nari")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            removeUnitAssignmentFromCachedMember(currentTuntasId, unitId, userId)
            pendingOperationRepository.enqueue(
                tuntasId = currentTuntasId,
                entityType = PendingEntityType.ORGANIZATIONAL_UNIT,
                entityId = unitId,
                operationType = PendingOperationType.UNIT_REMOVE_MEMBER,
                payload = UnitMemberPayload(unitId, userId)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    suspend fun leaveUnit(unitId: String): Result<Unit> {
        return try {
            val response = orgUnitApiService.leaveUnit("Bearer ${token()}", tuntasId(), unitId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorMessage("Klaida paliekant vienetą")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            val currentUserId = tokenManager.userId.first().orEmpty()
            removeUnitAssignmentFromCachedMember(currentTuntasId, unitId, currentUserId)
            pendingOperationRepository.enqueue(
                tuntasId = currentTuntasId,
                entityType = PendingEntityType.ORGANIZATIONAL_UNIT,
                entityId = unitId,
                operationType = PendingOperationType.UNIT_LEAVE,
                payload = mapOf("id" to unitId)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    suspend fun moveUnitMember(unitId: String, userId: String): Result<UnitMembershipDto> {
        return try {
            val response = orgUnitApiService.moveUnitMember("Bearer ${token()}", tuntasId(), unitId, userId)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorMessage("Klaida perkeliant nari")))
        } catch (e: IOException) {
            val currentTuntasId = tuntasId()
            addUnitAssignmentToCachedMember(currentTuntasId, unitId, userId, "PRIMARY")
            pendingOperationRepository.enqueue(
                tuntasId = currentTuntasId,
                entityType = PendingEntityType.ORGANIZATIONAL_UNIT,
                entityId = unitId,
                operationType = PendingOperationType.UNIT_MOVE_MEMBER,
                payload = UnitMemberPayload(unitId, userId)
            )
            Result.success(localMembership(unitId, userId, "PRIMARY", currentTuntasId))
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    private suspend fun localMembership(
        unitId: String,
        userId: String,
        assignmentType: String,
        currentTuntasId: String
    ): UnitMembershipDto {
        val unit = organizationalUnitDao.getUnit(unitId, currentTuntasId)?.toDto()
        return UnitMembershipDto(
            id = "local-${UUID.randomUUID()}",
            userId = userId,
            userName = "",
            userSurname = "",
            organizationalUnitId = unitId,
            organizationalUnitName = unit?.name.orEmpty(),
            tuntasId = currentTuntasId,
            assignmentType = assignmentType,
            assignedByUserId = null,
            joinedAt = Instant.now().toString(),
            leftAt = null
        )
    }

    private suspend fun cachedUnitMembers(currentTuntasId: String, unitId: String): List<UnitMembershipDto> {
        val unitName = organizationalUnitDao.getUnit(unitId, currentTuntasId)?.toDto()?.name.orEmpty()
        return memberDao.getMembers(currentTuntasId)
            .map { it.toDto() }
            .flatMap { member ->
                member.unitAssignments.orEmpty()
                    .filter { it.organizationalUnitId == unitId }
                    .map { assignment ->
                        UnitMembershipDto(
                            id = assignment.id,
                            userId = member.userId,
                            userName = member.name,
                            userSurname = member.surname,
                            organizationalUnitId = assignment.organizationalUnitId,
                            organizationalUnitName = assignment.organizationalUnitName.ifBlank { unitName },
                            tuntasId = currentTuntasId,
                            assignmentType = assignment.assignmentType,
                            assignedByUserId = null,
                            joinedAt = assignment.joinedAt,
                            leftAt = null
                        )
                    }
            }
            .sortedWith(compareBy({ it.userSurname.lowercase() }, { it.userName.lowercase() }))
    }

    private suspend fun addUnitAssignmentToCachedMember(
        currentTuntasId: String,
        unitId: String,
        userId: String,
        assignmentType: String
    ) {
        val cachedMember = memberDao.getMember(userId, currentTuntasId)?.toDto() ?: return
        val unit = organizationalUnitDao.getUnit(unitId, currentTuntasId)?.toDto()
        val updatedAssignments = cachedMember.unitAssignments.orEmpty()
            .filterNot { it.organizationalUnitId == unitId }
            .plus(
                lt.skautai.android.data.remote.MemberUnitAssignmentDto(
                    id = "local-${UUID.randomUUID()}",
                    organizationalUnitId = unitId,
                    organizationalUnitName = unit?.name.orEmpty(),
                    assignmentType = assignmentType,
                    joinedAt = Instant.now().toString()
                )
            )
        memberDao.upsert(cachedMember.copy(unitAssignments = updatedAssignments).toEntity(currentTuntasId))
    }

    private suspend fun removeUnitAssignmentFromCachedMember(
        currentTuntasId: String,
        unitId: String,
        userId: String
    ) {
        val cachedMember = memberDao.getMember(userId, currentTuntasId)?.toDto() ?: return
        val updatedAssignments = cachedMember.unitAssignments.orEmpty()
            .filterNot { it.organizationalUnitId == unitId }
        memberDao.upsert(cachedMember.copy(unitAssignments = updatedAssignments).toEntity(currentTuntasId))
    }
}
