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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import lt.skautai.android.data.remote.ItemHistoryDto
import lt.skautai.android.data.remote.ItemTransferDto
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.remote.ReservationDto
import lt.skautai.android.ui.common.MetadataRow
import lt.skautai.android.ui.common.RemoteImage
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.SkautaiTextField
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
import lt.skautai.android.util.hasPermission
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
    val isTransferring by viewModel.isTransferring.collectAsStateWithLifecycle()
    val orgUnits by viewModel.orgUnits.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val activeOrgUnitId by viewModel.activeOrgUnitId.collectAsStateWithLifecycle()
    val leadershipUnitIds by viewModel.leadershipUnitIds.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showQrDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSharedRequestDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showReturnDialog by remember { mutableStateOf(false) }
    var showRestockDialog by remember { mutableStateOf(false) }
    var showApproveDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectionReasonText by remember { mutableStateOf("") }
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
            else -> permissions.hasPermissionOwnUnit("items.update") &&
                (item.custodianId in leadershipUnitIds || item.custodianId == activeOrgUnitId)
        }
    } ?: false
    val canChangeStatus = canEdit
    val canDelete = canEdit && currentItem?.status != "INACTIVE"
    val canRestock = canEdit && currentItem?.status == "ACTIVE"
    val canDirectTransfer = currentItem?.let {
        canManageShared && it.custodianId == null && it.status == "ACTIVE" && it.quantity > 0 && it.type != "INDIVIDUAL"
    } ?: false
    val canReturnToShared = currentItem?.let {
        it.origin == "TRANSFERRED_FROM_TUNTAS" &&
            it.status == "ACTIVE" &&
            it.quantity > 0 &&
            (canManageShared || (permissions.hasPermissionOwnUnit("items.update") &&
                (it.custodianId in leadershipUnitIds || it.custodianId == activeOrgUnitId)))
    } ?: false
    val canRequestForUnit = currentItem?.let {
        !canManageShared &&
            !activeOrgUnitId.isNullOrBlank() &&
            it.custodianId == null &&
            it.status == "ACTIVE" &&
            it.quantity > 0
    } ?: false
    val canReviewAddition = currentItem?.let { item ->
        if (item.status != "PENDING_APPROVAL") return@let false
        when (item.targetScope) {
            "SHARED" -> permissions.hasPermission("items.review:ALL")
            "UNIT" -> permissions.hasPermissionOwnUnit("items.review") &&
                (item.custodianId in leadershipUnitIds || item.custodianId == activeOrgUnitId)
            else -> permissions.hasPermission("items.review:ALL")
        }
    } ?: false

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
                        itemHistory = state.itemHistory,
                        transfers = state.transfers,
                        canChangeStatus = canChangeStatus,
                        canDelete = canDelete,
                        isCreatingSharedRequest = isCreatingSharedRequest,
                        isUpdatingStatus = isUpdatingStatus,
                        canShowQr = canShowQr,
                        canRequestForUnit = canRequestForUnit,
                        canDirectTransfer = canDirectTransfer,
                        canReturnToShared = canReturnToShared,
                        canRestock = canRestock,
                        canReviewAddition = canReviewAddition,
                        isTransferring = isTransferring,
                        onRequestSharedItem = {
                            sharedRequestQuantity = "1"
                            sharedRequestQuantityError = null
                            showSharedRequestDialog = true
                        },
                        onStatusChange = { status -> viewModel.updateStatus(itemId, status) },
                        onDirectTransfer = { showTransferDialog = true },
                        onReturnToShared = { showReturnDialog = true },
                        onRestock = { showRestockDialog = true },
                        onDelete = { showDeleteDialog = true },
                        onShowQr = { showQrDialog = true },
                        onApprove = { showApproveDialog = true },
                        onReject = { showRejectDialog = true }
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
                    SkautaiTextField(
                        value = sharedRequestQuantity,
                        onValueChange = { value ->
                            sharedRequestQuantity = value.filter(Char::isDigit)
                            sharedRequestQuantityError = null
                        },
                        label = "Kiekis",
                        singleLine = true,
                        isError = sharedRequestQuantityError != null,
                        supportingText = sharedRequestQuantityError ?: "Galimas kiekis: ${currentItem.quantity} vnt.",
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

    if (showTransferDialog && currentItem != null) {
        TransferToUnitDialog(
            item = currentItem,
            orgUnits = orgUnits,
            isSubmitting = isTransferring,
            onDismiss = { if (!isTransferring) showTransferDialog = false },
            onConfirm = { targetUnitId, quantity, notes ->
                showTransferDialog = false
                viewModel.transferToUnit(currentItem.id, targetUnitId, quantity, notes)
            }
        )
    }

    if (showReturnDialog && currentItem != null) {
        QuantityNotesDialog(
            title = "Grąžinti į bendrą inventorių",
            description = "Nurodyk, kiek vieneto turimų vienetų grįžta į bendrą tunto inventorių.",
            maxQuantity = currentItem.quantity,
            confirmLabel = "Grąžinti",
            isSubmitting = isTransferring,
            onDismiss = { if (!isTransferring) showReturnDialog = false },
            onConfirm = { quantity, notes ->
                showReturnDialog = false
                viewModel.returnToShared(currentItem.id, quantity, notes)
            }
        )
    }

    if (showRestockDialog && currentItem != null) {
        RestockDialog(
            item = currentItem,
            isSubmitting = isTransferring,
            onDismiss = { if (!isTransferring) showRestockDialog = false },
            onConfirm = { quantity, purchaseDate, purchasePrice, notes ->
                showRestockDialog = false
                viewModel.restockItem(currentItem.id, quantity, purchaseDate, purchasePrice, notes)
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

    if (showApproveDialog && currentItem != null) {
        AlertDialog(
            onDismissRequest = { showApproveDialog = false },
            title = { Text("Patvirtinti prašymą?") },
            text = { Text("Daiktas „${currentItem.name}“ bus įtrauktas į inventorių kaip aktyvus.") },
            confirmButton = {
                TextButton(onClick = {
                    showApproveDialog = false
                    viewModel.reviewItemAddition(currentItem.id, "APPROVED")
                }) { Text("Patvirtinti") }
            },
            dismissButton = {
                TextButton(onClick = { showApproveDialog = false }) { Text("Atšaukti") }
            }
        )
    }

    if (showRejectDialog && currentItem != null) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Atmesti prašymą?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Nurodyk atmetimo priežastį (neprivaloma).")
                    SkautaiTextField(
                        value = rejectionReasonText,
                        onValueChange = { rejectionReasonText = it },
                        label = "Priežastis",
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showRejectDialog = false
                    viewModel.reviewItemAddition(
                        currentItem.id,
                        "REJECTED",
                        rejectionReasonText.ifBlank { null }
                    )
                    rejectionReasonText = ""
                }) { Text("Atmesti") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRejectDialog = false
                    rejectionReasonText = ""
                }) { Text("Atšaukti") }
            }
        )
    }
}

