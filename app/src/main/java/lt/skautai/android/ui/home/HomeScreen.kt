package lt.skautai.android.ui.home

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiSectionHeader
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.SkautaiStatusTone
import lt.skautai.android.ui.common.SkautaiSummaryCard
import lt.skautai.android.ui.common.SkautaiSurfaceRole
import lt.skautai.android.ui.common.skautaiSurfaceTone
import lt.skautai.android.util.LithuanianNameVocativeFormatter
import lt.skautai.android.util.NavRoutes
import lt.skautai.android.util.canCreateItems
import lt.skautai.android.util.canManageLocations
import lt.skautai.android.util.canManageSharedInventory

@Composable
fun HomeScreen(
    navController: NavController,
    permissions: Set<String> = emptySet(),
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val canCreateItems = permissions.canCreateItems()
    val canApproveInventory = permissions.canManageSharedInventory()
    val canManageLocations = permissions.canManageLocations()

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

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh(force = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            OverviewCard(
                uiState = uiState,
                userName = LithuanianNameVocativeFormatter.firstNameVocative(userName),
                onManageTuntai = { navController.navigate(NavRoutes.TuntasSelect.route) }
            )
        }

        if (uiState.availableUnits.size > 1) {
            item {
                SkautaiSectionHeader(
                    title = "Aktyvus vienetas",
                    subtitle = "Perjunk kontekstą tarp vienetų, kuriems priklausai."
                )
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
                SkautaiSectionHeader(
                    title = "Reikalauja demesio",
                    subtitle = "Svarbiausi veiksmai, kuriuos verta atlikti pirmiausia."
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (hasPendingApprovals) {
                        ActionTile(
                            title = "Laukia tavo patvirtinimo",
                            count = uiState.sharedPendingApprovalCount,
                            subtitle = "Inventoriaus irasu patvirtinimas",
                            icon = Icons.Default.PendingActions,
                            tone = MaterialTheme.colorScheme.surfaceBright,
                            badgeTone = SkautaiStatusTone.Warning,
                            onClick = { navController.navigate(NavRoutes.InventoryList.createRoute()) }
                        )
                    }
                    if (hasAssignedReservations) {
                        ActionTile(
                            title = "Rezervacijos, laukiancios sprendimo",
                            count = uiState.assignedReservationCount,
                            subtitle = "Patvirtink arba atmest",
                            icon = Icons.Default.EventAvailable,
                            tone = MaterialTheme.colorScheme.surfaceBright,
                            badgeTone = SkautaiStatusTone.Warning,
                            onClick = { navController.navigate(NavRoutes.ReservationList.createRoute(mode = "assigned")) }
                        )
                    }
                    if (hasAssignedRequisitions) {
                        ActionTile(
                            title = "Prašymai, laukiantys sprendimo",
                            count = uiState.assignedRequisitionCount,
                            subtitle = "Pirkimo ir papildymo prašymai",
                            icon = Icons.Default.Assignment,
                            tone = MaterialTheme.colorScheme.surfaceBright,
                            badgeTone = SkautaiStatusTone.Warning,
                            onClick = { navController.navigate(NavRoutes.RequestList.createRoute(mode = "assigned")) }
                        )
                    }
                }
            }
        }

        item {
            SkautaiSectionHeader(
                title = "Inventorius",
                subtitle = "Greita prieiga prie tunto, vieneto ir asmeninio inventoriaus."
            )
        }

        item {
            InventoryScopeColumn(
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

        if (canManageLocations) {
            item {
                SkautaiSectionHeader(
                    title = "Lokacijos",
                    subtitle = "Greita prieiga prie lokacijų katalogo ir sublokacijų."
                )
            }

            item {
                ActionTile(
                    title = "Atidaryti lokacijas",
                    count = null,
                    subtitle = "Peržiūrėk lokacijų katalogą ir kurk naujas sublokacijas.",
                    icon = Icons.Default.Place,
                    tone = MaterialTheme.colorScheme.surfaceBright,
                    badgeTone = SkautaiStatusTone.Neutral,
                    onClick = { navController.navigate(NavRoutes.LocationList.route) }
                )
            }
        }

        item {
            SkautaiSectionHeader(
                title = "Rezervacijos",
                subtitle = "Sek savo aktyvias rezervacijas ir greitai pereik prie sarasu.",
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
                tone = MaterialTheme.colorScheme.surfaceBright,
                badgeTone = SkautaiStatusTone.Info,
                onClick = { navController.navigate(NavRoutes.ReservationList.createRoute(mode = "my_active")) }
            )
        }

        item {
            SkautaiSectionHeader(
                title = "Prašymai",
                subtitle = "Pirkimo, papildymo ir paėmimo užklausos vienoje vietoje."
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionTile(
                    title = "Mano pirkimo prašymai",
                    count = uiState.myRequisitionCount,
                    subtitle = "Kuriuos pats pateikei",
                    icon = Icons.Default.Assignment,
                    tone = MaterialTheme.colorScheme.surfaceBright,
                    badgeTone = SkautaiStatusTone.Info,
                    onClick = { navController.navigate(NavRoutes.RequestList.createRoute(mode = "my_active")) }
                )
                ActionTile(
                    title = "Paėmimo prašymai",
                    count = null,
                    subtitle = "Paimti esamus daiktųs is bendro tunto inventoriaus",
                    icon = Icons.Default.Inbox,
                    tone = MaterialTheme.colorScheme.surfaceBright,
                    badgeTone = SkautaiStatusTone.Info,
                    onClick = { navController.navigate(NavRoutes.SharedRequestList.route) }
                )
            }
        }
    }
}

@Composable
private fun OverviewCard(
    uiState: HomeUiState,
    userName: String,
    onManageTuntai: () -> Unit
) {
    val actionCount = uiState.sharedPendingApprovalCount +
        uiState.assignedReservationCount +
        uiState.assignedRequisitionCount
    SkautaiSummaryCard(
        eyebrow = "Pagrindinė apžvalga",
        title = "Labas, $userName",
        subtitle = "Svarbiausi inventoriaus, rezervacijų ir prašymų srautai vienoje vietoje.",
        metrics = listOf(
            "Veiksmai" to actionCount.toString(),
            "Vieneto daiktai" to uiState.activeUnitItemCount.toString(),
            "Bendri daiktai" to uiState.sharedInventoryCount.toString()
        ),
        foresty = true,
        modifier = Modifier.fillMaxWidth()
    ) {
        FilledTonalButton(
            onClick = onManageTuntai,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = "Keisti tunta",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ActionTile(
    title: String,
    count: Int?,
    subtitle: String,
    icon: ImageVector,
    tone: Color,
    badgeTone: SkautaiStatusTone,
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
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
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
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (count != null) {
                    SkautaiStatusPill(label = "$count", tone = badgeTone)
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InventoryScopeColumn(
    activeUnitId: String?,
    activeUnitName: String?,
    activeUnitItemCount: Int,
    sharedInventoryCount: Int,
    personalLendingCount: Int,
    canCreateItems: Boolean,
    onOpenUnit: () -> Unit,
    onOpenShared: () -> Unit,
    onOpenPersonal: () -> Unit,
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
                    tone = skautaiSurfaceTone(SkautaiSurfaceRole.Identity),
                    toneLabel = "Vienetas",
                    onOpen = onOpenUnit,
                    showAdd = canCreateItems,
                    onAdd = onAddToUnit
                )
            )
        }
        add(
            ScopeTile(
                title = "Tunto bendras inventorius",
                count = sharedInventoryCount,
                icon = Icons.Default.Flag,
                tone = MaterialTheme.colorScheme.secondaryContainer,
                toneLabel = "Bendras",
                onOpen = onOpenShared,
                showAdd = canCreateItems,
                onAdd = onAddToShared
            )
        )
        if (canCreateItems) {
            add(
                ScopeTile(
                    title = "Mano asmeniniai daiktai",
                    count = personalLendingCount,
                    icon = Icons.Default.Person,
                    tone = MaterialTheme.colorScheme.surfaceContainerLow,
                    toneLabel = "Asmeninis",
                    onOpen = onOpenPersonal,
                    showAdd = true,
                    onAdd = onAddPersonal
                )
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tiles.forEach { tile ->
            ScopeTileCard(tile = tile)
        }
    }
}

private data class ScopeTile(
    val title: String,
    val count: Int?,
    val icon: ImageVector,
    val tone: Color,
    val toneLabel: String,
    val onOpen: () -> Unit,
    val showAdd: Boolean,
    val onAdd: () -> Unit
)

@Composable
private fun ScopeTileCard(tile: ScopeTile, modifier: Modifier = Modifier) {
    SkautaiCard(
        modifier = modifier.fillMaxWidth(),
        onClick = tile.onOpen,
        tonal = tile.tone
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                contentColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tile.icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = tile.toneLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = tile.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tile.count?.let { "$it irasu" } ?: "Peržiūrėti visą katalogą",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (tile.showAdd) {
                    Surface(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp),
                        onClick = tile.onAdd
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Pridėti",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
