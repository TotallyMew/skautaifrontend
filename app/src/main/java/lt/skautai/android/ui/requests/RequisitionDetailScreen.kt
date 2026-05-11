package lt.skautai.android.ui.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.RequisitionDto
import lt.skautai.android.data.remote.RequisitionItemDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.ui.common.SkautaiConfirmDialog
import lt.skautai.android.ui.common.SkautaiDangerButton
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiPrimaryButton
import lt.skautai.android.ui.common.SkautaiSecondaryButton
import lt.skautai.android.ui.common.SkautaiTextField
import lt.skautai.android.util.canCreateItems
import lt.skautai.android.util.canForwardUnitRequests
import lt.skautai.android.util.canReviewTopLevelRequisitions
import lt.skautai.android.util.hasPermission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequisitionDetailScreen(
    requestId: String,
    onBack: () -> Unit,
    viewModel: RequisitionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val activeOrgUnitId by viewModel.activeOrgUnitId.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var rejectReason by remember { mutableStateOf("") }
    var rejectTarget by remember { mutableStateOf("") }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showAddInventoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(requestId) { viewModel.loadRequest(requestId) }

    LaunchedEffect((uiState as? RequisitionDetailUiState.Success)?.error) {
        (uiState as? RequisitionDetailUiState.Success)?.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (rejectTarget.isNotBlank()) {
        AlertDialog(
            onDismissRequest = {
                rejectTarget = ""
                rejectReason = ""
            },
            title = { Text("Atmesti prašymą") },
            text = {
                SkautaiTextField(
                    value = rejectReason,
                    onValueChange = { rejectReason = it },
                    label = "Priežastis",
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    when (rejectTarget) {
                        "UNIT" -> viewModel.rejectInUnit(requestId, rejectReason.ifBlank { null })
                        "TOP" -> viewModel.rejectTopLevel(requestId, rejectReason.ifBlank { null })
                    }
                    rejectTarget = ""
                    rejectReason = ""
                }) { Text("Atmesti", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = {
                    rejectTarget = ""
                    rejectReason = ""
                }) { Text("Uždaryti") }
            }
        )
    }

    if (showCancelDialog) {
        SkautaiConfirmDialog(
            title = "Atšaukti prašymą",
            message = "Ar tikrai nori atšaukti šį pirkimo arba papildymo prašymą?",
            confirmText = "Atšaukti",
            dismissText = "Uždaryti",
            isDanger = true,
            onConfirm = {
                    showCancelDialog = false
                    viewModel.cancelRequest(requestId)
            },
            onDismiss = { showCancelDialog = false }
        )
    }

    val successState = uiState as? RequisitionDetailUiState.Success
    if (showAddInventoryDialog && successState != null) {
        AddPurchasedItemDialog(
            request = successState.request,
            inventoryItems = successState.inventoryItems,
            isSubmitting = successState.isActioning,
            onDismiss = { if (!successState.isActioning) showAddInventoryDialog = false },
            onConfirm = { requisitionItemId, action, existingItemId, notes ->
                showAddInventoryDialog = false
                viewModel.addPurchasedItemToInventory(requestId, requisitionItemId, action, existingItemId, notes)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pirkimo prašymas") },
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
                is RequisitionDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is RequisitionDetailUiState.Error -> {
                    SkautaiErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadRequest(requestId) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is RequisitionDetailUiState.Success -> {
                    val request = state.request
                    val canUnitReview = request.requestingUnitId != null &&
                        request.requestingUnitId == activeOrgUnitId &&
                        request.unitReviewStatus == "PENDING" &&
                        (
                            permissions.hasPermission("items.request.approve.unit") ||
                                permissions.canForwardUnitRequests()
                            )
                    val canTopLevelReview = permissions.canReviewTopLevelRequisitions() &&
                        request.topLevelReviewStatus == "PENDING"
                    val canMarkPurchased = permissions.canReviewTopLevelRequisitions() &&
                        request.status == "APPROVED"
                    val canAddToInventory = permissions.canCreateItems() &&
                        request.status == "PURCHASED"
                    val isOwnRequest = request.createdByUserId == currentUserId
                    val canCancel = isOwnRequest &&
                        request.status !in listOf("APPROVED", "REJECTED", "CANCELLED")

                    RequisitionDetailContent(
                        request = request,
                        isActioning = state.isActioning,
                        isOwnRequest = isOwnRequest,
                        canCancel = canCancel,
                        canUnitReview = canUnitReview,
                        canTopLevelReview = canTopLevelReview,
                        canMarkPurchased = canMarkPurchased,
                        canAddToInventory = canAddToInventory,
                        onCancel = { showCancelDialog = true },
                        onApproveInUnit = { viewModel.approveInUnit(requestId) },
                        onForwardToTop = { viewModel.forwardToTop(requestId) },
                        onRejectInUnit = { rejectTarget = "UNIT" },
                        onApproveTop = { viewModel.approveTopLevel(requestId) },
                        onRejectTop = { rejectTarget = "TOP" },
                        onMarkPurchased = { viewModel.markPurchased(requestId) },
                        onAddToInventory = { showAddInventoryDialog = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun RequisitionDetailContent(
    request: RequisitionDto,
    isActioning: Boolean,
    isOwnRequest: Boolean,
    canCancel: Boolean,
    canUnitReview: Boolean,
    canTopLevelReview: Boolean,
    canMarkPurchased: Boolean,
    canAddToInventory: Boolean,
    onCancel: () -> Unit,
    onApproveInUnit: () -> Unit,
    onForwardToTop: () -> Unit,
    onRejectInUnit: () -> Unit,
    onApproveTop: () -> Unit,
    onRejectTop: () -> Unit,
    onMarkPurchased: () -> Unit,
    onAddToInventory: () -> Unit
) {
    val item = request.items.firstOrNull()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = item?.itemName ?: "Pirkimo prašymas",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Pirkimo arba papildymo prašymas naujam / trūkstamam inventoriui",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = requisitionStatusLabel(request),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Detalės", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                HorizontalDivider()
                item?.itemDescription?.let { RequisitionInfoRow("Aprašymas", it) }
                item?.let { RequisitionInfoRow("Kiekis", "${it.quantityRequested}") }
                request.requestingUnitName?.let { RequisitionInfoRow("Vienetas", it) }
                request.neededByDate?.let { RequisitionInfoRow("Reikia iki", it) }
                request.notes?.let { RequisitionInfoRow("Pagrindimas", it) }
                RequisitionInfoRow("Sukurta", request.createdAt.take(10))
                if (isOwnRequest) {
                    RequisitionInfoRow("Kontekstas", "Tai tavo sukurtas prašymas")
                }
            }
        }

        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Prašomi daiktai", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                HorizontalDivider()
                request.items.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider()
                    RequisitionItemRow(
                        item = item,
                        showInventoryActions = index == request.items.lastIndex,
                        request = request,
                        canMarkPurchased = canMarkPurchased,
                        canAddToInventory = canAddToInventory,
                        isActioning = isActioning,
                        onMarkPurchased = onMarkPurchased,
                        onAddToInventory = onAddToInventory
                    )
                }
            }
        }

        if (canUnitReview) {
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Vieneto sprendimas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SkautaiPrimaryButton(
                            text = "Patvirtinti vienete",
                            onClick = onApproveInUnit,
                            enabled = !isActioning,
                            modifier = Modifier.weight(1f)
                        )
                        SkautaiSecondaryButton(
                            text = "Perduoti inventorininkui",
                            onClick = onForwardToTop,
                            enabled = !isActioning,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    SkautaiDangerButton(
                        text = "Atmesti",
                        onClick = onRejectInUnit,
                        enabled = !isActioning,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (canCancel) {
            SkautaiDangerButton(
                text = "Atšaukti prašymą",
                onClick = onCancel,
                enabled = !isActioning,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (canTopLevelReview) {
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Inventorininko / tuntininko sprendimas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SkautaiPrimaryButton(
                            text = "Patvirtinti",
                            onClick = onApproveTop,
                            enabled = !isActioning,
                            modifier = Modifier.weight(1f)
                        )
                        SkautaiDangerButton(
                            text = "Atmesti",
                            onClick = onRejectTop,
                            enabled = !isActioning,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RequisitionItemRow(
    item: RequisitionItemDto,
    showInventoryActions: Boolean,
    request: RequisitionDto,
    canMarkPurchased: Boolean,
    canAddToInventory: Boolean,
    isActioning: Boolean,
    onMarkPurchased: () -> Unit,
    onAddToInventory: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.itemName, fontWeight = FontWeight.SemiBold)
                item.itemDescription?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "${item.quantityRequested} vnt.",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        item.notes?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item.quantityApproved?.let {
            Text(
                text = "Patvirtinta: $it vnt.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        item.rejectionReason?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = "Atmetimo priežastis: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (showInventoryActions && canMarkPurchased) {
            SkautaiPrimaryButton(
                text = "Pažymėti kaip nupirkta",
                onClick = onMarkPurchased,
                enabled = !isActioning,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (showInventoryActions && canAddToInventory) {
            SkautaiPrimaryButton(
                text = "Pridėti į inventorių",
                onClick = onAddToInventory,
                enabled = !isActioning,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AddPurchasedItemDialog(
    request: RequisitionDto,
    inventoryItems: List<ItemDto>,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?, String?) -> Unit
) {
    val line = request.items.firstOrNull { it.itemId == null } ?: return
    var action by remember { mutableStateOf("NEW_ITEM") }
    var expanded by remember { mutableStateOf(false) }
    var selectedExistingItemId by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    val selectedItem = inventoryItems.firstOrNull { it.id == selectedExistingItemId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pridėti nupirktą daiktą") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(line.itemName, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { action = "NEW_ITEM" }, enabled = !isSubmitting) {
                        Text("Naujas įrašas")
                    }
                    OutlinedButton(onClick = { action = "RESTOCK_EXISTING" }, enabled = !isSubmitting) {
                        Text("Papildymas")
                    }
                }
                if (action == "RESTOCK_EXISTING") {
                    Box {
                        OutlinedTextField(
                            value = selectedItem?.name ?: "Pasirink daiktą",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Papildomas daiktas") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(onClick = { expanded = true }, modifier = Modifier.align(Alignment.CenterEnd)) {
                            Text("Rinktis")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            inventoryItems.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text("${item.name} (${item.quantity} vnt.)") },
                                    onClick = {
                                        selectedExistingItemId = item.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                SkautaiTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Pastabos",
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSubmitting && (action == "NEW_ITEM" || selectedExistingItemId != null),
                onClick = { onConfirm(line.id, action, selectedExistingItemId, notes) }
            ) { Text("Pridėti") }
        },
        dismissButton = {
            TextButton(enabled = !isSubmitting, onClick = onDismiss) { Text("Uždaryti") }
        }
    )
}

@Composable
private fun RequisitionInfoRow(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontWeight = FontWeight.Medium)
    }
}
