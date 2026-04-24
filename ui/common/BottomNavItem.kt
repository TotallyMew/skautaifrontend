package lt.skautai.android.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import lt.skautai.android.util.NavRoutes

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : BottomNavItem(
        route = NavRoutes.Home.route,
        label = "Pradzia",
        icon = Icons.Default.Home
    )

    data object Events : BottomNavItem(
        route = NavRoutes.EventList.route,
        label = "Renginiai",
        icon = Icons.Default.CalendarMonth
    )

    data object Members : BottomNavItem(
        route = NavRoutes.MemberList.route,
        label = "Nariai",
        icon = Icons.Default.Group
    )

    data object Units : BottomNavItem(
        route = NavRoutes.UnitList.route,
        label = "Vienetai",
        icon = Icons.Default.AccountTree
    )

    companion object {
        val all = listOf(Home, Events, Members, Units)
    }
}
