package lt.skautai.android.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavGraph.Companion.findStartDestination
import lt.skautai.android.MainViewModel
import lt.skautai.android.ui.auth.LoginScreen
import lt.skautai.android.ui.auth.RegisterInviteScreen
import lt.skautai.android.ui.auth.RegisterScreen
import lt.skautai.android.ui.events.EventCreateScreen
import lt.skautai.android.ui.events.EventDetailScreen
import lt.skautai.android.ui.events.EventListScreen
import lt.skautai.android.ui.home.HomeScreen
import lt.skautai.android.ui.inventory.InventoryAddEditScreen
import lt.skautai.android.ui.inventory.InventoryDetailScreen
import lt.skautai.android.ui.inventory.InventoryListScreen
import lt.skautai.android.ui.members.InviteAcceptScreen
import lt.skautai.android.ui.members.InviteCreateScreen
import lt.skautai.android.ui.members.MemberDetailScreen
import lt.skautai.android.ui.members.MemberListScreen
import lt.skautai.android.ui.requests.RequestCreateScreen
import lt.skautai.android.ui.requests.RequestDetailScreen
import lt.skautai.android.ui.requests.RequestListScreen
import lt.skautai.android.ui.requests.RequisitionCreateScreen
import lt.skautai.android.ui.requests.RequisitionDetailScreen
import lt.skautai.android.ui.requests.RequisitionListScreen
import lt.skautai.android.ui.reservations.ReservationCreateScreen
import lt.skautai.android.ui.reservations.ReservationDetailScreen
import lt.skautai.android.ui.reservations.ReservationListScreen
import lt.skautai.android.ui.reservations.ReservationMovementScreen
import lt.skautai.android.ui.superadmin.SuperAdminDashboardScreen
import lt.skautai.android.ui.superadmin.SuperAdminLoginScreen
import lt.skautai.android.ui.tuntas.TuntasSelectScreen
import lt.skautai.android.ui.units.UnitCreateScreen
import lt.skautai.android.ui.units.UnitDetailScreen
import lt.skautai.android.ui.units.UnitEditScreen
import lt.skautai.android.ui.units.UnitListScreen
import lt.skautai.android.util.NavRoutes
import lt.skautai.android.util.TokenManager

