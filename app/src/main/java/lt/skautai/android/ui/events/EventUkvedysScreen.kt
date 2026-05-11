package lt.skautai.android.ui.events

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.ui.common.SkautaiChip
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiTextField
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventUkvedysScreen(
    eventId: String,
    onBack: () -> Unit,
    viewModel: EventUkvedysViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(eventId) { viewModel.load(eventId) }

    LaunchedEffect((uiState as? EventUkvedysUiState.Success)?.error) {
        (uiState as? EventUkvedysUiState.Success)?.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val state = uiState
    val readOnly = (state as? EventUkvedysUiState.Success)?.event?.status?.let(::isEventReadOnlyStatus) == true
    val canInventory = !readOnly && ("events.inventory.distribute:ALL" in permissions ||
        (state as? EventUkvedysUiState.Success)?.event?.eventRoles
            ?.any { it.userId == state.currentUserId && it.role in setOf("VIRSININKAS", "KOMENDANTAS", "UKVEDYS") } == true)
    var pendingExportCsv by remember { mutableStateOf("") }
    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            runCatching { context.writeTextToUri(uri, pendingExportCsv) }
                .onSuccess { viewModel.showMessage("CSV eksportas išsaugotas.") }
                .onFailure { viewModel.showMessage(it.message ?: "Nepavyko išsaugoti CSV failo.") }
        }
        pendingExportCsv = ""
    }
    val importCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching { context.readTextFromUri(uri) }
                .onSuccess { viewModel.importEventInventoryCsv(eventId, it) }
                .onFailure { viewModel.showMessage(it.message ?: "Nepavyko perskaityti CSV failo.") }
        }
    }

    EventScreenScaffold(
        title = "Ūkvedžio suvestinė",
        onBack = onBack,
        snackbarHostState = snackbarHostState
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state) {
                is EventUkvedysUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is EventUkvedysUiState.Error -> SkautaiErrorState(
                    message = state.message,
                    onRetry = { viewModel.load(eventId) },
                    modifier = Modifier.align(Alignment.Center)
                )
                is EventUkvedysUiState.Success -> {
                    val selectedId = state.selectedPastovykleId
                    val activePurchaseItemIds = remember(state.purchases) {
                        state.purchases
                            .filter { it.status in listOf("DRAFT", "PURCHASED") }
                            .flatMap { it.items }
                            .map { it.eventInventoryItemId }
                            .toSet()
                    }
                    val filteredRequestsById = remember(state.pastovykleRequestsById, selectedId) {
                        if (selectedId == null) state.pastovykleRequestsById
                        else state.pastovykleRequestsById.filterKeys { it == selectedId }
                    }

                    if (!readOnly) state.createdPurchase?.let {
                        CreatedPurchaseDialog(
                            isWorking = state.isWorking,
                            onDismiss = { viewModel.dismissCreatedPurchase() },
                            onSave = { totalAmount, invoiceUri ->
                                viewModel.saveCreatedPurchaseDetails(eventId, it.id, totalAmount, invoiceUri)
                            }
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            EventDetailHero(
                                event = state.event,
                                subtitle = "Ūkvedžio suvestinė / ${state.inventoryPlan?.items?.size ?: 0} plano eil."
                            )
                        }
                        item {
                            if (state.pastovykles.isNotEmpty()) {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        SkautaiChip(
                                            label = "Visos pastovyklės",
                                            selected = selectedId == null,
                                            onClick = { viewModel.selectAllPastovyklės(eventId) }
                                        )
                                    }
                                    items(state.pastovykles, key = { it.id }) { pastovykle ->
                                        SkautaiChip(
                                            label = pastovykle.name,
                                            selected = pastovykle.id == selectedId,
                                            onClick = { viewModel.selectPastovykle(eventId, pastovykle.id) }
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        pendingExportCsv = viewModel.eventInventoryExportCsv()
                                        exportCsvLauncher.launch("renginio-planas-${LocalDate.now()}.csv")
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Eksportuoti CSV")
                                }
                                OutlinedButton(
                                    onClick = { importCsvLauncher.launch(arrayOf("text/*", "text/csv", "application/csv")) },
                                    enabled = canInventory && !state.isWorking,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Importuoti CSV")
                                }
                            }
                        }
                        item {
                            UkvedysTabsCard(
                                eventStatus = state.event.status,
                                inventoryPlan = state.inventoryPlan,
                                pastovykles = state.pastovykles,
                                pastovykleRequestsById = filteredRequestsById,
                                activePurchaseItemIds = activePurchaseItemIds,
                                canManage = canInventory,
                                isWorking = state.isWorking,
                                onCreatePurchase = { selected ->
                                    viewModel.createPurchaseFromSelected(eventId, selected)
                                },
                                onCreateBucket = { name, type, pastovykleId, notes ->
                                    viewModel.createInventoryBucket(eventId, name, type, pastovykleId, notes)
                                },
                                onDeleteBucket = { bucketId ->
                                    viewModel.deleteInventoryBucket(eventId, bucketId)
                                },
                                onCreateAllocation = { inventoryItemId, bucketId, quantity, notes ->
                                    viewModel.createAllocation(eventId, inventoryItemId, bucketId, quantity, notes)
                                },
                                onDeleteAllocation = { allocationId ->
                                    viewModel.deleteAllocation(eventId, allocationId)
                                },
                                onApproveRequest = { pastovykleId, requestId ->
                                    viewModel.approvePastovykleRequest(eventId, pastovykleId, requestId)
                                },
                                onRejectRequest = { pastovykleId, requestId ->
                                    viewModel.rejectPastovykleRequest(eventId, pastovykleId, requestId)
                                },
                                onFulfillRequest = { pastovykleId, requestId ->
                                    viewModel.fulfillPastovykleRequest(eventId, pastovykleId, requestId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun android.content.Context.writeTextToUri(uri: Uri, text: String) {
    contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
        writer.write(text)
    } ?: error("Nepavyko atidaryti failo rašymui.")
}

private fun android.content.Context.readTextFromUri(uri: Uri): String =
    contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        ?: error("Nepavyko atidaryti failo skaitymui.")

@Composable
private fun CreatedPurchaseDialog(
    isWorking: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Uri?) -> Unit
) {
    var totalAmount by remember { mutableStateOf("") }
    var invoiceUri by remember { mutableStateOf<Uri?>(null) }
    val invoicePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) invoiceUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pirkimas sukurtas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Gali iš karto įrašyti sumą ir prisegti sąskaitą arba užpildyti vėliau.")
                SkautaiTextField(
                    value = totalAmount,
                    onValueChange = { value -> totalAmount = value.filter { it.isDigit() || it == '.' || it == ',' } },
                    label = "Bendra suma (EUR)",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { invoicePicker.launch(arrayOf("application/pdf", "image/jpeg", "image/png")) },
                    enabled = !isWorking,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (invoiceUri == null) "Prisegti sąskaitą faktūrą" else "Pakeisti pasirinktą failą")
                }
                invoiceUri?.let {
                    Text("Failas pasirinktas", modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isWorking,
                onClick = { onSave(totalAmount, invoiceUri) }
            ) {
                Text("Išsaugoti")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isWorking) {
                Text("Užpildysiu vėliau")
            }
        }
    )
}
