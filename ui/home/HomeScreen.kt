package lt.skautai.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiHeroCard
import lt.skautai.android.ui.common.SkautaiSectionHeader
import lt.skautai.android.util.LithuanianNameVocativeFormatter
import lt.skautai.android.util.NavRoutes

@Composable
fun HomeScreen(
    navController: NavController,
    permissions: Set<String> = emptySet(),
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val canCreateItems = "items.create" in permissions
    val canApproveInventory = "items.transfer" in permissions

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (uiState.isLoading && uiState.availableUnits.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val hasPendingApprovals = canApproveInventory && uiState.sharedPendingApprovalCount > 0
    val hasAssignedReservations = uiState.assignedReservationCount > 0
    val hasAssignedRequisitions = uiState.assignedRequisitionCount > 0
    val hasAnyAction = hasPendingApprovals || hasAssignedReservations || hasAssignedRequisitions

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeroCard(
                userName = LithuanianNameVocativeFormatter.firstNameVocative(userName),
                onManageTuntai = { navController.navigate(NavRoutes.TuntasSelect.route) }
            )
        }

        if (uiState.availableUnits.size > 1) {
            item {
                SkautaiSectionHeader(title = "Aktyvus vienetas")
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.availableUnits, key = { it.id }) { unit ->
                        UnitChip(
                            unit = unit,
                            selected = uiState.activeUnitId == unit.id,
                            onClick = { viewModel.selectActiveUnit(unit.id) }
                        )
                    }
                }
            }
        }

        if (hasAnyAction) {
            item {
                SkautaiSectionHeader(title = "Reikalauja demesio")
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (hasPendingApprovals) {
                        ActionTile(
                            title = "Laukia tavo patvirtinimo",
                            count = uiState.sharedPendingApprovalCount,
                            subtitle = "Inventoriaus irasu patvirtinimas",
                            icon = Icons.Default.PendingActions,
                            tone = MaterialTheme.colorScheme.primaryContainer,
                            onClick = { navController.navigate(NavRoutes.InventoryList.createRoute()) }
                        )
                    }
                    if (hasAssignedReservations) {
                        ActionTile(
                            title = "Rezervacijos, laukiancios sprendimo",
                            count = uiState.assignedReservationCount,
                            subtitle = "Patvirtink arba atmest",
                            icon = Icons.Default.EventAvailable,
                            tone = MaterialTheme.colorScheme.secondaryContainer,
                            onClick = { navController.navigate(NavRoutes.ReservationList.createRoute(mode = "assigned")) }
                        )
                    }
                    if (hasAssignedRequisitions) {
                        ActionTile(
                            title = "Prasymai, laukiantys sprendimo",
                            count = uiState.assignedRequisitionCount,
                            subtitle = "Pirkimo ir papildymo prasymai",
                            icon = Icons.Default.Assignment,
                            tone = MaterialTheme.colorScheme.tertiaryContainer,
                            onClick = { navController.navigate(NavRoutes.RequestList.createRoute(mode = "assigned")) }
                        )
                    }
                }
            }
        }

        item {
            SkautaiSectionHeader(title = "Inventorius")
        }

        item {
            InventoryScopeGrid(
                activeUnitId = uiState.activeUnitId,
                activeUnitName = uiState.activeUnitName,
                activeUnitItemCount = uiState.activeUnitItemCount,
                sharedInventoryCount = uiState.sharedInventoryCount,
                personalLendingCount = uiState.personalLendingCount,
                canCreateItems = canCreateItems,
                onOpenUnit = {
                    navController.navigate(NavRoutes.InventoryList.createRoute(custodianId = uiState.activeUnitId))
                },
                onOpenShared = {
                    navController.navigate(NavRoutes.InventoryList.createRoute())
                },
                onOpenPersonal = {
                    navController.navigate(NavRoutes.InventoryList.createRoute(type = "INDIVIDUAL"))
                },
                onOpenAll = {
                    navController.navigate(NavRoutes.InventoryList.createRoute())
                },
                onAddToUnit = {
                    navController.navigate(NavRoutes.InventoryAddEdit.createRoute(mode = "UNIT_OWN"))
                },
                onAddToShared = {
                    navController.navigate(NavRoutes.InventoryAddEdit.createRoute(mode = "SHARED"))
                },
                onAddPersonal = {
                    navController.navigate(NavRoutes.InventoryAddEdit.createRoute(mode = "PERSONAL"))
                }
            )
        }

        item {
            SkautaiSectionHeader(
                title = "Rezervacijos",
                actionLabel = "Visos",
                onAction = { navController.navigate(NavRoutes.ReservationList.createRoute()) }
            )
        }

        item {
            ActionTile(
                title = "Mano rezervacijos",
                count = uiState.myReservationCount,
                subtitle = "Tavo aktyvios rezervacijos",
                icon = Icons.Default.EventAvailable,
                tone = MaterialTheme.colorScheme.primaryContainer,
                onClick = { navController.navigate(NavRoutes.ReservationList.createRoute(mode = "my_active")) }
            )
        }

        item {
            SkautaiSectionHeader(
                title = "Prasymai",
                actionLabel = "Visi",
                onAction = { navController.navigate(NavRoutes.RequestList.createRoute()) }
            )
        }

        item {
            ActionTile(
                title = "Mano prasymai",
                count = uiState.myRequisitionCount,
                subtitle = "Kuriuos pats pateikei",
                icon = Icons.Default.Inbox,
                tone = MaterialTheme.colorScheme.primaryContainer,
                onClick = { navController.navigate(NavRoutes.RequestList.createRoute(mode = "my_active")) }
            )
        }
    }
}

