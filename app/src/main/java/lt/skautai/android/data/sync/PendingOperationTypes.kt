package lt.skautai.android.data.sync

object PendingEntityType {
    const val ITEM = "ITEM"
    const val RESERVATION = "RESERVATION"
    const val BENDRAS_REQUEST = "BENDRAS_REQUEST"
    const val REQUISITION = "REQUISITION"
    const val LOCATION = "LOCATION"
    const val MEMBER = "MEMBER"
    const val ORGANIZATIONAL_UNIT = "ORGANIZATIONAL_UNIT"
    const val EVENT = "EVENT"
}

object PendingOperationType {
    const val ITEM_CREATE = "ITEM_CREATE"
    const val ITEM_UPDATE = "ITEM_UPDATE"
    const val ITEM_DELETE = "ITEM_DELETE"
    const val RESERVATION_CREATE = "RESERVATION_CREATE"
    const val RESERVATION_CANCEL = "RESERVATION_CANCEL"
    const val RESERVATION_REVIEW_UNIT = "RESERVATION_REVIEW_UNIT"
    const val RESERVATION_REVIEW_TOP_LEVEL = "RESERVATION_REVIEW_TOP_LEVEL"
    const val RESERVATION_UPDATE_STATUS = "RESERVATION_UPDATE_STATUS"
    const val RESERVATION_UPDATE_PICKUP = "RESERVATION_UPDATE_PICKUP"
    const val RESERVATION_UPDATE_RETURN = "RESERVATION_UPDATE_RETURN"
    const val RESERVATION_MOVEMENT = "RESERVATION_MOVEMENT"
    const val BENDRAS_REQUEST_CREATE = "BENDRAS_REQUEST_CREATE"
    const val BENDRAS_REQUEST_CANCEL = "BENDRAS_REQUEST_CANCEL"
    const val BENDRAS_REQUEST_REVIEW_UNIT = "BENDRAS_REQUEST_REVIEW_UNIT"
    const val BENDRAS_REQUEST_REVIEW_TOP_LEVEL = "BENDRAS_REQUEST_REVIEW_TOP_LEVEL"
    const val REQUISITION_CREATE = "REQUISITION_CREATE"
    const val REQUISITION_CANCEL = "REQUISITION_CANCEL"
    const val REQUISITION_REVIEW_UNIT = "REQUISITION_REVIEW_UNIT"
    const val REQUISITION_REVIEW_TOP_LEVEL = "REQUISITION_REVIEW_TOP_LEVEL"
    const val LOCATION_CREATE = "LOCATION_CREATE"
    const val LOCATION_UPDATE = "LOCATION_UPDATE"
    const val LOCATION_DELETE = "LOCATION_DELETE"
    const val MEMBER_ASSIGN_LEADERSHIP_ROLE = "MEMBER_ASSIGN_LEADERSHIP_ROLE"
    const val MEMBER_REMOVE_LEADERSHIP_ROLE = "MEMBER_REMOVE_LEADERSHIP_ROLE"
    const val MEMBER_STEP_DOWN_LEADERSHIP_ROLE = "MEMBER_STEP_DOWN_LEADERSHIP_ROLE"
    const val MEMBER_ASSIGN_RANK = "MEMBER_ASSIGN_RANK"
    const val MEMBER_REMOVE_RANK = "MEMBER_REMOVE_RANK"
    const val MEMBER_REMOVE = "MEMBER_REMOVE"
    const val UNIT_CREATE = "UNIT_CREATE"
    const val UNIT_UPDATE = "UNIT_UPDATE"
    const val UNIT_DELETE = "UNIT_DELETE"
    const val UNIT_ASSIGN_MEMBER = "UNIT_ASSIGN_MEMBER"
    const val UNIT_REMOVE_MEMBER = "UNIT_REMOVE_MEMBER"
    const val UNIT_LEAVE = "UNIT_LEAVE"
    const val UNIT_MOVE_MEMBER = "UNIT_MOVE_MEMBER"
    const val EVENT_CREATE = "EVENT_CREATE"
    const val EVENT_UPDATE = "EVENT_UPDATE"
    const val EVENT_CANCEL = "EVENT_CANCEL"
    const val EVENT_ASSIGN_ROLE = "EVENT_ASSIGN_ROLE"
    const val EVENT_REMOVE_ROLE = "EVENT_REMOVE_ROLE"
    const val EVENT_CREATE_BUCKET = "EVENT_CREATE_BUCKET"
    const val EVENT_UPDATE_BUCKET = "EVENT_UPDATE_BUCKET"
    const val EVENT_DELETE_BUCKET = "EVENT_DELETE_BUCKET"
    const val EVENT_CREATE_ITEM = "EVENT_CREATE_ITEM"
    const val EVENT_CREATE_ITEMS_BULK = "EVENT_CREATE_ITEMS_BULK"
    const val EVENT_UPDATE_ITEM = "EVENT_UPDATE_ITEM"
    const val EVENT_DELETE_ITEM = "EVENT_DELETE_ITEM"
    const val EVENT_CREATE_ALLOCATION = "EVENT_CREATE_ALLOCATION"
    const val EVENT_UPDATE_ALLOCATION = "EVENT_UPDATE_ALLOCATION"
    const val EVENT_DELETE_ALLOCATION = "EVENT_DELETE_ALLOCATION"
    const val EVENT_CREATE_PASTOVYKLE = "EVENT_CREATE_PASTOVYKLE"
    const val EVENT_UPDATE_PASTOVYKLE = "EVENT_UPDATE_PASTOVYKLE"
    const val EVENT_DELETE_PASTOVYKLE = "EVENT_DELETE_PASTOVYKLE"
    const val EVENT_ASSIGN_PASTOVYKLE_INVENTORY = "EVENT_ASSIGN_PASTOVYKLE_INVENTORY"
    const val EVENT_UPDATE_PASTOVYKLE_INVENTORY = "EVENT_UPDATE_PASTOVYKLE_INVENTORY"
    const val EVENT_DELETE_PASTOVYKLE_INVENTORY = "EVENT_DELETE_PASTOVYKLE_INVENTORY"
    const val EVENT_CREATE_PASTOVYKLE_REQUEST = "EVENT_CREATE_PASTOVYKLE_REQUEST"
    const val EVENT_APPROVE_PASTOVYKLE_REQUEST = "EVENT_APPROVE_PASTOVYKLE_REQUEST"
    const val EVENT_REJECT_PASTOVYKLE_REQUEST = "EVENT_REJECT_PASTOVYKLE_REQUEST"
    const val EVENT_SELF_PROVIDE_PASTOVYKLE_REQUEST = "EVENT_SELF_PROVIDE_PASTOVYKLE_REQUEST"
    const val EVENT_FULFILL_PASTOVYKLE_REQUEST = "EVENT_FULFILL_PASTOVYKLE_REQUEST"
    const val EVENT_ASSIGN_FROM_UNIT_INVENTORY = "EVENT_ASSIGN_FROM_UNIT_INVENTORY"
    const val EVENT_CREATE_PURCHASE = "EVENT_CREATE_PURCHASE"
    const val EVENT_ATTACH_PURCHASE_INVOICE = "EVENT_ATTACH_PURCHASE_INVOICE"
    const val EVENT_COMPLETE_PURCHASE = "EVENT_COMPLETE_PURCHASE"
    const val EVENT_ADD_PURCHASE_TO_INVENTORY = "EVENT_ADD_PURCHASE_TO_INVENTORY"
    const val EVENT_CREATE_INVENTORY_MOVEMENT = "EVENT_CREATE_INVENTORY_MOVEMENT"
}

