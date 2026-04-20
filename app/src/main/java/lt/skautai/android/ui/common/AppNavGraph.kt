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
import lt.skautai.android.ui.members.MemberListScreen
import lt.skautai.android.ui.members.MemberDetailScreen
import lt.skautai.android.ui.members.InviteCreateScreen
import lt.skautai.android.ui.reservations.ReservationListScreen
import lt.skautai.android.ui.reservations.ReservationDetailScreen
import lt.skautai.android.ui.reservations.ReservationCreateScreen
import lt.skautai.android.ui.requests.RequestListScreen
import lt.skautai.android.ui.requests.RequestDetailScreen
import lt.skautai.android.ui.requests.RequestCreateScreen
import lt.skautai.android.ui.units.UnitListScreen
import lt.skautai.android.ui.units.UnitCreateScreen
import lt.skautai.android.ui.units.UnitDetailScreen
import lt.skautai.android.ui.units.UnitEditScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    tokenManager: TokenManager,
    startDestination: String = NavRoutes.Login.route
) {
    val mainViewModel: MainViewModel = hiltViewModel()
    val activeTuntasId by tokenManager.activeTuntasId.collectAsState(initial = null)
    val permissions by tokenManager.permissions.collectAsState(initial = emptySet())

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
                    if ("items.create" in permissions) {
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
                ReservationListScreen(
                    onReservationClick = { id ->
                        navController.navigate(NavRoutes.ReservationDetail.createRoute(id))
                    },
                    onCreateClick = {
                        navController.navigate(NavRoutes.ReservationCreate.route)
                    }
                )
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
                RequestListScreen(
                    onRequestClick = { id ->
                        navController.navigate(NavRoutes.RequestDetail.createRoute(id))
                    },
                    onCreateClick = {
                        navController.navigate(NavRoutes.RequestCreate.route)
                    }
                )
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
                MemberListScreen(
                    onMemberClick = { userId ->
                        navController.navigate(NavRoutes.MemberDetail.createRoute(userId))
                    },
                    onInviteClick = {
                        navController.navigate(NavRoutes.InviteCreate.route)
                    },
                    canInvite = "invitations.create" in permissions
                )
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
            ReservationCreateScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = NavRoutes.RequestDetail.route,
            arguments = listOf(navArgument("requestId") { type = NavType.StringType })
        ) {
            val requestId = it.arguments?.getString("requestId")!!
            RequestDetailScreen(
                requestId = requestId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.RequestCreate.route) {
            RequestCreateScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.ReservationDetail.route,
            arguments = listOf(navArgument("reservationId") { type = NavType.StringType })
        ) {
            val reservationId = it.arguments?.getString("reservationId")!!
            ReservationDetailScreen(
                reservationId = reservationId,
                onBack = { navController.popBackStack() }
            )
        }


        composable(
            route = NavRoutes.MemberDetail.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) {
            val userId = it.arguments?.getString("userId")!!
            MemberDetailScreen(
                userId = userId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.InviteCreate.route) {
            InviteCreateScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.UnitList.route) {
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
                    if ("organizational_units.manage" in permissions) {
                        FloatingActionButton(
                            onClick = { navController.navigate(NavRoutes.UnitCreate.route) }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Pridėti vienetą")
                        }
                    }
                }
            ) {
                UnitListScreen(
                    onCreateClick = { navController.navigate(NavRoutes.UnitCreate.route) },
                    onUnitClick = { unitId -> navController.navigate(NavRoutes.UnitDetail.createRoute(unitId)) }
                )
            }
        }
        composable(NavRoutes.UnitCreate.route) {
            UnitCreateScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = NavRoutes.UnitDetail.route,
            arguments = listOf(navArgument("unitId") { type = NavType.StringType })
        ) {
            val unitId = it.arguments?.getString("unitId")!!
            UnitDetailScreen(
                unitId = unitId,
                onBack = { navController.popBackStack() },
                onEditClick = { id -> navController.navigate(NavRoutes.UnitEdit.createRoute(id)) }
            )
        }
        composable(
            route = NavRoutes.UnitEdit.route,
            arguments = listOf(navArgument("unitId") { type = NavType.StringType })
        ) {
            val unitId = it.arguments?.getString("unitId")!!
            UnitEditScreen(
                unitId = unitId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.TuntasSelect.route) {
            TuntasSelectScreen(navController)
        }
    }
}