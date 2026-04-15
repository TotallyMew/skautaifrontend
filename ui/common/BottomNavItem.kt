package lt.skautai.android.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.graphics.vector.ImageVector
import lt.skautai.android.util.NavRoutes

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Inventory : BottomNavItem(
        route = NavRoutes.InventoryList.route,
        label = "Inventorius",
        icon = Icons.Default.Inventory2
    )
    data object Reservations : BottomNavItem(
        route = NavRoutes.ReservationList.route,
        label = "Rezervacijos",
        icon = Icons.Default.SwapHoriz
    )
    data object Requests : BottomNavItem(
        route = NavRoutes.RequestList.route,
        label = "Prašymai",
        icon = Icons.Default.ListAlt
    )
    data object Members : BottomNavItem(
        route = NavRoutes.MemberList.route,
        label = "Nariai",
        icon = Icons.Default.Group
    )
    data object Events : BottomNavItem(
        route = NavRoutes.EventList.route,
        label = "Renginiai",
        icon = Icons.Default.CalendarMonth
    )

    companion object {
        val all = listOf(Inventory, Reservations, Requests, Members, Events)
    }
}