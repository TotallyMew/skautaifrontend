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
    const val REQUISITION_REVIEW_UNIT = "REQUISITION_REVIEW_UNIT"
    const val REQUISITION_REVIEW_TOP_LEVEL = "REQUISITION_REVIEW_TOP_LEVEL"
    const val LOCATION_CREATE = "LOCATION_CREATE"
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
}

data class IdPayload(val id: String)
data class ReviewPayload(val id: String, val action: String, val rejectionReason: String? = null)
data class ReservationReviewPayload(val id: String, val status: String, val notes: String? = null)
data class MemberAssignmentPayload(val userId: String, val assignmentId: String)
data class MemberRankPayload(val userId: String, val rankId: String)
data class UnitMemberPayload(val unitId: String, val userId: String)
