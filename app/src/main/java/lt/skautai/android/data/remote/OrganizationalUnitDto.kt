package lt.skautai.android.data.remote

import com.google.gson.annotations.SerializedName

data class OrganizationalUnitDto(
    val id: String,
    val tuntasId: String,
    val name: String,
    val type: String,
    @SerializedName("subType")
    val subtype: String?,
    val acceptedRankId: String?,
    val acceptedRankName: String?,
    val memberCount: Int = 0,
    val itemCount: Int = 0,
    val createdAt: String
)

data class OrganizationalUnitListResponseDto(
    val units: List<OrganizationalUnitDto>,
    val total: Int
)

data class CreateOrganizationalUnitRequestDto(
    val name: String,
    val type: String,
    @SerializedName("subType")
    val subType: String? = null,
    val acceptedRankId: String? = null
)

data class UpdateOrganizationalUnitRequestDto(
    val name: String? = null,
    val acceptedRankId: String? = null
)

data class UnitMembershipDto(
    val id: String,
    val userId: String,
    val userName: String,
    val userSurname: String,
    val organizationalUnitId: String,
    val organizationalUnitName: String,
    val tuntasId: String,
    val assignmentType: String,
    val assignedByUserId: String?,
    val joinedAt: String,
    val leftAt: String?,
    val isPubliclyVisible: Boolean = false,
    val canManageVisibility: Boolean = false,
    val isIdentityHidden: Boolean = false
)

data class UnitMembershipListResponseDto(
    val members: List<UnitMembershipDto>,
    val total: Int
)

data class AssignUnitMemberRequestDto(
    val userId: String,
    val assignmentType: String
)

data class UpdateUnitMemberVisibilityRequestDto(
    val isPubliclyVisible: Boolean
)

data class SeniorUnitAccessAuditDto(
    val id: String,
    val actorUserId: String,
    val actorUserName: String,
    val action: String,
    val accessMode: String,
    val createdAt: String
)

data class SeniorUnitAccessAuditListDto(
    val entries: List<SeniorUnitAccessAuditDto>,
    val total: Int
)
