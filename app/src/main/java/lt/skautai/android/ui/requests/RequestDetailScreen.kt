package lt.skautai.android.ui.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import lt.skautai.android.data.remote.BendrasRequestDto
import lt.skautai.android.data.remote.BendrasRequestItemDto
import lt.skautai.android.ui.common.SkautaiConfirmDialog
import lt.skautai.android.ui.common.SkautaiDangerButton
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiPrimaryButton
import lt.skautai.android.ui.common.SkautaiTextField
import lt.skautai.android.util.hasPermission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    requestId: String,
    onBack: () -> Unit,
    viewModel: RequestDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val activeOrgUnitId by viewModel.activeOrgUnitId.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }
    var rejectTarget by remember { mutableStateOf("") }

    LaunchedEffect(requestId) {
        viewModel.loadRequest(requestId)
    }

    LaunchedEffect((uiState as? RequestDetailUiState.Success)?.error) {
        (uiState as? RequestDetailUiState.Success)?.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (showCancelDialog) {
        SkautaiConfirmDialog(
            title = "Atšaukti paėmimo prašymą",
            message = "Ar tikrai nori atšaukti šį prašymą paimti daiktą iš bendro inventoriaus?",
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

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Atmesti paėmimo prašymą") },
            text = {
                Column {
                    Text("Atmetimo priežastis (neprivaloma):")
                    Spacer(modifier = Modifier.height(8.dp))
                    SkautaiTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = "Priežastis",
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showRejectDialog = false
                    when (rejectTarget) {
                        "draugininkas" -> viewModel.draugininkasReject(requestId, rejectReason.ifBlank { null })
                        "toplevel" -> viewModel.topLevelReject(requestId, rejectReason.ifBlank { null })
                    }
                    rejectReason = ""
                    rejectTarget = ""
                }) {
                    Text("Atmesti", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRejectDialog = false
                    rejectReason = ""
                    rejectTarget = ""
                }) {
                    Text("Uždaryti")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paėmimo prašymas") },
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
                is RequestDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is RequestDetailUiState.Error -> {
                    SkautaiErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadRequest(requestId) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is RequestDetailUiState.Success -> {
                    val request = state.request
                    val isOwnRequest = request.requestedByUserId == currentUserId
                    val isOwnUnitRequest = request.requestingUnitId != null &&
                        request.requestingUnitId == activeOrgUnitId
                    val canForwardReview = permissions.hasPermission("items.request.forward.bendras") && isOwnUnitRequest
                    val canApproveReview = permissions.hasPermission("items.request.approve.bendras")
                    val canCancel = isOwnRequest

                    RequestDetailContent(
                        request = request,
                        isActioning = state.isActioning,
                        canForwardReview = canForwardReview,
                        canApproveReview = canApproveReview,
                        canCancel = canCancel,
                        onCancel = { showCancelDialog = true },
                        onDraugininkasForward = { viewModel.draugininkasForward(requestId) },
                        onDraugininkasReject = {
                            rejectTarget = "draugininkas"
                            showRejectDialog = true
                        },
                        onTopLevelApprove = { viewModel.topLevelApprove(requestId) },
                        onTopLevelReject = {
                            rejectTarget = "toplevel"
                            showRejectDialog = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestDetailContent(
    request: BendrasRequestDto,
    isActioning: Boolean,
    canForwardReview: Boolean,
    canApproveReview: Boolean,
    canCancel: Boolean,
    onCancel: () -> Unit,
    onDraugininkasForward: () -> Unit,
    onDraugininkasReject: () -> Unit,
    onTopLevelApprove: () -> Unit,
    onTopLevelReject: () -> Unit
) {
    val requestItems = request.items.ifEmpty {
        request.itemId?.let {
            listOf(
                BendrasRequestItemDto(
                    id = request.id,
                    itemId = it,
                    itemName = request.itemName,
                    quantity = request.quantity
                )
            )
        }.orEmpty()
    }
    val totalQuantity = requestItems.sumOf { it.quantity }.takeIf { it > 0 } ?: request.quantity
    val title = when {
        requestItems.size > 1 -> "Paėmimo prašymas: ${requestItems.size} daiktai"
        requestItems.size == 1 -> requestItems.first().itemName
        else -> request.itemName
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Esamas bendro tunto inventoriaus daiktas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RequestStatusChip(status = request.topLevelStatus)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Paėmimo informacija", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                HorizontalDivider()
                request.itemDescription?.let { RequestInfoRow("Aprašymas", it) }
                RequestInfoRow("Bendras kiekis", totalQuantity.toString())
                request.neededByDate?.let { RequestInfoRow("Reikalinga iki", it.take(10)) }
                request.requestingUnitName?.let { RequestInfoRow("Vienetas", it) }
                request.requestedByUserName?.takeIf { it.isNotBlank() }?.let { RequestInfoRow("Kas prašo", it) }
                request.notes?.let { RequestInfoRow("Pastabos", it) }
                RequestInfoRow("Sukurta", request.createdAt.take(10))
            }
        }

        Card(
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
                requestItems.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider()
                    BendrasRequestItemRow(item)
                }
            }
        }

        if (request.needsDraugininkasApproval) {
            ReviewCard(title = "Vieneto peržiūra") {
                RequestInfoRow(
                    "Būsena",
                    when (request.draugininkasStatus) {
                        "PENDING" -> "Laukia vieneto sprendimo"
                        "FORWARDED" -> "Perduota inventorininkui"
                        "REJECTED" -> "Atmesta vienete"
                        else -> request.draugininkasStatus ?: "Nėra"
                    }
                )
                request.draugininkasRejectionReason?.let {
                    RequestInfoRow("Atmetimo priežastis", it)
                }

                if (request.draugininkasStatus == "PENDING" && canForwardReview) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SkautaiPrimaryButton(
                            text = "Perduoti",
                            onClick = onDraugininkasForward,
                            enabled = !isActioning,
                            modifier = Modifier.weight(1f)
                        )
                        SkautaiDangerButton(
                            text = "Atmesti",
                            onClick = onDraugininkasReject,
                            enabled = !isActioning,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (canApproveReview &&
            request.topLevelStatus == "PENDING" &&
            (!request.needsDraugininkasApproval || request.draugininkasStatus == "FORWARDED")
        ) {
            ReviewCard(title = "Inventorininko / tuntininko sprendimas") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SkautaiPrimaryButton(
                        text = "Patvirtinti",
                        onClick = onTopLevelApprove,
                        enabled = !isActioning,
                        modifier = Modifier.weight(1f)
                    )
                    SkautaiDangerButton(
                        text = "Atmesti",
                        onClick = onTopLevelReject,
                        enabled = !isActioning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (request.topLevelStatus == "PENDING" && canCancel) {
            SkautaiDangerButton(
                text = "Atšaukti paėmimo prašymą",
                onClick = onCancel,
                enabled = !isActioning,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BendrasRequestItemRow(item: BendrasRequestItemDto) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.itemName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = "${item.quantity} vnt.",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ReviewCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun RequestInfoRow(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
