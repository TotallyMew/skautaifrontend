package lt.skautai.android.util

sealed class NavRoutes(val route: String) {

    //Super-admin
    object SuperAdminLogin : NavRoutes("super_admin_login")
    object SuperAdminDashboard : NavRoutes("super_admin_dashboard")

    // Auth
    object Login : NavRoutes("login")
    object Register : NavRoutes("register")
    object RegisterInvite : NavRoutes("register_invite")

    // Inventory
    object InventoryList : NavRoutes("inventory_list")
    object InventoryDetail : NavRoutes("inventory_detail/{itemId}") {
        fun createRoute(itemId: String) = "inventory_detail/$itemId"
    }
    object InventoryAddEdit : NavRoutes("inventory_add_edit?itemId={itemId}") {
        fun createRoute(itemId: String? = null) =
            if (itemId != null) "inventory_add_edit?itemId=$itemId" else "inventory_add_edit"
    }

    // Reservations
    object ReservationList : NavRoutes("reservation_list")
    object ReservationCreate : NavRoutes("reservation_create")

    // Inventory Requests
    object RequestList : NavRoutes("request_list")
    object RequestDetail : NavRoutes("request_detail/{requestId}") {
        fun createRoute(requestId: String) = "request_detail/$requestId"
    }
    object RequestCreate : NavRoutes("request_create")

    // Members
    object MemberList : NavRoutes("member_list")
    object MemberDetail : NavRoutes("member_detail/{userId}") {
        fun createRoute(userId: String) = "member_detail/$userId"
    }

    // Organizational Units
    object UnitList : NavRoutes("unit_list")
    object UnitDetail : NavRoutes("unit_detail/{unitId}") {
        fun createRoute(unitId: String) = "unit_detail/$unitId"
    }

    object TuntasSelect : NavRoutes("tuntas_select")

    // Events
    object EventList : NavRoutes("event_list")
    object EventDetail : NavRoutes("event_detail/{eventId}") {
        fun createRoute(eventId: String) = "event_detail/$eventId"
    }
    object EventAddEdit : NavRoutes("event_add_edit?eventId={eventId}") {
        fun createRoute(eventId: String? = null) =
            if (eventId != null) "event_add_edit?eventId=$eventId" else "event_add_edit"
    }
}