package lt.skautai.android.util

sealed class NavRoutes(val route: String) {

    //Super-admin
    object SuperAdminLogin : NavRoutes("super_admin_login")
    object SuperAdminDashboard : NavRoutes("super_admin_dashboard")

    // Auth
    object Login : NavRoutes("login")
    object Register : NavRoutes("register")
    object RegisterInvite : NavRoutes("register_invite")
    object Home : NavRoutes("home")

    object InviteCreate : NavRoutes("invite_create")
    object InviteAccept : NavRoutes("invite_accept")

    // Inventory
    object InventoryList : NavRoutes("inventory_list?type={type}&category={category}&custodianId={custodianId}") {
        fun createRoute(type: String? = null, category: String? = null, custodianId: String? = null): String {
            val params = buildList {
                if (type != null) add("type=$type")
                if (category != null) add("category=$category")
                if (custodianId != null) add("custodianId=$custodianId")
            }
            return if (params.isEmpty()) {
                "inventory_list"
            } else {
                "inventory_list?${params.joinToString("&")}"
            }
        }
    }
    object InventoryDetail : NavRoutes("inventory_detail/{itemId}") {
        fun createRoute(itemId: String) = "inventory_detail/$itemId"
    }
    object InventoryAddEdit : NavRoutes("inventory_add_edit?itemId={itemId}&mode={mode}") {
        fun createRoute(itemId: String? = null, mode: String? = null): String {
            val params = buildList {
                if (itemId != null) add("itemId=$itemId")
                if (mode != null) add("mode=$mode")
            }
            return if (params.isEmpty()) "inventory_add_edit" else "inventory_add_edit?${params.joinToString("&")}"
        }
    }

    // Reservations
    object ReservationList : NavRoutes("reservation_list?mode={mode}") {
        fun createRoute(mode: String? = null): String =
            if (mode != null) "reservation_list?mode=$mode" else "reservation_list"
    }
    object ReservationCreate : NavRoutes("reservation_create")

    // Inventory Requests
    object RequestList : NavRoutes("request_list?mode={mode}") {
        fun createRoute(mode: String? = null): String =
            if (mode != null) "request_list?mode=$mode" else "request_list"
    }
    object RequestDetail : NavRoutes("request_detail/{requestId}") {
        fun createRoute(requestId: String) = "request_detail/$requestId"
    }
    object RequestCreate : NavRoutes("request_create")

    object SharedRequestList : NavRoutes("shared_request_list")
    object SharedRequestDetail : NavRoutes("shared_request_detail/{requestId}") {
        fun createRoute(requestId: String) = "shared_request_detail/$requestId"
    }



    object ReservationDetail : NavRoutes("reservation_detail/{reservationId}") {
        fun createRoute(reservationId: String) = "reservation_detail/$reservationId"
    }
    object ReservationMovement : NavRoutes("reservation_movement/{reservationId}/{mode}") {
        fun createRoute(reservationId: String, mode: String) = "reservation_movement/$reservationId/$mode"
    }


    // Members
    object MemberList : NavRoutes("member_list")
    object MemberDetail : NavRoutes("member_detail/{userId}") {
        fun createRoute(userId: String) = "member_detail/$userId"
    }

    // Organizational Units
    object UnitList : NavRoutes("unit_list")
    object UnitCreate : NavRoutes("unit_create")
    object UnitDetail : NavRoutes("unit_detail/{unitId}") {
        fun createRoute(unitId: String) = "unit_detail/$unitId"
    }
    object UnitEdit : NavRoutes("unit_edit/{unitId}") {
        fun createRoute(unitId: String) = "unit_edit/$unitId"
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
