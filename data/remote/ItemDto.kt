package lt.skautai.android.data.remote

data class ItemDto(
    val id: String,
    val tuntasId: String,
    val custodianId: String?,
    val custodianName: String?,
    val origin: String,
    val name: String,
    val description: String?,
    val type: String,
    val category: String,
    val condition: String,
    val quantity: Int,
    val locationId: String?,
    val temporaryStorageLabel: String?,
    val sourceSharedItemId: String?,
    val quantityBreakdown: List<ItemDistributionDto> = emptyList(),
    val totalQuantityAcrossCustodians: Int = quantity,
    val responsibleUserId: String?,
    val photoUrl: String?,
    val purchaseDate: String?,
    val purchasePrice: Double?,
    val notes: String?,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)

data class ItemDistributionDto(
    val holderName: String,
    val quantity: Int
)

data class ItemListResponseDto(
    val items: List<ItemDto>,
    val total: Int
)

data class CreateItemRequestDto(
    val name: String,
    val description: String? = null,
    val type: String,
    val category: String,
    val custodianId: String? = null,
    val origin: String = "UNIT_ACQUIRED",
    val quantity: Int = 1,
    val condition: String = "GOOD",
    val locationId: String? = null,
    val temporaryStorageLabel: String? = null,
    val sourceSharedItemId: String? = null,
    val responsibleUserId: String? = null,
    val photoUrl: String? = null,
    val purchaseDate: String? = null,
    val purchasePrice: Double? = null,
    val notes: String? = null
)

data class UpdateItemRequestDto(
    val name: String? = null,
    val description: String? = null,
    val type: String? = null,
    val category: String? = null,
    val condition: String? = null,
    val quantity: Int? = null,
    val custodianId: String? = null,
    val locationId: String? = null,
    val temporaryStorageLabel: String? = null,
    val sourceSharedItemId: String? = null,
    val responsibleUserId: String? = null,
    val photoUrl: String? = null,
    val purchaseDate: String? = null,
    val purchasePrice: Double? = null,
    val notes: String? = null,
    val status: String? = null
)
