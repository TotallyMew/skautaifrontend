package lt.skautai.android.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
import lt.skautai.android.util.TokenManager

@Singleton
class OrganizationalUnitRepository @Inject constructor(
    private val orgUnitApiService: OrganizationalUnitApiService,
    private val tokenManager: TokenManager,
    private val organizationalUnitDao: OrganizationalUnitDao
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
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant vienetus"))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
                    Result.failure(Exception(response.errorBody()?.string() ?: "Vienetas nerastas"))
                }
            }
        } catch (e: Exception) {
            val currentTuntasId = tokenManager.activeTuntasId.first()
            val cachedUnit = currentTuntasId?.let {
                organizationalUnitDao.getUnit(unitId, it)?.toDto()
            }
            if (cachedUnit != null) Result.success(cachedUnit) else Result.failure(e)
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
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida kuriant vieneta"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateUnit(unitId: String, request: UpdateOrganizationalUnitRequestDto): Result<OrganizationalUnitDto> {
        return try {
            val response = orgUnitApiService.updateUnit("Bearer ${token()}", tuntasId(), unitId, request)
            if (response.isSuccessful) {
                val unit = response.body()!!
                organizationalUnitDao.upsert(unit.toEntity())
                Result.success(unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida atnaujinant vieneta"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteUnit(unitId: String): Result<Unit> {
        return try {
            val currentTuntasId = tuntasId()
            val response = orgUnitApiService.deleteUnit("Bearer ${token()}", currentTuntasId, unitId)
            if (response.isSuccessful) {
                organizationalUnitDao.deleteUnit(unitId, currentTuntasId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida trinant vieneta"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getUnitMembers(unitId: String): Result<List<UnitMembershipDto>> {
        return try {
            val response = orgUnitApiService.getUnitMembers("Bearer ${token()}", tuntasId(), unitId)
            if (response.isSuccessful) Result.success(response.body()!!.members)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant narius"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun assignUnitMember(unitId: String, request: AssignUnitMemberRequestDto): Result<UnitMembershipDto> {
        return try {
            val response = orgUnitApiService.assignUnitMember("Bearer ${token()}", tuntasId(), unitId, request)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida priskiriant nari"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun removeUnitMember(unitId: String, userId: String): Result<Unit> {
        return try {
            val response = orgUnitApiService.removeUnitMember("Bearer ${token()}", tuntasId(), unitId, userId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida salinant nari"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun leaveUnit(unitId: String): Result<Unit> {
        return try {
            val response = orgUnitApiService.leaveUnit("Bearer ${token()}", tuntasId(), unitId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida paliekant vieneta"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun moveUnitMember(unitId: String, userId: String): Result<UnitMembershipDto> {
        return try {
            val response = orgUnitApiService.moveUnitMember("Bearer ${token()}", tuntasId(), unitId, userId)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida perkeliant nari"))
        } catch (e: Exception) { Result.failure(e) }
    }
}
