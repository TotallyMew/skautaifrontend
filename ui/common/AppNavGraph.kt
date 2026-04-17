package lt.skautai.android.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import lt.skautai.android.MainViewModel
import lt.skautai.android.ui.auth.LoginScreen
import lt.skautai.android.ui.auth.RegisterScreen
import lt.skautai.android.ui.auth.RegisterInviteScreen
import lt.skautai.android.ui.superadmin.SuperAdminLoginScreen
import lt.skautai.android.ui.superadmin.SuperAdminDashboardScreen
import lt.skautai.android.util.NavRoutes
import lt.skautai.android.util.TokenManager
import androidx.hilt.navigation.compose.hiltViewModel
import lt.skautai.android.ui.inventory.InventoryAddEditScreen
import lt.skautai.android.ui.tuntas.TuntasSelectScreen
import lt.skautai.android.ui.inventory.InventoryListScreen
import lt.skautai.android.ui.inventory.InventoryDetailScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
@Composable
fun AppNavGraph(
    navController: NavHostController,
    tokenManager: TokenManager,
    startDestination: String = NavRoutes.Login.route
) {
    val mainViewModel: MainViewModel = hiltViewModel()
    val activeTuntasId by tokenManager.activeTuntasId.collectAsState(initial = null)

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

        // Super Admin
        composable(NavRoutes.SuperAdminLogin.route) {
            SuperAdminLoginScreen(navController)
        }
        composable(NavRoutes.SuperAdminDashboard.route) {
            SuperAdminDashboardScreen()
        }

        // Main screens wrapped in MainScaffold
        composable(NavRoutes.InventoryList.route) {
            MainScaffold(
                navController = navController,
                tokenManager = tokenManager,
                onLogout = {
                    mainViewModel.logout {
                        navController.navigate(NavRoutes.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                floatingActionButton = {
                    androidx.compose.material3.FloatingActionButton(
                        onClick = {
                            navController.navigate(NavRoutes.InventoryAddEdit.createRoute(null))
                        }
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Add,
                            contentDescription = "Pridėti daiktą"
                        )
                    }
                }
            ) {
                InventoryListScreen(navController)
            }
        }
        composable(NavRoutes.ReservationList.route) {
            MainScaffold(
                navController = navController,
                tokenManager = tokenManager,
                onLogout = {
                    mainViewModel.logout {
                        navController.navigate(NavRoutes.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            ) {
                // ReservationListScreen(navController)
            }
        }
        composable(NavRoutes.RequestList.route) {
            MainScaffold(
                navController = navController,
                tokenManager = tokenManager,
                onLogout = {
                    mainViewModel.logout {
                        navController.navigate(NavRoutes.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            ) {
                // RequestListScreen(navController)
            }
        }
        composable(NavRoutes.MemberList.route) {
            MainScaffold(
                navController = navController,
                tokenManager = tokenManager,
                onLogout = {
                    mainViewModel.logout {
                        navController.navigate(NavRoutes.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            ) {
                // MemberListScreen(navController)
            }
        }
        composable(NavRoutes.EventList.route) {
            MainScaffold(
                navController = navController,
                tokenManager = tokenManager,
                onLogout = {
                    mainViewModel.logout {
                        navController.navigate(NavRoutes.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            ) {
                // EventListScreen(navController)
            }
        }

        // Detail screens
        composable(
            route = NavRoutes.InventoryDetail.route,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) {
            val itemId = it.arguments?.getString("itemId")!!
            InventoryDetailScreen(
                itemId = itemId,
                navController = navController
            )
        }
        composable(
            route = NavRoutes.InventoryAddEdit.route,
            arguments = listOf(navArgument("itemId") {
                type = NavType.StringType
                defaultValue = null
                nullable = true
            })
        ) {
            val itemId = it.arguments?.getString("itemId")
            InventoryAddEditScreen(
                itemId = itemId,
                navController = navController
            )
        }
        composable(NavRoutes.ReservationCreate.route) {
            // ReservationCreateScreen(navController)
        }
        composable(
            route = NavRoutes.RequestDetail.route,
            arguments = listOf(navArgument("requestId") { type = NavType.StringType })
        ) {
            // RequestDetailScreen(navController, it.arguments?.getInt("requestId")!!)
        }
        composable(NavRoutes.RequestCreate.route) {
            // RequestCreateScreen(navController)
        }
        composable(
            route = NavRoutes.MemberDetail.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) {
            // MemberDetailScreen(navController, it.arguments?.getInt("userId")!!)
        }
        composable(NavRoutes.UnitList.route) {
            // UnitListScreen(navController)
        }
        composable(
            route = NavRoutes.UnitDetail.route,
            arguments = listOf(navArgument("unitId") { type = NavType.StringType })
        ) {
            // UnitDetailScreen(navController, it.arguments?.getInt("unitId")!!)
        }
        composable(
            route = NavRoutes.EventDetail.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) {
            // EventDetailScreen(navController, it.arguments?.getInt("eventId")!!)
        }
        composable(
            route = NavRoutes.EventAddEdit.route,
            arguments = listOf(navArgument("eventId") {
                type = NavType.StringType
                defaultValue = -1
            })
        ) {
            // EventAddEditScreen(navController, it.arguments?.getInt("eventId"))
        }
        composable(NavRoutes.TuntasSelect.route) {
            TuntasSelectScreen(navController)
        }
    }
}