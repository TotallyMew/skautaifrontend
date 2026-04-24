package lt.skautai.android.ui.events

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.EventInventoryBucketDto
import lt.skautai.android.data.remote.EventInventoryItemDto
import lt.skautai.android.data.remote.EventInventoryPlanDto
import lt.skautai.android.data.remote.EventPurchaseDto
import lt.skautai.android.data.remote.EventRoleDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.StovyklaDetailsDto
import lt.skautai.android.ui.common.inventoryCategoryLabel
import lt.skautai.android.ui.theme.ScoutPalette

private val EventAccent = ScoutPalette.Forest
private val EventAccentSoft = ScoutPalette.ForestMist
private val EventSupport = ScoutPalette.Moss
private val EventSupportSoft = ScoutPalette.MossMist
private val EventSuccessContainer = ScoutPalette.ForestSoft
private val EventSuccessContent = ScoutPalette.ForestDeep
private val EventWarningContainer = ScoutPalette.GoldWarning
private val EventWarningContent = ScoutPalette.GoldWarningText

private data class EventTabSpec(
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
                TextButton(onClick = {
                    showCancelDialog = false
                    viewModel.cancelEvent(eventId)
                }) {
                    Text("Atsaukti", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Uzdaryti") }
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
                is EventDetailUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is EventDetailUiState.Error -> ErrorState(state.message) { viewModel.loadEvent(eventId) }
                is EventDetailUiState.Success -> {
                    val myRoles = state.event.eventRoles.map { it.role }.toSet()
                    EventDetailContent(
                        event = state.event,
                        inventoryPlan = state.inventoryPlan,
                        purchases = state.purchases,
                        items = state.items,
                        members = state.members,
                        isCancelling = state.isCancelling,
                        isWorking = state.isWorking,
                        canManage = "events.manage" in permissions || "VIRSININKAS" in myRoles,
                        canStart = "events.manage" in permissions || myRoles.any { it in setOf("VIRSININKAS", "KOMENDANTAS") },
                        canInventory = "events.inventory.distribute" in permissions || myRoles.any { it in setOf("VIRSININKAS", "KOMENDANTAS", "UKVEDYS") },
                        onCancel = { showCancelDialog = true },
                        onActivate = { viewModel.updateStatus(eventId, "ACTIVE") },
                        onComplete = { viewModel.updateStatus(eventId, "COMPLETED") },
                        onCreatePurchase = { selected -> viewModel.createPurchaseFromSelected(eventId, selected) },
                        onCreateNeed = { itemId, name, quantity, bucketId, responsibleUserId, notes ->
                            viewModel.createNeed(eventId, itemId, name, quantity, bucketId, responsibleUserId, notes)
                        },
                        onCreateNeedsBulk = { selectedQuantities, bucketId, responsibleUserId, notes ->
                            viewModel.createNeedsBulk(eventId, selectedQuantities, bucketId, responsibleUserId, notes)
                        },
                        onUpdateNeed = { item, name, quantity, bucketId, responsibleUserId, notes ->
                            viewModel.updateNeed(eventId, item.id, name, quantity, bucketId, responsibleUserId, notes)
                        },
                        onAssignRole = { userId, role -> viewModel.assignRole(eventId, userId, role) },
                        onRemoveRole = { roleId -> viewModel.removeRole(eventId, roleId) },
                        onCompletePurchase = { purchaseId -> viewModel.completePurchase(eventId, purchaseId) },
                        onAddPurchaseToInventory = { purchaseId -> viewModel.addPurchaseToInventory(eventId, purchaseId) },
                        onAttachInvoice = { purchaseId, uri -> viewModel.attachInvoice(eventId, purchaseId, uri) },
                        onDownloadInvoice = { purchaseId -> viewModel.downloadInvoice(eventId, purchaseId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Bandyti dar karta") }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun EventDetailContent(
    event: EventDto,
    inventoryPlan: EventInventoryPlanDto?,
    purchases: List<EventPurchaseDto>,
    items: List<ItemDto>,
    members: List<MemberDto>,
    isCancelling: Boolean,
    isWorking: Boolean,
    canManage: Boolean,
    canStart: Boolean,
    canInventory: Boolean,
    onCancel: () -> Unit,
    onActivate: () -> Unit,
    onComplete: () -> Unit,
    onCreatePurchase: (Set<String>) -> Unit,
    onCreateNeed: (String?, String, String, String?, String?, String) -> Unit,
    onCreateNeedsBulk: (Map<String, Int>, String?, String?, String) -> Unit,
    onUpdateNeed: (EventInventoryItemDto, String, String, String?, String?, String) -> Unit,
    onAssignRole: (String, String) -> Unit,
    onRemoveRole: (String) -> Unit,
    onCompletePurchase: (String) -> Unit,
    onAddPurchaseToInventory: (String) -> Unit,
    onAttachInvoice: (String, Uri) -> Unit,
    onDownloadInvoice: (String) -> Unit
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
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                EventHeader(
                    event = event,
                    isCancelling = isCancelling,
                    canManage = canManage,
                    canStart = canStart,
                    onActivate = onActivate,
                    onComplete = onComplete,
                    onCancel = onCancel
                )
            }
            stickyHeader {
                EventTabBar(tabs = tabs, selectedTab = selectedTab, onTabSelected = { selectedTab = it })
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
                        canManage = canInventory,
                        isWorking = isWorking,
                        onCreatePurchase = onCreatePurchase
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
                        onUpdateNeed = onUpdateNeed
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
private fun EventHeader(
    event: EventDto,
    isCancelling: Boolean,
    canManage: Boolean,
    canStart: Boolean,
    onActivate: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                HeaderActions(
                    event = event,
                    isCancelling = isCancelling,
                    canManage = canManage,
                    onCancel = onCancel
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EventStatusPill(status = event.status)
                Text(
                    eventTypeLabel(event.type),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    "${event.startDate.take(10)} - ${event.endDate.take(10)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            event.notes?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (event.type == "STOVYKLA") {
                event.stovyklaDetails?.let { StovyklaDetailsCompact(it) }
            }

            EventPrimaryStatusAction(event, canManage, canStart, onActivate, onComplete)
        }
    }
}

@Composable
private fun EventPrimaryStatusAction(
    event: EventDto,
    canManage: Boolean,
    canStart: Boolean,
    onActivate: () -> Unit,
    onComplete: () -> Unit
) {
    when {
        event.status == "PLANNING" && canStart -> {
            Button(onClick = onActivate, modifier = Modifier.fillMaxWidth()) { Text("Pradeti rengini") }
        }
        event.status == "ACTIVE" && canManage -> {
            Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) { Text("Baigti rengini") }
        }
    }
}

@Composable
private fun HeaderActions(
    event: EventDto,
    isCancelling: Boolean,
    canManage: Boolean,
    onCancel: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (canManage && event.status in listOf("PLANNING", "ACTIVE")) {
            Box {
                IconButton(onClick = { expanded = true }, enabled = !isCancelling) {
                    if (isCancelling) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Default.MoreVert, contentDescription = "Daugiau veiksmu")
                    }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Atsaukti rengini", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            expanded = false
                            onCancel()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EventTabBar(
    tabs: List<EventTabSpec>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = EventAccentSoft,
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEachIndexed { index, tab ->
                    EventSegment(
                        label = tab.label,
                        icon = tab.icon,
                        selected = selectedTab == index,
                        onClick = { onTabSelected(index) },
                        modifier = Modifier.widthIn(min = 116.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EventSegment(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = if (selected) EventAccent else Color.Transparent
    val content = if (selected) ScoutPalette.White else EventAccent
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun EventStatusBanner(status: String) {
    val (label, color) = when (status) {
        "PLANNING" -> "Planuojamas" to EventAccent
        "ACTIVE" -> "Vyksta" to EventSupport
        "COMPLETED" -> "Užbaigtas" to ScoutPalette.MossDeep
        "CANCELLED" -> "Atšauktas" to MaterialTheme.colorScheme.error
        else -> status to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = color,
        contentColor = ScoutPalette.White,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun EventStatusPill(status: String) {
    val (label, color) = when (status) {
        "PLANNING" -> "Planuojamas" to EventAccent
        "ACTIVE" -> "Vyksta" to EventSupport
        "COMPLETED" -> "Užbaigtas" to ScoutPalette.MossDeep
        "CANCELLED" -> "Atšauktas" to MaterialTheme.colorScheme.error
        else -> status to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = color, contentColor = ScoutPalette.White, shape = RoundedCornerShape(999.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun EventModeChip(selected: Boolean, text: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = EventAccent,
            selectedLabelColor = ScoutPalette.White,
            containerColor = EventAccentSoft,
            labelColor = EventAccent
        )
    )
}

@Composable
private fun EmptyStateText(text: String) {
    Surface(
        color = EventSupportSoft,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(14.dp)
        )
    }
}

@Composable
private fun PurposeBadge(text: String) {
    Surface(
        color = EventSuccessContainer,
        contentColor = EventSuccessContent,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun EventListSection(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = EventSupportSoft,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    subtitle?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun EventListGroupHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = EventAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            "$count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EventInventoryListRow(
    item: EventInventoryItemDto,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    bottom: (@Composable RowScope.() -> Unit)? = null
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            leading?.invoke()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    planItemSubtitle(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!item.notes.isNullOrBlank()) {
                    Text(
                    item.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                bottom?.let {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), content = it)
                }
            }
            if (trailing != null) trailing.invoke() else EventQuantitySummary(item)
        }
        HorizontalDivider()
    }
}

@Composable
private fun EventQuantitySummary(item: EventInventoryItemDto) {
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "${item.availableQuantity}/${item.plannedQuantity}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        if (item.shortageQuantity > 0) {
            EventMetricPill("Truksta ${item.shortageQuantity}", EventMetricTone.Warning)
        } else if (item.reservationGroupId != null) {
            EventMetricPill("Rezervuota", EventMetricTone.Good)
        }
    }
}

@Composable
private fun EventMetricPill(text: String, tone: EventMetricTone = EventMetricTone.Neutral) {
    val (container, content) = when (tone) {
        EventMetricTone.Good -> EventSuccessContainer to EventSuccessContent
        EventMetricTone.Warning -> EventWarningContainer to EventWarningContent
        EventMetricTone.Neutral -> EventAccentSoft to EventAccent
    }
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1
        )
    }
}

private enum class EventMetricTone { Good, Warning, Neutral }

@Composable
private fun NeedsCard(
    inventoryPlan: EventInventoryPlanDto?,
    members: List<MemberDto>,
    canEdit: Boolean,
    isWorking: Boolean,
    onOpenInventoryPicker: () -> Unit,
    onCreateNeed: (String?, String, String, String?, String?, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var selectedBucketId by remember { mutableStateOf<String?>(null) }
    var responsibleUserId by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    val buckets = inventoryPlan?.buckets.orEmpty()
    val planItems = inventoryPlan?.items.orEmpty()
    val shortageCount = planItems.count { it.shortageQuantity > 0 }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Poreikiai", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "${planItems.size} daiktu, $shortageCount truksta (Pirkimai)",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()

            if (canEdit) {
                Button(
                    onClick = onOpenInventoryPicker,
                    enabled = !isWorking,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Prideti is inventoriaus")
                }

                EventListSection(title = "Naujas poreikis", subtitle = "Daiktui, kurio nera inventoriuje") {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Daiktas") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it.filter(Char::isDigit) },
                            label = { Text("Kiekis") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        DropdownField(
                            label = "Paskirtis",
                            value = buckets.firstOrNull { it.id == selectedBucketId }?.name ?: "Pasirinkti",
                            options = buckets.map { it.id to it.name },
                            onSelect = { selectedBucketId = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    DropdownField(
                        label = "Atsakingas",
                        value = members.firstOrNull { it.userId == responsibleUserId }?.fullName() ?: "Nepasirinkta",
                        options = members.map { it.userId to it.fullName() },
                        onSelect = { responsibleUserId = it }
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Pastabos") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onCreateNeed(null, name, quantity, selectedBucketId, responsibleUserId, notes)
                            name = ""
                            quantity = ""
                            notes = ""
                        },
                        enabled = !isWorking,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Prideti poreiki") }
                }
            }

            if (planItems.isEmpty()) {
                EmptyStateText("Dar nieko nepasirinkote. Pridėkite daiktų iš inventoriaus arba sukurkite naują pirkinių sąrašą.")
            } else {
                EventListSection(
                    title = "Poreikiu sarasas",
                    subtitle = "${planItems.size} eil. / ${planItems.sumOf { it.plannedQuantity }} vnt."
                ) {
                    planItems
                        .sortedWith(compareBy<EventInventoryItemDto>({ it.bucketName ?: "Be paskirties" }, { it.name.lowercase() }))
                        .groupBy { it.bucketName ?: "Be paskirties" }
                        .forEach { (bucketName, bucketItems) ->
                            EventListGroupHeader(bucketName, bucketItems.size)
                            bucketItems.forEach { item ->
                                EventInventoryListRow(item = item)
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun BulkInventoryItemRow(
    item: ItemDto,
    quantity: Int,
    onCheckedChange: (Boolean) -> Unit,
    onQuantityChange: (Int) -> Unit
) {
    val selected = quantity > 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) EventAccentSoft else Color.Transparent, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = onCheckedChange
        )
        Column(Modifier.weight(1f)) {
            Text(item.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(inventoryCategoryLabel(item.category), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${item.quantity}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            if (selected) {
                Text("pasirinkta $quantity", style = MaterialTheme.typography.labelSmall, color = EventAccent)
            }
        }
        if (selected) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                    onClick = { onQuantityChange(quantity - 1) },
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("-")
                }
                Text(quantity.toString(), modifier = Modifier.widthIn(min = 24.dp), fontWeight = FontWeight.SemiBold)
                OutlinedButton(
                    onClick = { onQuantityChange(quantity + 1) },
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("+")
                }
            }
        }
    }
}

@Composable
private fun InventoryPickerSheet(
    items: List<ItemDto>,
    buckets: List<EventInventoryBucketDto>,
    members: List<MemberDto>,
    isWorking: Boolean,
    onCreateNeedsBulk: (Map<String, Int>, String?, String?, String) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var selectedBucketId by remember { mutableStateOf<String?>(null) }
    var responsibleUserId by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var selectedQuantities by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var collapsedCategories by remember { mutableStateOf(setOf<String>()) }
    val filteredItems = remember(items, search) {
        val query = search.trim()
        items.filter { item ->
            query.isBlank() ||
                item.name.contains(query, ignoreCase = true) ||
                inventoryCategoryLabel(item.category).contains(query, ignoreCase = true)
        }
    }
    val groups = remember(filteredItems) {
        filteredItems
            .sortedWith(compareBy<ItemDto>({ inventoryCategoryLabel(it.category) }, { it.name.lowercase() }))
            .groupBy { inventoryCategoryLabel(it.category) }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Pasirinkti is inventoriaus", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Paieska inventoriuje") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            DropdownField(
                label = "Paskirtis",
                value = buckets.firstOrNull { it.id == selectedBucketId }?.name ?: "Pasirinkti",
                options = buckets.map { it.id to it.name },
                onSelect = { selectedBucketId = it },
                modifier = Modifier.weight(1f)
            )
            DropdownField(
                label = "Atsakingas",
                value = members.firstOrNull { it.userId == responsibleUserId }?.fullName() ?: "Nepasirinkta",
                options = members.map { it.userId to it.fullName() },
                onSelect = { responsibleUserId = it },
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Pastabos visiems pazymetiems") },
            modifier = Modifier.fillMaxWidth()
        )
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)
        ) {
            LazyColumn(contentPadding = PaddingValues(8.dp)) {
                groups.forEach { (category, categoryItems) ->
                    item(key = "category_$category") {
                        CategoryAccordionHeader(
                            title = category,
                            count = categoryItems.size,
                            collapsed = category in collapsedCategories,
                            onClick = {
                                collapsedCategories = if (category in collapsedCategories) {
                                    collapsedCategories - category
                                } else {
                                    collapsedCategories + category
                                }
                            }
                        )
                    }
                    if (category !in collapsedCategories) {
                        items(categoryItems, key = { it.id }) { item ->
                            val selectedQuantity = selectedQuantities[item.id] ?: 0
                            BulkInventoryItemRow(
                                item = item,
                                quantity = selectedQuantity,
                                onCheckedChange = { checked ->
                                    selectedQuantities = if (checked) {
                                        selectedQuantities + (item.id to maxOf(1, selectedQuantity))
                                    } else {
                                        selectedQuantities - item.id
                                    }
                                },
                                onQuantityChange = { newQuantity ->
                                    selectedQuantities = if (newQuantity > 0) {
                                        selectedQuantities + (item.id to newQuantity)
                                    } else {
                                        selectedQuantities - item.id
                                    }
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
        Button(
            onClick = {
                onCreateNeedsBulk(selectedQuantities, selectedBucketId, responsibleUserId, notes)
                selectedQuantities = emptyMap()
                notes = ""
            },
            enabled = !isWorking && selectedQuantities.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Prideti pasirinktus (${selectedQuantities.size})")
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CategoryAccordionHeader(
    title: String,
    count: Int,
    collapsed: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = EventAccent)
            Text("$count daiktai", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess, contentDescription = null)
    }
}

@Composable
private fun UkvedysCard(
    eventStatus: String,
    inventoryPlan: EventInventoryPlanDto?,
    canManage: Boolean,
    isWorking: Boolean,
    onCreatePurchase: (Set<String>) -> Unit
) {
    var selected by remember { mutableStateOf(setOf<String>()) }
    var returnStates by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val shortageItems = inventoryPlan?.items.orEmpty().filter { it.shortageQuantity > 0 }
    val planItems = inventoryPlan?.items.orEmpty()
    val returnMode = eventStatus == "COMPLETED"

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (returnMode) "Grazinimas" else "Ukvedzio suvestine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            when {
                returnMode && planItems.isEmpty() -> {
                    EmptyStateText("Nera ka grazinti. Poreikiu planas tuscias.")
                }
                returnMode -> {
                    ReturnInventoryList(
                        items = planItems,
                        returnStates = returnStates,
                        onReturnStatesChange = { returnStates = it }
                    )
                }
                shortageItems.isEmpty() -> {
                    EmptyStateText("Trukstamu daiktu nera. Kai poreikiuose atsiras trukumu, cia juos pazymesi pirkimui.")
                }
                else -> {
                    ShortageInventoryList(
                        items = shortageItems,
                        selected = selected,
                        onSelectedChange = { selected = it }
                    )
                }
            }
            Button(
                onClick = { onCreatePurchase(selected) },
                enabled = !returnMode && canManage && !isWorking && selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Sukurti pirkima is pazymetu (${selected.size})") }
        }
    }
}

@Composable
private fun ReturnInventoryList(
    items: List<EventInventoryItemDto>,
    returnStates: Map<String, String>,
    onReturnStatesChange: (Map<String, String>) -> Unit
) {
    EventListSection(
        title = "Grazinimo sarasas",
        subtitle = "${items.size} eil. / ${items.sumOf { it.plannedQuantity }} vnt."
    ) {
        items
            .sortedWith(compareBy<EventInventoryItemDto>({ it.bucketName ?: "Be paskirties" }, { it.name.lowercase() }))
            .groupBy { it.bucketName ?: "Be paskirties" }
            .forEach { (bucketName, bucketItems) ->
                EventListGroupHeader(bucketName, bucketItems.size)
                bucketItems.forEach { item ->
                    val state = returnStates[item.id]
                                    EventInventoryListRow(
                                        item = item,
                                        trailing = { EventMetricPill("${item.plannedQuantity}") },
                        bottom = {
                            EventModeChip(
                                selected = state == "OK",
                                text = "Sveika",
                                onClick = { onReturnStatesChange(returnStates + (item.id to "OK")) }
                            )
                            EventModeChip(
                                selected = state == "DAMAGED",
                                text = "Sugadinta",
                                onClick = { onReturnStatesChange(returnStates + (item.id to "DAMAGED")) }
                            )
                        }
                    )
                }
            }
    }
}

@Composable
private fun ShortageInventoryList(
    items: List<EventInventoryItemDto>,
    selected: Set<String>,
    onSelectedChange: (Set<String>) -> Unit
) {
    EventListSection(
        title = "Trukumu sarasas",
        subtitle = "${items.size} eil. / ${items.sumOf { it.shortageQuantity }} vnt. pirkti"
    ) {
        items
            .sortedWith(compareBy<EventInventoryItemDto>({ it.bucketName ?: "Be paskirties" }, { it.name.lowercase() }))
            .groupBy { it.bucketName ?: "Be paskirties" }
            .forEach { (bucketName, bucketItems) ->
                EventListGroupHeader(bucketName, bucketItems.size)
                bucketItems.forEach { item ->
                    EventInventoryListRow(
                        item = item,
                        leading = {
                            Checkbox(
                                checked = item.id in selected,
                                onCheckedChange = { checked ->
                                    onSelectedChange(if (checked) selected + item.id else selected - item.id)
                                }
                            )
                        },
                        trailing = {
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                EventMetricPill("${item.availableQuantity}/${item.plannedQuantity}", EventMetricTone.Neutral)
                                EventMetricPill("Pirkti ${item.shortageQuantity}", EventMetricTone.Warning)
                            }
                        }
                    )
                }
            }
    }
}

@Composable
private fun PurchasesCard(
    eventNotes: String?,
    purchases: List<EventPurchaseDto>,
    canManage: Boolean,
    isWorking: Boolean,
    onCompletePurchase: (String) -> Unit,
    onAddPurchaseToInventory: (String) -> Unit,
    onAttachInvoice: (String, Uri) -> Unit,
    onDownloadInvoice: (String) -> Unit
) {
    var invoiceTargetPurchaseId by remember { mutableStateOf<String?>(null) }
    val spent = purchases.sumOf { it.totalAmount ?: 0.0 }
    val budget = parseBudget(eventNotes)
    val invoicePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val purchaseId = invoiceTargetPurchaseId
        if (uri != null && purchaseId != null) onAttachInvoice(purchaseId, uri)
        invoiceTargetPurchaseId = null
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Pirkimai", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            BudgetProgress(spent = spent, budget = budget)
            if (purchases.isEmpty()) EmptyStateText("Pirkimu dar nera. Pazymek trukstamus daiktus Ukvedzio skiltyje ir sukurk pirkima.")
            purchases.forEach { purchase ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    EventInfoRow("Busena", purchaseStatusLabel(purchase.status))
                    purchase.totalAmount?.let { EventInfoRow("Suma", String.format("%.2f EUR", it)) }
                    purchase.invoiceFileUrl?.let { EventInfoRow("Saskaita", invoiceTypeLabel(it)) }
                    purchase.items.forEach { item -> EventInfoRow(item.itemName, "${item.purchasedQuantity} vnt.") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                invoiceTargetPurchaseId = purchase.id
                                invoicePicker.launch(arrayOf("application/pdf", "image/jpeg", "image/png"))
                            },
                            enabled = canManage && !isWorking,
                            modifier = Modifier.weight(1f)
                        ) { Text("Prisegti") }
                        OutlinedButton(
                            onClick = { onDownloadInvoice(purchase.id) },
                            enabled = purchase.invoiceFileUrl != null && !isWorking,
                            modifier = Modifier.weight(1f)
                        ) { Text("Parsisiusti") }
                    }
                    if (purchase.status == "DRAFT") {
                        OutlinedButton(
                            onClick = { onCompletePurchase(purchase.id) },
                            enabled = canManage && !isWorking,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Pazymeti nupirkta") }
                    }
                    if (purchase.status == "PURCHASED") {
                        Button(
                            onClick = { onAddPurchaseToInventory(purchase.id) },
                            enabled = canManage && !isWorking,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Prideti i inventoriu") }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun BudgetProgress(spent: Double, budget: Double?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val title = if (budget != null && budget > 0.0) {
            "Isleista ${String.format("%.2f", spent)} EUR / Biudzetas ${String.format("%.2f", budget)} EUR"
        } else {
            "Isleista ${String.format("%.2f", spent)} EUR"
        }
        Text(title, fontWeight = FontWeight.SemiBold)
        if (budget != null && budget > 0.0) {
            LinearProgressIndicator(
                progress = { (spent / budget).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PlanCard(
    event: EventDto,
    inventoryPlan: EventInventoryPlanDto?,
    members: List<MemberDto>,
    canEdit: Boolean,
    isWorking: Boolean,
    onUpdateNeed: (EventInventoryItemDto, String, String, String?, String?, String) -> Unit
) {
    var editing by remember { mutableStateOf<EventInventoryItemDto?>(null) }
    val buckets = inventoryPlan?.buckets.orEmpty()

    editing?.let { item ->
        EditNeedDialog(
            item = item,
            buckets = buckets,
            members = members,
            isWorking = isWorking,
            onDismiss = { editing = null },
            onSave = { name, quantity, bucketId, responsibleUserId, notes ->
                onUpdateNeed(item, name, quantity, bucketId, responsibleUserId, notes)
                editing = null
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Inventoriaus planas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            event.inventorySummary?.let { summary ->
                EventInfoRow("Planuojama", summary.totalPlannedQuantity.toString())
                EventInfoRow("Yra inventoriuje", summary.totalAvailableQuantity.toString())
                EventInfoRow("Truksta / pirkti", summary.totalShortageQuantity.toString())
                EventInfoRow("Paskirstyta", summary.totalAllocatedQuantity.toString())
            }
            if (inventoryPlan == null || inventoryPlan.items.isEmpty()) {
                Text("Planas dar tuscias", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                EventListSection(
                    title = "Plano eilutes",
                    subtitle = "${inventoryPlan.items.size} eil. / ${inventoryPlan.items.sumOf { it.plannedQuantity }} vnt."
                ) {
                    inventoryPlan.items
                        .sortedWith(compareBy<EventInventoryItemDto>({ it.bucketName ?: "Be paskirties" }, { it.name.lowercase() }))
                        .groupBy { it.bucketName ?: "Be paskirties" }
                        .forEach { (bucketName, bucketItems) ->
                            EventListGroupHeader(bucketName, bucketItems.size)
                            bucketItems.forEach { item ->
                                EventInventoryListRow(
                                    item = item,
                                    bottom = {
                                        if (canEdit) {
                                            TextButton(
                                                onClick = { editing = item },
                                                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                                            ) {
                                                Text("Redaguoti")
                                            }
                                        }
                                    }
                                )
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun EditNeedDialog(
    item: EventInventoryItemDto,
    buckets: List<EventInventoryBucketDto>,
    members: List<MemberDto>,
    isWorking: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String?, String?, String) -> Unit
) {
    var name by remember(item.id) { mutableStateOf(item.name) }
    var quantity by remember(item.id) { mutableStateOf(item.plannedQuantity.toString()) }
    var bucketId by remember(item.id) { mutableStateOf(item.bucketId) }
    var responsibleUserId by remember(item.id) { mutableStateOf(item.responsibleUserId) }
    var notes by remember(item.id) { mutableStateOf(item.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Redaguoti plano eilute") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Daiktas") })
                OutlinedTextField(value = quantity, onValueChange = { quantity = it.filter(Char::isDigit) }, label = { Text("Kiekis") })
                DropdownField("Paskirtis", buckets.firstOrNull { it.id == bucketId }?.name ?: "Pasirinkti", buckets.map { it.id to it.name }, { bucketId = it })
                DropdownField("Atsakingas", members.firstOrNull { it.userId == responsibleUserId }?.fullName() ?: "Nepasirinkta", members.map { it.userId to it.fullName() }, { responsibleUserId = it })
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Pastabos") })
            }
        },
        confirmButton = {
            TextButton(enabled = !isWorking, onClick = { onSave(name, quantity, bucketId, responsibleUserId, notes) }) {
                Text("Issaugoti")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Atsaukti") } }
    )
}

@Composable
private fun StabasCard(
    roles: List<EventRoleDto>,
    canManage: Boolean,
    onRemoveRole: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Stabas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            if (roles.isEmpty()) {
                EmptyStateText("Stabo nariu dar nera.")
            }
            roles.forEach { role ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(role.userName ?: role.userId, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(eventRoleLabel(role.role), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (canManage && role.role != "VIRSININKAS") {
                        TextButton(onClick = { onRemoveRole(role.id) }) { Text("Salinti") }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun StaffPickerSheet(
    members: List<MemberDto>,
    isWorking: Boolean,
    onAssignRole: (String, String) -> Unit
) {
    var selectedUserId by remember { mutableStateOf<String?>(null) }
    var selectedRole by remember { mutableStateOf("VADOVAS") }
    val roleOptions = listOf("VIRSININKAS", "KOMENDANTAS", "UKVEDYS", "PROGRAMERIS", "MAISTININKAS", "VADOVAS", "SAVANORIS")

    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Prideti i staba", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        DropdownField(
            label = "Zmogus",
            value = members.firstOrNull { it.userId == selectedUserId }?.fullName() ?: "Pasirinkti",
            options = members.map { it.userId to it.fullName() },
            onSelect = { selectedUserId = it }
        )
        DropdownField(
            label = "Role",
            value = eventRoleLabel(selectedRole),
            options = roleOptions.map { it to eventRoleLabel(it) },
            onSelect = { selectedRole = it }
        )
        Button(
            onClick = { selectedUserId?.let { onAssignRole(it, selectedRole) } },
            enabled = !isWorking && selectedUserId != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Prideti")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = {
                    onSelect(id)
                    expanded = false
                })
            }
        }
    }
}

@Composable
private fun EventInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StovyklaDetailsCompact(details: StovyklaDetailsDto) {
    details.actualParticipants?.let { EventInfoRow("Stabo zmones", it.toString()) }
}

private fun MemberDto.fullName(): String = "$name $surname".trim()

private fun invoiceTypeLabel(url: String): String = when (url.substringAfterLast('.', "").lowercase()) {
    "pdf" -> "PDF saskaita"
    "jpg", "jpeg", "png" -> "Saskaitos nuotrauka"
    else -> "Saskaitos failas"
}

private fun eventTypeLabel(type: String) = when (type) {
    "STOVYKLA" -> "Stovykla"
    "SUEIGA" -> "Sueiga"
    "RENGINYS" -> "Renginys"
    else -> type
}

private fun purchaseStatusLabel(status: String) = when (status) {
    "DRAFT" -> "Ruosiama"
    "PURCHASED" -> "Nupirkta"
    "ADDED_TO_INVENTORY" -> "Prideta i inventoriu"
    "CANCELLED" -> "Atsaukta"
    else -> status
}

private fun planItemSubtitle(item: EventInventoryItemDto): String {
    val parts = mutableListOf<String>()
    item.bucketName?.takeIf { it.isNotBlank() }?.let { parts += "Paskirtis: $it" }
    if (item.reservationGroupId != null) parts += "Rezervuota"
    item.responsibleUserName?.takeIf { it.isNotBlank() }?.let { parts += "Atsakingas: $it" }
    return parts.joinToString(" / ").ifBlank { "Paskirtis neparinkta" }
}

private fun eventRoleLabel(role: String) = when (role) {
    "VIRSININKAS" -> "Viršininkas"
    "KOMENDANTAS" -> "Komendantas"
    "UKVEDYS" -> "Ūkvedys"
    "PROGRAMERIS" -> "Programeris"
    "MAISTININKAS" -> "Maistininkas"
    "VADOVAS" -> "Vadovas"
    "SAVANORIS" -> "Savanoris"
    else -> role
}

private fun parseBudget(notes: String?): Double? {
    val text = notes ?: return null
    val regex = Regex("(biudzetas|budget)\\s*[:=]?\\s*(\\d+(?:[.,]\\d+)?)", RegexOption.IGNORE_CASE)
    return regex.find(text)
        ?.groupValues
        ?.getOrNull(2)
        ?.replace(',', '.')
        ?.toDoubleOrNull()
}

private fun inventoryCategoryFilters(items: List<ItemDto>): List<String> {
    val known = listOf("CAMPING", "TOOLS", "COOKING", "FIRST_AID", "UNIFORMS", "BOOKS", "PERSONAL_LOANS")
    val dynamic = items
        .map { it.category.trim() }
        .filter { it.isNotBlank() && it !in known }
        .distinctBy { it.normalizeLt() }
        .sortedBy { inventoryCategoryLabel(it).normalizeLt() }
    return listOf("Visi") + known + dynamic
}

private fun categoryFilterLabel(category: String): String =
    if (category == "Visi") category else inventoryCategoryLabel(category)

private fun categoryMatches(item: ItemDto, selectedCategory: String): Boolean {
    if (selectedCategory == "Visi") return true
    val haystack = "${item.category} ${item.type} ${item.name}".normalizeLt()
    val needle = selectedCategory.normalizeLt()
    return haystack.contains(needle)
}

private fun String.normalizeLt(): String = lowercase()
    .replace('ą', 'a')
    .replace('č', 'c')
    .replace('ę', 'e')
    .replace('ė', 'e')
    .replace('į', 'i')
    .replace('š', 's')
    .replace('ų', 'u')
    .replace('ū', 'u')
    .replace('ž', 'z')
