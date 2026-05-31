package lt.skautai.android.data.remote

data class InventoryKitItemDto(
    val id: String,
    val itemId: String,
    val itemName: String,
    val itemCondition: String,
    val itemStatus: String,
    val availableQuantity: Int,
    val quantity: Int,
    val locationId: String? = null,
    val locationName: String? = null,
    val locationPath: String? = null,
    val notes: String? = null
)

data class InventoryKitDto(
    val id: String,
    val tuntasId: String,
    val custodianId: String? = null,
    val custodianName: String? = null,
    val name: String,
    val description: String? = null,
    val locationId: String? = null,
    val locationName: String? = null,
    val locationPath: String? = null,
    val temporaryStorageLabel: String? = null,
    val responsibleUserId: String? = null,
    val responsibleUserName: String? = null,
    val createdByUserId: String? = null,
    val createdByUserName: String? = null,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val items: List<InventoryKitItemDto> = emptyList()
)

data class InventoryKitListDto(
    val kits: List<InventoryKitDto>,
    val total: Int
)

data class InventoryKitItemRequestDto(
    val itemId: String,
    val quantity: Int = 1,
    val notes: String? = null
)

data class CreateInventoryKitRequestDto(
    val name: String,
    val description: String? = null,
    val custodianId: String? = null,
    val locationId: String? = null,
    val temporaryStorageLabel: String? = null,
    val responsibleUserId: String? = null,
    val items: List<InventoryKitItemRequestDto> = emptyList()
)

data class UpdateInventoryKitRequestDto(
    val name: String? = null,
    val description: String? = null,
    val custodianId: String? = null,
    val locationId: String? = null,
    val temporaryStorageLabel: String? = null,
    val responsibleUserId: String? = null,
    val clearLocationId: Boolean = false,
    val clearResponsibleUserId: Boolean = false,
    val status: String? = null,
    val items: List<InventoryKitItemRequestDto>? = null
)
