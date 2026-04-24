package lt.skautai.android.ui.common

fun shouldShowBottomNavItem(item: BottomNavItem, permissions: Set<String>): Boolean {
    return when (item) {
        is BottomNavItem.Inventory -> true
        is BottomNavItem.Reservations -> true
        is BottomNavItem.Events -> "events.view" in permissions
        is BottomNavItem.Members -> "members.view" in permissions
        else -> true
    }
}
