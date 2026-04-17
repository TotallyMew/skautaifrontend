package lt.skautai.android.data.remote

import com.google.gson.annotations.SerializedName

data class RoleDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("roleType") val roleType: String,
    @SerializedName("isSystemRole") val isSystemRole: Boolean
)

data class RoleListDto(
    @SerializedName("roles") val roles: List<RoleDto>,
    @SerializedName("total") val total: Int
)