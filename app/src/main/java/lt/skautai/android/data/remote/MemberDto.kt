package lt.skautai.android.data.remote

import com.google.gson.annotations.SerializedName

data class MemberLeadershipRoleDto(
    @SerializedName("id") val id: String,
    @SerializedName("roleId") val roleId: String,
    @SerializedName("roleName") val roleName: String,
    @SerializedName("organizationalUnitId") val organizationalUnitId: String?,
    @SerializedName("organizationalUnitName") val organizationalUnitName: String?,
    @SerializedName("assignedByUserId") val assignedByUserId: String?,
    @SerializedName("assignedAt") val assignedAt: String,
    @SerializedName("startsAt") val startsAt: String?,
    @SerializedName("expiresAt") val expiresAt: String?,
    @SerializedName("leftAt") val leftAt: String?,
    @SerializedName("termNumber") val termNumber: Int,
    @SerializedName("termStatus") val termStatus: String
)

data class MemberRankDto(
    @SerializedName("id") val id: String,
    @SerializedName("roleId") val roleId: String,
    @SerializedName("roleName") val roleName: String,
    @SerializedName("assignedByUserId") val assignedByUserId: String?,
    @SerializedName("assignedAt") val assignedAt: String
)

data class MemberUnitAssignmentDto(
    @SerializedName("id") val id: String,
    @SerializedName("organizationalUnitId") val organizationalUnitId: String,
    @SerializedName("organizationalUnitName") val organizationalUnitName: String,
    @SerializedName("assignmentType") val assignmentType: String,
    @SerializedName("joinedAt") val joinedAt: String,
    @SerializedName("isPubliclyVisible") val isPubliclyVisible: Boolean = false
)

data class MemberDto(
    @SerializedName("userId") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("surname") val surname: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String?,
    @SerializedName("joinedAt") val joinedAt: String,
    @SerializedName("unitAssignments") val unitAssignments: List<MemberUnitAssignmentDto>? = emptyList(),
    @SerializedName("leadershipRoles") val leadershipRoles: List<MemberLeadershipRoleDto>,
    @SerializedName("leadershipRoleHistory") val leadershipRoleHistory: List<MemberLeadershipRoleDto> = emptyList(),
    @SerializedName("ranks") val ranks: List<MemberRankDto>,
    @SerializedName("isIdentityHidden") val isIdentityHidden: Boolean = false
)

data class MemberListDto(
    @SerializedName("members") val members: List<MemberDto>,
    @SerializedName("total") val total: Int
)

data class AssignLeadershipRoleRequestDto(
    @SerializedName("roleId") val roleId: String,
    @SerializedName("organizationalUnitId") val organizationalUnitId: String? = null,
    @SerializedName("startsAt") val startsAt: String? = null,
    @SerializedName("expiresAt") val expiresAt: String? = null,
    @SerializedName("termNumber") val termNumber: Int = 1
)

data class UpdateLeadershipRoleRequestDto(
    @SerializedName("startsAt") val startsAt: String? = null,
    @SerializedName("expiresAt") val expiresAt: String? = null,
    @SerializedName("termStatus") val termStatus: String? = null,
    @SerializedName("organizationalUnitId") val organizationalUnitId: String? = null
)

data class TransferTuntininkasRequestDto(
    @SerializedName("successorUserId") val successorUserId: String
)

data class CreateLeadershipChangeRequestDto(
    @SerializedName("reason") val reason: String? = null
)

data class ReviewLeadershipChangeRequestDto(
    @SerializedName("action") val action: String,
    @SerializedName("successorUserId") val successorUserId: String? = null,
    @SerializedName("reviewNote") val reviewNote: String? = null
)

data class LeadershipChangeRequestDto(
    @SerializedName("id") val id: String,
    @SerializedName("tuntasId") val tuntasId: String,
    @SerializedName("requesterUserId") val requesterUserId: String,
    @SerializedName("requesterName") val requesterName: String,
    @SerializedName("roleAssignmentId") val roleAssignmentId: String,
    @SerializedName("roleId") val roleId: String,
    @SerializedName("roleName") val roleName: String,
    @SerializedName("organizationalUnitId") val organizationalUnitId: String,
    @SerializedName("organizationalUnitName") val organizationalUnitName: String,
    @SerializedName("status") val status: String,
    @SerializedName("reason") val reason: String?,
    @SerializedName("reviewedByUserId") val reviewedByUserId: String?,
    @SerializedName("successorUserId") val successorUserId: String?,
    @SerializedName("successorName") val successorName: String?,
    @SerializedName("reviewNote") val reviewNote: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
    @SerializedName("reviewedAt") val reviewedAt: String?,
    @SerializedName("resolvedAssignmentId") val resolvedAssignmentId: String?
)

data class LeadershipChangeRequestListDto(
    @SerializedName("requests") val requests: List<LeadershipChangeRequestDto>,
    @SerializedName("total") val total: Int
)

data class AssignRankRequestDto(
    @SerializedName("roleId") val roleId: String
)
