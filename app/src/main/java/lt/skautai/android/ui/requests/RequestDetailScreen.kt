package lt.skautai.android.ui.requests

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
            title = { Text("Atšaukti prašymą") },
            text = { Text("Ar tikrai norite atšaukti šį prašymą?") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    viewModel.cancelRequest(requestId)
                }) {
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

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Atmesti prašymą") },
            text = {
                Column {
                    Text("Atmetimo priežastis (neprivaloma):")
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
                    Text("Uždaryti")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prašymas") },
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
                            Text("Bandyti dar kartą")
                        }
                    }
                }

                is RequestDetailUiState.Success -> {
                    RequestDetailContent(
                        request = state.request,
                        isActioning = state.isActioning,
                        canForwardReview = "items.request.forward.bendras" in permissions,
                        canApproveReview = "items.request.approve.bendras" in permissions,
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = request.itemName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            RequestStatusChip(status = request.topLevelStatus)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Detalės",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                HorizontalDivider()
                request.itemDescription?.let { RequestInfoRow("Aprašymas", it) }
                RequestInfoRow("Kiekis", request.quantity.toString())
                request.neededByDate?.let { RequestInfoRow("Reikalinga iki", it.take(10)) }
                request.requestingUnitName?.let { RequestInfoRow("Draugovė", it) }
                request.notes?.let { RequestInfoRow("Pagrindimas", it) }
                RequestInfoRow("Sukurta", request.createdAt.take(10))
            }
        }

        if (request.needsDraugininkasApproval) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Draugininko peržiūra",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    HorizontalDivider()
                    RequestInfoRow(
                        "Būsena",
                        when (request.draugininkasStatus) {
                            "PENDING" -> "Laukiama"
                            "FORWARDED" -> "Perduota"
                            "REJECTED" -> "Atmesta"
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
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Atmesti")
                            }
                        }
                    }
                }
            }
        }

        if (canApproveReview &&
            request.topLevelStatus == "PENDING" &&
            (!request.needsDraugininkasApproval || request.draugininkasStatus == "FORWARDED")
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Tuntininko sprendimas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    HorizontalDivider()
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
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Atmesti")
                        }
                    }
                }
            }
        }

        if (request.topLevelStatus == "PENDING") {
            OutlinedButton(
                onClick = onCancel,
                enabled = !isActioning,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isActioning) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("Atšaukti prašymą")
                }
            }
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}