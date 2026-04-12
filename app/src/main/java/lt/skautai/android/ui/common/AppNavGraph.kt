package lt.skautai.android.ui.common

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import lt.skautai.android.ui.auth.LoginScreen
import lt.skautai.android.ui.auth.RegisterInviteScreen
import lt.skautai.android.ui.auth.RegisterScreen
import lt.skautai.android.util.NavRoutes

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = NavRoutes.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth
        composable(NavRoutes.Login.route) {
            LoginScreen(navController)
        }
        composable(NavRoutes.Register.route) {
            RegisterScreen(navController)
        }

        composable(NavRoutes.RegisterInvite.route) {
            RegisterInviteScreen(navController)
        }

        // Inventory
        composable(NavRoutes.InventoryList.route) {
            // InventoryListScreen(navController)
        }
        composable(
            route = NavRoutes.InventoryDetail.route,
            arguments = listOf(navArgument("itemId") { type = NavType.IntType })
        ) {
            // InventoryDetailScreen(navController, it.arguments?.getInt("itemId")!!)
        }
        composable(
            route = NavRoutes.InventoryAddEdit.route,
            arguments = listOf(navArgument("itemId") {
                type = NavType.IntType
                defaultValue = -1
            })
        ) {
            // InventoryAddEditScreen(navController, it.arguments?.getInt("itemId"))
        }

        // Reservations
        composable(NavRoutes.ReservationList.route) {
            // ReservationListScreen(navController)
        }
        composable(NavRoutes.ReservationCreate.route) {
            // ReservationCreateScreen(navController)
        }

        // Inventory Requests
        composable(NavRoutes.RequestList.route) {
            // RequestListScreen(navController)
        }
        composable(
            route = NavRoutes.RequestDetail.route,
            arguments = listOf(navArgument("requestId") { type = NavType.IntType })
        ) {
            // RequestDetailScreen(navController, it.arguments?.getInt("requestId")!!)
        }
        composable(NavRoutes.RequestCreate.route) {
            // RequestCreateScreen(navController)
        }

        // Members
        composable(NavRoutes.MemberList.route) {
            // MemberListScreen(navController)
        }
        composable(
            route = NavRoutes.MemberDetail.route,
            arguments = listOf(navArgument("userId") { type = NavType.IntType })
        ) {
            // MemberDetailScreen(navController, it.arguments?.getInt("userId")!!)
        }

        // Organizational Units
        composable(NavRoutes.UnitList.route) {
            // UnitListScreen(navController)
        }
        composable(
            route = NavRoutes.UnitDetail.route,
            arguments = listOf(navArgument("unitId") { type = NavType.IntType })
        ) {
            // UnitDetailScreen(navController, it.arguments?.getInt("unitId")!!)
        }

        // Events
        composable(NavRoutes.EventList.route) {
            // EventListScreen(navController)
        }
        composable(
            route = NavRoutes.EventDetail.route,
            arguments = listOf(navArgument("eventId") { type = NavType.IntType })
        ) {
            // EventDetailScreen(navController, it.arguments?.getInt("eventId")!!)
        }
        composable(
            route = NavRoutes.EventAddEdit.route,
            arguments = listOf(navArgument("eventId") {
                type = NavType.IntType
                defaultValue = -1
            })
        ) {
            // EventAddEditScreen(navController, it.arguments?.getInt("eventId"))
        }
    }
}