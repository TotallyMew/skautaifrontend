package lt.skautai.android.data.remote

import com.google.gson.annotations.SerializedName

data class EventRoleDto(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("userName") val userName: String?,
    @SerializedName("role") val role: String,
    @SerializedName("targetGroup") val targetGroup: String?,
    @SerializedName("assignedByUserId") val assignedByUserId: String?,
    @SerializedName("assignedAt") val assignedAt: String
)

data class StovyklaDetailsDto(
    @SerializedName("id") val id: String,
    @SerializedName("registrationDeadline") val registrationDeadline: String?,
    @SerializedName("expectedParticipants") val expectedParticipants: Int?,
    @SerializedName("actualParticipants") val actualParticipants: Int?
)

data class EventDto(
    @SerializedName("id") val id: String,
    @SerializedName("tuntasId") val tuntasId: String,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("locationId") val locationId: String?,
    @SerializedName("organizationalUnitId") val organizationalUnitId: String?,
    @SerializedName("createdByUserId") val createdByUserId: String?,
    @SerializedName("status") val status: String,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("eventRoles") val eventRoles: List<EventRoleDto>,
    @SerializedName("stovyklaDetails") val stovyklaDetails: StovyklaDetailsDto?,
    @SerializedName("inventorySummary") val inventorySummary: EventInventorySummaryDto?
)

data class EventListDto(
    @SerializedName("events") val events: List<EventDto>,
    @SerializedName("total") val total: Int
)

data class CreateEventRequestDto(
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("registrationDeadline") val registrationDeadline: String? = null,
    @SerializedName("expectedParticipants") val expectedParticipants: Int? = null
)

data class UpdateEventRequestDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class EventInventorySummaryDto(
    @SerializedName("totalPlannedQuantity") val totalPlannedQuantity: Int,
    @SerializedName("totalAvailableQuantity") val totalAvailableQuantity: Int,
    @SerializedName("totalShortageQuantity") val totalShortageQuantity: Int,
    @SerializedName("totalAllocatedQuantity") val totalAllocatedQuantity: Int,
    @SerializedName("itemsNeedingPurchase") val itemsNeedingPurchase: Int
)

data class EventInventoryBucketDto(
    @SerializedName("id") val id: String,
    @SerializedName("eventId") val eventId: String,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("pastovykleId") val pastovykleId: String?,
    @SerializedName("pastovykleName") val pastovykleName: String?,
    @SerializedName("notes") val notes: String?
)

data class EventInventoryItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("eventId") val eventId: String,
    @SerializedName("itemId") val itemId: String?,
    @SerializedName("bucketId") val bucketId: String?,
    @SerializedName("bucketName") val bucketName: String?,
    @SerializedName("reservationGroupId") val reservationGroupId: String?,
    @SerializedName("name") val name: String,
    @SerializedName("plannedQuantity") val plannedQuantity: Int,
    @SerializedName("availableQuantity") val availableQuantity: Int,
    @SerializedName("shortageQuantity") val shortageQuantity: Int,
    @SerializedName("allocatedQuantity") val allocatedQuantity: Int,
    @SerializedName("unallocatedQuantity") val unallocatedQuantity: Int,
    @SerializedName("needsPurchase") val needsPurchase: Boolean,
    @SerializedName("notes") val notes: String?,
    @SerializedName("responsibleUserId") val responsibleUserId: String?,
    @SerializedName("responsibleUserName") val responsibleUserName: String?,
    @SerializedName("createdByUserId") val createdByUserId: String?,
    @SerializedName("createdAt") val createdAt: String
)

data class EventInventoryAllocationDto(
    @SerializedName("id") val id: String,
    @SerializedName("eventInventoryItemId") val eventInventoryItemId: String,
    @SerializedName("bucketId") val bucketId: String,
    @SerializedName("bucketName") val bucketName: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("notes") val notes: String?
)

data class EventInventoryPlanDto(
    @SerializedName("buckets") val buckets: List<EventInventoryBucketDto>,
    @SerializedName("items") val items: List<EventInventoryItemDto>,
    @SerializedName("allocations") val allocations: List<EventInventoryAllocationDto>
)

data class CreateEventInventoryBucketRequestDto(
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("pastovykleId") val pastovykleId: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class CreateEventInventoryItemRequestDto(
    @SerializedName("itemId") val itemId: String? = null,
    @SerializedName("name") val name: String,
    @SerializedName("plannedQuantity") val plannedQuantity: Int,
    @SerializedName("bucketId") val bucketId: String? = null,
    @SerializedName("responsibleUserId") val responsibleUserId: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class CreateEventInventoryItemsBulkRequestDto(
    @SerializedName("items") val items: List<CreateEventInventoryItemRequestDto>
)

data class EventInventoryItemListDto(
    @SerializedName("items") val items: List<EventInventoryItemDto>,
    @SerializedName("total") val total: Int
)

data class UpdateEventInventoryItemRequestDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("plannedQuantity") val plannedQuantity: Int? = null,
    @SerializedName("bucketId") val bucketId: String? = null,
    @SerializedName("responsibleUserId") val responsibleUserId: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class AssignEventRoleRequestDto(
    @SerializedName("userId") val userId: String,
    @SerializedName("role") val role: String,
    @SerializedName("targetGroup") val targetGroup: String? = null
)

data class CreateEventInventoryAllocationRequestDto(
    @SerializedName("eventInventoryItemId") val eventInventoryItemId: String,
    @SerializedName("bucketId") val bucketId: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("notes") val notes: String? = null
)

data class EventPurchaseItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("purchaseId") val purchaseId: String,
    @SerializedName("eventInventoryItemId") val eventInventoryItemId: String,
    @SerializedName("itemName") val itemName: String,
    @SerializedName("purchasedQuantity") val purchasedQuantity: Int,
    @SerializedName("unitPrice") val unitPrice: Double?,
    @SerializedName("lineTotal") val lineTotal: Double?,
    @SerializedName("addedToInventory") val addedToInventory: Boolean,
    @SerializedName("addedToInventoryItemId") val addedToInventoryItemId: String?,
    @SerializedName("notes") val notes: String?
)

data class EventPurchaseDto(
    @SerializedName("id") val id: String,
    @SerializedName("eventId") val eventId: String,
    @SerializedName("purchasedByUserId") val purchasedByUserId: String?,
    @SerializedName("purchasedByName") val purchasedByName: String?,
    @SerializedName("status") val status: String,
    @SerializedName("purchaseDate") val purchaseDate: String?,
    @SerializedName("totalAmount") val totalAmount: Double?,
    @SerializedName("invoiceFileUrl") val invoiceFileUrl: String?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
    @SerializedName("items") val items: List<EventPurchaseItemDto>
)

data class EventPurchaseListDto(
    @SerializedName("purchases") val purchases: List<EventPurchaseDto>,
    @SerializedName("total") val total: Int
)

data class CreateEventPurchaseItemRequestDto(
    @SerializedName("eventInventoryItemId") val eventInventoryItemId: String,
    @SerializedName("purchasedQuantity") val purchasedQuantity: Int,
    @SerializedName("unitPrice") val unitPrice: Double? = null,
    @SerializedName("notes") val notes: String? = null
)

data class CreateEventPurchaseRequestDto(
    @SerializedName("purchaseDate") val purchaseDate: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("items") val items: List<CreateEventPurchaseItemRequestDto>
)

data class AttachEventPurchaseInvoiceRequestDto(
    @SerializedName("invoiceFileUrl") val invoiceFileUrl: String
)
