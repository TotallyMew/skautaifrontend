package lt.skautai.android.ui.common

fun shouldShowBottomNavItem(item: BottomNavItem, permissions: Set<String>): Boolean {
    return when (item) {
        is BottomNavItem.Events -> "events.view" in permissions
        is BottomNavItem.Members -> "members.view" in permissions
        is BottomNavItem.Units ->
            "organizational_units.manage" in permissions ||
                "unit.members.manage" in permissions ||
                "unit.members.manage:ALL" in permissions ||
                "unit.members.manage:OWN_UNIT" in permissions
        else -> true
    }
}
