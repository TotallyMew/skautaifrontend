package lt.skautai.android.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.ui.graphics.vector.ImageVector
import lt.skautai.android.util.NavRoutes

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : BottomNavItem(
        route = NavRoutes.Home.route,
        label = "Pradžia",
        icon = Icons.Default.Home
    )

    data object Events : BottomNavItem(
        route = NavRoutes.EventList.route,
        label = "Renginiai",
        icon = Icons.Default.CalendarMonth
    )

    data object Inventory : BottomNavItem(
        route = NavRoutes.InventoryList.route,
        label = "Inventorius",
        icon = Icons.Default.Inventory2
    )

    data object Reservations : BottomNavItem(
        route = NavRoutes.ReservationList.route,
        label = "Rezervacijos",
        icon = Icons.Default.EventAvailable
    )

    data object Members : BottomNavItem(
        route = NavRoutes.MemberList.route,
        label = "Nariai",
        icon = Icons.Default.Group
    )

    companion object {
        val all = listOf(Home, Inventory, Reservations, Events, Members)
    }
}