@Composable
private fun HeroCard(userName: String, onManageTuntai: () -> Unit) {
    SkautaiHeroCard(
        title = "Labas, $userName",
        subtitle = "Greita tavo vieneto, tunto ir asmeninio inventoriaus apzvalga.",
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = onManageTuntai
        ) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Keisti tunta")
        }
    }
}

@Composable
private fun ActionTile(
    title: String,
    count: Int,
    subtitle: String,
    icon: ImageVector,
    tone: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SkautaiCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        tonal = tone
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.56f),
                contentColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(
                    modifier = Modifier.size(46.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "$count",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun InventoryScopeGrid(
    activeUnitId: String?,
    activeUnitName: String?,
    activeUnitItemCount: Int,
    sharedInventoryCount: Int,
    personalLendingCount: Int,
    canCreateItems: Boolean,
    onOpenUnit: () -> Unit,
    onOpenShared: () -> Unit,
    onOpenPersonal: () -> Unit,
    onOpenAll: () -> Unit,
    onAddToUnit: () -> Unit,
    onAddToShared: () -> Unit,
    onAddPersonal: () -> Unit
) {
    val tiles = buildList {
        if (activeUnitId != null) {
            add(
                ScopeTile(
                    title = activeUnitName ?: "Mano vienetas",
                    count = activeUnitItemCount,
                    icon = Icons.Default.Group,
                    tone = MaterialTheme.colorScheme.primaryContainer,
                    onOpen = onOpenUnit,
                    showAdd = canCreateItems,
                    onAdd = onAddToUnit
                )
            )
        }
        add(
            ScopeTile(
                title = "Tunto bendras",
                count = sharedInventoryCount,
                icon = Icons.Default.Flag,
                tone = MaterialTheme.colorScheme.tertiaryContainer,
                onOpen = onOpenShared,
                showAdd = canCreateItems,
                onAdd = onAddToShared
            )
        )
        add(
            ScopeTile(
                title = "Mano asmeniniai",
                count = personalLendingCount,
                icon = Icons.Default.Person,
                tone = MaterialTheme.colorScheme.surfaceVariant,
                onOpen = onOpenPersonal,
                showAdd = canCreateItems,
                onAdd = onAddPersonal
            )
        )
        add(
            ScopeTile(
                title = "Visas inventorius",
                count = null,
                icon = Icons.Default.Inventory2,
                tone = MaterialTheme.colorScheme.primaryContainer,
                onOpen = onOpenAll,
                showAdd = false,
                onAdd = {}
            )
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { tile ->
                    ScopeTileCard(tile = tile, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class ScopeTile(
    val title: String,
    val count: Int?,
    val icon: ImageVector,
    val tone: Color,
    val onOpen: () -> Unit,
    val showAdd: Boolean,
    val onAdd: () -> Unit
)

@Composable
private fun ScopeTileCard(tile: ScopeTile, modifier: Modifier = Modifier) {
    SkautaiCard(
        modifier = modifier.aspectRatio(1f),
        onClick = tile.onOpen,
        tonal = tile.tone
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.48f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(34.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tile.icon,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (tile.count != null) {
                        Text(
                            text = "${tile.count}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = tile.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (tile.showAdd) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp),
                    onClick = tile.onAdd
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Prideti",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitChip(
    unit: OrganizationalUnitDto,
    selected: Boolean,
    onClick: () -> Unit
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    SkautaiCard(onClick = onClick, tonal = container) {
        Text(
            text = unit.name,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            color = content,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