@Composable
private fun RestockDialog(
    item: ItemDto,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int, String?, Double?, String?) -> Unit
) {
    var quantityText by remember { mutableStateOf("1") }
    var purchaseDate by remember { mutableStateOf("") }
    var purchasePriceText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Papildyti kiekį") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(item.name, fontWeight = FontWeight.SemiBold)
                SkautaiTextField(
                    value = quantityText,
                    onValueChange = {
                        quantityText = it.filter(Char::isDigit)
                        error = null
                    },
                    label = "Kiekis",
                    singleLine = true,
                    isError = error != null,
                    supportingText = error,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                SkautaiTextField(
                    value = purchaseDate,
                    onValueChange = { purchaseDate = it },
                    label = "Pirkimo data (YYYY-MM-DD)",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                SkautaiTextField(
                    value = purchasePriceText,
                    onValueChange = { purchasePriceText = it.replace(',', '.') },
                    label = "Kaina",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                SkautaiTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Pastabos",
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSubmitting,
                onClick = {
                    val quantity = quantityText.toIntOrNull()
                    val price = purchasePriceText.takeIf { it.isNotBlank() }?.toDoubleOrNull()
                    when {
                        quantity == null || quantity < 1 -> error = "Kiekis turi būti bent 1"
                        purchasePriceText.isNotBlank() && price == null -> error = "Kaina turi būti skaičius"
                        else -> onConfirm(quantity, purchaseDate.ifBlank { null }, price, notes.ifBlank { null })
                    }
                }
            ) { Text("Papildyti") }
        },
        dismissButton = {
            TextButton(enabled = !isSubmitting, onClick = onDismiss) { Text("Atšaukti") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferToUnitDialog(
    item: ItemDto,
    orgUnits: List<OrganizationalUnitDto>,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedUnitId by remember { mutableStateOf<String?>(null) }
    var quantityText by remember { mutableStateOf("1") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val selectedUnit = orgUnits.find { it.id == selectedUnitId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Perduoti vienetui") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Perduok dalį bendro inventoriaus tiesiogiai pasirinktam vienetui.")
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedUnit?.name ?: "Pasirink vienetą",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vienetas") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        orgUnits.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.name) },
                                onClick = {
                                    selectedUnitId = unit.id
                                    error = null
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                SkautaiTextField(
                    value = quantityText,
                    onValueChange = {
                        quantityText = it.filter(Char::isDigit)
                        error = null
                    },
                    label = "Kiekis",
                    supportingText = error ?: "Galimas kiekis: ${item.quantity} vnt.",
                    isError = error != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                SkautaiTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Pastabos",
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSubmitting,
                onClick = {
                    val unitId = selectedUnitId
                    val quantity = quantityText.toIntOrNull()
                    when {
                        unitId.isNullOrBlank() -> error = "Pasirink vienetą"
                        quantity == null || quantity < 1 -> error = "Įveskite teigiamą kiekį"
                        quantity > item.quantity -> error = "Kiekis negali viršyti turimo kiekio"
                        else -> onConfirm(unitId, quantity, notes.ifBlank { null })
                    }
                }
            ) {
                Text("Perduoti")
            }
        },
        dismissButton = {
            TextButton(enabled = !isSubmitting, onClick = onDismiss) {
                Text("Atšaukti")
            }
        }
    )
}

@Composable
private fun QuantityNotesDialog(
    title: String,
    description: String,
    maxQuantity: Int,
    confirmLabel: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int, String?) -> Unit
) {
    var quantityText by remember { mutableStateOf("1") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(description)
                SkautaiTextField(
                    value = quantityText,
                    onValueChange = {
                        quantityText = it.filter(Char::isDigit)
                        error = null
                    },
                    label = "Kiekis",
                    supportingText = error ?: "Galimas kiekis: $maxQuantity vnt.",
                    isError = error != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                SkautaiTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Pastabos",
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSubmitting,
                onClick = {
                    val quantity = quantityText.toIntOrNull()
                    when {
                        quantity == null || quantity < 1 -> error = "Įveskite teigiamą kiekį"
                        quantity > maxQuantity -> error = "Kiekis negali viršyti turimo kiekio"
                        else -> onConfirm(quantity, notes.ifBlank { null })
                    }
                }
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(enabled = !isSubmitting, onClick = onDismiss) {
                Text("Atšaukti")
            }
        }
    )
}

@Composable
private fun ItemDetailContent(
    item: ItemDto,
    reservations: List<ReservationDto>,
    assignments: List<ItemAssignmentDto>,
    conditionLog: List<ItemConditionLogDto>,
    itemHistory: List<ItemHistoryDto>,
    transfers: List<ItemTransferDto>,
    canChangeStatus: Boolean,
    canDelete: Boolean,
    isCreatingSharedRequest: Boolean,
    isUpdatingStatus: Boolean,
    canShowQr: Boolean,
    canRequestForUnit: Boolean,
    canDirectTransfer: Boolean,
    canReturnToShared: Boolean,
    canRestock: Boolean,
    canReviewAddition: Boolean,
    isTransferring: Boolean,
    onRequestSharedItem: () -> Unit,
    onStatusChange: (String) -> Unit,
    onDirectTransfer: () -> Unit,
    onReturnToShared: () -> Unit,
    onRestock: () -> Unit,
    onDelete: () -> Unit,
    onShowQr: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val isSharedTransfer = item.origin == "TRANSFERRED_FROM_TUNTAS"
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
                    StatusPill(label = inventoryTypeLabel(item.effectiveInventoryType()))
                    StatusPill(label = inventoryCategoryLabel(item.category))
                }

                Text(
                    text = if (isSharedTransfer) {
                        "Šis daiktas yra vieneto inventoriuje. Valdymo teisės priklauso nuo rolės ir daikto kilmės."
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
                        text = "${item.quantity} ${item.unitLabel()}",
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
                MetadataRow("Tipas", inventoryTypeLabel(item.effectiveInventoryType()))
                MetadataRow("Kategorija", inventoryCategoryLabel(item.category))
                MetadataRow("Būsena", itemStatusLabel(item.status))
                MetadataRow("Būklė", itemConditionLabel(item.condition))
                item.customFields.fieldValue("Priežastis")?.let { MetadataRow("Priežastis", it) }
                item.customFields.fieldValue("Žymos")?.let { MetadataRow("Žymos", it) }
                MetadataRow("Kilmė", originDisplay)
                MetadataRow("Saugotojas", item.custodianName ?: "Bendras sandėlis")
                MetadataRow("Atsakingas", item.responsibleUserName ?: "Nepriskirtas")
                MetadataRow("Vieta", item.locationPath ?: item.locationName ?: "Nenurodyta")
                item.purchaseDate?.let { MetadataRow("Pirkta", it.take(10)) }
                item.purchasePrice?.let { MetadataRow("Kaina", String.format("%.2f EUR", it)) }
            }
        }

        val customFields = item.customFields.orEmpty()
            .filterNot { field -> managedCustomFieldNames.any { it.equals(field.fieldName, ignoreCase = true) } }
        if (customFields.isNotEmpty()) {
            SkautaiCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Papildomi laukai", style = MaterialTheme.typography.titleLarge)
                    customFields.forEach { field ->
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

        if (itemHistory.isNotEmpty()) {
            ItemHistoryCard(entries = itemHistory)
        }

        if (transfers.isNotEmpty()) {
            ItemTransfersCard(transfers = transfers)
        }

        if (conditionLog.isNotEmpty()) {
            ItemConditionLogCard(entries = conditionLog)
        }

        if (item.status == "PENDING_APPROVAL") {
            SkautaiCard(
                modifier = Modifier.fillMaxWidth(),
                tonal = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Laukiama patvirtinimo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    if (item.submittedByUserName != null) {
                        Text(
                            text = "Pateikė: ${item.submittedByUserName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        if (canReviewAddition) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f)
                ) { Text("Patvirtinti") }
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f)
                ) { Text("Atmesti") }
            }
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

        if (canDirectTransfer) {
            Button(
                onClick = onDirectTransfer,
                enabled = !isTransferring && !isUpdatingStatus && !isCreatingSharedRequest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isTransferring) "Perduodama..." else "Perduoti vienetui be prašymo")
            }
        }

        if (canRestock) {
            Button(
                onClick = onRestock,
                enabled = !isTransferring && !isUpdatingStatus && !isCreatingSharedRequest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isTransferring) "Papildoma..." else "Papildyti kiekį")
            }
        }

        if (canReturnToShared) {
            OutlinedButton(
                onClick = onReturnToShared,
                enabled = !isTransferring && !isUpdatingStatus && !isCreatingSharedRequest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isTransferring) "Grąžinama..." else "Grąžinti į bendrą inventorių")
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
    item.origin == "TRANSFERRED_FROM_TUNTAS" -> "Vieneto inventorius"
    else -> "Nenurodyta"
}

