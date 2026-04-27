package lt.skautai.android.data.remote

enum class LocationVisibility(val apiValue: String, val displayName: String) {
    PUBLIC("PUBLIC", "Vieša"),
    UNIT("UNIT", "Padalinio"),
    PRIVATE("PRIVATE", "Privati");

    companion object {
        fun fromApiValue(value: String): LocationVisibility =
            entries.firstOrNull { it.apiValue == value } ?: PUBLIC
    }
}

data class LocationDto(
    val id: String,
    val tuntasId: String,
    val name: String,
    val visibility: String,
    val parentLocationId: String?,
    val ownerUserId: String?,
    val ownerUnitId: String?,
    val ownerUnitName: String?,
    val fullPath: String,
    val hasChildren: Boolean,
    val isLeafSelectable: Boolean,
    val isEditable: Boolean,
    val address: String?,
    val description: String?,
    val latitude: Double?,
    val longitude: Double?,
    val createdAt: String
)

data class LocationListResponseDto(
    val locations: List<LocationDto>,
    val total: Int
)

data class CreateLocationRequestDto(
    val name: String,
    val visibility: String = "PUBLIC",
    val parentLocationId: String? = null,
    val ownerUnitId: String? = null,
    val address: String? = null,
    val description: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class UpdateLocationRequestDto(
    val name: String? = null,
    val visibility: String? = null,
    val parentLocationId: String? = null,
    val ownerUnitId: String? = null,
    val address: String? = null,
    val description: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
