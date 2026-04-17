package lt.skautai.android.data.remote

import com.google.gson.annotations.SerializedName

data class CreateInvitationRequestDto(
    @SerializedName("roleId") val roleId: String,
    @SerializedName("organizationalUnitId") val organizationalUnitId: String?,
    @SerializedName("expiresInHours") val expiresInHours: Int = 72
)

data class InvitationResponseDto(
    @SerializedName("code") val code: String,
    @SerializedName("roleName") val roleName: String,
    @SerializedName("tuntasName") val tuntasName: String,
    @SerializedName("expiresAt") val expiresAt: String
)