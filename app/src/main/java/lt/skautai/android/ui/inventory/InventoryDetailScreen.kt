package lt.skautai.android.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.ui.common.MetadataRow
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiEmptyState
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.inventoryCategoryLabel
import lt.skautai.android.ui.common.inventoryTypeLabel
import lt.skautai.android.ui.common.itemConditionLabel
import lt.skautai.android.ui.common.itemOriginLabel
import lt.skautai.android.ui.common.itemStatusLabel
import lt.skautai.android.util.NavRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryDetailScreen(
    itemId: String,
    navController: NavController,
    viewModel: InventoryDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    val deleteError by viewModel.deleteError.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        viewModel.loadItem(itemId)
    }

    LaunchedEffect(deleted) {
        if (deleted) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(deleteError) {
        deleteError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onDeleteErrorShown()
        }
    }

    val currentItem = (uiState as? InventoryDetailUiState.Success)?.item
    val canManageShared = "items.transfer" in permissions
    val isTransferredFromTuntas = currentItem?.origin == "TRANSFERRED_FROM_TUNTAS"
    val canEdit = "items.update" in permissions && (!isTransferredFromTuntas || canManageShared)
    val canDelete = "items.delete" in permissions && (!isTransferredFromTuntas || canManageShared)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daikto informacija") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atgal")
                    }
                },
                actions = {
                    if (canEdit) {
                        IconButton(
                            onClick = { navController.navigate(NavRoutes.InventoryAddEdit.createRoute(itemId)) }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Redaguoti")
                        }
                    }
                    if (canDelete) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Istrinti",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is InventoryDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is InventoryDetailUiState.Error -> {
                    SkautaiEmptyState(
                        title = "Nepavyko uzkrauti daikto",
                        subtitle = state.message,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is InventoryDetailUiState.Success -> {
                    ItemDetailContent(
                        item = state.item,
                        canEdit = canEdit,
                        canDelete = canDelete,
                        onEdit = { navController.navigate(NavRoutes.InventoryAddEdit.createRoute(itemId)) },
                        onDelete = { showDeleteDialog = true }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Istrinti daikta?") },
            text = {
                Text("Daiktas bus pazymetas kaip neaktyvus. Sis veiksmas neatsaukiamas.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteItem(itemId)
                    }
                ) {
                    Text("Istrinti", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Atsaukti")
                }
            }
        )
    }
}

@Composable
private fun ItemDetailContent(
    item: ItemDto,
    canEdit: Boolean,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isSharedTransfer = item.origin == "TRANSFERRED_FROM_TUNTAS"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SkautaiCard(
            modifier = Modifier.fillMaxWidth(),
            tonal = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = listOfNotNull(item.locationId, item.custodianName).joinToString(" · ").ifBlank {
                                "Vieta dar nenurodyta"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(label = itemStatusLabel(item.status))
                    StatusPill(label = itemConditionLabel(item.condition))
                    StatusPill(label = inventoryTypeLabel(item.type))
                    StatusPill(label = inventoryCategoryLabel(item.category))
                }

                Text(
                    text = if (isSharedTransfer) {
                        "Sis daiktas atkeliaves is bendro inventoriaus, todel jo valdymas ribojamas pagal role."
                    } else {
                        "Savo vieneto daiktas gali buti pilnai tvarkomas, jei naudotojas turi tam reikiamas teises."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkautaiCard(
                modifier = Modifier.weight(1f),
                tonal = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Kiekis",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${item.quantity} vnt.",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
            SkautaiCard(
                modifier = Modifier.weight(1f),
                tonal = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Kilme",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = itemOriginLabel(item.origin),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        SkautaiCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Metaduomenys", style = MaterialTheme.typography.titleLarge)
                MetadataRow("Tipas", inventoryTypeLabel(item.type))
                MetadataRow("Kategorija", inventoryCategoryLabel(item.category))
                MetadataRow("Busena", itemStatusLabel(item.status))
                MetadataRow("Bukle", itemConditionLabel(item.condition))
                MetadataRow("Kilme", itemOriginLabel(item.origin))
                MetadataRow("Saugotojas", item.custodianName ?: "Bendras sandelis")
                MetadataRow("Vieta", item.locationId ?: "Nenurodyta")
                item.purchaseDate?.let { MetadataRow("Pirkta", it.take(10)) }
                item.purchasePrice?.let { MetadataRow("Kaina", String.format("%.2f EUR", it)) }
            }
        }

        item.description?.takeIf { it.isNotBlank() }?.let {
            SkautaiCard(
                modifier = Modifier.fillMaxWidth(),
                tonal = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "Aprasymas", style = MaterialTheme.typography.titleMedium)
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item.notes?.takeIf { it.isNotBlank() }?.let {
            SkautaiCard(
                modifier = Modifier.fillMaxWidth(),
                tonal = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Pastabos",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        if (canEdit || canDelete) {
            SkautaiCard(
                modifier = Modifier.fillMaxWidth(),
                tonal = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "Veiksmai", style = MaterialTheme.typography.titleLarge)
                    if (canEdit) {
                        Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                            Text("Redaguoti daikta")
                        }
                    }
                    if (canDelete) {
                        Button(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                            Text("Pazymeti neaktyviu")
                        }
                    }
                }
            }
        }

        Text(
            text = "Sukurta ${item.createdAt.take(10)} · Atnaujinta ${item.updatedAt.take(10)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatusPill(label: String) {
    SkautaiStatusPill(
        label = label,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
}
