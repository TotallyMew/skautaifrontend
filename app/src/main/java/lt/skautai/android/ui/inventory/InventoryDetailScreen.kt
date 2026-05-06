package lt.skautai.android.ui.inventory

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.ItemAssignmentDto
import lt.skautai.android.data.remote.ItemConditionLogDto
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
import lt.skautai.android.ui.common.itemStatusLabel
import lt.skautai.android.util.NavRoutes
import lt.skautai.android.util.QrCodeBitmap
import lt.skautai.android.util.QrPdfShareLauncher
import lt.skautai.android.util.QrPayload
import lt.skautai.android.util.canManageAllItems
import lt.skautai.android.util.canManageSharedInventory
import lt.skautai.android.util.hasPermissionOwnUnit
import lt.skautai.android.util.toPrintableQrItemOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryDetailScreen(
    itemId: String,
    navController: NavController,
    viewModel: InventoryDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    val sharedRequestCreated by viewModel.sharedRequestCreated.collectAsStateWithLifecycle()
    val shareMessage by viewModel.shareMessage.collectAsStateWithLifecycle()
    val isCreatingSharedRequest by viewModel.isCreatingSharedRequest.collectAsStateWithLifecycle()
    val isUpdatingStatus by viewModel.isUpdatingStatus.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val activeOrgUnitId by viewModel.activeOrgUnitId.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showQrDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSharedRequestDialog by remember { mutableStateOf(false) }
    var sharedRequestQuantity by remember { mutableStateOf("1") }
    var sharedRequestQuantityError by remember { mutableStateOf<String?>(null) }

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

    LaunchedEffect(shareMessage) {
        shareMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onShareMessageShown()
        }
    }

    val currentItem = (uiState as? InventoryDetailUiState.Success)?.item
    val canManageShared = permissions.canManageSharedInventory()
    val isTransferredFromTuntas = currentItem?.origin == "TRANSFERRED_FROM_TUNTAS"
    val canShowQr = currentItem?.let { !it.id.startsWith("local-") && it.qrToken.isNotBlank() } ?: false
    val canEdit = currentItem?.let { item ->
        when {
            isTransferredFromTuntas -> canManageShared
            item.custodianId == null -> permissions.canManageAllItems()
            permissions.canManageAllItems() -> true
            else -> permissions.hasPermissionOwnUnit("items.update") && item.custodianId == activeOrgUnitId
        }
    } ?: false
    val canChangeStatus = canEdit
    val canDelete = canEdit && currentItem?.status != "INACTIVE"

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
                            Icon(Icons.Default.Delete, contentDescription = "Ištrinti")
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
                        assignments = state.assignments,
                        conditionLog = state.conditionLog,
                        canChangeStatus = canChangeStatus,
                        canDelete = canDelete,
                        isCreatingSharedRequest = isCreatingSharedRequest,
                        isUpdatingStatus = isUpdatingStatus,
                        canShowQr = canShowQr,
                        onRequestSharedItem = {
                            sharedRequestQuantity = "1"
                            sharedRequestQuantityError = null
                            showSharedRequestDialog = true
                        },
                        onStatusChange = { status -> viewModel.updateStatus(itemId, status) },
                        onDelete = { showDeleteDialog = true },
                        onShowQr = { showQrDialog = true }
                    )
                }
            }
        }
    }

    if (showQrDialog && currentItem != null) {
        ItemQrDialog(
            item = currentItem,
            onDismiss = { showQrDialog = false },
            onSharePdf = {
                runCatching {
                    val printableItem = currentItem.toPrintableQrItemOrNull()
                        ?: error("Šio daikto QR PDF sugeneruoti negalima.")
                    QrPdfShareLauncher.share(context, listOf(printableItem))
                }.onSuccess {
                    viewModel.onQrPdfShared()
                }.onFailure {
                    viewModel.onQrPdfShareFailed(
                        it.message ?: "Nepavyko sugeneruoti QR PDF"
                    )
                }
            }
        )
    }

    if (showSharedRequestDialog && currentItem != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isCreatingSharedRequest) showSharedRequestDialog = false
            },
            title = { Text("Prašyti paėmimo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Nurodyk, kiek šio daikto vienetų reikia gauti į aktyvų vienetą.")
                    OutlinedTextField(
                        value = sharedRequestQuantity,
                        onValueChange = { value ->
                            sharedRequestQuantity = value.filter(Char::isDigit)
                            sharedRequestQuantityError = null
                        },
                        label = { Text("Kiekis") },
                        singleLine = true,
                        isError = sharedRequestQuantityError != null,
                        supportingText = {
                            sharedRequestQuantityError?.let { Text(it) }
                                ?: Text("Galimas kiekis: ${currentItem.quantity} vnt.")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isCreatingSharedRequest,
                    onClick = {
                        val quantity = sharedRequestQuantity.toIntOrNull()
                        when {
                            quantity == null || quantity < 1 -> {
                                sharedRequestQuantityError = "Įveskite teigiamą kiekį"
                            }
                            quantity > currentItem.quantity -> {
                                sharedRequestQuantityError = "Kiekis negali viršyti turimo kiekio"
                            }
                            else -> {
                                showSharedRequestDialog = false
                                viewModel.requestSharedItemForActiveUnit(currentItem.id, quantity)
                            }
                        }
                    }
                ) {
                    Text("Pateikti")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isCreatingSharedRequest,
                    onClick = { showSharedRequestDialog = false }
                ) {
                    Text("Atšaukti")
                }
            }
        )
    }

    if (showDeleteDialog && currentItem != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Ištrinti inventorių?") },
            text = { Text("Daiktas bus pažymėtas kaip neaktyvus ir liks matomas neaktyvaus inventoriaus filtre.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteItem(itemId)
                    }
                ) {
                    Text("Ištrinti")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Atšaukti")
                }
            }
        )
    }
}

