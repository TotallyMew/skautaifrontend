package lt.skautai.android.data.remote

import retrofit2.Response
import retrofit2.http.*

interface MemberApiService {

    @GET("api/members")
    suspend fun getMembers(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String
    ): Response<MemberListDto>

    @GET("api/members/{userId}")
    suspend fun getMember(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("userId") userId: String
    ): Response<MemberDto>

    @POST("api/members/{userId}/leadership-roles")
    suspend fun assignLeadershipRole(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("userId") userId: String,
        @Body request: AssignLeadershipRoleRequestDto
    ): Response<MemberLeadershipRoleDto>

    @PUT("api/members/{userId}/leadership-roles/{assignmentId}")
    suspend fun updateLeadershipRole(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("userId") userId: String,
        @Path("assignmentId") assignmentId: String,
        @Body request: UpdateLeadershipRoleRequestDto
    ): Response<MemberLeadershipRoleDto>

    @DELETE("api/members/{userId}/leadership-roles/{assignmentId}")
    suspend fun removeLeadershipRole(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("userId") userId: String,
        @Path("assignmentId") assignmentId: String
    ): Response<Void>

    @POST("api/members/me/leadership-roles/{assignmentId}/step-down")
    suspend fun stepDownLeadershipRole(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("assignmentId") assignmentId: String
    ): Response<Void>

    @POST("api/members/me/leadership-roles/{assignmentId}/resignation-request")
    suspend fun createLeadershipResignationRequest(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("assignmentId") assignmentId: String,
        @Body request: CreateLeadershipChangeRequestDto
    ): Response<LeadershipChangeRequestDto>

    @GET("api/leadership-change-requests")
    suspend fun getLeadershipChangeRequests(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Query("status") status: String = "PENDING"
    ): Response<LeadershipChangeRequestListDto>

    @POST("api/leadership-change-requests/{requestId}/review")
    suspend fun reviewLeadershipChangeRequest(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("requestId") requestId: String,
        @Body request: ReviewLeadershipChangeRequestDto
    ): Response<LeadershipChangeRequestDto>

    @POST("api/members/me/tuntininkas/transfer")
    suspend fun transferTuntininkas(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Body request: TransferTuntininkasRequestDto
    ): Response<Void>

    @POST("api/members/{userId}/ranks")
    suspend fun assignRank(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("userId") userId: String,
        @Body request: AssignRankRequestDto
    ): Response<MemberRankDto>

    @DELETE("api/members/{userId}/ranks/{rankId}")
    suspend fun removeRank(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("userId") userId: String,
        @Path("rankId") rankId: String
    ): Response<Void>

    @DELETE("api/members/{userId}/remove")
    suspend fun removeMember(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("userId") userId: String
    ): Response<Void>
}
