package lt.skautai.android.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.ReservationDto
import lt.skautai.android.ui.common.MetadataRow
import lt.skautai.android.ui.common.RemoteImage
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiErrorState
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
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    val sharedRequestCreated by viewModel.sharedRequestCreated.collectAsStateWithLifecycle()
    val isCreatingSharedRequest by viewModel.isCreatingSharedRequest.collectAsStateWithLifecycle()
    val isUpdatingStatus by viewModel.isUpdatingStatus.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(itemId) {
        viewModel.loadItem(itemId)
    }

    LaunchedEffect(deleted) {
        if (deleted) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(actionError) {
        actionError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onActionErrorShown()
        }
    }

    LaunchedEffect(sharedRequestCreated) {
        if (sharedRequestCreated) {
            snackbarHostState.showSnackbar("Paėmimo prašymas sukurtas.")
            viewModel.onSharedRequestMessageShown()
        }
    }

    val currentItem = (uiState as? InventoryDetailUiState.Success)?.item
    val canManageShared = "items.transfer" in permissions
    val isTransferredFromTuntas = currentItem?.origin == "TRANSFERRED_FROM_TUNTAS"
    val canEdit = "items.update" in permissions && (!isTransferredFromTuntas || canManageShared)
    val canChangeStatus = canEdit

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
                }
            )
        },
        snackbarHost = { SkautaiErrorSnackbarHost(hostState = snackbarHostState) }
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
                    SkautaiErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadItem(itemId) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is InventoryDetailUiState.Success -> {
                    ItemDetailContent(
                        item = state.item,
                        reservations = state.reservations,
                        canChangeStatus = canChangeStatus,
                        isCreatingSharedRequest = isCreatingSharedRequest,
                        isUpdatingStatus = isUpdatingStatus,
                        onRequestSharedItem = { viewModel.requestSharedItemForActiveUnit(itemId) },
                        onStatusChange = { status -> viewModel.updateStatus(itemId, status) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemDetailContent(
    item: ItemDto,
    reservations: List<ReservationDto>,
    canChangeStatus: Boolean,
    isCreatingSharedRequest: Boolean,
    isUpdatingStatus: Boolean,
    onRequestSharedItem: () -> Unit,
    onStatusChange: (String) -> Unit
) {
    val isSharedTransfer = item.origin == "TRANSFERRED_FROM_TUNTAS"
    val canRequestForUnit = item.custodianId == null && item.status == "ACTIVE" && item.quantity > 0

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
                            text = listOfNotNull(item.locationPath ?: item.locationName, item.custodianName).joinToString(" · ").ifBlank {
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

        item.photoUrl?.takeIf { it.isNotBlank() }?.let { photoUrl ->
            RemoteImage(
                imageUrl = photoUrl,
                contentDescription = item.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.45f)
            )
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
                MetadataRow("Vieta", item.locationPath ?: item.locationName ?: "Nenurodyta")
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

        if (reservations.isNotEmpty()) {
            ItemReservationsCard(reservations = reservations)
        }

        if (canRequestForUnit) {
            SkautaiCard(
                modifier = Modifier.fillMaxWidth(),
                tonal = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Gauti i vieneta",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Jei daiktas jau yra bendrame tunto inventoriuje, kurk paemimo prasyma, o ne pirkimo prasyma.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f)
                    )
                    Button(
                        onClick = onRequestSharedItem,
                        enabled = !isCreatingSharedRequest && !isUpdatingStatus,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isCreatingSharedRequest) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text("Prasyti paemimo i aktyvu vieneta")
                        }
                    }
                }
            }
        }

        if (canChangeStatus) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onStatusChange("ACTIVE") },
                    enabled = item.status != "ACTIVE" && !isUpdatingStatus && !isCreatingSharedRequest,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isUpdatingStatus && item.status != "ACTIVE") "Keiciama..." else "Aktyvus")
                }
                OutlinedButton(
                    onClick = { onStatusChange("INACTIVE") },
                    enabled = item.status != "INACTIVE" && !isUpdatingStatus && !isCreatingSharedRequest,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isUpdatingStatus && item.status != "INACTIVE") "Keiciama..." else "Neaktyvus")
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
private fun ItemReservationsCard(reservations: List<ReservationDto>) {
    SkautaiCard(
        modifier = Modifier.fillMaxWidth(),
        tonal = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Rezervacijos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            reservations.forEach { reservation ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${reservation.startDate.take(10)} - ${reservation.endDate.take(10)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        SkautaiStatusPill(
                            label = if (reservation.status == "ACTIVE") "Aktyvi" else "Patvirtinta",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    val context = listOfNotNull(
                        reservation.reservedByName?.takeIf { it.isNotBlank() },
                        reservation.requestingUnitName?.takeIf { it.isNotBlank() }
                    ).joinToString(" / ")
                    if (context.isNotBlank()) {
                        Text(
                            text = context,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
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
