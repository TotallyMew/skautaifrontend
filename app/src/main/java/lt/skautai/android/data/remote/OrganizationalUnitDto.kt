package lt.skautai.android.data.remote

data class OrganizationalUnitDto(
    val id: String,
    val tuntasId: String,
    val parentId: String?,
    val name: String,
    val type: String,
    val acceptedRankId: String?,
    val acceptedRankName: String?,
    val createdAt: String
)

data class OrganizationalUnitListResponseDto(
    val units: List<OrganizationalUnitDto>,
    val total: Int
)