package lt.skautai.android.data.remote

import com.google.gson.annotations.SerializedName

data class EventRoleDto(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("userName") val userName: String?,
    @SerializedName("role") val role: String,
    @SerializedName("targetGroup") val targetGroup: String?,
    @SerializedName("pastovykleId") val pastovykleId: String?,
    @SerializedName("assignedByUserId") val assignedByUserId: String?,
    @SerializedName("assignedAt") val assignedAt: String
)

data class EventDto(
    @SerializedName("id") val id: String,
    @SerializedName("tuntasId") val tuntasId: String,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("customTypeLabel") val customTypeLabel: String? = null,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("locationId") val locationId: String?,
    @SerializedName("organizationalUnitId") val organizationalUnitId: String?,
    @SerializedName("createdByUserId") val createdByUserId: String?,
    @SerializedName("status") val status: String,
    @SerializedName("inventoryBudgetAmount") val inventoryBudgetAmount: Double? = null,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("eventRoles") val eventRoles: List<EventRoleDto>,
    @SerializedName("inventorySummary") val inventorySummary: EventInventorySummaryDto?,
    @SerializedName("financeSummary") val financeSummary: EventFinanceSummaryDto? = null
)

data class EventListDto(
    @SerializedName("events") val events: List<EventDto>,
    @SerializedName("total") val total: Int,
    @SerializedName("limit") val limit: Int? = null,
    @SerializedName("offset") val offset: Int = 0,
    @SerializedName("hasMore") val hasMore: Boolean = false
)

