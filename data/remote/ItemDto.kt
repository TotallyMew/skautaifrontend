package lt.skautai.android.data.remote

data class ItemDto(
    val id: String,
    val tuntasId: String,
    val ownerType: String,
    val ownerId: String,
    val name: String,
    val description: String?,
    val category: String,
    val condition: String,
    val quantity: Int,
    val locationId: String?,
    val responsibleUserId: String?,
    val photoUrl: String?,
    val purchaseDate: String?,
    val purchasePrice: Double?,
    val notes: String?,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)

data class ItemListResponseDto(
    val items: List<ItemDto>,
    val total: Int
)
data class CreateItemRequestDto(
    val name: String,
    val description: String? = null,
    val category: String,
    val ownerType: String,
    val ownerId: String,
    val quantity: Int = 1,
    val locationId: String? = null,
    val responsibleUserId: String? = null,
    val photoUrl: String? = null,
    val purchaseDate: String? = null,
    val purchasePrice: Double? = null,
    val notes: String? = null
)

data class UpdateItemRequestDto(
    val name: String? = null,
    val description: String? = null,
    val category: String? = null,
    val condition: String? = null,
    val quantity: Int? = null,
    val locationId: String? = null,
    val responsibleUserId: String? = null,
    val photoUrl: String? = null,
    val purchaseDate: String? = null,
    val purchasePrice: Double? = null,
    val notes: String? = null,
    val status: String? = null
)