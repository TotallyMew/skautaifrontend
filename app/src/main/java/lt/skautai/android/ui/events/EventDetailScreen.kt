package lt.skautai.android.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiErrorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onOpenMovement: (String) -> Unit,
    onOpenPastovykleLeader: (String) -> Unit,
    onOpenPastovyklės: (String) -> Unit,
    onOpenNeeds: (String) -> Unit,
    onOpenUkvedys: (String) -> Unit,
    onOpenPurchases: (String) -> Unit,
    onOpenReconciliation: (String) -> Unit,
    onOpenPlan: (String) -> Unit,
    onOpenStaff: (String) -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCancelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    LaunchedEffect((uiState as? EventDetailUiState.Success)?.error) {
        (uiState as? EventDetailUiState.Success)?.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Atšaukti renginį") },
            text = { Text("Ar tikrai? Visi suplanuoti poreikiai ir pirkimai liks istorijoje, bet renginys bus pažymėtas kaip atšauktas.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        viewModel.cancelEvent(eventId, onSuccess = onBack)
                    }
                ) {
                    Text("Atšaukti", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Uždaryti")
                }
            }
        )
    }

    EventScreenScaffold(
        title = "Renginys",
        onBack = onBack,
        snackbarHostState = snackbarHostState
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is EventDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is EventDetailUiState.Error -> {
                    SkautaiErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadEvent(eventId) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is EventDetailUiState.Success -> {
                    val readOnly = isEventReadOnlyStatus(state.event.status)
                    val myRoles = state.event.eventRoles
                        .filter { it.userId == state.currentUserId }
                        .map { it.role }
                        .toSet()
                    val canManage = !readOnly && ("events.manage" in permissions || "VIRSININKAS" in myRoles)
                    val canStart = !readOnly && ("events.manage" in permissions || myRoles.any { it in setOf("VIRSININKAS", "KOMENDANTAS") })
                    val canInventory = "events.manage" in permissions ||
                        "events.inventory.distribute" in permissions ||
                        myRoles.any { it in setOf("VIRSININKAS", "KOMENDANTAS", "UKVEDYS") }
                    val myPastovyklės = state.pastovykles
                        .filter { it.responsibleUserId == state.currentUserId }

                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            EventHeader(
                                event = state.event,
                                isCancelling = state.isCancelling,
                                canManage = canManage,
                                canStart = canStart,
                                onEdit = { onEdit(eventId) },
                                onActivate = { viewModel.updateStatus(eventId, "ACTIVE") },
                                onComplete = { viewModel.updateStatus(eventId, "WRAP_UP") },
                                onCancel = { showCancelDialog = true }
                            )
                        }

                        if (myPastovyklės.isNotEmpty()) {
                            item {
                                EventDetailSection(
                                    title = "Mano atsakomybės",
                                    subtitle = "Pastovyklės, kurias valdai šiame renginyje."
                                ) {
                                    PastovykleLeaderEntryCard(
                                        count = myPastovyklės.size,
                                        onOpen = { onOpenPastovykleLeader(eventId) }
                                    )
                                }
                            }
                        }

                        item {
                            EventDetailSection(
                                title = "Darbo sritys",
                                subtitle = "Planavimas, inventorius ir komanda vienoje vietoje."
                            ) {
                                val workAreas = buildList {
                                    if (canInventory) {
                                        add(EventWorkArea(Icons.Default.SwapHoriz, "Judėjimas", "Išdavimas ir grąžinimas") { onOpenMovement(eventId) })
                                        add(EventWorkArea(Icons.Default.Checklist, "Poreikiai", "Greitas kūrimas") { onOpenNeeds(eventId) })
                                        add(
                                            EventWorkArea(
                                                Icons.AutoMirrored.Filled.Assignment,
                                                "Planas",
                                                state.event.inventorySummary?.let {
                                                    "${it.totalAvailableQuantity}/${it.totalPlannedQuantity} aprūpinta"
                                                } ?: "Eilutės ir atsakingi"
                                            ) { onOpenPlan(eventId) }
                                        )
                                        add(EventWorkArea(Icons.Default.ShoppingCart, "Pirkimai", "Būsena ir sąskaitos") { onOpenPurchases(eventId) })
                                        if (state.event.status in listOf("WRAP_UP", "COMPLETED")) {
                                            add(EventWorkArea(Icons.Default.Inventory2, "Suvedimas", "Grąžinimai ir pirkimai") { onOpenReconciliation(eventId) })
                                        }
                                        add(EventWorkArea(Icons.Default.Inventory2, "Ūkvedys", "Trūkumai ir atsargos") { onOpenUkvedys(eventId) })
                                    }
                                    if (canManage || canInventory) {
                                        add(EventWorkArea(Icons.Default.Groups, "Pastovyklės", "Grupės ir vadovai") { onOpenPastovyklės(eventId) })
                                    }
                                    if (canManage) {
                                        add(EventWorkArea(Icons.Default.Groups, "Štabas", "${state.event.eventRoles.size} nariai") { onOpenStaff(eventId) })
                                    }
                                }
                                EventWorkAreaGrid(workAreas = workAreas)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class EventWorkArea(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

@Composable
private fun EventWorkAreaGrid(workAreas: List<EventWorkArea>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        workAreas.chunked(2).forEach { rowItems ->
            EventNavTileRow {
                rowItems.forEach { area ->
                    EventSectionNavCard(
                        icon = area.icon,
                        title = area.title,
                        subtitle = area.subtitle,
                        onClick = area.onClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(132.dp)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .height(132.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EventSectionNavCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EventDetailNavTile(
        icon = icon,
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        onClick = onClick
    )
}

@Composable
private fun PastovykleLeaderEntryCard(count: Int, onOpen: () -> Unit) {
    SkautaiCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen,
        tonal = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = if (count == 1) "Mano pastovyklė" else "Mano pastovyklės ($count)",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("Atidaryti", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun EventNavTileRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

