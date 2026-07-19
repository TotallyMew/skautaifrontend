package lt.skautai.android.data.remote

import com.google.gson.annotations.SerializedName

data class CreateInvitationRequestDto(
    @SerializedName("roleId") val roleId: String,
    @SerializedName("organizationalUnitId") val organizationalUnitId: String?,
    @SerializedName("expiresInHours") val expiresInHours: Int = 72
)

data class AcceptInvitationRequestDto(
    @SerializedName("code") val code: String
)

data class InvitationResponseDto(
    @SerializedName("code") val code: String,
    @SerializedName("tuntasId") val tuntasId: String? = null,
    @SerializedName("roleName") val roleName: String,
    @SerializedName("tuntasName") val tuntasName: String,
    @SerializedName("expiresAt") val expiresAt: String,
    @SerializedName("organizationalUnitId") val organizationalUnitId: String? = null,
    @SerializedName("organizationalUnitName") val organizationalUnitName: String? = null
)

data class InvitationUnitOptionDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String
)

data class InvitationRoleOptionDto(
    @SerializedName("role") val role: RoleDto,
    @SerializedName("organizationalUnits") val organizationalUnits: List<InvitationUnitOptionDto>,
    @SerializedName("canInviteWithoutOrganizationalUnit") val canInviteWithoutOrganizationalUnit: Boolean
)

data class InvitationOptionsDto(
    @SerializedName("roles") val roles: List<InvitationRoleOptionDto>
)
