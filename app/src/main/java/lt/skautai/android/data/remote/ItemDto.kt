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