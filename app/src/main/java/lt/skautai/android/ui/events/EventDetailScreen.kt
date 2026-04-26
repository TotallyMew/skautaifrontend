package lt.skautai.android.ui.events

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.EventInventoryItemDto
import lt.skautai.android.data.remote.EventInventoryPlanDto
import lt.skautai.android.data.remote.EventPurchaseDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.SkautaiStatusTone
import lt.skautai.android.ui.common.SkautaiSummaryCard

data class EventTabSpec(
    val label: String,
    val icon: ImageVector
)

private sealed interface EventSheet {
    data object InventoryPicker : EventSheet
    data object StaffPicker : EventSheet
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onOpenMovement: (String) -> Unit,
    onOpenPastovykleLeader: (String) -> Unit,
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
            title = { Text("Atsaukti rengini") },
            text = { Text("Ar tikrai? Visi suplanuoti poreikiai ir pirkimai liks istorijoje, bet renginys bus pazymetas kaip atsauktas.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        viewModel.cancelEvent(eventId, onSuccess = onBack)
                    }
                ) {
                    Text("Atsaukti", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Uzdaryti")
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
        snackbarHost = { SkautaiErrorSnackbarHost(hostState = snackbarHostState) }
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
                    SkautaiErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadEvent(eventId) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is EventDetailUiState.Success -> {
                    val myRoles = state.event.eventRoles.map { it.role }.toSet()
                    EventDetailContent(
                        event = state.event,
                        inventoryPlan = state.inventoryPlan,
                        purchases = state.purchases,
                        pastovykles = state.pastovykles,
                        currentUserId = state.currentUserId,
                        pastovykleInventoryById = state.pastovykleInventoryById,
                        pastovykleRequestsById = state.pastovykleRequestsById,
                        items = state.items,
                        members = state.members,
                        isCancelling = state.isCancelling,
                        isWorking = state.isWorking,
                        canManage = "events.manage" in permissions || "VIRSININKAS" in myRoles,
                        canStart = "events.manage" in permissions || myRoles.any { it in setOf("VIRSININKAS", "KOMENDANTAS") },
                        canInventory = "events.inventory.distribute" in permissions || myRoles.any { it in setOf("VIRSININKAS", "KOMENDANTAS", "UKVEDYS") },
                        onCancel = { showCancelDialog = true },
                        onEdit = { onEdit(eventId) },
                        onActivate = { viewModel.updateStatus(eventId, "ACTIVE") },
                        onComplete = { viewModel.updateStatus(eventId, "COMPLETED") },
                        onCreatePurchase = { selected -> viewModel.createPurchaseFromSelected(eventId, selected) },
                        onCreateBucket = { name, type, pastovykleId, notes ->
                            viewModel.createInventoryBucket(eventId, name, type, pastovykleId, notes)
                        },
                        onUpdateBucket = { bucketId, name, type, pastovykleId, notes ->
                            viewModel.updateInventoryBucket(eventId, bucketId, name, type, pastovykleId, notes)
                        },
                        onDeleteBucket = { bucketId -> viewModel.deleteInventoryBucket(eventId, bucketId) },
                        onCreateNeed = { itemId, name, quantity, bucketId, responsibleUserId, notes ->
                            viewModel.createNeed(eventId, itemId, name, quantity, bucketId, responsibleUserId, notes)
                        },
                        onCreateNeedsBulk = { selectedQuantities, bucketId, responsibleUserId, notes ->
                            viewModel.createNeedsBulk(eventId, selectedQuantities, bucketId, responsibleUserId, notes)
                        },
                        onUpdateNeed = { item, name, quantity, bucketId, responsibleUserId, notes ->
                            viewModel.updateNeed(eventId, item.id, name, quantity, bucketId, responsibleUserId, notes)
                        },
                        onDeleteNeed = { inventoryItemId -> viewModel.deleteNeed(eventId, inventoryItemId) },
                        onCreateAllocation = { inventoryItemId, bucketId, quantity, notes ->
                            viewModel.createAllocation(eventId, inventoryItemId, bucketId, quantity, notes)
                        },
                        onUpdateAllocation = { allocationId, quantity, notes ->
                            viewModel.updateAllocation(eventId, allocationId, quantity, notes)
                        },
                        onDeleteAllocation = { allocationId -> viewModel.deleteAllocation(eventId, allocationId) },
                        onAssignRole = { userId, role -> viewModel.assignRole(eventId, userId, role) },
                        onRemoveRole = { roleId -> viewModel.removeRole(eventId, roleId) },
                        onCompletePurchase = { purchaseId -> viewModel.completePurchase(eventId, purchaseId) },
                        onAddPurchaseToInventory = { purchaseId -> viewModel.addPurchaseToInventory(eventId, purchaseId) },
                        onAttachInvoice = { purchaseId, uri -> viewModel.attachInvoice(eventId, purchaseId, uri) },
                        onDownloadInvoice = { purchaseId -> viewModel.downloadInvoice(eventId, purchaseId) },
                        onCreatePastovykleRequest = { pastovykleId, inventoryItemId, quantity, notes ->
                            viewModel.createPastovykleRequest(eventId, pastovykleId, inventoryItemId, quantity, notes)
                        },
                        onApprovePastovykleRequest = { pastovykleId, requestId ->
                            viewModel.approvePastovykleRequest(eventId, pastovykleId, requestId)
                        },
                        onRejectPastovykleRequest = { pastovykleId, requestId ->
                            viewModel.rejectPastovykleRequest(eventId, pastovykleId, requestId)
                        },
                        onSelfProvidePastovykleRequest = { pastovykleId, requestId, notes ->
                            viewModel.selfProvidePastovykleRequest(eventId, pastovykleId, requestId, notes)
                        },
                        onFulfillPastovykleRequest = { pastovykleId, requestId ->
                            viewModel.fulfillPastovykleRequest(eventId, pastovykleId, requestId)
                        },
                        onAssignFromUnitInventory = { pastovykleId, itemId, quantity, notes ->
                            viewModel.assignFromUnitInventory(eventId, pastovykleId, itemId, quantity, notes)
                        },
                        onOpenPastovykleLeader = { onOpenPastovykleLeader(eventId) },
                        onOpenMovement = { onOpenMovement(eventId) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun EventDetailContent(
    event: EventDto,
    inventoryPlan: EventInventoryPlanDto?,
    purchases: List<EventPurchaseDto>,
    pastovykles: List<lt.skautai.android.data.remote.PastovykleDto>,
    currentUserId: String?,
    pastovykleInventoryById: Map<String, List<lt.skautai.android.data.remote.PastovykleInventoryDto>>,
    pastovykleRequestsById: Map<String, List<lt.skautai.android.data.remote.EventInventoryRequestDto>>,
    items: List<ItemDto>,
    members: List<MemberDto>,
    isCancelling: Boolean,
    isWorking: Boolean,
    canManage: Boolean,
    canStart: Boolean,
    canInventory: Boolean,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onActivate: () -> Unit,
    onComplete: () -> Unit,
    onCreatePurchase: (Set<String>) -> Unit,
    onCreateBucket: (String, String, String?, String) -> Unit,
    onUpdateBucket: (String, String, String, String?, String) -> Unit,
    onDeleteBucket: (String) -> Unit,
    onCreateNeed: (String?, String, String, String?, String?, String) -> Unit,
    onCreateNeedsBulk: (Map<String, Int>, String?, String?, String) -> Unit,
    onUpdateNeed: (EventInventoryItemDto, String, String, String?, String?, String) -> Unit,
    onDeleteNeed: (String) -> Unit,
    onCreateAllocation: (String, String, String, String) -> Unit,
    onUpdateAllocation: (String, String, String) -> Unit,
    onDeleteAllocation: (String) -> Unit,
    onAssignRole: (String, String) -> Unit,
    onRemoveRole: (String) -> Unit,
    onCompletePurchase: (String) -> Unit,
    onAddPurchaseToInventory: (String) -> Unit,
    onAttachInvoice: (String, Uri) -> Unit,
    onDownloadInvoice: (String) -> Unit,
    onCreatePastovykleRequest: (String, String, String, String) -> Unit,
    onApprovePastovykleRequest: (String, String) -> Unit,
    onRejectPastovykleRequest: (String, String) -> Unit,
    onSelfProvidePastovykleRequest: (String, String, String) -> Unit,
    onFulfillPastovykleRequest: (String, String) -> Unit,
    onAssignFromUnitInventory: (String, String, String, String) -> Unit,
    onOpenPastovykleLeader: () -> Unit,
    onOpenMovement: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        EventTabSpec("Poreikiai", Icons.Default.Checklist),
        EventTabSpec("Ukvedys", Icons.Default.Inventory2),
        EventTabSpec("Pirkimai", Icons.Default.ReceiptLong),
        EventTabSpec("Planas", Icons.Default.Assignment),
        EventTabSpec("Stabas", Icons.Default.Groups)
    )
    var activeSheet by remember { mutableStateOf<EventSheet?>(null) }
    val listState = rememberLazyListState()
    val bottomPadding = if (selectedTab == 4 && canManage) 88.dp else 20.dp
    val myPastovykles = remember(pastovykles, currentUserId) {
        pastovykles.filter { it.responsibleUserId == currentUserId }
    }

    activeSheet?.let { sheet ->
        ModalBottomSheet(onDismissRequest = { activeSheet = null }) {
            when (sheet) {
                EventSheet.InventoryPicker -> InventoryPickerSheet(
                    items = items,
                    buckets = inventoryPlan?.buckets.orEmpty(),
                    members = members,
                    isWorking = isWorking,
                    onCreateNeedsBulk = { selected, bucketId, responsibleId, notes ->
                        onCreateNeedsBulk(selected, bucketId, responsibleId, notes)
                        activeSheet = null
                    }
                )

                EventSheet.StaffPicker -> StaffPickerSheet(
                    members = members,
                    isWorking = isWorking,
                    onAssignRole = { userId, role ->
                        onAssignRole(userId, role)
                        activeSheet = null
                    }
                )
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 4 && canManage) {
                FloatingActionButton(onClick = { activeSheet = EventSheet.StaffPicker }) {
                    Icon(Icons.Default.Add, contentDescription = "Prideti i staba")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                EventHeader(
                    event = event,
                    isCancelling = isCancelling,
                    canManage = canManage,
                    canStart = canStart,
                    onEdit = onEdit,
                    onActivate = onActivate,
                    onComplete = onComplete,
                    onCancel = onCancel
                )
            }
            item {
                MovementEntryCard(onOpenMovement = onOpenMovement)
            }
            if (myPastovykles.isNotEmpty()) {
                item {
                    PastovykleLeaderEntryCard(count = myPastovykles.size, onOpen = onOpenPastovykleLeader)
                }
            }
            stickyHeader {
                EventTabBar(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
            item {
                EventTabSummary(
                    selectedTab = selectedTab,
                    inventoryPlan = inventoryPlan,
                    purchases = purchases,
                    event = event
                )
            }
            item {
                when (selectedTab) {
                    0 -> NeedsCard(
                        inventoryPlan = inventoryPlan,
                        members = members,
                        canEdit = canInventory,
                        isWorking = isWorking,
                        onOpenInventoryPicker = { activeSheet = EventSheet.InventoryPicker },
                        onCreateNeed = onCreateNeed
                    )

                    1 -> UkvedysCard(
                        eventStatus = event.status,
                        inventoryPlan = inventoryPlan,
                        pastovykles = pastovykles,
                        pastovykleRequestsById = pastovykleRequestsById,
                        canManage = canInventory,
                        isWorking = isWorking,
                        onCreatePurchase = onCreatePurchase,
                        onCreateBucket = onCreateBucket,
                        onUpdateBucket = onUpdateBucket,
                        onDeleteBucket = onDeleteBucket,
                        onCreateAllocation = onCreateAllocation,
                        onUpdateAllocation = onUpdateAllocation,
                        onDeleteAllocation = onDeleteAllocation,
                        onApproveRequest = onApprovePastovykleRequest,
                        onRejectRequest = onRejectPastovykleRequest,
                        onFulfillRequest = onFulfillPastovykleRequest
                    )

                    2 -> PurchasesCard(
                        eventNotes = event.notes,
                        purchases = purchases,
                        canManage = canInventory,
                        isWorking = isWorking,
                        onCompletePurchase = onCompletePurchase,
                        onAddPurchaseToInventory = onAddPurchaseToInventory,
                        onAttachInvoice = onAttachInvoice,
                        onDownloadInvoice = onDownloadInvoice
                    )

                    3 -> PlanCard(
                        event = event,
                        inventoryPlan = inventoryPlan,
                        members = members,
                        canEdit = canInventory,
                        isWorking = isWorking,
                        onUpdateNeed = onUpdateNeed,
                        onDeleteNeed = onDeleteNeed
                    )

                    4 -> StabasCard(
                        roles = event.eventRoles,
                        canManage = canManage,
                        onRemoveRole = onRemoveRole
                    )
                }
            }
        }
    }
}

@Composable
private fun EventTabSummary(
    selectedTab: Int,
    inventoryPlan: EventInventoryPlanDto?,
    purchases: List<EventPurchaseDto>,
    event: EventDto
) {
    val items = inventoryPlan?.items.orEmpty()
    when (selectedTab) {
        0 -> {
            SkautaiSummaryCard(
                title = "Poreikiu santrauka",
                subtitle = "Kas jau suplanuota ir ko dar truksta renginiui.",
                metrics = listOf(
                    "Eilutes" to items.size.toString(),
                    "Vnt." to items.sumOf { it.plannedQuantity }.toString(),
                    "Trukumai" to items.count { it.shortageQuantity > 0 }.toString()
                ),
                foresty = true
            )
        }

        1 -> {
            SkautaiSummaryCard(
                title = if (event.status == "COMPLETED") "Grazinimo santrauka" else "Ukvedzio santrauka",
                subtitle = if (event.status == "COMPLETED") {
                    "Greita grazinamu daiktu apzvalga."
                } else {
                    "Daiktai, kuriuos reikia papildomai nupirkti arba paskirstyti."
                },
                metrics = listOf(
                    "Pirkti" to items.sumOf { it.shortageQuantity }.toString(),
                    "Truksta" to items.count { it.shortageQuantity > 0 }.toString(),
                    "Rezervuota" to items.count { it.reservationGroupId != null }.toString()
                ),
                foresty = true
            )
        }

        2 -> {
            SkautaiSummaryCard(
                title = "Pirkimu santrauka",
                subtitle = "Sukurtu pirkimu busena ir bendra suma.",
                metrics = listOf(
                    "Pirkimai" to purchases.size.toString(),
                    "Nupirkta" to purchases.count { it.status == "PURCHASED" || it.status == "ADDED_TO_INVENTORY" }.toString(),
                    "Suma" to String.format("%.0f EUR", purchases.sumOf { it.totalAmount ?: 0.0 })
                ),
                foresty = true
            )
        }

        3 -> {
            val summary = event.inventorySummary
            SkautaiSummaryCard(
                title = "Plano santrauka",
                subtitle = "Bendra renginio inventoriaus plano pusiausvyra.",
                metrics = listOf(
                    "Planuota" to (summary?.totalPlannedQuantity ?: 0).toString(),
                    "Turima" to (summary?.totalAvailableQuantity ?: 0).toString(),
                    "Paskirstyta" to (summary?.totalAllocatedQuantity ?: 0).toString()
                ),
                foresty = true
            )
        }

        4 -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SkautaiStatusPill(label = "${event.eventRoles.size} nariai", tone = SkautaiStatusTone.Info)
                SkautaiStatusPill(
                    label = "${event.eventRoles.map { it.role }.distinct().size} rolės",
                    tone = SkautaiStatusTone.Neutral
                )
            }
        }
    }
}

@Composable
private fun MovementEntryCard(onOpenMovement: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = "Inventoriaus judejimas",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onOpenMovement) {
                Text("Atidaryti")
            }
        }
    }
}

@Composable
private fun PastovykleLeaderEntryCard(count: Int, onOpen: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = if (count == 1) "Mano pastovykle" else "Mano pastovyklės ($count)",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onOpen) {
                Text("Atidaryti")
            }
        }
    }
}