@Composable
fun AppNavGraph(
    navController: NavHostController,
    tokenManager: TokenManager,
    startDestination: String = NavRoutes.Login.route
) {
    val mainViewModel: MainViewModel = hiltViewModel()
    val permissions by tokenManager.permissions.collectAsState(initial = emptySet())

    val onLogout = {
        mainViewModel.logout {
            navController.navigate(NavRoutes.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
    val navigateBackToHome: () -> Unit = {
        val popped = navController.popBackStack()
        if (!popped) {
            navController.navigate(NavRoutes.Home.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoutes.Login.route) {
            LoginScreen(navController)
        }
        composable(NavRoutes.Register.route) {
            RegisterScreen(navController)
        }
        composable(NavRoutes.RegisterInvite.route) {
            RegisterInviteScreen(navController)
        }

        composable(NavRoutes.SuperAdminLogin.route) {
            SuperAdminLoginScreen(navController)
        }
        composable(NavRoutes.SuperAdminDashboard.route) {
            SuperAdminDashboardScreen()
        }

        composable(NavRoutes.Home.route) {
            MainScaffold(
                navController = navController,
                tokenManager = tokenManager,
                onLogout = onLogout
            ) {
                HomeScreen(navController = navController, permissions = permissions)
            }
        }

        composable(
            route = NavRoutes.InventoryList.route,
            arguments = listOf(
                navArgument("type") {
                    type = NavType.StringType
                    defaultValue = null
                    nullable = true
                },
                navArgument("category") {
                    type = NavType.StringType
                    defaultValue = null
                    nullable = true
                },
                navArgument("custodianId") {
                    type = NavType.StringType
                    defaultValue = null
                    nullable = true
                }
            )
        ) {
            val refreshReservations by it.savedStateHandle
                .getStateFlow("refreshReservations", false)
                .collectAsState()
            MainScaffold(
                navController = navController,
                tokenManager = tokenManager,
                onLogout = onLogout,
                showBackNavigation = true,
                onNavigateBack = navigateBackToHome,
                floatingActionButton = {
                    if ("items.create" in permissions) {
                        FloatingActionButton(
                            onClick = {
                                navController.navigate(NavRoutes.InventoryAddEdit.createRoute(mode = "SHARED"))
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Prideti daikta")
                        }
                    }
                }
            ) {
                InventoryListScreen(navController)
            }
        }

        composable(
            route = NavRoutes.ReservationList.route,
            arguments = listOf(
                navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = "all"
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val refreshReservations by backStackEntry.savedStateHandle
                .getStateFlow("refreshReservations", false)
                .collectAsState()
            MainScaffold(
                navController = navController,
                tokenManager = tokenManager,
                onLogout = onLogout
            ) {
                ReservationListScreen(
                    onReservationClick = { id ->
                        navController.navigate(NavRoutes.ReservationDetail.createRoute(id))
                    },
                      onCreateClick = {
                          navController.navigate(NavRoutes.ReservationCreate.route)
                      },
                      onModeClick = { mode ->
                          navController.navigate(NavRoutes.ReservationList.createRoute(mode = mode))
                      },
                      refreshSignal = refreshReservations,
                      onRefreshHandled = {
                          backStackEntry.savedStateHandle["refreshReservations"] = false
                      }
                  )
            }
        }

        composable(
            route = NavRoutes.RequestList.route,
            arguments = listOf(
                navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = "all"
                    nullable = true
                }
            )
        ) {
            MainScaffold(
                navController = navController,
                tokenManager = tokenManager,
                onLogout = onLogout
            ) {
                RequisitionListScreen(
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
                onLogout = onLogout
            ) {
                MemberListScreen(
                    onMemberClick = { userId ->
                        navController.navigate(NavRoutes.MemberDetail.createRoute(userId))
                    },
                    onInviteClick = {
                        navController.navigate(NavRoutes.InviteCreate.route)
                    },
                    onEmptyActionClick = {
                        navController.navigate(NavRoutes.Home.route) {
                            launchSingleTop = true
                        }
                    },
                    canInvite = "invitations.create" in permissions
                )
            }
        }

        composable(NavRoutes.UnitList.route) {
            MainScaffold(
                navController = navController,
                tokenManager = tokenManager,
                onLogout = onLogout,
                floatingActionButton = {
                    if ("organizational_units.manage" in permissions) {
                        FloatingActionButton(
                            onClick = { navController.navigate(NavRoutes.UnitCreate.route) }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Prideti vieneta")
                        }
                    }
                }
            ) {
                UnitListScreen(
                    onCreateClick = { navController.navigate(NavRoutes.UnitCreate.route) },
                    onUnitClick = { unitId ->
                        navController.navigate(NavRoutes.UnitDetail.createRoute(unitId))
                    }
                )
            }
        }

        composable(NavRoutes.EventList.route) {
            MainScaffold(
                navController = navController,
                tokenManager = tokenManager,
                onLogout = onLogout
            ) {
                EventListScreen(
                    onEventClick = { eventId ->
                        navController.navigate(NavRoutes.EventDetail.createRoute(eventId))
                    },
                    onCreateClick = {
                        navController.navigate(NavRoutes.EventAddEdit.createRoute(null))
                    }
                )
            }
        }

        composable(
            route = NavRoutes.InventoryDetail.route,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) {
            val itemId = it.arguments?.getString("itemId")!!
            InventoryDetailScreen(itemId = itemId, navController = navController)
        }

        composable(
            route = NavRoutes.InventoryAddEdit.route,
            arguments = listOf(
                navArgument("itemId") {
                    type = NavType.StringType
                    defaultValue = null
                    nullable = true
                },
                navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = null
                    nullable = true
                }
            )
        ) {
            val itemId = it.arguments?.getString("itemId")
            val mode = it.arguments?.getString("mode")
            InventoryAddEditScreen(itemId = itemId, mode = mode, navController = navController)
        }

        composable(NavRoutes.ReservationCreate.route) {
            ReservationCreateScreen(onBack = {
                navController.previousBackStackEntry?.savedStateHandle?.set("refreshReservations", true)
                navController.popBackStack()
            })
        }

        composable(
            route = NavRoutes.ReservationDetail.route,
            arguments = listOf(navArgument("reservationId") { type = NavType.StringType })
        ) {
            val reservationId = it.arguments?.getString("reservationId")!!
            ReservationDetailScreen(
                reservationId = reservationId,
                onBack = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("refreshReservations", true)
                    navController.popBackStack()
                },
                onIssue = { navController.navigate(NavRoutes.ReservationMovement.createRoute(reservationId, "issue")) },
                onReturn = { navController.navigate(NavRoutes.ReservationMovement.createRoute(reservationId, "return")) },
                onMarkReturned = { navController.navigate(NavRoutes.ReservationMovement.createRoute(reservationId, "mark_returned")) }
            )
        }

        composable(
            route = NavRoutes.ReservationMovement.route,
            arguments = listOf(
                navArgument("reservationId") { type = NavType.StringType },
                navArgument("mode") { type = NavType.StringType }
            )
        ) {
            ReservationMovementScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = NavRoutes.RequestDetail.route,
            arguments = listOf(navArgument("requestId") { type = NavType.StringType })
        ) {
            val requestId = it.arguments?.getString("requestId")!!
            RequisitionDetailScreen(
                requestId = requestId,
                onBack = navigateBackToHome
            )
        }

        composable(NavRoutes.RequestCreate.route) {
            RequisitionCreateScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.SharedRequestList.route) {
            MainScaffold(
                navController = navController,
                tokenManager = tokenManager,
                onLogout = onLogout
            ) {
                RequestListScreen(
                    onRequestClick = { id ->
                        navController.navigate(NavRoutes.SharedRequestDetail.createRoute(id))
                    },
                    onCreateClick = {
                        navController.navigate(NavRoutes.InventoryList.createRoute())
                    }
                )
            }
        }

        composable(
            route = NavRoutes.SharedRequestDetail.route,
            arguments = listOf(navArgument("requestId") { type = NavType.StringType })
        ) {
            val requestId = it.arguments?.getString("requestId")!!
            RequestDetailScreen(
                requestId = requestId,
                onBack = navigateBackToHome
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
            InviteCreateScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.InviteAccept.route) {
            MainScaffold(
                navController = navController,
                tokenManager = tokenManager,
                onLogout = onLogout
            ) {
                InviteAcceptScreen()
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
            UnitEditScreen(unitId = unitId, onBack = { navController.popBackStack() })
        }

        composable(
            route = NavRoutes.EventDetail.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) {
            val eventId = it.arguments?.getString("eventId")!!
            EventDetailScreen(
                eventId = eventId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.EventAddEdit.route,
            arguments = listOf(navArgument("eventId") {
                type = NavType.StringType
                defaultValue = null
                nullable = true
            })
        ) {
            EventCreateScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.TuntasSelect.route) {
            TuntasSelectScreen(navController)
        }

        composable(NavRoutes.SyncStatus.route) {
            MainScaffold(
                navController = navController,
                tokenManager = tokenManager,
                onLogout = onLogout,
                showBackNavigation = true,
                onNavigateBack = { navController.popBackStack() }
            ) {
                SyncStatusScreen()
            }
        }
    }
}
