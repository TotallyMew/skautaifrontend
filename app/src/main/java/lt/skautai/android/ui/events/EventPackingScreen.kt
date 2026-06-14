package lt.skautai.android.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.EventPackingContainerDto
import lt.skautai.android.data.remote.EventPackingLineDto
import lt.skautai.android.data.remote.EventPackingListDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiChip
import lt.skautai.android.ui.common.SkautaiEmptyState
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiPrimaryButton
import lt.skautai.android.ui.common.SkautaiSecondaryButton
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.SkautaiStatusTone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventPackingScreen(
    eventId: String,
    onBack: () -> Unit,
    viewModel: EventPackingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(eventId) { viewModel.load(eventId) }

    LaunchedEffect((uiState as? EventPackingUiState.Success)?.error) {
        (uiState as? EventPackingUiState.Success)?.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    EventScreenScaffold(
        title = "Pakavimas",
        onBack = onBack,
        snackbarHostState = snackbarHostState
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                EventPackingUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is EventPackingUiState.Error -> SkautaiErrorState(
                    message = state.message,
                    onRetry = { viewModel.load(eventId) },
                    modifier = Modifier.align(Alignment.Center)
                )
                is EventPackingUiState.Success -> PackingContent(
                    eventId = eventId,
                    packingList = state.packingList,
                    isWorking = state.isWorking,
                    onGenerate = { viewModel.generate(eventId) },
                    onCreateContainer = { name -> viewModel.createContainer(eventId, name) },
                    onUpdateStatus = viewModel::updateLineStatus,
                    onUpdateLineDetails = viewModel::updateLineDetails
                )
            }
        }
    }
}

