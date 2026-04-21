package lt.skautai.android.data.remote

data class LocationDto(
    val id: String,
    val tuntasId: String,
    val name: String,
    val address: String?,
    val description: String?,
    val createdAt: String
)

data class LocationListResponseDto(
    val locations: List<LocationDto>,
    val total: Int
)

data class CreateLocationRequestDto(
    val name: String,
    val address: String? = null,
    val description: String? = null
)