private fun ItemDto.effectiveInventoryType(): String =
    if (origin == "TRANSFERRED_FROM_TUNTAS" && custodianId != null) "COLLECTIVE" else type

@Composable
private fun ItemHistoryCard(entries: List<ItemHistoryDto>) {
    SkautaiCard(
        modifier = Modifier.fillMaxWidth(),
        tonal = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Daikto istorija",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            entries.forEach { entry ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = itemHistoryLabel(entry),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = listOfNotNull(
                            entry.createdAt.take(10),
                            entry.performedByUserName,
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

private fun itemHistoryLabel(entry: ItemHistoryDto): String {
    val quantity = entry.quantityChange?.let { " ($it vnt.)" }.orEmpty()
    return when (entry.eventType) {
        "CREATED" -> "Sukurtas įrašas$quantity"
        "PURCHASED_NEW" -> "Nupirkta ir sukurta inventoriuje$quantity"
        "RESTOCKED" -> "Papildytas kiekis$quantity"
        "MANUAL_RESTOCKED" -> "Papildytas kiekis$quantity"
        "EVENT_PURCHASED_NEW" -> "Nupirkta renginiui ir sukurta inventoriuje$quantity"
        "EVENT_PURCHASE_RESTOCKED" -> "Papildyta po renginio pirkimo$quantity"
        "TRANSFERRED_TO_UNIT" -> "Perduota vienetui$quantity"
        "RECEIVED_FROM_SHARED" -> "Gauta iš bendro inventoriaus$quantity"
        "RETURNED_TO_SHARED" -> "Grąžinta į bendrą inventorių$quantity"
        "RECEIVED_FROM_UNIT" -> "Gauta atgal iš vieneto$quantity"
        "RESERVATION_ISSUED" -> "Išduota rezervacijai$quantity"
        "RESERVATION_RETURN_MARKED" -> "Pažymėta kaip grąžinama$quantity"
        "RESERVATION_RETURNED" -> "Grąžinta iš rezervacijos$quantity"
        "EVENT_RECONCILE_RETURNED" -> "Grąžinta po renginio$quantity"
        "EVENT_RECONCILE_DAMAGED" -> "Pažymėta sugadinta po renginio$quantity"
        "EVENT_RECONCILE_MISSING" -> "Pažymėta dingusi po renginio$quantity"
        "EVENT_RECONCILE_CONSUMED" -> "Sunaudota renginyje$quantity"
        "DEACTIVATED" -> "Deaktyvuotas įrašas"
        else -> entry.eventType + quantity
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
private fun ItemTransfersCard(transfers: List<ItemTransferDto>) {
    SkautaiCard(
        modifier = Modifier.fillMaxWidth(),
        tonal = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Judėjimo istorija",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            transfers.forEach { transfer ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "${transfer.fromCustodianName ?: "Bendras sandėlis"} -> ${transfer.toCustodianName ?: "Bendras sandėlis"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = listOfNotNull(
                            transfer.completedAt?.take(10) ?: transfer.createdAt.take(10),
                            transfer.initiatedByUserName?.let { "inicijavo $it" },
                            transfer.approvedByUserName?.let { "patvirtino $it" },
                            transfer.notes
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

private fun ItemDto.unitLabel(): String =
    customFields.fieldValue("Mato vienetas") ?: "vnt."

private fun List<lt.skautai.android.data.remote.ItemCustomFieldDto>.fieldValue(name: String): String? =
    firstOrNull { it.fieldName.equals(name, ignoreCase = true) }
        ?.fieldValue
        ?.takeIf { it.isNotBlank() }

private val managedCustomFieldNames = setOf("Mato vienetas", "Žymos", "Priežastis")

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
            TextButton(onClick = onSharePdf) { Text("Dalintis PDF") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Uždaryti") }
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
