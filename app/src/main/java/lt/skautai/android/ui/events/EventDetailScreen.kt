package lt.skautai.android.ui.events

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
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.EventRoleDto
import lt.skautai.android.data.remote.StovyklaDetailsDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    onBack: () -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCancelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    LaunchedEffect((uiState as? EventDetailUiState.Success)?.error) {
        (uiState as? EventDetailUiState.Success)?.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Atšaukti renginį") },
            text = { Text("Ar tikrai norite atšaukti šį renginį?") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    viewModel.cancelEvent(eventId)
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
                title = { Text("Renginys") },
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
                is EventDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is EventDetailUiState.Error -> {
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
                        Button(onClick = { viewModel.loadEvent(eventId) }) {
                            Text("Bandyti dar kartą")
                        }
                    }
                }

                is EventDetailUiState.Success -> {
                    EventDetailContent(
                        event = state.event,
                        isCancelling = state.isCancelling,
                        canManage = "events.manage" in permissions,
                        onCancel = { showCancelDialog = true },
                        onActivate = { viewModel.updateStatus(eventId, "ACTIVE") },
                        onComplete = { viewModel.updateStatus(eventId, "COMPLETED") }
                    )
                }
            }
        }
    }
}

@Composable
private fun EventDetailContent(
    event: EventDto,
    isCancelling: Boolean,
    canManage: Boolean,
    onCancel: () -> Unit,
    onActivate: () -> Unit,
    onComplete: () -> Unit
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
                text = event.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            EventStatusChip(status = event.status)
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
                    text = "Informacija",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                HorizontalDivider()
                EventInfoRow("Tipas", eventTypeLabel(event.type))
                EventInfoRow("Pradžia", event.startDate.take(10))
                EventInfoRow("Pabaiga", event.endDate.take(10))
                EventInfoRow("Sukurta", event.createdAt.take(10))
                event.notes?.let { EventInfoRow("Pastabos", it) }
            }
        }

        if (event.type == "STOVYKLA") {
            event.stovyklaDetails?.let {
                StovyklaDetailsCard(details = it)
            }
        }

        if (event.eventRoles.isNotEmpty()) {
            EventRolesCard(roles = event.eventRoles)
        }

        if (canManage && event.status in listOf("PLANNING", "ACTIVE")) {
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
                    if (event.status == "PLANNING") {
                        Button(
                            onClick = onActivate,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pradėti renginį")
                        }
                    }
                    if (event.status == "ACTIVE") {
                        Button(
                            onClick = onComplete,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Baigti renginį")
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
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        } else {
                            Text("Atšaukti renginį")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StovyklaDetailsCard(details: StovyklaDetailsDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Stovyklos detalės",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            details.registrationDeadline?.let {
                EventInfoRow("Registracijos terminas", it)
            }
            details.expectedParticipants?.let {
                EventInfoRow("Planuojami dalyviai", it.toString())
            }
            details.actualParticipants?.let {
                EventInfoRow("Faktiniai dalyviai", it.toString())
            }
        }
    }
}

@Composable
private fun EventRolesCard(roles: List<EventRoleDto>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Dalyviai",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            roles.forEach { role ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = eventRoleLabel(role.role),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    role.targetGroup?.let {
                        Text(
                            text = "(${eventTargetGroupLabel(it)})",
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
private fun EventInfoRow(label: String, value: String) {
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

private fun eventTypeLabel(type: String) = when (type) {
    "STOVYKLA" -> "Stovykla"
    "SUEIGA" -> "Sueiga"
    "RENGINYS" -> "Renginys"
    else -> type
}

private fun eventRoleLabel(role: String) = when (role) {
    "VIRSININKAS" -> "Viršininkas"
    "KOMENDANTAS" -> "Komendantas"
    "UKVEDYS" -> "Ūkvedys"
    "PASTOVYKLES_GURU" -> "Pastovyklės guru"
    "VADOVAS" -> "Vadovas"
    "SAVANORIS" -> "Savanoris"
    "PATYRE_SKAUTAS" -> "Patyręs skautas"
    "SKAUTAS" -> "Skautas"
    "PROGRAMERIS" -> "Programeris"
    "MAISTININKAS" -> "Maistininkas"
    else -> role
}

private fun eventTargetGroupLabel(group: String) = when (group) {
    "PATYRE_SKAUTAI" -> "Patyrę skautai"
    "SKAUTAI_VILKAI" -> "Skautai / Vilkai"
    "TEVAI" -> "Tėvai"
    else -> group
}
