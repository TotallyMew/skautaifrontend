package lt.skautai.android.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.ui.common.SkautaiErrorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventNeedsScreen(
    eventId: String,
    onBack: () -> Unit,
    viewModel: EventNeedsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showInventoryPicker by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        viewModel.load(eventId)
    }

    LaunchedEffect((uiState as? EventNeedsUiState.Success)?.event?.id) {
        if (uiState is EventNeedsUiState.Success) {
            viewModel.loadMembers()
        }
    }

    LaunchedEffect((uiState as? EventNeedsUiState.Success)?.error) {
        (uiState as? EventNeedsUiState.Success)?.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val state = uiState
    val canInventory = "events.inventory.distribute:ALL" in permissions ||
        (state as? EventNeedsUiState.Success)?.event?.eventRoles
            ?.any { it.userId == state.currentUserId && it.role in setOf("VIRSININKAS", "KOMENDANTAS", "UKVEDYS") } == true

    if (showInventoryPicker && state is EventNeedsUiState.Success && !isEventReadOnlyStatus(state.event.status)) {
        LaunchedEffect(Unit) { viewModel.loadItemCatalog(eventId) }
        ModalBottomSheet(onDismissRequest = { showInventoryPicker = false }) {
            InventoryPickerSheet(
                items = state.items,
                buckets = state.inventoryPlan?.buckets.orEmpty(),
                isWorking = state.isWorking,
                onCreateNeedsBulk = { selected, bucketId, responsibleId, notes ->
                    viewModel.createNeedsBulk(eventId, selected, bucketId, responsibleId, notes)
                    showInventoryPicker = false
                }
            )
        }
    }

    EventScreenScaffold(
        title = "Poreikiai",
        onBack = onBack,
        snackbarHostState = snackbarHostState
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state) {
                is EventNeedsUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is EventNeedsUiState.Error -> SkautaiErrorState(
                    message = state.message,
                    onRetry = { viewModel.load(eventId) },
                    modifier = Modifier.align(Alignment.Center)
                )
                is EventNeedsUiState.Success -> {
                    val readOnly = isEventReadOnlyStatus(state.event.status)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EventDetailHero(
                            event = state.event,
                            subtitle = "Poreikių kūrimas · ${state.inventoryPlan?.items?.size ?: 0} plano eil."
                        )
                        EventDetailSection(
                            title = "Kurti poreikius",
                            subtitle = "Pasirink kurti poreikius iš inventoriaus arba ranka.",
                            actionLabel = if (showHelp) "Mažiau" else "Kaip veikia",
                            onAction = { showHelp = !showHelp }
                        ) {
                            if (showHelp) {
                                Text(
                                    "Sandėlio pasirinkimas tinka jau turimiems daiktams. Rankinis įvedimas skirtas naujam pirkiniui, paslaugai arba daiktui, kurio kataloge dar nėra.",
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        if (readOnly) {
                            EmptyStateText("Renginys baigtas arba atšauktas. Poreikius galima tik peržiūrėti.")
                        } else if (!canInventory) {
                            EmptyStateText("Neturite teisės kurti renginio poreikių.")
                        } else {
                            NeedsCard(
                                inventoryPlan = state.inventoryPlan,
                                isWorking = state.isWorking,
                                onOpenInventoryPicker = { showInventoryPicker = true },
                                onCreateManualNeeds = { needs ->
                                    viewModel.createManualNeedsBulk(eventId, needs)
                                },
                                onManualValidationError = viewModel::showValidationError
                            )
                        }
                    }
                }
            }
        }
    }
}
