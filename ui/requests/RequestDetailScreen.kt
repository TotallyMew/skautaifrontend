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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.BendrasRequestDto

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
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Atsaukti paemimo prasyma") },
            text = { Text("Ar tikrai nori atsaukti si prasyma paimti daikta is bendro inventoriaus?") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    viewModel.cancelRequest(requestId)
                }) {
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

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Atmesti paemimo prasyma") },
            text = {
                Column {
                    Text("Atmetimo priezastis (neprivaloma):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
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
                    Text("Uzdaryti")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paemimo prasymas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atgal")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadRequest(requestId) }) {
                            Text("Bandyti dar karta")
                        }
                    }
                }

                is RequestDetailUiState.Success -> {
                    val request = state.request
                    val isOwnRequest = request.requestedByUserId == currentUserId
                    val isOwnUnitRequest = request.requestingUnitId != null &&
                        request.requestingUnitId == activeOrgUnitId
                    val canForwardReview = "items.request.forward.bendras" in permissions && isOwnUnitRequest
                    val canApproveReview = "items.request.approve.bendras" in permissions
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
                    text = request.itemName,
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
                Text("Paemimo informacija", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                HorizontalDivider()
                request.itemDescription?.let { RequestInfoRow("Aprasymas", it) }
                RequestInfoRow("Kiekis", request.quantity.toString())
                request.neededByDate?.let { RequestInfoRow("Reikalinga iki", it.take(10)) }
                request.requestingUnitName?.let { RequestInfoRow("Vienetas", it) }
                request.notes?.let { RequestInfoRow("Pastabos", it) }
                RequestInfoRow("Sukurta", request.createdAt.take(10))
            }
        }

        if (request.needsDraugininkasApproval) {
            ReviewCard(title = "Vieneto perziura") {
                RequestInfoRow(
                    "Busena",
                    when (request.draugininkasStatus) {
                        "PENDING" -> "Laukia vieneto sprendimo"
                        "FORWARDED" -> "Perduota inventorininkui"
                        "REJECTED" -> "Atmesta vienete"
                        else -> request.draugininkasStatus ?: "Nera"
                    }
                )
                request.draugininkasRejectionReason?.let {
                    RequestInfoRow("Atmetimo priezastis", it)
                }

                if (request.draugininkasStatus == "PENDING" && canForwardReview) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onDraugininkasForward,
                            enabled = !isActioning,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Perduoti")
                        }
                        OutlinedButton(
                            onClick = onDraugininkasReject,
                            enabled = !isActioning,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Atmesti")
                        }
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
                    Button(
                        onClick = onTopLevelApprove,
                        enabled = !isActioning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Patvirtinti")
                    }
                    OutlinedButton(
                        onClick = onTopLevelReject,
                        enabled = !isActioning,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Atmesti")
                    }
                }
            }
        }

        if (request.topLevelStatus == "PENDING" && canCancel) {
            OutlinedButton(
                onClick = onCancel,
                enabled = !isActioning,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                if (isActioning) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text("Atsaukti paemimo prasyma")
                }
            }
        }
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
