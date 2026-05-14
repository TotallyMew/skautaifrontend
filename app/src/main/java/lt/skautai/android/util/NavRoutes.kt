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
    object Profile : NavRoutes("profile")

    // Inventory
    object InventoryList : NavRoutes("inventory_list?type={type}&category={category}&custodianId={custodianId}&sharedOnly={sharedOnly}&personalOwner={personalOwner}") {
        fun createRoute(
            type: String? = null,
            category: String? = null,
            custodianId: String? = null,
            sharedOnly: Boolean? = null,
            personalOwner: String? = null
        ): String {
            val params = buildList {
                if (type != null) add("type=$type")
                if (category != null) add("category=$category")
                if (custodianId != null) add("custodianId=$custodianId")
                if (sharedOnly != null) add("sharedOnly=$sharedOnly")
                if (personalOwner != null) add("personalOwner=$personalOwner")
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
    object InventoryQrScanner : NavRoutes("inventory_qr_scanner")
    object InventoryAddEdit : NavRoutes("inventory_add_edit?itemId={itemId}&mode={mode}&custodianId={custodianId}") {
        fun createRoute(itemId: String? = null, mode: String? = null, custodianId: String? = null): String {
            val params = buildList {
                if (itemId != null) add("itemId=$itemId")
                if (mode != null) add("mode=$mode")
                if (custodianId != null) add("custodianId=$custodianId")
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
    object SharedRequestCreate : NavRoutes("shared_request_create")



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

    object LocationList : NavRoutes("location_list")
    object LocationDetail : NavRoutes("location_detail/{locationId}") {
        fun createRoute(locationId: String) = "location_detail/$locationId"
    }
    object LocationAddEdit : NavRoutes("location_add_edit?locationId={locationId}&parentLocationId={parentLocationId}") {
        fun createRoute(locationId: String? = null, parentLocationId: String? = null): String {
            val params = buildList {
                if (locationId != null) add("locationId=$locationId")
                if (parentLocationId != null) add("parentLocationId=$parentLocationId")
            }
            return if (params.isEmpty()) "location_add_edit" else "location_add_edit?${params.joinToString("&")}"
        }
    }

    // Events
    object EventList : NavRoutes("event_list")
    object InventoryTemplates : NavRoutes("inventory_templates")
    object EventDetail : NavRoutes("event_detail/{eventId}") {
        fun createRoute(eventId: String) = "event_detail/$eventId"
    }
    object EventNeeds : NavRoutes("event_needs/{eventId}") {
        fun createRoute(eventId: String) = "event_needs/$eventId"
    }
    object EventUkvedys : NavRoutes("event_ukvedys/{eventId}") {
        fun createRoute(eventId: String) = "event_ukvedys/$eventId"
    }
    object EventPurchases : NavRoutes("event_purchases/{eventId}") {
        fun createRoute(eventId: String) = "event_purchases/$eventId"
    }
    object EventReconciliation : NavRoutes("event_reconciliation/{eventId}") {
        fun createRoute(eventId: String) = "event_reconciliation/$eventId"
    }
    object EventPlan : NavRoutes("event_plan/{eventId}") {
        fun createRoute(eventId: String) = "event_plan/$eventId"
    }
    object EventStaff : NavRoutes("event_staff/{eventId}") {
        fun createRoute(eventId: String) = "event_staff/$eventId"
    }
    object EventPastovyklės : NavRoutes("event_pastovykles/{eventId}") {
        fun createRoute(eventId: String) = "event_pastovykles/$eventId"
    }
    object EventMovement : NavRoutes("event_movement/{eventId}") {
        fun createRoute(eventId: String) = "event_movement/$eventId"
    }
    object EventMovementQr : NavRoutes("event_movement_qr/{eventId}/{mode}") {
        fun createRoute(eventId: String, mode: String) = "event_movement_qr/$eventId/$mode"
    }
    object PastovykleLeader : NavRoutes("event_pastovykle/{eventId}") {
        fun createRoute(eventId: String) = "event_pastovykle/$eventId"
    }
    object EventAddEdit : NavRoutes("event_add_edit?eventId={eventId}") {
        fun createRoute(eventId: String? = null) =
            if (eventId != null) "event_add_edit?eventId=$eventId" else "event_add_edit"
    }

    object SyncStatus : NavRoutes("sync_status")

    companion object {
        fun titleFor(currentRoute: String?): String = when (currentRoute?.substringBefore("?")) {
            Home.route -> "Pradžia"
            InventoryList.route.substringBefore("?") -> "Inventorius"
            InventoryDetail.route -> "Daikto informacija"
            InventoryQrScanner.route -> "QR skenavimas"
            InventoryAddEdit.route.substringBefore("?") -> "Inventoriaus forma"
            ReservationList.route.substringBefore("?") -> "Rezervacijos"
            ReservationCreate.route -> "Nauja rezervacija"
            ReservationDetail.route -> "Rezervacijos informacija"
            ReservationMovement.route -> "Išdavimas ir grąžinimas"
            RequestList.route.substringBefore("?") -> "Pirkimai"
            RequestDetail.route -> "Pirkimo informacija"
            RequestCreate.route -> "Naujas pirkimas"
            InviteCreate.route -> "Naujas kvietimas"
            InviteAccept.route -> "Kvietimai"
            Profile.route -> "Mano profilis"
            SharedRequestList.route -> "Paėmimai"
            SharedRequestDetail.route -> "Paėmimo informacija"
            SharedRequestCreate.route -> "Naujas paėmimas"
            MemberList.route -> "Nariai"
            MemberDetail.route -> "Nario informacija"
            UnitList.route -> "Vienetai"
            UnitCreate.route -> "Naujas vienetas"
            UnitDetail.route -> "Vieneto informacija"
            UnitEdit.route -> "Vieneto redagavimas"
            LocationList.route -> "Lokacijos"
            LocationDetail.route -> "Lokacijos informacija"
            LocationAddEdit.route.substringBefore("?") -> "Lokacijos forma"
            TuntasSelect.route -> "Tunto pasirinkimas"
            EventList.route -> "Renginiai"
            InventoryTemplates.route -> "Inventoriaus sablonai"
            EventDetail.route -> "Renginio informacija"
            EventAddEdit.route.substringBefore("?") -> "Renginio forma"
            EventNeeds.route -> "Renginio poreikiai"
            EventUkvedys.route -> "Ūkvedys"
            EventPurchases.route -> "Renginio pirkimai"
            EventReconciliation.route -> "Suvedimas"
            EventPlan.route -> "Inventoriaus planas"
            EventStaff.route -> "Komanda"
            EventPastovyklės.route -> "Pastovyklės"
            EventMovement.route -> "Inventoriaus judėjimas"
            EventMovementQr.route -> "QR judėjimas"
            PastovykleLeader.route -> "Pastovyklės vadovas"
            SyncStatus.route -> "Sinchronizavimas"
            else -> "Skautų Inventorius"
        }
    }
}