data class IdPayload(val id: String)
data class ReviewPayload(val id: String, val action: String, val rejectionReason: String? = null)
data class ReservationReviewPayload(val id: String, val status: String, val notes: String? = null)
data class MemberAssignmentPayload(val userId: String, val assignmentId: String)
data class MemberRankPayload(val userId: String, val rankId: String)
data class UnitMemberPayload(val unitId: String, val userId: String)
data class EventRoleRemovalPayload(val eventId: String, val roleId: String)
data class EventBucketCreatePayload(
    val eventId: String,
    val request: lt.skautai.android.data.remote.CreateEventInventoryBucketRequestDto
)
data class EventBucketUpdatePayload(
    val eventId: String,
    val bucketId: String,
    val request: lt.skautai.android.data.remote.UpdateEventInventoryBucketRequestDto
)
data class EventBucketDeletePayload(
    val eventId: String,
    val bucketId: String
)
data class EventItemCreatePayload(
    val eventId: String,
    val request: lt.skautai.android.data.remote.CreateEventInventoryItemRequestDto
)
data class EventItemsBulkCreatePayload(
    val eventId: String,
    val request: lt.skautai.android.data.remote.CreateEventInventoryItemsBulkRequestDto
)
data class EventItemUpdatePayload(
    val eventId: String,
    val inventoryItemId: String,
    val request: lt.skautai.android.data.remote.UpdateEventInventoryItemRequestDto
)
data class EventItemDeletePayload(
    val eventId: String,
    val inventoryItemId: String
)
data class EventAllocationCreatePayload(
    val eventId: String,
    val request: lt.skautai.android.data.remote.CreateEventInventoryAllocationRequestDto
)
data class EventAllocationUpdatePayload(
    val eventId: String,
    val allocationId: String,
    val request: lt.skautai.android.data.remote.UpdateEventInventoryAllocationRequestDto
)
data class EventAllocationDeletePayload(
    val eventId: String,
    val allocationId: String
)
data class EventPastovykleUpsertPayload(
    val eventId: String,
    val request: lt.skautai.android.data.remote.CreatePastovykleRequestDto
)
data class EventPastovykleUpdatePayload(
    val eventId: String,
    val pastovykleId: String,
    val request: lt.skautai.android.data.remote.UpdatePastovykleRequestDto
)
data class EventPastovykleDeletePayload(
    val eventId: String,
    val pastovykleId: String
)
data class EventPastovykleInventoryAssignPayload(
    val eventId: String,
    val pastovykleId: String,
    val request: lt.skautai.android.data.remote.AssignPastovykleInventoryRequestDto
)
data class EventPastovykleInventoryUpdatePayload(
    val eventId: String,
    val pastovykleId: String,
    val inventoryId: String,
    val request: lt.skautai.android.data.remote.UpdatePastovykleInventoryRequestDto
)
data class EventPastovykleInventoryDeletePayload(
    val eventId: String,
    val pastovykleId: String,
    val inventoryId: String
)
data class EventPastovykleRequestCreatePayload(
    val eventId: String,
    val pastovykleId: String,
    val request: lt.skautai.android.data.remote.CreatePastovykleInventoryRequestRequestDto
)
data class EventPastovykleRequestActionPayload(
    val eventId: String,
    val pastovykleId: String,
    val requestId: String,
    val quantity: Int? = null,
    val notes: String? = null
)
data class EventAssignFromUnitInventoryPayload(
    val eventId: String,
    val pastovykleId: String,
    val itemId: String,
    val quantity: Int,
    val notes: String? = null
)
data class EventPurchasePayload(
    val eventId: String,
    val purchaseId: String
)
data class EventAttachPurchaseInvoicePayload(
    val eventId: String,
    val purchaseId: String,
    val invoiceFileUrl: String? = null,
    val stagedDocumentUrl: String? = null
)
data class EventInventoryMovementPayload(
    val eventId: String,
    val request: lt.skautai.android.data.remote.CreateEventInventoryMovementRequestDto
)
