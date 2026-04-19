package lt.skautai.android.ui.reservations

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
import lt.skautai.android.data.remote.ReservationDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationDetailScreen(
    reservationId: String,
    onBack: () -> Unit,
    viewModel: ReservationDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCancelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(reservationId) {
        viewModel.loadReservation(reservationId)
    }

    LaunchedEffect((uiState as? ReservationDetailUiState.Success)?.error) {
        (uiState as? ReservationDetailUiState.Success)?.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Atšaukti rezervaciją") },
            text = { Text("Ar tikrai norite atšaukti šią rezervaciją?") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    viewModel.cancelReservation(reservationId)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rezervacija") },
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
                is ReservationDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is ReservationDetailUiState.Error -> {
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
                        Button(onClick = { viewModel.loadReservation(reservationId) }) {
                            Text("Bandyti dar kartą")
                        }
                    }
                }

                is ReservationDetailUiState.Success -> {
                    ReservationDetailContent(
                        reservation = state.reservation,
                        isCancelling = state.isCancelling,
                        onCancel = { showCancelDialog = true },
                        onApprove = { viewModel.approveReservation(reservationId) },
                        onReject = { viewModel.rejectReservation(reservationId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReservationDetailContent(
    reservation: ReservationDto,
    isCancelling: Boolean,
    onCancel: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
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
                text = reservation.itemName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            ReservationStatusChip(status = reservation.status)
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
                ReservationInfoRow("Kiekis", reservation.quantity.toString())
                ReservationInfoRow("Pradžia", reservation.startDate.take(10))
                ReservationInfoRow("Pabaiga", reservation.endDate.take(10))
                reservation.notes?.let {
                    ReservationInfoRow("Pastabos", it)
                }
                ReservationInfoRow("Sukurta", reservation.createdAt.take(10))
            }
        }

        if (reservation.status == "PENDING") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Veiksmai",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onApprove,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Patvirtinti")
                        }
                        OutlinedButton(
                            onClick = onReject,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Atmesti")
                        }
                    }
                    OutlinedButton(
                        onClick = onCancel,
                        enabled = !isCancelling,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        if (isCancelling) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text("Atšaukti rezervaciją")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReservationInfoRow(label: String, value: String) {
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