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
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiErrorState

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
            title = { Text("Atmesti prasyma") },
            text = {
                OutlinedTextField(
                    value = rejectReason,
                    onValueChange = { rejectReason = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    label = { Text("Priezastis") }
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
                }) { Text("Uzdaryti") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pirkimo prasymas") },
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
                        request.unitReviewStatus == "PENDING"
                    val canTopLevelReview = "requisitions.approve" in permissions &&
                        request.topLevelReviewStatus == "PENDING"
                    val isOwnRequest = request.createdByUserId == currentUserId

                    RequisitionDetailContent(
                        request = request,
                        isActioning = state.isActioning,
                        isOwnRequest = isOwnRequest,
                        canUnitReview = canUnitReview,
                        canTopLevelReview = canTopLevelReview,
                        onApproveInUnit = { viewModel.approveInUnit(requestId) },
                        onForwardToTop = { viewModel.forwardToTop(requestId) },
                        onRejectInUnit = { rejectTarget = "UNIT" },
                        onApproveTop = { viewModel.approveTopLevel(requestId) },
                        onRejectTop = { rejectTarget = "TOP" }
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
    canUnitReview: Boolean,
    canTopLevelReview: Boolean,
    onApproveInUnit: () -> Unit,
    onForwardToTop: () -> Unit,
    onRejectInUnit: () -> Unit,
    onApproveTop: () -> Unit,
    onRejectTop: () -> Unit
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
            text = item?.itemName ?: "Pirkimo prasymas",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Pirkimo arba papildymo prasymas naujam / trukstamam inventoriui",
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
                Text("Detales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                HorizontalDivider()
                item?.itemDescription?.let { RequisitionInfoRow("Aprasymas", it) }
                item?.let { RequisitionInfoRow("Kiekis", "${it.quantityRequested}") }
                request.requestingUnitName?.let { RequisitionInfoRow("Vienetas", it) }
                request.neededByDate?.let { RequisitionInfoRow("Reikia iki", it) }
                request.notes?.let { RequisitionInfoRow("Pagrindimas", it) }
                RequisitionInfoRow("Sukurta", request.createdAt.take(10))
                if (isOwnRequest) {
                    RequisitionInfoRow("Kontekstas", "Tai tavo sukurtas prasymas")
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
                        Button(onClick = onApproveInUnit, enabled = !isActioning, modifier = Modifier.weight(1f)) {
                            Text("Patvirtinti vienete")
                        }
                        OutlinedButton(onClick = onForwardToTop, enabled = !isActioning, modifier = Modifier.weight(1f)) {
                            Text("Perduoti inventorininkui")
                        }
                    }
                    OutlinedButton(
                        onClick = onRejectInUnit,
                        enabled = !isActioning,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Atmesti")
                    }
                }
            }
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
                        Button(onClick = onApproveTop, enabled = !isActioning, modifier = Modifier.weight(1f)) {
                            Text("Patvirtinti")
                        }
                        OutlinedButton(onClick = onRejectTop, enabled = !isActioning, modifier = Modifier.weight(1f)) {
                            Text("Atmesti")
                        }
                    }
                }
            }
        }
    }
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
