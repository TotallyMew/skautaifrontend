package lt.skautai.android.ui.reservations

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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.ReservationDto
import lt.skautai.android.data.remote.ReservationItemDto
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiConfirmDialog
import lt.skautai.android.ui.common.SkautaiDangerButton
import lt.skautai.android.ui.common.SkautaiPrimaryButton
import lt.skautai.android.ui.common.SkautaiSecondaryButton
import lt.skautai.android.ui.reservations.physicalStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationDetailScreen(
    reservationId: String,
    onBack: () -> Unit,
    onIssue: () -> Unit,
    onReturn: () -> Unit,
    onMarkReturned: () -> Unit,
    viewModel: ReservationDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val userId by viewModel.userId.collectAsStateWithLifecycle()
    val activeOrgUnitId by viewModel.activeOrgUnitId.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCancelDialog by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

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
        SkautaiConfirmDialog(
            title = "Atšaukti rezervaciją",
            message = "Ar tikrai norite atšaukti šią rezervaciją?",
            confirmText = "Atšaukti",
            dismissText = "Uždaryti",
            isDanger = true,
            onConfirm = {
                    showCancelDialog = false
                    viewModel.cancelReservation(reservationId)
            },
            onDismiss = { showCancelDialog = false }
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
        snackbarHost = { SkautaiErrorSnackbarHost(hostState = snackbarHostState) }
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
                    SkautaiErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadReservation(reservationId) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is ReservationDetailUiState.Success -> {
                    val canReviewUnit = state.reservation.canReviewUnit(permissions, activeOrgUnitId)
                    val canReviewTopLevel = state.reservation.canReviewTopLevel(permissions)
                    ReservationDetailContent(
                        reservation = state.reservation,
                        isCancelling = state.isCancelling,
                        canReviewUnit = canReviewUnit,
                        canReviewTopLevel = canReviewTopLevel,
                        canManageMovements = state.reservation.canManageMovements(permissions, activeOrgUnitId),
                        isOwnReservation = state.reservation.reservedByUserId == userId,
                        canCancel = state.reservation.canBeCancelledBy(userId),
                        onCancel = { showCancelDialog = true },
                        onApproveUnit = { viewModel.reviewUnitReservation(reservationId, "APPROVED") },
                        onRejectUnit = { viewModel.reviewUnitReservation(reservationId, "REJECTED") },
                        onApproveTopLevel = { viewModel.reviewTopLevelReservation(reservationId, "APPROVED") },
                        onRejectTopLevel = { viewModel.reviewTopLevelReservation(reservationId, "REJECTED") },
                        onIssue = onIssue,
                        onReturn = onReturn,
                        onMarkReturned = onMarkReturned,
                        onPickupTimePropose = { value -> viewModel.updatePickupTime(reservationId, value, "PROPOSE") },
                        onPickupTimeAccept = { viewModel.updatePickupTime(reservationId, null, "ACCEPT") },
                        onReturnTimePropose = { value -> viewModel.updateReturnTime(reservationId, value, "PROPOSE") },
                        onReturnTimeAccept = { viewModel.updateReturnTime(reservationId, null, "ACCEPT") }
                    )
                }
            }
        }
    }
}

private fun ReservationDto.canReviewUnit(
    permissions: Set<String>,
    activeOrgUnitId: String?
): Boolean =
    status == "PENDING" &&
        (unitReviewStatus == "PENDING" ||
            (unitReviewStatus == null && items.any { it.custodianId != null })) &&
        permissions.canApproveUnitReservations() &&
        items.any { it.custodianId != null && it.custodianId == activeOrgUnitId }

private fun ReservationDto.canReviewTopLevel(
    permissions: Set<String>
): Boolean =
    status == "PENDING" &&
        (topLevelReviewStatus == "PENDING" ||
            (topLevelReviewStatus == null && items.any { it.custodianId == null })) &&
        permissions.canApproveTopLevelReservations() &&
        items.any { it.custodianId == null }

private fun Set<String>.canApproveTopLevelReservations(): Boolean =
    "reservations.approve:ALL" in this

private fun Set<String>.canApproveUnitReservations(): Boolean =
    "reservations.approve:OWN_UNIT" in this || canApproveTopLevelReservations()

private fun ReservationDto.canManageMovements(
    permissions: Set<String>,
    activeOrgUnitId: String?
): Boolean =
    items.any { item ->
        if (item.custodianId == null) {
            permissions.canApproveTopLevelReservations()
        } else {
            permissions.canApproveOwnUnitReservations() && item.custodianId == activeOrgUnitId
        }
    }

private fun Set<String>.canApproveOwnUnitReservations(): Boolean =
    "reservations.approve:OWN_UNIT" in this

private fun ReservationDto.canBeCancelledBy(userId: String?): Boolean =
    userId != null &&
        reservedByUserId == userId &&
        status in listOf("PENDING", "APPROVED")

