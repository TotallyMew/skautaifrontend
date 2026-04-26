package lt.skautai.android.data.repository

import kotlinx.coroutines.flow.first
import lt.skautai.android.data.remote.AssignLeadershipRoleRequestDto
import lt.skautai.android.data.remote.AssignRankRequestDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.MemberLeadershipRoleDto
import lt.skautai.android.data.remote.MemberListDto
import lt.skautai.android.data.remote.MemberRankDto
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.remote.RoleDto
import lt.skautai.android.data.remote.SuperAdminApiService
import lt.skautai.android.data.remote.TuntasDto
import lt.skautai.android.data.remote.UpdateLeadershipRoleRequestDto
import lt.skautai.android.util.TokenManager
import lt.skautai.android.util.errorMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SuperAdminRepository @Inject constructor(
    private val superAdminApiService: SuperAdminApiService,
    private val tokenManager: TokenManager
) {

    private suspend fun token(): String =
        tokenManager.token.first() ?: throw IllegalStateException("Nav prisijungta")

    suspend fun getTuntai(): Result<List<TuntasDto>> = runCatching {
        val response = superAdminApiService.getTuntai("Bearer ${token()}")
        if (!response.isSuccessful) {
            throw Exception(response.errorMessage("Klaida gaunant tuntus"))
        }
        response.body().orEmpty()
    }

    suspend fun approveTuntas(id: String): Result<String> = runCatching {
        val response = superAdminApiService.approveTuntas("Bearer ${token()}", id)
        if (!response.isSuccessful) {
            throw Exception(response.errorMessage("Klaida tvirtinant tuntas"))
        }
        response.body()?.message ?: "Tuntas patvirtintas"
    }

    suspend fun rejectTuntas(id: String): Result<String> = runCatching {
        val response = superAdminApiService.rejectTuntas("Bearer ${token()}", id)
        if (!response.isSuccessful) {
            throw Exception(response.errorMessage("Klaida atmetant tuntas"))
        }
        response.body()?.message ?: "Tuntas atmestas"
    }

    suspend fun getRoles(tuntasId: String): Result<List<RoleDto>> = runCatching {
        val response = superAdminApiService.getRoles("Bearer ${token()}", tuntasId)
        if (!response.isSuccessful) {
            throw Exception(response.errorMessage("Klaida gaunant roles"))
        }
        response.body()?.roles.orEmpty()
    }

    suspend fun getOrganizationalUnits(tuntasId: String): Result<List<OrganizationalUnitDto>> = runCatching {
        val response = superAdminApiService.getOrganizationalUnits("Bearer ${token()}", tuntasId)
        if (!response.isSuccessful) {
            throw Exception(response.errorMessage("Klaida gaunant vienetus"))
        }
        response.body()?.units.orEmpty()
    }

    suspend fun getMembers(tuntasId: String): Result<MemberListDto> = runCatching {
        val response = superAdminApiService.getMembers("Bearer ${token()}", tuntasId)
        if (!response.isSuccessful) {
            throw Exception(response.errorMessage("Klaida gaunant narius"))
        }
        response.body() ?: MemberListDto(emptyList(), 0)
    }

    suspend fun getMember(tuntasId: String, userId: String): Result<MemberDto> = runCatching {
        val response = superAdminApiService.getMember("Bearer ${token()}", tuntasId, userId)
        if (!response.isSuccessful) {
            throw Exception(response.errorMessage("Klaida gaunant nario informaciją"))
        }
        response.body() ?: throw Exception("Narys nerastas")
    }

    suspend fun assignLeadershipRole(
        tuntasId: String,
        userId: String,
        request: AssignLeadershipRoleRequestDto
    ): Result<MemberLeadershipRoleDto> = runCatching {
        val response = superAdminApiService.assignLeadershipRole("Bearer ${token()}", tuntasId, userId, request)
        if (!response.isSuccessful) {
            throw Exception(response.errorMessage("Klaida priskiriant pareigas"))
        }
        response.body() ?: throw Exception("Nepavyko priskirti pareigų")
    }

    suspend fun updateLeadershipRole(
        tuntasId: String,
        userId: String,
        assignmentId: String,
        request: UpdateLeadershipRoleRequestDto
    ): Result<MemberLeadershipRoleDto> = runCatching {
        val response = superAdminApiService.updateLeadershipRole(
            "Bearer ${token()}",
            tuntasId,
            userId,
            assignmentId,
            request
        )
        if (!response.isSuccessful) {
            throw Exception(response.errorMessage("Klaida atnaujinant pareigas"))
        }
        response.body() ?: throw Exception("Nepavyko atnaujinti pareigų")
    }

    suspend fun removeLeadershipRole(
        tuntasId: String,
        userId: String,
        assignmentId: String
    ): Result<String> = runCatching {
        val response = superAdminApiService.removeLeadershipRole("Bearer ${token()}", tuntasId, userId, assignmentId)
        if (!response.isSuccessful) {
            throw Exception(response.errorMessage("Klaida šalinant pareigas"))
        }
        response.body()?.message ?: "Pareigos pašalintos"
    }

    suspend fun assignRank(
        tuntasId: String,
        userId: String,
        request: AssignRankRequestDto
    ): Result<MemberRankDto> = runCatching {
        val response = superAdminApiService.assignRank("Bearer ${token()}", tuntasId, userId, request)
        if (!response.isSuccessful) {
            throw Exception(response.errorMessage("Klaida priskiriant laipsnį"))
        }
        response.body() ?: throw Exception("Nepavyko priskirti laipsnio")
    }

    suspend fun removeRank(
        tuntasId: String,
        userId: String,
        rankId: String
    ): Result<String> = runCatching {
        val response = superAdminApiService.removeRank("Bearer ${token()}", tuntasId, userId, rankId)
        if (!response.isSuccessful) {
            throw Exception(response.errorMessage("Klaida šalinant laipsnį"))
        }
        response.body()?.message ?: "Laipsnis pašalintas"
    }
}
