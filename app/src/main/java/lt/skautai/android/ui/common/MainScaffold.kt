package lt.skautai.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch
import lt.skautai.android.util.NavRoutes
import lt.skautai.android.util.canManageLocations
import lt.skautai.android.util.canViewMembers
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
    val drawerScrollState = rememberScrollState()

    val visibleNavItems = BottomNavItem.all.filter { item ->
        shouldShowBottomNavItem(item, permissions)
    }

    val quickAccessItems = buildList {
        add(
            DrawerNavItem(
                label = "Pradžia",
                icon = Icons.Default.Home,
                selected = currentRoute == NavRoutes.Home.route,
                onClick = { navController.navigate(NavRoutes.Home.route) }
            )
        )
        add(
            DrawerNavItem(
                label = "Inventorius",
                icon = Icons.Default.Inventory2,
                selected = currentRoute == NavRoutes.InventoryList.route,
                onClick = { navController.navigate(NavRoutes.InventoryList.createRoute()) }
            )
        )
        add(
            DrawerNavItem(
                label = "Rezervacijos",
                icon = Icons.Default.EventAvailable,
                selected = currentRoute == NavRoutes.ReservationList.route,
                onClick = { navController.navigate(NavRoutes.ReservationList.createRoute()) }
            )
        )
        add(
            DrawerNavItem(
                label = "Pirkimai",
                icon = Icons.Default.ShoppingCart,
                selected = currentRoute == NavRoutes.RequestList.route,
                onClick = { navController.navigate(NavRoutes.RequestList.createRoute()) }
            )
        )
        add(
            DrawerNavItem(
                label = "Paėmimai",
                icon = Icons.Default.Inbox,
                selected = currentRoute == NavRoutes.SharedRequestList.route,
                onClick = { navController.navigate(NavRoutes.SharedRequestList.route) }
            )
        )
    }

    val managementItems = buildList {
        if (permissions.canManageLocations()) {
            add(
                DrawerNavItem(
                    label = "Lokacijos",
                    icon = Icons.Default.Place,
                    selected = currentRoute == NavRoutes.LocationList.route,
                    onClick = { navController.navigate(NavRoutes.LocationList.route) }
                )
            )
        }
        add(
            DrawerNavItem(
                label = "Kvietimai",
                icon = Icons.Default.MarkEmailUnread,
                selected = currentRoute == NavRoutes.InviteAccept.route,
                onClick = { navController.navigate(NavRoutes.InviteAccept.route) }
            )
        )

        if (permissions.canViewMembers()) {
            add(
                DrawerNavItem(
                    label = "Nariai",
                    icon = BottomNavItem.Members.icon,
                    selected = currentRoute == NavRoutes.MemberList.route,
                    onClick = { navController.navigate(NavRoutes.MemberList.route) }
                )
            )
        }

        add(
            DrawerNavItem(
                label = "Vienetai",
                icon = Icons.Default.AccountTree,
                selected = currentRoute == NavRoutes.UnitList.route,
                onClick = { navController.navigate(NavRoutes.UnitList.route) }
            )
        )
    }

    val accountItems = buildList {
        add(
            DrawerNavItem(
                label = "Profilis",
                icon = Icons.Default.Person,
                selected = currentRoute == NavRoutes.Profile.route,
                onClick = { navController.navigate(NavRoutes.Profile.route) }
            )
        )
        add(
            DrawerNavItem(
                label = if (activeTuntasId != null) "Keisti tuntą" else "Pasirinkti tuntą",
                icon = Icons.Default.SwapHoriz,
                selected = currentRoute == NavRoutes.TuntasSelect.route,
                onClick = { navController.navigate(NavRoutes.TuntasSelect.route) }
            )
        )

        if (syncStatus.failedCount > 0) {
            add(
                DrawerNavItem(
                    label = "Bandyti sinchronizaciją dar kartą",
                    icon = Icons.Default.SwapHoriz,
                    selected = false,
                    onClick = { pendingSyncViewModel.retryFailed() }
                )
            )
        }
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
                        .verticalScroll(drawerScrollState)
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
                                text = "Skautų Inventorius",
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

                    DrawerSection(
                        title = "Greita prieiga",
                        items = quickAccessItems,
                        onItemClick = { action ->
                            scope.launch { drawerState.close() }
                            action()
                        }
                    )

                    if (managementItems.isNotEmpty()) {
                        DrawerSection(
                            title = "Valdymas",
                            items = managementItems,
                            onItemClick = { action ->
                                scope.launch { drawerState.close() }
                                action()
                            }
                        )
                    }

                    DrawerSection(
                        title = "Paskyra",
                        items = accountItems,
                        onItemClick = { action ->
                            scope.launch { drawerState.close() }
                            action()
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))

                    NavigationDrawerItem(
                        label = { Text("Atsijungti") },
                        icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onLogout()
                        },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
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
                Column {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
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
                                            val isFromBottomNavRoute = BottomNavItem.all.any { it.route == currentRoute }
                                            navController.navigate(destination) {
                                                popUpTo(NavRoutes.Home.route) {
                                                    saveState = isFromBottomNavRoute
                                                    inclusive = false
                                                }
                                                launchSingleTop = true
                                                restoreState = isFromBottomNavRoute
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
            "${status.failedCount} ${syncErrorLabel(status.failedCount)}",
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
            "Interneto nėra",
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

private fun syncErrorLabel(count: Int): String = when {
    count % 10 == 1 && count % 100 != 11 -> "sinchronizacijos klaida"
    count % 10 in 2..9 && count % 100 !in 12..19 -> "sinchronizacijos klaidos"
    else -> "sinchronizacijos klaidų"
}

private fun currentRouteTitle(currentRoute: String?): String = when (currentRoute) {
    NavRoutes.Home.route -> "Pradžia"
    NavRoutes.InventoryList.route -> "Inventorius"
    NavRoutes.InventoryDetail.route -> "Daikto informacija"
    NavRoutes.InventoryQrScanner.route -> "QR skenavimas"
    NavRoutes.InventoryAddEdit.route -> "Inventoriaus forma"
    NavRoutes.ReservationList.route -> "Rezervacijos"
    NavRoutes.ReservationCreate.route -> "Nauja rezervacija"
    NavRoutes.ReservationDetail.route -> "Rezervacijos informacija"
    NavRoutes.ReservationMovement.route -> "Išdavimas ir grąžinimas"
    NavRoutes.RequestList.route -> "Pirkimai"
    NavRoutes.RequestDetail.route -> "Pirkimo informacija"
    NavRoutes.RequestCreate.route -> "Naujas pirkimas"
    NavRoutes.InviteCreate.route -> "Naujas kvietimas"
    NavRoutes.InviteAccept.route -> "Kvietimai"
    NavRoutes.Profile.route -> "Mano profilis"
    NavRoutes.SharedRequestList.route -> "Paėmimai"
    NavRoutes.SharedRequestDetail.route -> "Paėmimo informacija"
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

@Composable
private fun DrawerSection(
    title: String,
    items: List<DrawerNavItem>,
    onItemClick: ((() -> Unit)) -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items.forEachIndexed { index, item ->
                NavigationDrawerItem(
                    label = { Text(item.label) },
                    icon = { Icon(item.icon, contentDescription = null) },
                    selected = item.selected,
                    onClick = { onItemClick(item.onClick) },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                if (index != items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.size(4.dp))
}

private data class DrawerNavItem(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit
)

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
