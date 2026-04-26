package lt.skautai.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch
import lt.skautai.android.util.NavRoutes
import lt.skautai.android.util.TokenManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    navController: NavController,
    tokenManager: TokenManager,
    onLogout: () -> Unit,
    showBackNavigation: Boolean = false,
    onNavigateBack: (() -> Unit)? = null,
    topBarActions: @Composable (RowScope.() -> Unit) = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    val pendingSyncViewModel: PendingSyncViewModel = hiltViewModel()
    val syncStatus by pendingSyncViewModel.syncStatus.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val userName by tokenManager.userName.collectAsState(initial = "")
    val activeTuntasId by tokenManager.activeTuntasId.collectAsState(initial = null)
    val activeTuntasName by tokenManager.activeTuntasName.collectAsState(initial = null)
    val permissions by tokenManager.permissions.collectAsState(initial = emptySet())

    val topBarTitle = currentRouteTitle(currentRoute)

    val visibleNavItems = BottomNavItem.all.filter { item ->
        shouldShowBottomNavItem(item, permissions)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Skautu Inventorius",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (!userName.isNullOrBlank()) {
                                Text(
                                    text = userName ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                                )
                            }
                            Text(
                                text = activeTuntasName?.takeIf { it.isNotBlank() }
                                    ?: if (activeTuntasId != null) "Tuntas" else "Tuntas dar nepasirinktas",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Navigacija",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                NavigationDrawerItem(
                    label = { Text(if (activeTuntasId != null) "Keisti tunta" else "Pasirinkti tunta") },
                    icon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                    selected = currentRoute == NavRoutes.TuntasSelect.route,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(NavRoutes.TuntasSelect.route)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Kvietimai") },
                    icon = { Icon(Icons.Default.MarkEmailUnread, contentDescription = null) },
                    selected = currentRoute == NavRoutes.InviteAccept.route,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(NavRoutes.InviteAccept.route)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Inventorius") },
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                    selected = currentRoute == NavRoutes.InventoryList.route,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(NavRoutes.InventoryList.createRoute())
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Lokacijos") },
                    icon = { Icon(Icons.Default.Place, contentDescription = null) },
                    selected = currentRoute == NavRoutes.LocationList.route,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(NavRoutes.LocationList.route)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Rezervacijos") },
                    icon = { Icon(Icons.Default.EventAvailable, contentDescription = null) },
                    selected = currentRoute == NavRoutes.ReservationList.route,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(NavRoutes.ReservationList.createRoute())
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Pirkimai") },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                    selected = currentRoute == NavRoutes.RequestList.route,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(NavRoutes.RequestList.createRoute())
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Paemimai") },
                    icon = { Icon(Icons.Default.Inbox, contentDescription = null) },
                    selected = currentRoute == NavRoutes.SharedRequestList.route,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(NavRoutes.SharedRequestList.route)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                if ("members.view" in permissions) {
                    NavigationDrawerItem(
                        label = { Text("Nariai") },
                        icon = { Icon(BottomNavItem.Members.icon, contentDescription = null) },
                        selected = currentRoute == NavRoutes.MemberList.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(NavRoutes.MemberList.route)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                if (
                    "organizational_units.manage" in permissions ||
                    "unit.members.manage" in permissions ||
                    "unit.members.manage:ALL" in permissions ||
                    "unit.members.manage:OWN_UNIT" in permissions
                ) {
                    NavigationDrawerItem(
                        label = { Text("Vienetai") },
                        icon = { Icon(Icons.Default.AccountTree, contentDescription = null) },
                        selected = currentRoute == NavRoutes.UnitList.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(NavRoutes.UnitList.route)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                if (syncStatus.failedCount > 0) {
                    NavigationDrawerItem(
                        label = { Text("Bandyti sync dar karta") },
                        icon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            pendingSyncViewModel.retryFailed()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                NavigationDrawerItem(
                    label = { Text("Atsijungti") },
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onLogout()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(topBarTitle, style = MaterialTheme.typography.titleLarge)
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (showBackNavigation) {
                                    onNavigateBack?.invoke()
                                } else {
                                    scope.launch { drawerState.open() }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (showBackNavigation) {
                                    Icons.AutoMirrored.Filled.ArrowBack
                                } else {
                                    Icons.Default.Menu
                                },
                                contentDescription = if (showBackNavigation) "Atgal" else "Meniu"
                            )
                        }
                    },
                    actions = {
                        if (syncStatus.pendingCount > 0 || syncStatus.failedCount > 0 || syncStatus.isOffline) {
                            SyncStatusPill(
                                status = syncStatus,
                                onClick = { navController.navigate(NavRoutes.SyncStatus.route) }
                            )
                        }
                        topBarActions()
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    visibleNavItems.take(4).forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    val destination = bottomNavDestination(item)
                                    if (item.route == NavRoutes.Home.route) {
                                        val poppedToHome = navController.popBackStack(
                                            route = NavRoutes.Home.route,
                                            inclusive = false
                                        )
                                        if (!poppedToHome) {
                                            navController.navigate(NavRoutes.Home.route) {
                                                popUpTo(0) { inclusive = true }
                                                launchSingleTop = true
                                            }
                                        }
                                    } else {
                                        navController.navigate(destination) {
                                            popUpTo(NavRoutes.Home.route) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = {
                                Text(
                                    text = item.label,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                    )
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            },
            floatingActionButton = floatingActionButton
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                content()
            }
        }
    }
}

private fun bottomNavDestination(item: BottomNavItem): String = when (item) {
    BottomNavItem.Inventory -> NavRoutes.InventoryList.createRoute()
    BottomNavItem.Reservations -> NavRoutes.ReservationList.createRoute()
    else -> item.route
}

@Composable
private fun SyncStatusPill(
    status: PendingSyncStatus,
    onClick: () -> Unit
) {
    val (label, icon, containerColor, contentColor) = when {
        status.failedCount > 0 -> Quadruple(
            "${status.failedCount} sync klaid.",
            Icons.Default.SwapHoriz,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        status.pendingCount > 0 -> Quadruple(
            "${status.pendingCount} laukia",
            Icons.Default.SwapHoriz,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        status.isOffline -> Quadruple(
            "Offline",
            Icons.Default.CloudOff,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        else -> return
    }

    Surface(
        onClick = onClick,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun currentRouteTitle(currentRoute: String?): String = when (currentRoute) {
    NavRoutes.Home.route -> "Pradzia"
    NavRoutes.InventoryList.route -> "Inventorius"
    NavRoutes.InventoryDetail.route -> "Daikto informacija"
    NavRoutes.InventoryAddEdit.route -> "Inventoriaus forma"
    NavRoutes.ReservationList.route -> "Rezervacijos"
    NavRoutes.ReservationCreate.route -> "Nauja rezervacija"
    NavRoutes.ReservationDetail.route -> "Rezervacijos informacija"
    NavRoutes.ReservationMovement.route -> "Issdavimas ir grazinimas"
    NavRoutes.RequestList.route -> "Pirkimai"
    NavRoutes.RequestDetail.route -> "Pirkimo informacija"
    NavRoutes.RequestCreate.route -> "Naujas pirkimas"
    NavRoutes.InviteCreate.route -> "Naujas kvietimas"
    NavRoutes.InviteAccept.route -> "Kvietimai"
    NavRoutes.SharedRequestList.route -> "Paemimai"
    NavRoutes.SharedRequestDetail.route -> "Paemimo informacija"
    NavRoutes.MemberList.route -> "Nariai"
    NavRoutes.MemberDetail.route -> "Nario informacija"
    NavRoutes.UnitList.route -> "Vienetai"
    NavRoutes.UnitCreate.route -> "Naujas vienetas"
    NavRoutes.UnitDetail.route -> "Vieneto informacija"
    NavRoutes.UnitEdit.route -> "Vieneto redagavimas"
    NavRoutes.LocationList.route -> "Lokacijos"
    NavRoutes.LocationDetail.route -> "Lokacijos informacija"
    NavRoutes.LocationAddEdit.route -> "Lokacijos forma"
    NavRoutes.TuntasSelect.route -> "Tunto pasirinkimas"
    NavRoutes.EventList.route -> "Renginiai"
    NavRoutes.EventDetail.route -> "Renginio informacija"
    NavRoutes.EventAddEdit.route -> "Renginio forma"
    NavRoutes.SyncStatus.route -> "Sinchronizavimas"
    else -> ""
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