data class CreateEventRequestDto(
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("customTypeLabel") val customTypeLabel: String? = null,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("organizationalUnitId") val organizationalUnitId: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class UpdateEventRequestDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("customTypeLabel") val customTypeLabel: String? = null,
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

data class EventFinanceSummaryDto(
    @SerializedName("inventoryBudgetAmount") val inventoryBudgetAmount: Double? = null,
    @SerializedName("purchaseTotal") val purchaseTotal: Double,
    @SerializedName("extraCostTotal") val extraCostTotal: Double,
    @SerializedName("spentTotal") val spentTotal: Double,
    @SerializedName("remainingAmount") val remainingAmount: Double? = null,
    @SerializedName("overBudget") val overBudget: Boolean
)

data class EventExtraCostDto(
    @SerializedName("id") val id: String,
    @SerializedName("eventId") val eventId: String,
    @SerializedName("category") val category: String,
    @SerializedName("label") val label: String,
    @SerializedName("quantity") val quantity: Double? = null,
    @SerializedName("unit") val unit: String? = null,
    @SerializedName("unitPrice") val unitPrice: Double? = null,
    @SerializedName("totalAmount") val totalAmount: Double,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("createdByUserId") val createdByUserId: String? = null,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class EventFinanceDto(
    @SerializedName("eventId") val eventId: String,
    @SerializedName("summary") val summary: EventFinanceSummaryDto,
    @SerializedName("extraCosts") val extraCosts: List<EventExtraCostDto>
)

data class UpdateEventFinanceBudgetRequestDto(
    @SerializedName("inventoryBudgetAmount") val inventoryBudgetAmount: Double? = null
)

data class CreateEventExtraCostRequestDto(
    @SerializedName("category") val category: String,
    @SerializedName("label") val label: String,
    @SerializedName("quantity") val quantity: Double? = null,
    @SerializedName("unit") val unit: String? = null,
    @SerializedName("unitPrice") val unitPrice: Double? = null,
    @SerializedName("totalAmount") val totalAmount: Double? = null,
    @SerializedName("notes") val notes: String? = null
)

data class UpdateEventExtraCostRequestDto(
    @SerializedName("category") val category: String? = null,
    @SerializedName("label") val label: String? = null,
    @SerializedName("quantity") val quantity: Double? = null,
    @SerializedName("unit") val unit: String? = null,
    @SerializedName("unitPrice") val unitPrice: Double? = null,
    @SerializedName("totalAmount") val totalAmount: Double? = null,
    @SerializedName("notes") val notes: String? = null
)

data class EventInventoryBucketDto(
    @SerializedName("id") val id: String,
    @SerializedName("eventId") val eventId: String,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("pastovykleId") val pastovykleId: String?,
    @SerializedName("pastovykleName") val pastovykleName: String?,
    @SerializedName("locationId") val locationId: String?,
    @SerializedName("locationPath") val locationPath: String?,
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
    @SerializedName("sourceCustodianName") val sourceCustodianName: String? = null,
    @SerializedName("sourceLocationPath") val sourceLocationPath: String? = null,
    @SerializedName("sourceTemporaryStorageLabel") val sourceTemporaryStorageLabel: String? = null,
    @SerializedName("sourceResponsibleUserName") val sourceResponsibleUserName: String? = null,
    @SerializedName("sourcePickupSummary") val sourcePickupSummary: String? = null,
    @SerializedName("sources") val sources: List<EventInventorySourceDto>? = emptyList(),
    @SerializedName("responsibleUserId") val responsibleUserId: String?,
    @SerializedName("responsibleUserName") val responsibleUserName: String?,
    @SerializedName("createdByUserId") val createdByUserId: String?,
    @SerializedName("createdAt") val createdAt: String
)

data class EventInventorySourceDto(
    @SerializedName("id") val id: String,
    @SerializedName("eventInventoryItemId") val eventInventoryItemId: String,
    @SerializedName("itemId") val itemId: String? = null,
    @SerializedName("reservationGroupId") val reservationGroupId: String? = null,
    @SerializedName("plannedQuantity") val plannedQuantity: Int,
    @SerializedName("reservedQuantity") val reservedQuantity: Int,
    @SerializedName("pickupCustodianName") val pickupCustodianName: String? = null,
    @SerializedName("pickupLocationPath") val pickupLocationPath: String? = null,
    @SerializedName("pickupTemporaryStorageLabel") val pickupTemporaryStorageLabel: String? = null,
    @SerializedName("pickupResponsibleUserName") val pickupResponsibleUserName: String? = null,
    @SerializedName("pickupSummary") val pickupSummary: String? = null,
    @SerializedName("sourceStatus") val sourceStatus: String,
    @SerializedName("notes") val notes: String? = null,
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
    @SerializedName("locationId") val locationId: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class UpdateEventInventoryBucketRequestDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("pastovykleId") val pastovykleId: String? = null,
    @SerializedName("locationId") val locationId: String? = null,
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

data class InventoryTemplateItemRequestDto(
    @SerializedName("itemId") val itemId: String? = null,
    @SerializedName("itemName") val itemName: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("category") val category: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class CreateInventoryTemplateRequestDto(
    @SerializedName("name") val name: String,
    @SerializedName("eventType") val eventType: String? = null,
    @SerializedName("items") val items: List<InventoryTemplateItemRequestDto>
)

data class UpdateInventoryTemplateRequestDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("eventType") val eventType: String? = null,
    @SerializedName("items") val items: List<InventoryTemplateItemRequestDto>? = null
)

data class ApplyInventoryTemplateRequestDto(
    @SerializedName("templateId") val templateId: String
)

data class InventoryTemplateItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("templateId") val templateId: String,
    @SerializedName("itemId") val itemId: String? = null,
    @SerializedName("itemName") val itemName: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("category") val category: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class InventoryTemplateDto(
    @SerializedName("id") val id: String,
    @SerializedName("tuntasId") val tuntasId: String,
    @SerializedName("name") val name: String,
    @SerializedName("eventType") val eventType: String? = null,
    @SerializedName("createdByUserId") val createdByUserId: String? = null,
    @SerializedName("createdByUserName") val createdByUserName: String? = null,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("items") val items: List<InventoryTemplateItemDto> = emptyList()
)

data class InventoryTemplateListDto(
    @SerializedName("templates") val templates: List<InventoryTemplateDto>,
    @SerializedName("total") val total: Int
)

data class AppliedTemplateReservedItemDto(
    @SerializedName("templateItemName") val templateItemName: String,
    @SerializedName("itemId") val itemId: String,
    @SerializedName("itemName") val itemName: String,
    @SerializedName("eventInventoryItemId") val eventInventoryItemId: String,
    @SerializedName("reservationGroupId") val reservationGroupId: String,
    @SerializedName("quantity") val quantity: Int
)

data class AppliedTemplatePurchaseItemDto(
    @SerializedName("templateItemName") val templateItemName: String,
    @SerializedName("eventInventoryItemId") val eventInventoryItemId: String,
    @SerializedName("purchaseId") val purchaseId: String,
    @SerializedName("purchaseItemId") val purchaseItemId: String,
    @SerializedName("quantity") val quantity: Int
)

data class AppliedInventoryTemplateDto(
    @SerializedName("reserved") val reserved: List<AppliedTemplateReservedItemDto> = emptyList(),
    @SerializedName("toPurchase") val toPurchase: List<AppliedTemplatePurchaseItemDto> = emptyList(),
    @SerializedName("sources") val sources: List<AppliedTemplateSourceDto> = emptyList(),
    @SerializedName("shortages") val shortages: List<AppliedTemplateShortageDto> = emptyList(),
    @SerializedName("reservedTotal") val reservedTotal: Int,
    @SerializedName("toPurchaseTotal") val toPurchaseTotal: Int
)

data class AppliedTemplateSourceDto(
    @SerializedName("templateItemName") val templateItemName: String,
    @SerializedName("eventInventoryItemId") val eventInventoryItemId: String,
    @SerializedName("sourceId") val sourceId: String,
    @SerializedName("itemId") val itemId: String? = null,
    @SerializedName("itemName") val itemName: String? = null,
    @SerializedName("reservedQuantity") val reservedQuantity: Int,
    @SerializedName("plannedQuantity") val plannedQuantity: Int,
    @SerializedName("pickupSummary") val pickupSummary: String? = null,
    @SerializedName("sourceStatus") val sourceStatus: String
)

data class AppliedTemplateShortageDto(
    @SerializedName("templateItemName") val templateItemName: String,
    @SerializedName("eventInventoryItemId") val eventInventoryItemId: String,
    @SerializedName("shortageQuantity") val shortageQuantity: Int
)

data class UpdateEventInventoryItemRequestDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("plannedQuantity") val plannedQuantity: Int? = null,
    @SerializedName("bucketId") val bucketId: String? = null,
    @SerializedName("responsibleUserId") val responsibleUserId: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class CreateEventInventorySourceRequestDto(
    @SerializedName("itemId") val itemId: String? = null,
    @SerializedName("plannedQuantity") val plannedQuantity: Int,
    @SerializedName("notes") val notes: String? = null
)

data class UpdateEventInventorySourceRequestDto(
    @SerializedName("plannedQuantity") val plannedQuantity: Int? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("sourceStatus") val sourceStatus: String? = null
)

data class AssignEventRoleRequestDto(
    @SerializedName("userId") val userId: String,
    @SerializedName("role") val role: String,
    @SerializedName("targetGroup") val targetGroup: String? = null,
    @SerializedName("pastovykleId") val pastovykleId: String? = null
)

data class AssignPastovykleLeaderRequestDto(
    @SerializedName("userId") val userId: String
)

data class CreateEventInventoryAllocationRequestDto(
    @SerializedName("eventInventoryItemId") val eventInventoryItemId: String,
    @SerializedName("bucketId") val bucketId: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("notes") val notes: String? = null
)

data class UpdateEventInventoryAllocationRequestDto(
    @SerializedName("quantity") val quantity: Int? = null,
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

data class EventPurchaseInvoiceDto(
    @SerializedName("id") val id: String,
    @SerializedName("purchaseId") val purchaseId: String,
    @SerializedName("fileUrl") val fileUrl: String,
    @SerializedName("createdAt") val createdAt: String
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
    @SerializedName("invoices") val invoices: List<EventPurchaseInvoiceDto> = emptyList(),
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
    @SerializedName("items") val items: List<EventPurchaseItemDto>
)

data class EventPurchaseListDto(
    @SerializedName("purchases") val purchases: List<EventPurchaseDto>,
    @SerializedName("total") val total: Int,
    @SerializedName("limit") val limit: Int? = null,
    @SerializedName("offset") val offset: Int = 0,
    @SerializedName("hasMore") val hasMore: Boolean = false
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

data class UpdateEventPurchaseRequestDto(
    @SerializedName("purchaseDate") val purchaseDate: String? = null,
    @SerializedName("totalAmount") val totalAmount: Double? = null,
    @SerializedName("notes") val notes: String? = null
)

data class AttachEventPurchaseInvoiceRequestDto(
    @SerializedName("invoiceFileUrl") val invoiceFileUrl: String
)

data class EventReconciliationReturnLineDto(
    @SerializedName("custodyId") val custodyId: String,
    @SerializedName("eventInventoryItemId") val eventInventoryItemId: String,
    @SerializedName("itemId") val itemId: String?,
    @SerializedName("itemName") val itemName: String,
    @SerializedName("pastovykleId") val pastovykleId: String?,
    @SerializedName("pastovykleName") val pastovykleName: String?,
    @SerializedName("holderUserId") val holderUserId: String?,
    @SerializedName("holderUserName") val holderUserName: String?,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("returnedQuantity") val returnedQuantity: Int,
    @SerializedName("remainingQuantity") val remainingQuantity: Int,
    @SerializedName("reconciledQuantity") val reconciledQuantity: Int = returnedQuantity,
    @SerializedName("pendingQuantity") val pendingQuantity: Int = remainingQuantity,
    @SerializedName("status") val status: String,
    @SerializedName("isReturned") val isReturned: Boolean = false,
    @SerializedName("currentHolderSummary") val currentHolderSummary: String? = null,
    @SerializedName("sourcePickupSummary") val sourcePickupSummary: String? = null,
    @SerializedName("returnDecision") val returnDecision: String? = null,
    @SerializedName("returnedToSummary") val returnedToSummary: String? = null,
    @SerializedName("returnCondition") val returnCondition: String? = null,
    @SerializedName("auditLog") val auditLog: List<EventReconciliationAuditDto> = emptyList(),
    @SerializedName("notes") val notes: String?
)

data class EventReconciliationAuditDto(
    @SerializedName("id") val id: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("expectedQuantity") val expectedQuantity: Int,
    @SerializedName("actualQuantity") val actualQuantity: Int,
    @SerializedName("result") val result: String,
    @SerializedName("actualLocationId") val actualLocationId: String? = null,
    @SerializedName("actualLocationNote") val actualLocationNote: String? = null,
    @SerializedName("conditionAtCheck") val conditionAtCheck: String? = null,
    @SerializedName("checkedByUserId") val checkedByUserId: String,
    @SerializedName("checkedAt") val checkedAt: String,
    @SerializedName("notes") val notes: String? = null
)

data class EventReconciliationPurchaseLineDto(
    @SerializedName("purchaseId") val purchaseId: String,
    @SerializedName("purchaseItemId") val purchaseItemId: String,
    @SerializedName("eventInventoryItemId") val eventInventoryItemId: String,
    @SerializedName("itemId") val itemId: String?,
    @SerializedName("itemName") val itemName: String,
    @SerializedName("purchasedQuantity") val purchasedQuantity: Int,
    @SerializedName("status") val status: String,
    @SerializedName("invoiceFileUrl") val invoiceFileUrl: String?,
    @SerializedName("notes") val notes: String?
)

data class EventPurchaseReconciliationCandidateDto(
    @SerializedName("itemId") val itemId: String,
    @SerializedName("name") val name: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("custodianId") val custodianId: String?,
    @SerializedName("custodianName") val custodianName: String?,
    @SerializedName("recommended") val recommended: Boolean
)

data class EventPurchaseReconciliationCandidateListDto(
    @SerializedName("candidates") val candidates: List<EventPurchaseReconciliationCandidateDto>,
    @SerializedName("total") val total: Int
)

data class EventReconciliationDto(
    @SerializedName("eventId") val eventId: String,
    @SerializedName("status") val status: String,
    @SerializedName("openReturns") val openReturns: List<EventReconciliationReturnLineDto>,
    @SerializedName("returnedToEventStorage") val returnedToEventStorage: List<EventReconciliationReturnLineDto>,
    @SerializedName("unresolvedPurchases") val unresolvedPurchases: List<EventReconciliationPurchaseLineDto>,
    @SerializedName("canComplete") val canComplete: Boolean
)

data class ReconcileEventReturnLineRequestDto(
    @SerializedName("custodyId") val custodyId: String,
    @SerializedName("decision") val decision: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("returnToMode") val returnToMode: String? = null,
    @SerializedName("returnLocationId") val returnLocationId: String? = null,
    @SerializedName("returnLocationNote") val returnLocationNote: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class ReconcileEventReturnsRequestDto(
    @SerializedName("returns") val returns: List<ReconcileEventReturnLineRequestDto>
)

data class ReconcileEventPurchaseLineRequestDto(
    @SerializedName("purchaseItemId") val purchaseItemId: String,
    @SerializedName("decision") val decision: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("existingItemId") val existingItemId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class ReconcileEventPurchasesRequestDto(
    @SerializedName("purchases") val purchases: List<ReconcileEventPurchaseLineRequestDto>
)

data class PastovykleDto(
    @SerializedName("id") val id: String,
    @SerializedName("eventId") val eventId: String,
    @SerializedName("name") val name: String,
    @SerializedName("responsibleUserId") val responsibleUserId: String?,
    @SerializedName("ageGroup") val ageGroup: String?,
    @SerializedName("notes") val notes: String?
)

data class PastovykleListDto(
    @SerializedName("pastovykles") val pastovykles: List<PastovykleDto>,
    @SerializedName("total") val total: Int
)

data class CreatePastovykleRequestDto(
    @SerializedName("name") val name: String,
    @SerializedName("responsibleUserId") val responsibleUserId: String? = null,
    @SerializedName("ageGroup") val ageGroup: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class UpdatePastovykleRequestDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("responsibleUserId") val responsibleUserId: String? = null,
    @SerializedName("clearResponsibleUser") val clearResponsibleUser: Boolean = false,
    @SerializedName("ageGroup") val ageGroup: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class PastovykleMemberDto(
    @SerializedName("id") val id: String,
    @SerializedName("pastovykleId") val pastovykleId: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("userName") val userName: String,
    @SerializedName("status") val status: String,
    @SerializedName("addedAt") val addedAt: String,
    @SerializedName("addedByUserId") val addedByUserId: String
)

data class PastovykleMemberListDto(
    @SerializedName("members") val members: List<PastovykleMemberDto>,
    @SerializedName("total") val total: Int
)

data class AddPastovykleMemberRequestDto(
    @SerializedName("userId") val userId: String
)

data class PastovykleInventoryDto(
    @SerializedName("id") val id: String,
    @SerializedName("pastovykleId") val pastovykleId: String,
    @SerializedName("itemId") val itemId: String,
    @SerializedName("itemName") val itemName: String,
    @SerializedName("distributedByUserId") val distributedByUserId: String?,
    @SerializedName("recipientUserId") val recipientUserId: String?,
    @SerializedName("recipientType") val recipientType: String?,
    @SerializedName("quantityAssigned") val quantityAssigned: Int,
    @SerializedName("quantityReturned") val quantityReturned: Int,
    @SerializedName("assignedAt") val assignedAt: String,
    @SerializedName("returnedAt") val returnedAt: String?,
    @SerializedName("notes") val notes: String?
)

data class PastovykleInventoryListDto(
    @SerializedName("inventory") val inventory: List<PastovykleInventoryDto>,
    @SerializedName("total") val total: Int
)

data class AssignPastovykleInventoryRequestDto(
    @SerializedName("itemId") val itemId: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("recipientUserId") val recipientUserId: String? = null,
    @SerializedName("recipientType") val recipientType: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class UpdatePastovykleInventoryRequestDto(
    @SerializedName("quantityReturned") val quantityReturned: Int? = null,
    @SerializedName("returnedAt") val returnedAt: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class EventInventoryRequestDto(
    @SerializedName("id") val id: String,
    @SerializedName("eventId") val eventId: String,
    @SerializedName("eventInventoryItemId") val eventInventoryItemId: String,
    @SerializedName("itemId") val itemId: String?,
    @SerializedName("itemName") val itemName: String,
    @SerializedName("pastovykleId") val pastovykleId: String,
    @SerializedName("pastovykleName") val pastovykleName: String,
    @SerializedName("targetGroup") val targetGroup: String? = null,
    @SerializedName("requestedByUserId") val requestedByUserId: String,
    @SerializedName("requestedByName") val requestedByName: String?,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("status") val status: String,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("reviewedAt") val reviewedAt: String?,
    @SerializedName("reviewedByUserId") val reviewedByUserId: String?,
    @SerializedName("reviewedByUserName") val reviewedByUserName: String?,
    @SerializedName("fulfilledAt") val fulfilledAt: String?,
    @SerializedName("resolvedByUserId") val resolvedByUserId: String?,
    @SerializedName("resolvedByUserName") val resolvedByUserName: String?,
    @SerializedName("provider") val provider: String = "UKVEDYS",
    @SerializedName("dueAt") val dueAt: String? = null,
    @SerializedName("responsibleUserId") val responsibleUserId: String? = null,
    @SerializedName("responsibleUserName") val responsibleUserName: String? = null,
    @SerializedName("providerHistory") val providerHistory: List<EventInventoryRequestProviderHistoryDto> = emptyList()
)

data class EventInventoryRequestProviderHistoryDto(
    @SerializedName("id") val id: String,
    @SerializedName("fromProvider") val fromProvider: String?,
    @SerializedName("toProvider") val toProvider: String,
    @SerializedName("changedByUserId") val changedByUserId: String,
    @SerializedName("changedByUserName") val changedByUserName: String?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String
)

data class EventInventoryRequestListDto(
    @SerializedName("requests") val requests: List<EventInventoryRequestDto>,
    @SerializedName("total") val total: Int
)

data class CreatePastovykleInventoryRequestRequestDto(
    @SerializedName("eventInventoryItemId") val eventInventoryItemId: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("provider") val provider: String = "UKVEDYS",
    @SerializedName("dueAt") val dueAt: String? = null,
    @SerializedName("responsibleUserId") val responsibleUserId: String? = null
)

data class UpdateEventInventoryRequestRequestDto(
    @SerializedName("provider") val provider: String? = null,
    @SerializedName("dueAt") val dueAt: String? = null,
    @SerializedName("clearDueAt") val clearDueAt: Boolean = false,
    @SerializedName("responsibleUserId") val responsibleUserId: String? = null,
    @SerializedName("clearResponsibleUserId") val clearResponsibleUserId: Boolean = false,
    @SerializedName("notes") val notes: String? = null
)

data class EventInventoryConflictDto(
    @SerializedName("itemId") val itemId: String,
    @SerializedName("itemName") val itemName: String,
    @SerializedName("availableQuantity") val availableQuantity: Int,
    @SerializedName("requestedQuantity") val requestedQuantity: Int,
    @SerializedName("overlappingEvents") val overlappingEvents: List<String>
)

data class EventInventoryReadinessDto(
    @SerializedName("readinessPercent") val readinessPercent: Int,
    @SerializedName("totalQuantity") val totalQuantity: Int,
    @SerializedName("completedQuantity") val completedQuantity: Int,
    @SerializedName("openQuantity") val openQuantity: Int,
    @SerializedName("overdueCount") val overdueCount: Int,
    @SerializedName("unassignedCount") val unassignedCount: Int,
    @SerializedName("conflicts") val conflicts: List<EventInventoryConflictDto>
)

data class CreateEventInventoryRequestRequestDto(
    @SerializedName("eventInventoryItemId") val eventInventoryItemId: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("notes") val notes: String? = null
)

data class FulfillPastovykleInventoryRequestRequestDto(
    @SerializedName("quantity") val quantity: Int? = null,
    @SerializedName("notes") val notes: String? = null
)

data class MarkPastovykleInventoryRequestSelfProvidedRequestDto(
    @SerializedName("notes") val notes: String? = null
)

data class AssignUnitInventoryToPastovykleRequestDto(
    @SerializedName("itemId") val itemId: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("notes") val notes: String? = null
)

data class EventInventoryCustodyDto(
    @SerializedName("id") val id: String,
    @SerializedName("eventInventoryItemId") val eventInventoryItemId: String,
    @SerializedName("itemName") val itemName: String,
    @SerializedName("pastovykleId") val pastovykleId: String?,
    @SerializedName("pastovykleName") val pastovykleName: String?,
    @SerializedName("holderUserId") val holderUserId: String?,
    @SerializedName("holderUserName") val holderUserName: String?,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("returnedQuantity") val returnedQuantity: Int,
    @SerializedName("remainingQuantity") val remainingQuantity: Int,
    @SerializedName("status") val status: String,
    @SerializedName("createdByUserId") val createdByUserId: String,
    @SerializedName("createdByUserName") val createdByUserName: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("closedAt") val closedAt: String?,
    @SerializedName("notes") val notes: String?
)

data class EventInventoryMovementDto(
    @SerializedName("id") val id: String,
    @SerializedName("eventId") val eventId: String,
    @SerializedName("eventInventoryItemId") val eventInventoryItemId: String,
    @SerializedName("itemName") val itemName: String,
    @SerializedName("custodyId") val custodyId: String?,
    @SerializedName("movementType") val movementType: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("fromPastovykleId") val fromPastovykleId: String?,
    @SerializedName("fromPastovykleName") val fromPastovykleName: String?,
    @SerializedName("toPastovykleId") val toPastovykleId: String?,
    @SerializedName("toPastovykleName") val toPastovykleName: String?,
    @SerializedName("fromUserId") val fromUserId: String?,
    @SerializedName("fromUserName") val fromUserName: String?,
    @SerializedName("toUserId") val toUserId: String?,
    @SerializedName("toUserName") val toUserName: String?,
    @SerializedName("performedByUserId") val performedByUserId: String,
    @SerializedName("performedByUserName") val performedByUserName: String?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String
)

data class EventInventoryCustodyListDto(
    @SerializedName("custody") val custody: List<EventInventoryCustodyDto>,
    @SerializedName("total") val total: Int
)

data class EventInventoryMovementListDto(
    @SerializedName("movements") val movements: List<EventInventoryMovementDto>,
    @SerializedName("total") val total: Int
)

data class EventInventoryTransferRequestDto(
    @SerializedName("id") val id: String,
    @SerializedName("eventId") val eventId: String,
    @SerializedName("sourceCustodyId") val sourceCustodyId: String,
    @SerializedName("eventInventoryItemId") val eventInventoryItemId: String,
    @SerializedName("itemName") val itemName: String,
    @SerializedName("requestedByUserId") val requestedByUserId: String,
    @SerializedName("requestedByUserName") val requestedByUserName: String?,
    @SerializedName("requestedFromUserId") val requestedFromUserId: String,
    @SerializedName("requestedFromUserName") val requestedFromUserName: String?,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("status") val status: String,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("respondedAt") val respondedAt: String?,
    @SerializedName("respondedByUserId") val respondedByUserId: String?,
    @SerializedName("movementId") val movementId: String?
)

data class EventInventoryTransferRequestListDto(
    @SerializedName("requests") val requests: List<EventInventoryTransferRequestDto>,
    @SerializedName("total") val total: Int
)

data class CreateEventInventoryTransferRequestDto(
    @SerializedName("sourceCustodyId") val sourceCustodyId: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("notes") val notes: String? = null
)

data class RespondEventInventoryTransferRequestDto(
    @SerializedName("approve") val approve: Boolean,
    @SerializedName("notes") val notes: String? = null
)

data class CreateEventInventoryMovementRequestDto(
    @SerializedName("eventInventoryItemId") val eventInventoryItemId: String,
    @SerializedName("movementType") val movementType: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("pastovykleId") val pastovykleId: String? = null,
    @SerializedName("toUserId") val toUserId: String? = null,
    @SerializedName("fromCustodyId") val fromCustodyId: String? = null,
    @SerializedName("requestId") val requestId: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class EventPackingContainerDto(
    @SerializedName("id") val id: String,
    @SerializedName("eventId") val eventId: String,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("status") val status: String,
    @SerializedName("sortOrder") val sortOrder: Int,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class EventPackingLineDto(
    @SerializedName("id") val id: String,
    @SerializedName("eventId") val eventId: String,
    @SerializedName("eventInventoryItemId") val eventInventoryItemId: String,
    @SerializedName("allocationId") val allocationId: String?,
    @SerializedName("containerId") val containerId: String?,
    @SerializedName("containerName") val containerName: String?,
    @SerializedName("bucketId") val bucketId: String?,
    @SerializedName("bucketName") val bucketName: String?,
    @SerializedName("itemId") val itemId: String?,
    @SerializedName("itemName") val itemName: String,
    @SerializedName("requiredQuantity") val requiredQuantity: Int,
    @SerializedName("status") val status: String,
    @SerializedName("sourceSummary") val sourceSummary: String?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("checkedByUserId") val checkedByUserId: String?,
    @SerializedName("checkedByUserName") val checkedByUserName: String?,
    @SerializedName("checkedAt") val checkedAt: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class EventPackingSummaryDto(
    @SerializedName("totalLines") val totalLines: Int,
    @SerializedName("doneLines") val doneLines: Int,
    @SerializedName("totalQuantity") val totalQuantity: Int,
    @SerializedName("doneQuantity") val doneQuantity: Int,
    @SerializedName("progressPercent") val progressPercent: Int
)

data class EventPackingListDto(
    @SerializedName("eventId") val eventId: String,
    @SerializedName("containers") val containers: List<EventPackingContainerDto>,
    @SerializedName("lines") val lines: List<EventPackingLineDto>,
    @SerializedName("summary") val summary: EventPackingSummaryDto
)

data class CreateEventPackingContainerRequestDto(
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String = "BOX",
    @SerializedName("notes") val notes: String? = null
)

data class UpdateEventPackingLineRequestDto(
    @SerializedName("status") val status: String? = null,
    @SerializedName("containerId") val containerId: String? = null,
    @SerializedName("clearContainer") val clearContainer: Boolean = false,
    @SerializedName("notes") val notes: String? = null
)
