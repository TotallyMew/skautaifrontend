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

data class MemberDto(
    @SerializedName("userId") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("surname") val surname: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String?,
    @SerializedName("joinedAt") val joinedAt: String,
    @SerializedName("leadershipRoles") val leadershipRoles: List<MemberLeadershipRoleDto>,
    @SerializedName("ranks") val ranks: List<MemberRankDto>
)

data class MemberListDto(
    @SerializedName("members") val members: List<MemberDto>,
    @SerializedName("total") val total: Int
)