@Composable
private fun ItemDetailContent(
    item: ItemDto,
    reservations: List<ReservationDto>,
    assignments: List<ItemAssignmentDto>,
    conditionLog: List<ItemConditionLogDto>,
    canChangeStatus: Boolean,
    canDelete: Boolean,
    isCreatingSharedRequest: Boolean,
    isUpdatingStatus: Boolean,
    canShowQr: Boolean,
    onRequestSharedItem: () -> Unit,
    onStatusChange: (String) -> Unit,
    onDelete: () -> Unit,
    onShowQr: () -> Unit
) {
    val isSharedTransfer = item.origin == "TRANSFERRED_FROM_TUNTAS"
    val canRequestForUnit = item.custodianId == null && item.status == "ACTIVE" && item.quantity > 0
    val originDisplay = itemOriginDisplay(item)

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
                        "Šis daiktas atkeliavęs iš bendro inventoriaus, todėl jo valdymas ribojamas pagal rolę."
                    } else {
                        "Savo vieneto daiktas gali būti pilnai tvarkomas, jei naudotojas turi tam reikiamas teises."
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
                        text = "Kilmė",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = originDisplay,
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
                MetadataRow("Būsena", itemStatusLabel(item.status))
                MetadataRow("Būklė", itemConditionLabel(item.condition))
                MetadataRow("Kilmė", originDisplay)
                MetadataRow("Saugotojas", item.custodianName ?: "Bendras sandėlis")
                MetadataRow("Atsakingas", item.responsibleUserName ?: "Nepriskirtas")
                MetadataRow("Vieta", item.locationPath ?: item.locationName ?: "Nenurodyta")
                item.purchaseDate?.let { MetadataRow("Pirkta", it.take(10)) }
                item.purchasePrice?.let { MetadataRow("Kaina", String.format("%.2f EUR", it)) }
            }
        }

        if (item.customFields.isNotEmpty()) {
            SkautaiCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Papildomi laukai", style = MaterialTheme.typography.titleLarge)
                    item.customFields.forEach { field ->
                        MetadataRow(field.fieldName, field.fieldValue ?: "Nenurodyta")
                    }
                }
            }
        }

        if (canShowQr) {
            FilledTonalButton(
                onClick = onShowQr,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Rodyti daikto QR kodą")
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
                    Text(text = "Aprašymas", style = MaterialTheme.typography.titleMedium)
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

        if (assignments.isNotEmpty()) {
            ItemAssignmentsCard(assignments = assignments)
        }

        if (conditionLog.isNotEmpty()) {
            ItemConditionLogCard(entries = conditionLog)
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
                        text = "Gauti į vienetą",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Jei daiktas jau yra bendrame tunto inventoriuje, kurk paėmimo prašymą, o ne pirkimo prašymą.",
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
                            Text("Prašyti paėmimo į aktyvų vienetą")
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
                    Text(if (isUpdatingStatus && item.status != "ACTIVE") "Keičiama..." else "Aktyvus")
                }
                OutlinedButton(
                    onClick = { onStatusChange("INACTIVE") },
                    enabled = item.status != "INACTIVE" && !isUpdatingStatus && !isCreatingSharedRequest,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isUpdatingStatus && item.status != "INACTIVE") "Keičiama..." else "Neaktyvus")
                }
            }
        }

        if (canDelete) {
            OutlinedButton(
                onClick = onDelete,
                enabled = !isUpdatingStatus && !isCreatingSharedRequest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Ištrinti inventorių")
            }
        }
        Text(
            text = "Sukurta ${item.createdAt.take(10)} · Atnaujinta ${item.updatedAt.take(10)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun itemOriginDisplay(item: ItemDto): String = when {
    item.type == "INDIVIDUAL" -> item.createdByUserName?.takeIf { it.isNotBlank() } ?: "Asmeninis daiktas"
    item.custodianName?.isNotBlank() == true -> item.custodianName!!
    item.custodianId == null -> "Tunto inventorius"
    item.origin == "TRANSFERRED_FROM_TUNTAS" -> "Tunto inventorius"
    else -> "Nenurodyta"
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
private fun ItemAssignmentsCard(assignments: List<ItemAssignmentDto>) {
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
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Atsakingo žmogaus istorija",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            assignments.forEach { assignment ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = assignment.assignedToUserName ?: assignment.assignedToUserId,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = listOfNotNull(
                            "Nuo ${assignment.assignedAt.take(10)}",
                            assignment.unassignedAt?.let { "iki ${it.take(10)}" },
                            assignment.assignedByUserName?.let { "priskyrė $it" }
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemConditionLogCard(entries: List<ItemConditionLogDto>) {
    SkautaiCard(
        modifier = Modifier.fillMaxWidth(),
        tonal = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Būklės istorija",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            entries.forEach { entry ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = listOfNotNull(
                            entry.previousCondition?.let { itemConditionLabel(it) },
                            itemConditionLabel(entry.newCondition)
                        ).joinToString(" -> "),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = listOfNotNull(
                            entry.reportedAt.take(10),
                            entry.reportedByUserName,
                            entry.notes
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

@Composable
private fun ItemQrDialog(
    item: ItemDto,
    onDismiss: () -> Unit,
    onSharePdf: () -> Unit
) {
    val qrBitmap = remember(item.id) {
        QrCodeBitmap.create(QrPayload.forScanToken(item.qrToken))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Uzdaryti")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onSharePdf) {
                Text("Dalintis PDF")
            }
        },
        title = {
            Text("Inventoriaus QR kodas")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR kodas daiktui ${item.name}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = QrPayload.forScanToken(item.qrToken),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
