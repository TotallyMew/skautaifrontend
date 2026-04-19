package lt.skautai.android.ui.reservations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.ReservationDto

@Composable
fun ReservationListScreen(
    onReservationClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    viewModel: ReservationListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadReservations()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is ReservationListUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is ReservationListUiState.Error -> {
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
                    Button(onClick = viewModel::loadReservations) {
                        Text("Bandyti dar kartą")
                    }
                }
            }

            is ReservationListUiState.Success -> {
                if (state.reservations.isEmpty()) {
                    Text(
                        text = "Nėra rezervacijų",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(state.reservations, key = { it.id }) { reservation ->
                            ReservationCard(
                                reservation = reservation,
                                onClick = { onReservationClick(reservation.id) }
                            )
                        }
                    }
                }

                FloatingActionButton(
                    onClick = onCreateClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nauja rezervacija")
                }
            }
        }
    }
}

@Composable
fun ReservationCard(
    reservation: ReservationDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reservation.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                ReservationStatusChip(status = reservation.status)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${reservation.startDate.take(10)} — ${reservation.endDate.take(10)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Kiekis: ${reservation.quantity}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ReservationStatusChip(status: String) {
    val (label, color) = when (status) {
        "PENDING" -> "Laukiama" to MaterialTheme.colorScheme.tertiary
        "APPROVED" -> "Patvirtinta" to MaterialTheme.colorScheme.primary
        "ACTIVE" -> "Aktyvi" to MaterialTheme.colorScheme.primary
        "RETURNED" -> "Grąžinta" to MaterialTheme.colorScheme.onSurfaceVariant
        "CANCELLED" -> "Atšaukta" to MaterialTheme.colorScheme.error
        "REJECTED" -> "Atmesta" to MaterialTheme.colorScheme.error
        else -> status to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = color
    )
}