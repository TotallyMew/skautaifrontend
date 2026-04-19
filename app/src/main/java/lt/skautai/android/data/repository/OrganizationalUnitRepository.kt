package lt.skautai.android.data.repository

import lt.skautai.android.data.remote.*
import lt.skautai.android.util.TokenManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrganizationalUnitRepository @Inject constructor(
    private val orgUnitApiService: OrganizationalUnitApiService,
    private val tokenManager: TokenManager
) {

    private suspend fun token() = tokenManager.token.first()
        ?: throw Exception("Nav prisijungta")

    private suspend fun tuntasId() = tokenManager.activeTuntasId.first()
        ?: throw Exception("Tuntas nepasirinktas")

    suspend fun getUnits(type: String? = null): Result<List<OrganizationalUnitDto>> {
        return try {
            val response = orgUnitApiService.getUnits("Bearer ${token()}", tuntasId(), type)
            if (response.isSuccessful) Result.success(response.body()!!.units)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant vienetus"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getUnit(unitId: String): Result<OrganizationalUnitDto> {
        return try {
            val response = orgUnitApiService.getUnit("Bearer ${token()}", tuntasId(), unitId)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Vienetas nerastas"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun createUnit(request: CreateOrganizationalUnitRequestDto): Result<OrganizationalUnitDto> {
        return try {
            val response = orgUnitApiService.createUnit("Bearer ${token()}", tuntasId(), request)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida kuriant vienetą"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateUnit(unitId: String, request: UpdateOrganizationalUnitRequestDto): Result<OrganizationalUnitDto> {
        return try {
            val response = orgUnitApiService.updateUnit("Bearer ${token()}", tuntasId(), unitId, request)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida atnaujinant vienetą"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteUnit(unitId: String): Result<Unit> {
        return try {
            val response = orgUnitApiService.deleteUnit("Bearer ${token()}", tuntasId(), unitId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida trinant vienetą"))
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
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida priskiriant narį"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun removeUnitMember(unitId: String, userId: String): Result<Unit> {
        return try {
            val response = orgUnitApiService.removeUnitMember("Bearer ${token()}", tuntasId(), unitId, userId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Klaida šalinant narį"))
        } catch (e: Exception) { Result.failure(e) }
    }
}
