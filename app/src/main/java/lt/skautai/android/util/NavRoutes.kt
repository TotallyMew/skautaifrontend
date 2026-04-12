package lt.skautai.android.util

sealed class NavRoutes(val route: String) {

    // Auth
    object Login : NavRoutes("login")
    object Register : NavRoutes("register")

    // Inventory
    object InventoryList : NavRoutes("inventory_list")
    object InventoryDetail : NavRoutes("inventory_detail/{itemId}") {
        fun createRoute(itemId: Int) = "inventory_detail/$itemId"
    }
    object InventoryAddEdit : NavRoutes("inventory_add_edit?itemId={itemId}") {
        fun createRoute(itemId: Int? = null) =
            if (itemId != null) "inventory_add_edit?itemId=$itemId" else "inventory_add_edit"
    }

    // Reservations
    object ReservationList : NavRoutes("reservation_list")
    object ReservationCreate : NavRoutes("reservation_create")

    // Inventory Requests
    object RequestList : NavRoutes("request_list")
    object RequestDetail : NavRoutes("request_detail/{requestId}") {
        fun createRoute(requestId: Int) = "request_detail/$requestId"
    }
    object RequestCreate : NavRoutes("request_create")

    // Members
    object MemberList : NavRoutes("member_list")
    object MemberDetail : NavRoutes("member_detail/{userId}") {
        fun createRoute(userId: Int) = "member_detail/$userId"
    }

    // Organizational Units
    object UnitList : NavRoutes("unit_list")
    object UnitDetail : NavRoutes("unit_detail/{unitId}") {
        fun createRoute(unitId: Int) = "unit_detail/$unitId"
    }

    // Events
    object EventList : NavRoutes("event_list")
    object EventDetail : NavRoutes("event_detail/{eventId}") {
        fun createRoute(eventId: Int) = "event_detail/$eventId"
    }
    object EventAddEdit : NavRoutes("event_add_edit?eventId={eventId}") {
        fun createRoute(eventId: Int? = null) =
            if (eventId != null) "event_add_edit?eventId=$eventId" else "event_add_edit"
    }
    object RegisterInvite : NavRoutes("register_invite")
}