@Composable
private fun ReservationDetailContent(
    reservation: ReservationDto,
    isCancelling: Boolean,
    canReviewUnit: Boolean,
    canReviewTopLevel: Boolean,
    canManageMovements: Boolean,
    isOwnReservation: Boolean,
    canCancel: Boolean,
    onCancel: () -> Unit,
    onApproveUnit: () -> Unit,
    onRejectUnit: () -> Unit,
    onApproveTopLevel: () -> Unit,
    onRejectTopLevel: () -> Unit,
    onIssue: () -> Unit,
    onReturn: () -> Unit,
    onMarkReturned: () -> Unit,
    onPickupTimePropose: (String) -> Unit,
    onPickupTimeAccept: () -> Unit,
    onReturnTimePropose: (String) -> Unit,
    onReturnTimeAccept: () -> Unit
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
                    text = reservation.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Esamo inventoriaus rezervacija pagal datas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ReservationStatusChip(status = reservation.status)
                ReservationPhysicalStatusPill(status = reservation.items.physicalStatus())
            }
        }

        ReservationTimingCard(reservation)

        ReservationReviewCard(reservation)

        if (reservation.status in listOf("APPROVED", "ACTIVE")) {
            TimeProposalCard(
                title = "Atsiėmimo laikas",
                currentTime = reservation.pickupAt,
                currentLocationPath = reservation.pickupLocationPath,
                proposalStatus = reservation.pickupProposalStatus,
                proposedByUserId = reservation.pickupProposedByUserId,
                reservedByUserId = reservation.reservedByUserId,
                isOwnReservation = isOwnReservation,
                onPropose = onPickupTimePropose,
                onAccept = onPickupTimeAccept
            )
        }

        if (reservation.status == "ACTIVE") {
            TimeProposalCard(
                title = "Grąžinimo laikas",
                currentTime = reservation.returnAt,
                currentLocationPath = reservation.returnLocationPath,
                proposalStatus = reservation.returnProposalStatus,
                proposedByUserId = reservation.returnProposedByUserId,
                reservedByUserId = reservation.reservedByUserId,
                isOwnReservation = isOwnReservation,
                onPropose = onReturnTimePropose,
                onAccept = onReturnTimeAccept
            )
        }

        if (reservation.status in listOf("APPROVED", "ACTIVE")) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Išdavimas ir grąžinimas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    HorizontalDivider()
                    if (canManageMovements) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SkautaiPrimaryButton(
                                text = "Išduoti",
                                onClick = onIssue,
                                enabled = reservation.items.any { it.remainingToIssue > 0 },
                                modifier = Modifier.weight(1f)
                            )
                            SkautaiSecondaryButton(
                                text = "Pažymėti gautą",
                                onClick = onReturn,
                                enabled = reservation.items.any { it.remainingToReceive > 0 },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (isOwnReservation) {
                        SkautaiSecondaryButton(
                            text = "Pažymėti grąžintą",
                            onClick = onMarkReturned,
                            enabled = reservation.items.any { it.remainingToMarkReturned > 0 },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
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
                Text(
                    text = "Rezervuoti daiktai",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                HorizontalDivider()
                reservation.items.forEach { item ->
                    ReservationItemRow(item)
                }
            }
        }

        if (canReviewUnit || canReviewTopLevel || canCancel) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Rezervacijos veiksmai",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    HorizontalDivider()
                    if (canReviewUnit) {
                        ReviewActionRow(
                            approveText = "Patvirtinti vieneto dalį",
                            rejectText = "Atmesti vieneto dalį",
                            onApprove = onApproveUnit,
                            onReject = onRejectUnit
                        )
                    }
                    if (canReviewTopLevel) {
                        ReviewActionRow(
                            approveText = "Patvirtinti tunto dalį",
                            rejectText = "Atmesti tunto dalį",
                            onApprove = onApproveTopLevel,
                            onReject = onRejectTopLevel
                        )
                    }
                    if (canCancel) {
                        SkautaiDangerButton(
                            text = "Atšaukti rezervaciją",
                            onClick = onCancel,
                            enabled = !isCancelling,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun ReservationTimingCard(reservation: ReservationDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    text = "${reservation.startDate.take(10)} - ${reservation.endDate.take(10)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider()
            reservation.reservedByName?.takeIf { it.isNotBlank() }?.let {
                ReservationInfoRow("Rezervavo", it)
            }
            reservation.requestingUnitName?.takeIf { it.isNotBlank() }?.let {
                ReservationInfoRow("Vienetas", it)
            }
            ReservationInfoRow("Daiktų rūšys", reservation.totalItems.toString())
            ReservationInfoRow("Bendras kiekis", reservation.totalQuantity.toString())
            reservation.notes?.takeIf { it.isNotBlank() }?.let {
                ReservationInfoRow("Pastabos", it)
            }
            ReservationInfoRow("Sukurta", reservation.createdAt.take(10))
        }
    }
}

@Composable
private fun TimeProposalCard(
    title: String,
    currentTime: String?,
    currentLocationPath: String?,
    proposalStatus: String,
    proposedByUserId: String?,
    reservedByUserId: String,
    isOwnReservation: Boolean,
    onPropose: (String) -> Unit,
    onAccept: () -> Unit
) {
    val initialDateTime = remember(currentTime) { parseProposalDateTime(currentTime) }
    var selectedDate by remember(currentTime) { mutableStateOf(initialDateTime.first) }
    var selectedHour by remember(currentTime) { mutableStateOf(initialDateTime.second) }
    var selectedMinute by remember(currentTime) { mutableStateOf(initialDateTime.third) }
    var showDatePicker by remember { mutableStateOf(false) }
    val proposalByOwner = proposedByUserId == reservedByUserId
    val canAcceptProposal = proposalStatus == "PENDING" &&
        currentTime != null &&
        proposalByOwner != isOwnReservation
    val proposalValue = selectedDate?.let { date ->
        "%sT%02d:%02d:00Z".format(date, selectedHour, selectedMinute)
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
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            ReservationInfoRow(
                "Būsena",
                when (proposalStatus) {
                    "PENDING" -> "Laukia atsakymo"
                    "ACCEPTED" -> "Suderinta"
                    else -> "Dar nepasiūlyta"
                }
            )
            currentTime?.let {
                ReservationInfoRow("Siūlomas laikas", it.take(16).replace("T", " "))
            }
            currentLocationPath?.takeIf { it.isNotBlank() }?.let {
                ReservationInfoRow("Lokacija", it)
            }
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedDate?.toString() ?: "Pasirinkti datą")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeStepper(
                    label = "Valanda",
                    value = "%02d".format(selectedHour),
                    onDecrease = { selectedHour = (selectedHour + 23) % 24 },
                    onIncrease = { selectedHour = (selectedHour + 1) % 24 },
                    modifier = Modifier.weight(1f)
                )
                TimeStepper(
                    label = "Minutės",
                    value = "%02d".format(selectedMinute),
                    onDecrease = { selectedMinute = (selectedMinute + 55) % 60 },
                    onIncrease = { selectedMinute = (selectedMinute + 5) % 60 },
                    modifier = Modifier.weight(1f)
                )
            }
            if (canAcceptProposal) {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Priimti siūlomą laiką")
                }
            }
            Button(
                onClick = { proposalValue?.let(onPropose) },
                enabled = proposalValue != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (proposalStatus == "PENDING") "Siūlyti kitą laiką" else "Siūlyti laiką")
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                ?.atStartOfDay()
                ?.toInstant(ZoneOffset.UTC)
                ?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) {
                    Text("Pasirinkti")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Atšaukti")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun TimeStepper(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onDecrease) { Text("-") }
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = onIncrease) { Text("+") }
            }
        }
    }
}