@Composable
private fun PackingContent(
    eventId: String,
    packingList: EventPackingListDto,
    isWorking: Boolean,
    onGenerate: () -> Unit,
    onCreateContainer: (String) -> Unit,
    onUpdateStatus: (String, String, String) -> Unit,
    onUpdateLineDetails: (String, String, String?, String?) -> Unit
) {
    var statusFilter by remember { mutableStateOf<String?>(null) }
    var containerName by remember { mutableStateOf("") }
    var showContainerDialog by remember { mutableStateOf(false) }
    var editingLine by remember { mutableStateOf<EventPackingLineDto?>(null) }
    val lines = packingList.lines
        .filter { statusFilter == null || it.status == statusFilter }
        .sortedWith(compareBy({ it.containerName ?: "" }, { it.itemName.lowercase() }))
    val grouped = lines.groupBy { it.containerName ?: "Nesupakuota" }

    if (showContainerDialog) {
        AlertDialog(
            onDismissRequest = { showContainerDialog = false },
            title = { Text("Nauja pakavimo vieta") },
            text = {
                OutlinedTextField(
                    value = containerName,
                    onValueChange = { containerName = it },
                    label = { Text("Pvz. Deze A, Maisas virtuvei") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isWorking,
                    onClick = {
                        onCreateContainer(containerName)
                        containerName = ""
                        showContainerDialog = false
                    }
                ) { Text("Sukurti") }
            },
            dismissButton = {
                TextButton(onClick = { showContainerDialog = false }) { Text("Uzdaryti") }
            }
        )
    }

    editingLine?.let { line ->
        PackingLineDetailsDialog(
            line = line,
            containers = packingList.containers,
            isWorking = isWorking,
            onDismiss = { editingLine = null },
            onSave = { selectedContainerId, notes ->
                onUpdateLineDetails(eventId, line.id, selectedContainerId, notes)
                editingLine = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PackingSummaryCard(
                packingList = packingList,
                isWorking = isWorking,
                onGenerate = onGenerate,
                onCreateContainer = { showContainerDialog = true }
            )
        }
        item {
            PackingStatusFilters(
                selectedStatus = statusFilter,
                onSelect = { statusFilter = it }
            )
        }
        if (lines.isEmpty()) {
            item {
                SkautaiEmptyState(
                    title = if (packingList.lines.isEmpty()) "Pakavimo sarasas tuscias" else "Pagal filtra eiluciu nera",
                    subtitle = if (packingList.lines.isEmpty()) "Sugeneruok sarasa is renginio inventoriaus plano." else "Pasirink kita statuso filtra.",
                    icon = Icons.Default.Inventory2,
                    actionLabel = if (packingList.lines.isEmpty()) "Sugeneruoti" else null,
                    onAction = if (packingList.lines.isEmpty() && !isWorking) onGenerate else null
                )
            }
        } else {
            grouped.forEach { (containerName, containerLines) ->
                item(key = "header_$containerName") {
                    EventListGroupHeader(containerName, containerLines.size)
                }
                items(containerLines, key = { it.id }) { line ->
                    PackingLineCard(
                        line = line,
                        isWorking = isWorking,
                        onEditDetails = { editingLine = line },
                        onNextStatus = { nextStatus -> onUpdateStatus(eventId, line.id, nextStatus) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PackingSummaryCard(
    packingList: EventPackingListDto,
    isWorking: Boolean,
    onGenerate: () -> Unit,
    onCreateContainer: () -> Unit
) {
    val summary = packingList.summary
    SkautaiCard(modifier = Modifier.fillMaxWidth(), tonal = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pakavimo progresas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${summary.doneLines}/${summary.totalLines} eil. / ${summary.doneQuantity}/${summary.totalQuantity} vnt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SkautaiStatusPill(
                    label = "${summary.progressPercent}%",
                    tone = if (summary.progressPercent == 100 && summary.totalLines > 0) SkautaiStatusTone.Success else SkautaiStatusTone.Info
                )
            }
            LinearProgressIndicator(
                progress = { summary.progressPercent.coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SkautaiSecondaryButton(
                    text = if (packingList.lines.isEmpty()) "Sugeneruoti is plano" else "Atnaujinti is plano",
                    onClick = onGenerate,
                    enabled = !isWorking,
                    leadingIcon = Icons.Default.Refresh,
                    modifier = Modifier.weight(1f)
                )
                SkautaiSecondaryButton(
                    text = "Nauja vieta",
                    onClick = onCreateContainer,
                    enabled = !isWorking,
                    leadingIcon = Icons.Default.Add,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PackingStatusFilters(
    selectedStatus: String?,
    onSelect: (String?) -> Unit
) {
    val filters = listOf(
        null to "Visos",
        "TODO" to "Paimti",
        "PICKED" to "Paimta",
        "PACKED" to "Supakuota",
        "LOADED" to "Pakrauta",
        "RETURNED" to "Grazinta"
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filters, key = { it.first ?: "ALL" }) { (status, label) ->
            SkautaiChip(
                label = label,
                selected = selectedStatus == status,
                onClick = { onSelect(status) }
            )
        }
    }
}

@Composable
private fun PackingLineCard(
    line: EventPackingLineDto,
    isWorking: Boolean,
    onEditDetails: () -> Unit,
    onNextStatus: (String) -> Unit
) {
    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = line.itemName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val details = buildList {
                        line.bucketName?.takeIf { it.isNotBlank() }?.let { add("Paskirtis: $it") }
                        line.sourceSummary?.takeIf { it.isNotBlank() }?.let { add(it) }
                        line.notes?.takeIf { it.isNotBlank() }?.let { add(it) }
                    }
                    if (details.isNotEmpty()) {
                        Text(
                            text = details.joinToString(" / "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "${line.requiredQuantity} vnt.",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    SkautaiStatusPill(label = packingStatusLabel(line.status), tone = packingStatusTone(line.status))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SkautaiSecondaryButton(
                    text = line.containerName ?: "Priskirti vieta",
                    onClick = onEditDetails,
                    enabled = !isWorking,
                    modifier = Modifier.weight(1f)
                )
            }
            nextPackingStatus(line.status)?.let { nextStatus ->
                SkautaiPrimaryButton(
                    text = packingActionLabel(nextStatus),
                    onClick = { onNextStatus(nextStatus) },
                    enabled = !isWorking,
                    leadingIcon = Icons.Default.Check,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PackingLineDetailsDialog(
    line: EventPackingLineDto,
    containers: List<EventPackingContainerDto>,
    isWorking: Boolean,
    onDismiss: () -> Unit,
    onSave: (String?, String?) -> Unit
) {
    var selectedContainerId by remember(line.id) { mutableStateOf(line.containerId) }
    var notes by remember(line.id) { mutableStateOf(line.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pakavimo eilute") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(line.itemName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Pakavimo vieta", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        SkautaiChip(
                            label = "Nesupakuota",
                            selected = selectedContainerId == null,
                            onClick = { selectedContainerId = null }
                        )
                    }
                    items(containers, key = { it.id }) { container ->
                        SkautaiChip(
                            label = container.name,
                            selected = selectedContainerId == container.id,
                            onClick = { selectedContainerId = container.id }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Pastaba") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isWorking,
                onClick = { onSave(selectedContainerId, notes) }
            ) { Text("Issaugoti") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Uzdaryti") }
        }
    )
}

private fun nextPackingStatus(status: String): String? = when (status) {
    "TODO" -> "PICKED"
    "PICKED" -> "PACKED"
    "PACKED" -> "LOADED"
    "LOADED" -> "RETURNED"
    else -> null
}

private fun packingActionLabel(status: String): String = when (status) {
    "PICKED" -> "Pazymeti kaip paimta"
    "PACKED" -> "Pazymeti kaip supakuota"
    "LOADED" -> "Pazymeti kaip pakrauta"
    "RETURNED" -> "Pazymeti kaip grazinta"
    else -> "Atnaujinti"
}

private fun packingStatusLabel(status: String): String = when (status) {
    "TODO" -> "Reikia paimti"
    "PICKED" -> "Paimta"
    "PACKED" -> "Supakuota"
    "LOADED" -> "Pakrauta"
    "RETURNED" -> "Grazinta"
    else -> status
}

private fun packingStatusTone(status: String): SkautaiStatusTone = when (status) {
    "RETURNED", "LOADED", "PACKED" -> SkautaiStatusTone.Success
    "PICKED" -> SkautaiStatusTone.Info
    "TODO" -> SkautaiStatusTone.Warning
    else -> SkautaiStatusTone.Neutral
}
