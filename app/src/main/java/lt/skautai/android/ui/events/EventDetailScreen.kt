package lt.skautai.android.ui.events

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiErrorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onOpenMovement: (String) -> Unit,
    onOpenPastovykleLeader: (String) -> Unit,
    onOpenNeeds: (String) -> Unit,
    onOpenUkvedys: (String) -> Unit,
    onOpenPurchases: (String) -> Unit,
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
            title = { Text("Atsaukti rengini") },
            text = { Text("Ar tikrai? Visi suplanuoti poreikiai ir pirkimai liks istorijoje, bet renginys bus pazymetas kaip atsauktas.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        viewModel.cancelEvent(eventId, onSuccess = onBack)
                    }
                ) {
                    Text("Atsaukti", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Uzdaryti")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Renginys") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atgal")
                    }
                }
            )
        },
        snackbarHost = { SkautaiErrorSnackbarHost(hostState = snackbarHostState) }
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
                    val myRoles = state.event.eventRoles
                        .filter { it.userId == state.currentUserId }
                        .map { it.role }
                        .toSet()
                    val canManage = "events.manage" in permissions || "VIRSININKAS" in myRoles
                    val canStart = "events.manage" in permissions || myRoles.any { it in setOf("VIRSININKAS", "KOMENDANTAS") }
                    val canInventory = "events.inventory.distribute" in permissions || myRoles.any { it in setOf("VIRSININKAS", "KOMENDANTAS", "UKVEDYS") }
                    val myPastovykles = state.event.eventRoles
                        .filter { it.userId == state.currentUserId && it.role == "PASTOVYKLE_LEADER" }

                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            EventHeader(
                                event = state.event,
                                isCancelling = state.isCancelling,
                                canManage = canManage,
                                canStart = canStart,
                                onEdit = { onEdit(eventId) },
                                onActivate = { viewModel.updateStatus(eventId, "ACTIVE") },
                                onComplete = { viewModel.updateStatus(eventId, "COMPLETED") },
                                onCancel = { showCancelDialog = true }
                            )
                        }
                        item {
                            MovementEntryCard(onOpenMovement = { onOpenMovement(eventId) })
                        }
                        if (myPastovykles.isNotEmpty()) {
                            item {
                                PastovykleLeaderEntryCard(
                                    count = myPastovykles.size,
                                    onOpen = { onOpenPastovykleLeader(eventId) }
                                )
                            }
                        }
                        item {
                            EventSectionNavCard(
                                icon = Icons.Default.Checklist,
                                title = "Poreikiai",
                                subtitle = state.event.inventorySummary?.let {
                                    "${it.totalPlannedQuantity} vnt. planuota, ${it.totalShortageQuantity} truksta"
                                } ?: "Inventoriaus poreikiai",
                                onClick = { onOpenNeeds(eventId) }
                            )
                        }
                        if (canInventory) {
                            item {
                                EventSectionNavCard(
                                    icon = Icons.Default.Inventory2,
                                    title = "Ukvedys",
                                    subtitle = "Buckets, paskirstymai, pastovykliu prasymai",
                                    onClick = { onOpenUkvedys(eventId) }
                                )
                            }
                        }
                        item {
                            EventSectionNavCard(
                                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                title = "Pirkimai",
                                subtitle = "Pirkimu busena ir saskaitu valdymas",
                                onClick = { onOpenPurchases(eventId) }
                            )
                        }
                        item {
                            EventSectionNavCard(
                                icon = Icons.AutoMirrored.Filled.Assignment,
                                title = "Inventoriaus planas",
                                subtitle = state.event.inventorySummary?.let {
                                    "Turima ${it.totalAvailableQuantity}/${it.totalPlannedQuantity} vnt., paskirstyta ${it.totalAllocatedQuantity}"
                                } ?: "Plano eilutes ir paskirstymas",
                                onClick = { onOpenPlan(eventId) }
                            )
                        }
                        item {
                            EventSectionNavCard(
                                icon = Icons.Default.Groups,
                                title = "Stabas",
                                subtitle = "${state.event.eventRoles.size} nariai",
                                onClick = { onOpenStaff(eventId) }
                            )
                        }
                    }
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
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MovementEntryCard(onOpenMovement: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = "Inventoriaus judejimas",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onOpenMovement) {
                Text("Atidaryti")
            }
        }
    }
}

@Composable
private fun PastovykleLeaderEntryCard(count: Int, onOpen: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = if (count == 1) "Mano pastovykle" else "Mano pastovyklės ($count)",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onOpen) {
                Text("Atidaryti")
            }
        }
    }
}