private fun parseProposalDateTime(value: String?): Triple<LocalDate?, Int, Int> {
    if (value.isNullOrBlank()) return Triple(null, 12, 0)
    return runCatching {
        val normalized = if (value.endsWith("Z")) value else "${value.take(19)}Z"
        val dateTime = Instant.parse(normalized).atZone(ZoneOffset.UTC).toLocalDateTime()
        Triple(dateTime.toLocalDate(), dateTime.hour, (dateTime.minute / 5) * 5)
    }.getOrDefault(Triple(null, 12, 0))
}

@Composable
private fun ReservationReviewCard(reservation: ReservationDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Patvirtinimai",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            ReservationInfoRow("Vieneto patvirtinimas", reservation.unitReviewStatus.toLithuanianReviewStatus())
            ReservationInfoRow("Tunto patvirtinimas", reservation.topLevelReviewStatus.toLithuanianReviewStatus())
        }
    }
}

@Composable
private fun ReviewActionRow(
    approveText: String,
    rejectText: String,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SkautaiPrimaryButton(
            text = approveText,
            onClick = onApprove,
            modifier = Modifier.weight(1f)
        )
        SkautaiDangerButton(
            text = rejectText,
            onClick = onReject,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ReservationItemRow(item: ReservationItemDto) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.itemName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            item.remainingAfterReservation?.let { remaining ->
                Text(
                    text = "Liks: $remaining • Išduota: ${item.issuedQuantity} • Grąžinta: ${item.returnedQuantity}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (remaining == 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                    }
                )
            }
        }
        Text(
            text = "x${item.quantity}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun String?.toLithuanianReviewStatus(): String =
    when (this) {
        "NOT_REQUIRED" -> "Nereikia"
        "PENDING" -> "Laukia"
        "APPROVED" -> "Patvirtinta"
        "REJECTED" -> "Atmesta"
        null -> "Nenurodyta"
        else -> this
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
