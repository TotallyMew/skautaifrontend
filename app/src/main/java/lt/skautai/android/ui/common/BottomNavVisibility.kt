package lt.skautai.android.ui.common

import lt.skautai.android.util.canViewInventory
import lt.skautai.android.util.canViewMembers
import lt.skautai.android.util.canViewReservations

fun shouldShowBottomNavItem(item: BottomNavItem, permissions: Set<String>): Boolean {
    return when (item) {
        is BottomNavItem.Inventory -> permissions.canViewInventory()
        is BottomNavItem.Reservations -> permissions.canViewReservations()
        is BottomNavItem.Events -> "events.view" in permissions
        is BottomNavItem.Members -> permissions.canViewMembers()
        else -> true
    }
}
