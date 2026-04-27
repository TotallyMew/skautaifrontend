package lt.skautai.android.ui.reservations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.ReservationDto
import lt.skautai.android.data.remote.ReservationItemDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiEmptyState
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.SkautaiSummaryCard
import lt.skautai.android.ui.common.SkautaiStatusTone
import lt.skautai.android.ui.theme.ScoutStatusColors

@Composable
fun ReservationListScreen(
    onReservationClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    onModeClick: (String) -> Unit,
    refreshSignal: Boolean = false,
    onRefreshHandled: () -> Unit = {},
    viewModel: ReservationListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isMyActiveMode = viewModel.mode == "my_active"
    val isAssignedMode = viewModel.mode == "assigned"
    val isTrackedMode = viewModel.mode == "tracked"

    androidx.compose.runtime.LaunchedEffect(refreshSignal) {
        if (refreshSignal) {
            viewModel.loadReservations()
            onRefreshHandled()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is ReservationListUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is ReservationListUiState.Error -> {
                SkautaiErrorState(
                    message = state.message,
                    onRetry = viewModel::loadReservations,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is ReservationListUiState.Success -> {
                if (state.reservations.isEmpty()) {
                    SkautaiEmptyState(
                        title = when {
                            isAssignedMode -> "Tvirtinimu nera"
                            isMyActiveMode -> "Aktyviu rezervaciju nera"
                            isTrackedMode -> "Sekamu rezervaciju nera"
                            else -> "Rezervaciju dar nera"
                        },
                        subtitle = when {
                            isAssignedMode -> "Siuo metu nera rezervaciju, kurios lauktu tavo patvirtinimo."
                            isMyActiveMode -> "Cia matysi tik savo patvirtintas ir aktyvias rezervacijas."
                            isTrackedMode -> "Cia matysi patvirtintas rezervacijas, kurias reikia isduoti arba priimti."
                            else -> "Rezervacija skirta uzsakyti jau esama inventoriaus daikta konkreciam laikotarpiui."
                        },
                        icon = Icons.Default.EventAvailable,
                        actionLabel = when {
                            isAssignedMode || isTrackedMode -> "Rodyti mano rezervacijas"
                            else -> "Kurti rezervacija"
                        },
                        onAction = {
                            if (isAssignedMode || isTrackedMode) {
                                onModeClick("my_active")
                            } else {
                                onCreateClick()
                            }
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        item {
                            ReservationModeHeader(
                                mode = viewModel.mode,
                                myCount = state.myCount,
                                assignedCount = state.assignedCount,
                                trackedCount = state.trackedCount,
                                onModeClick = onModeClick
                            )
                        }
                        items(state.reservations, key = { it.id }) { reservation ->
                            ReservationCard(
                                reservation = reservation,
                                onClick = { onReservationClick(reservation.id) }
                            )
                        }
                    }
                }

                if (!isAssignedMode) {
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
}

@Composable
private fun ReservationModeHeader(
    mode: String,
    myCount: Int,
    assignedCount: Int,
    trackedCount: Int,
    onModeClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SkautaiSummaryCard(
            title = when (mode) {
                "assigned" -> "Man skirta tvirtinti"
                "my_active" -> "Mano rezervacijos"
                "tracked" -> "Sekamos rezervacijos"
                else -> "Visos rezervacijos"
            },
            subtitle = when (mode) {
                "assigned" -> "Rezervacijos, kurios laukia tavo sprendimo."
                "my_active" -> "Tik tavo patvirtintos ir aktyvios rezervacijos."
                "tracked" -> "Patvirtintos rezervacijos, kurias reikia isduoti arba priimti."
                else -> "Cia matai visa rezervaciju istorija."
            },
            metrics = listOf(
                "Mano" to myCount.toString(),
                "Skirtos" to assignedCount.toString(),
                "Sekamos" to trackedCount.toString()
            ),
            foresty = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReservationModeTile(
                title = "Mano",
                count = myCount,
                selected = mode == "my_active",
                icon = Icons.Default.EventAvailable,
                onClick = { onModeClick("my_active") },
                modifier = Modifier.weight(1f)
            )
            ReservationModeTile(
                title = "Man skirta",
                count = assignedCount,
                selected = mode == "assigned",
                icon = Icons.Default.Inbox,
                onClick = { onModeClick("assigned") },
                modifier = Modifier.weight(1f)
            )
            ReservationModeTile(
                title = "Sekamos",
                count = trackedCount,
                selected = mode == "tracked",
                icon = Icons.Default.Visibility,
                onClick = { onModeClick("tracked") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ReservationModeTile(
    title: String,
    count: Int,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null)
            Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun ReservationCard(
    reservation: ReservationDto,
    onClick: () -> Unit
) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reservation.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${reservation.startDate.take(10)} - ${reservation.endDate.take(10)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ReservationStatusChip(status = reservation.status)
            }
            ReservationPhysicalStatusPill(status = reservation.items.physicalStatus())
            Text(
                text = "${reservation.totalItems} daiktu rusys / ${reservation.totalQuantity} vnt.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = reservation.items.joinToString(limit = 3, truncated = "...") {
                    "${it.itemName} x${it.quantity}"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ReservationPhysicalStatusPill(status: ReservationPhysicalStatus) {
    val (label, container, content) = when (status) {
        ReservationPhysicalStatus.NOT_ISSUED -> Triple(
            "Neisduota",
            ScoutStatusColors.NeutralContainer,
            ScoutStatusColors.OnNeutralContainer
        )
        ReservationPhysicalStatus.PARTIALLY_ISSUED -> Triple(
            "Dalinai isduota",
            ScoutStatusColors.PendingContainer,
            ScoutStatusColors.OnPendingContainer
        )
        ReservationPhysicalStatus.ISSUED -> Triple(
            "Isduota",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        ReservationPhysicalStatus.MARKED_RETURNED -> Triple(
            "Grazinta, laukia gavimo",
            ScoutStatusColors.PendingContainer,
            ScoutStatusColors.OnPendingContainer
        )
        ReservationPhysicalStatus.RECEIVED -> Triple(
            "Gauta",
            ScoutStatusColors.InfoContainer,
            ScoutStatusColors.OnInfoContainer
        )
    }
    SkautaiStatusPill(label = label, containerColor = container, contentColor = content)
}

enum class ReservationPhysicalStatus {
    NOT_ISSUED,
    PARTIALLY_ISSUED,
    ISSUED,
    MARKED_RETURNED,
    RECEIVED
}

fun List<ReservationItemDto>.physicalStatus(): ReservationPhysicalStatus {
    val total = sumOf { it.quantity }
    val issued = sumOf { it.issuedQuantity }
    val markedReturned = sumOf { it.markedReturnedQuantity }
    val received = sumOf { it.returnedQuantity }
    return when {
        total == 0 || issued == 0 -> ReservationPhysicalStatus.NOT_ISSUED
        received >= total -> ReservationPhysicalStatus.RECEIVED
        markedReturned > received -> ReservationPhysicalStatus.MARKED_RETURNED
        issued < total -> ReservationPhysicalStatus.PARTIALLY_ISSUED
        else -> ReservationPhysicalStatus.ISSUED
    }
}

@Composable
fun ReservationStatusChip(status: String) {
    val (label, container, content) = when (status) {
        "PENDING" -> Triple("Laukia", ScoutStatusColors.PendingContainer, ScoutStatusColors.OnPendingContainer)
        "APPROVED" -> Triple("Patvirtinta", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        "ACTIVE" -> Triple("Aktyvi", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        "RETURNED" -> Triple("Grazinta", ScoutStatusColors.InfoContainer, ScoutStatusColors.OnInfoContainer)
        "CANCELLED" -> Triple("Atsaukta", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        "REJECTED" -> Triple("Atmesta", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        else -> Triple(status, ScoutStatusColors.NeutralContainer, ScoutStatusColors.OnNeutralContainer)
    }
    SkautaiStatusPill(label = label, containerColor = container, contentColor = content)
}
