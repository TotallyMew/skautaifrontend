package lt.skautai.android.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface SuperAdminApiService {

    @GET("api/super-admin/tuntai")
    suspend fun getTuntai(
        @Header("Authorization") token: String
    ): Response<List<TuntasDto>>

    @POST("api/super-admin/tuntai/{id}/approve")
    suspend fun approveTuntas(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<MessageResponseDto>

    @POST("api/super-admin/tuntai/{id}/reject")
    suspend fun rejectTuntas(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<MessageResponseDto>

    @GET("api/super-admin/tuntai/{id}/roles")
    suspend fun getRoles(
        @Header("Authorization") token: String,
        @Path("id") tuntasId: String
    ): Response<RoleListDto>

    @GET("api/super-admin/tuntai/{id}/organizational-units")
    suspend fun getOrganizationalUnits(
        @Header("Authorization") token: String,
        @Path("id") tuntasId: String
    ): Response<OrganizationalUnitListResponseDto>

    @GET("api/super-admin/tuntai/{id}/members")
    suspend fun getMembers(
        @Header("Authorization") token: String,
        @Path("id") tuntasId: String
    ): Response<MemberListDto>

    @GET("api/super-admin/tuntai/{id}/members/{userId}")
    suspend fun getMember(
        @Header("Authorization") token: String,
        @Path("id") tuntasId: String,
        @Path("userId") userId: String
    ): Response<MemberDto>

    @POST("api/super-admin/tuntai/{id}/members/{userId}/leadership-roles")
    suspend fun assignLeadershipRole(
        @Header("Authorization") token: String,
        @Path("id") tuntasId: String,
        @Path("userId") userId: String,
        @Body request: AssignLeadershipRoleRequestDto
    ): Response<MemberLeadershipRoleDto>

    @PUT("api/super-admin/tuntai/{id}/members/{userId}/leadership-roles/{assignmentId}")
    suspend fun updateLeadershipRole(
        @Header("Authorization") token: String,
        @Path("id") tuntasId: String,
        @Path("userId") userId: String,
        @Path("assignmentId") assignmentId: String,
        @Body request: UpdateLeadershipRoleRequestDto
    ): Response<MemberLeadershipRoleDto>

    @DELETE("api/super-admin/tuntai/{id}/members/{userId}/leadership-roles/{assignmentId}")
    suspend fun removeLeadershipRole(
        @Header("Authorization") token: String,
        @Path("id") tuntasId: String,
        @Path("userId") userId: String,
        @Path("assignmentId") assignmentId: String
    ): Response<MessageResponseDto>

    @POST("api/super-admin/tuntai/{id}/members/{userId}/ranks")
    suspend fun assignRank(
        @Header("Authorization") token: String,
        @Path("id") tuntasId: String,
        @Path("userId") userId: String,
        @Body request: AssignRankRequestDto
    ): Response<MemberRankDto>

    @DELETE("api/super-admin/tuntai/{id}/members/{userId}/ranks/{rankId}")
    suspend fun removeRank(
        @Header("Authorization") token: String,
        @Path("id") tuntasId: String,
        @Path("userId") userId: String,
        @Path("rankId") rankId: String
    ): Response<MessageResponseDto>
}
