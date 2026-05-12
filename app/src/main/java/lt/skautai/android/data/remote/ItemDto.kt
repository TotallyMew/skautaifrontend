package lt.skautai.android.data.remote

data class ItemDto(
    val id: String,
    val qrToken: String,
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
    val locationName: String?,
    val locationPath: String?,
    val temporaryStorageLabel: String?,
    val sourceSharedItemId: String?,
    val quantityBreakdown: List<ItemDistributionDto> = emptyList(),
    val totalQuantityAcrossCustodians: Int = quantity,
    val responsibleUserId: String?,
    val responsibleUserName: String? = null,
    val createdByUserId: String?,
    val createdByUserName: String?,
    val photoUrl: String?,
    val purchaseDate: String?,
    val purchasePrice: Double?,
    val notes: String?,
    val customFields: List<ItemCustomFieldDto> = emptyList(),
    val status: String,
    val submittedByUserId: String? = null,
    val submittedByUserName: String? = null,
    val targetScope: String? = null,
    val reviewedByUserId: String? = null,
    val rejectionReason: String? = null,
    val createdAt: String,
    val updatedAt: String
)

data class ReviewItemAdditionRequestDto(
    val decision: String,
    val rejectionReason: String? = null
)

data class ItemCustomFieldDto(
    val id: String? = null,
    val fieldName: String,
    val fieldValue: String? = null
)

data class ItemDistributionDto(
    val holderName: String,
    val quantity: Int
)

data class ItemListResponseDto(
    val items: List<ItemDto>,
    val total: Int
)

data class ItemQrResolveResponseDto(
    val itemId: String
)

data class ItemAssignmentDto(
    val id: String,
    val itemId: String,
    val assignedToUserId: String,
    val assignedToUserName: String? = null,
    val assignedByUserId: String? = null,
    val assignedByUserName: String? = null,
    val assignedAt: String,
    val unassignedAt: String? = null,
    val reason: String? = null,
    val notes: String? = null
)

data class ItemAssignmentListResponseDto(
    val assignments: List<ItemAssignmentDto>,
    val total: Int
)

data class ItemConditionLogDto(
    val id: String,
    val itemId: String,
    val previousCondition: String? = null,
    val newCondition: String,
    val reportedByUserId: String? = null,
    val reportedByUserName: String? = null,
    val reportedAt: String,
    val notes: String? = null
)

data class ItemConditionLogListResponseDto(
    val entries: List<ItemConditionLogDto>,
    val total: Int
)

data class ItemTransferDto(
    val id: String,
    val itemId: String,
    val fromCustodianId: String? = null,
    val fromCustodianName: String? = null,
    val toCustodianId: String? = null,
    val toCustodianName: String? = null,
    val initiatedByUserId: String? = null,
    val initiatedByUserName: String? = null,
    val approvedByUserId: String? = null,
    val approvedByUserName: String? = null,
    val notes: String? = null,
    val status: String,
    val createdAt: String,
    val completedAt: String? = null
)

data class ItemTransferListResponseDto(
    val transfers: List<ItemTransferDto>,
    val total: Int
)

data class ItemHistoryDto(
    val id: String,
    val itemId: String,
    val eventType: String,
    val quantityChange: Int? = null,
    val performedByUserId: String? = null,
    val performedByUserName: String? = null,
    val requisitionId: String? = null,
    val notes: String? = null,
    val createdAt: String
)

data class ItemHistoryListResponseDto(
    val entries: List<ItemHistoryDto>,
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
    val notes: String? = null,
    val customFields: List<ItemCustomFieldDto> = emptyList(),
    val duplicateHandling: String = "ASK",
    val duplicateTargetItemId: String? = null
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
    val customFields: List<ItemCustomFieldDto>? = null,
    val status: String? = null,
    val clearCustodianId: Boolean = false,
    val clearLocationId: Boolean = false,
    val clearSourceSharedItemId: Boolean = false,
    val clearResponsibleUserId: Boolean = false
)

data class TransferItemToUnitRequestDto(
    val targetUnitId: String,
    val quantity: Int,
    val notes: String? = null
)

data class ReturnItemToSharedRequestDto(
    val quantity: Int,
    val notes: String? = null
)

data class RestockItemRequestDto(
    val quantity: Int,
    val purchaseDate: String? = null,
    val purchasePrice: Double? = null,
    val notes: String? = null
)
