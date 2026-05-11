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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.EventInventoryItemDto
import lt.skautai.android.data.remote.InventoryTemplateDto
import lt.skautai.android.ui.common.SkautaiConfirmDialog
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiErrorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventPlanScreen(
    eventId: String,
    onBack: () -> Unit,
    viewModel: EventPlanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(eventId) { viewModel.load(eventId) }

    LaunchedEffect((uiState as? EventPlanUiState.Success)?.error) {
        (uiState as? EventPlanUiState.Success)?.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val state = uiState
    val canInventory = "events.inventory.distribute:ALL" in permissions ||
        (state as? EventPlanUiState.Success)?.event?.eventRoles
            ?.any { it.userId == state.currentUserId && it.role in setOf("VIRSININKAS", "KOMENDANTAS", "UKVEDYS") } == true

    EventScreenScaffold(
        title = "Inventoriaus planas",
        onBack = onBack,
        snackbarHostState = snackbarHostState
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state) {
                is EventPlanUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is EventPlanUiState.Error -> SkautaiErrorState(
                    message = state.message,
                    onRetry = { viewModel.load(eventId) },
                    modifier = Modifier.align(Alignment.Center)
                )
                is EventPlanUiState.Success -> {
                    val readOnly = isEventReadOnlyStatus(state.event.status)
                    var searchQuery by remember { mutableStateOf("") }
                    var selectedBucketId by remember { mutableStateOf<String?>(null) }
                    var editingItem by remember { mutableStateOf<EventInventoryItemDto?>(null) }
                    var deletingItem by remember { mutableStateOf<EventInventoryItemDto?>(null) }
                    var showMemberPicker by remember { mutableStateOf(false) }
                    var showTemplatePicker by remember { mutableStateOf(false) }

                    val buckets = state.inventoryPlan?.buckets.orEmpty()
                    val planItems = state.inventoryPlan?.items.orEmpty()
                    val filteredGrouped = remember(planItems, searchQuery, selectedBucketId) {
                        planItems
                            .filter { item ->
                                (selectedBucketId == null || item.bucketId == selectedBucketId) &&
                                (searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true))
                            }
                            .sortedWith(compareBy({ it.bucketName ?: "Be paskirties" }, { it.name.lowercase() }))
                            .groupBy { it.bucketName ?: "Be paskirties" }
                    }

                    if (!readOnly) editingItem?.let { item ->
                        if (showMemberPicker) {
                            ModalBottomSheet(onDismissRequest = { showMemberPicker = false }) {
                                MemberPickerSheet(
                                    members = state.members,
                                    title = "Atsakingas zmogus",
                                    onSelect = { _ -> showMemberPicker = false },
                                    onDismiss = { showMemberPicker = false }
                                )
                            }
                        }
                        EditNeedDialog(
                            item = item,
                            buckets = buckets,
                            members = state.members,
                            isWorking = state.isWorking,
                            onDismiss = { editingItem = null },
                            onSave = { name, quantity, bucketId, responsibleUserId, notes ->
                                viewModel.updateNeed(eventId, item.id, name, quantity, bucketId, responsibleUserId, notes)
                                editingItem = null
                            }
                        )
                    }

                    if (!readOnly) deletingItem?.let { item ->
                        SkautaiConfirmDialog(
                            title = "Trinti poreikį?",
                            message = "Poreikis ${item.name} bus ištrintas iš plano.",
                            confirmText = "Trinti",
                            dismissText = "Atšaukti",
                            isDanger = true,
                            enabled = !state.isWorking,
                            onConfirm = {
                                viewModel.deleteNeed(eventId, item.id)
                                deletingItem = null
                            },
                            onDismiss = { deletingItem = null }
                        )
                    }

                    if (!readOnly && showTemplatePicker) {
                        InventoryTemplateDialog(
                            templates = state.templates,
                            isWorking = state.isWorking,
                            onDismiss = { showTemplatePicker = false },
                            onSelect = { template ->
                                viewModel.applyTemplate(eventId, template)
                                showTemplatePicker = false
                            }
                        )
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EventDetailHero(
                                event = state.event,
                                subtitle = "Inventoriaus planas · ${planItems.size} eil."
                            )
                            if (canInventory && !readOnly) {
                                Button(
                                    onClick = {
                                        viewModel.loadTemplates()
                                        showTemplatePicker = true
                                    },
                                    enabled = !state.isWorking,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Kurti iš šablono")
                                }
                            }
                            EventDetailSearchBar(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = "Ieškoti plano eilučių",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        BucketFilterChips(
                            buckets = buckets,
                            selectedBucketId = selectedBucketId,
                            onSelect = { selectedBucketId = it }
                        )
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            item(key = "header") {
                                PlanCard(event = state.event, inventoryPlan = state.inventoryPlan)
                            }
                            if (planItems.isEmpty()) {
                                item { EmptyStateText("Planas dar tuščias.") }
                            } else if (filteredGrouped.isEmpty()) {
                                item { EmptyStateText("Nerasta eilučių pagal paiešką.") }
                            } else {
                                filteredGrouped.forEach { (bucketName, bucketItems) ->
                                    item(key = "grp_$bucketName") {
                                        EventListGroupHeader(bucketName, bucketItems.size)
                                    }
                                    items(bucketItems, key = { it.id }) { item ->
                                        EventInventoryListRow(
                                            item = item,
                                            bottom = if (canInventory && !readOnly) ({
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    TextButton(
                                                        onClick = {
                                                            viewModel.loadMembers()
                                                            editingItem = item
                                                        },
                                                        contentPadding = PaddingValues(0.dp)
                                                    ) { Text("Redaguoti") }
                                                    TextButton(
                                                        onClick = { deletingItem = item },
                                                        contentPadding = PaddingValues(0.dp)
                                                    ) { Text("Trinti") }
                                                }
                                            }) else null
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryTemplateDialog(
    templates: List<InventoryTemplateDto>,
    isWorking: Boolean,
    onDismiss: () -> Unit,
    onSelect: (InventoryTemplateDto) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kurti iš šablono") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (templates.isEmpty()) {
                    Text("Šablonų dar nėra.")
                } else {
                    templates.forEach { template ->
                        TextButton(
                            onClick = { onSelect(template) },
                            enabled = !isWorking,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${template.name} (${template.items.sumOf { it.quantity }} vnt.)")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Uždaryti") }
        }
    )
}
