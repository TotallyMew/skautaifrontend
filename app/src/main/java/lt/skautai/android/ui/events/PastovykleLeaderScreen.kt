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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.EventInventoryAllocationDto
import lt.skautai.android.data.remote.EventInventoryRequestDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.PastovykleInventoryDto
import lt.skautai.android.data.remote.PastovykleMemberDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiEmptyState
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.SkautaiStatusTone
import lt.skautai.android.ui.common.SkautaiTextField
import lt.skautai.android.ui.common.codeLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastovykleLeaderScreen(
    eventId: String,
    onBack: () -> Unit,
    viewModel: PastovykleLeaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(eventId) {
        viewModel.load(eventId)
    }

    LaunchedEffect((uiState as? PastovykleLeaderUiState.Success)?.error) {
        (uiState as? PastovykleLeaderUiState.Success)?.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    EventScreenScaffold(
        title = "Mano pastovyklė",
        onBack = onBack,
        snackbarHostState = snackbarHostState
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                PastovykleLeaderUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is PastovykleLeaderUiState.Error -> {
                    SkautaiErrorState(
                        message = state.message,
                        onRetry = { viewModel.load(eventId) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is PastovykleLeaderUiState.Success -> {
                    val readOnly = isEventReadOnlyStatus(state.event.status)
                    val coLeaderPastovykleIds = state.event.eventRoles
                        .filter { it.role == "PASTOVYKLES_GURU" && it.userId == state.currentUserId && it.pastovykleId != null }
                        .mapNotNull { it.pastovykleId }
                        .toSet()
                    val myPastovyklės = state.pastovykles.filter {
                        it.responsibleUserId == state.currentUserId || it.id in coLeaderPastovykleIds
                    }
                    var selectedPastovykleId by remember(myPastovyklės) {
                        mutableStateOf(myPastovyklės.firstOrNull()?.id)
                    }
                    val selectedPastovykle = myPastovyklės.firstOrNull { it.id == selectedPastovykleId }
                    val inventory = state.pastovykleInventoryById[selectedPastovykleId].orEmpty()
                    val requests = state.pastovykleRequestsById[selectedPastovykleId].orEmpty()
                    val members = state.pastovykleMembersById[selectedPastovykleId].orEmpty()
                    val sharedItems = state.items.filter { it.custodianId == null }
                    val unitItems = state.items.filter { it.custodianId == state.activeOrgUnitId }
                    val pastovykleBucketIds = state.inventoryPlan?.buckets
                        .orEmpty()
                        .filter { it.pastovykleId == selectedPastovykleId }
                        .map { it.id }
                        .toSet()
                    val allocations = state.inventoryPlan?.allocations
                        .orEmpty()
                        .filter { it.bucketId in pastovykleBucketIds }

                    if (myPastovyklės.isEmpty()) {
                        SkautaiEmptyState(
                            title = "Pastovyklė nepriskirta",
                            subtitle = "Kai būsi paskirtas pastovyklės vadovu, čia matysi savo poreikius, paskirtą inventorių ir veiksmus.",
                            icon = Icons.Default.Groups,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                EventDetailHero(
                                    event = state.event,
                                    subtitle = selectedPastovykle?.let { "Pastovyklė · ${it.name}" } ?: "Pastovyklė"
                                )
                            }

                            item {
                                DropdownField(
                                    label = "Pastovyklė",
                                    value = selectedPastovykle?.name ?: "Pasirinkti",
                                    options = myPastovyklės.map { it.id to it.name },
                                    onSelect = { selectedPastovykleId = it }
                                )
                            }

                            if (selectedPastovykle != null) {
                                item {
                                    EventDetailMetricRow(
                                        metrics = listOf(
                                            "Pasirengta" to "${state.readiness?.readinessPercent ?: 0}%",
                                            "Paskirta" to allocations.sumOf { it.quantity }.toString(),
                                            "Vėluoja" to (state.readiness?.overdueCount ?: 0).toString()
                                        )
                                    )
                                }
                                state.readiness?.conflicts?.takeIf { it.isNotEmpty() }?.let { conflicts ->
                                    item {
                                        EventDetailSection(
                                            title = "Inventoriaus konfliktai",
                                            subtitle = "Tas pats fizinis inventorius suplanuotas persidengiantiems renginiams."
                                        ) {
                                            conflicts.forEach { conflict ->
                                                CompactInventoryRow(
                                                    title = conflict.itemName,
                                                    subtitle = conflict.overlappingEvents.joinToString(),
                                                    trailing = "${conflict.requestedQuantity}/${conflict.availableQuantity}"
                                                )
                                            }
                                        }
                                    }
                                }
                                item {
                                    RequestNeedCard(
                                        items = sharedItems,
                                        members = state.candidateMembers,
                                        isWorking = state.isWorking || readOnly,
                                        onCreateRequest = { itemId, customName, quantity, notes, provider, dueAt, responsibleUserId ->
                                            viewModel.createPastovykleRequest(
                                                eventId = eventId,
                                                pastovykleId = selectedPastovykle.id,
                                                itemId = itemId,
                                                customName = customName,
                                                quantityText = quantity,
                                                notes = notes,
                                                provider = provider,
                                                dueAt = dueAt,
                                                responsibleUserId = responsibleUserId
                                            )
                                        }
                                    )
                                }
                                item {
                                    BringFromUnitCard(
                                        unitItems = unitItems,
                                        isWorking = state.isWorking || readOnly,
                                        onAssign = { itemId, quantity, notes ->
                                            viewModel.assignFromUnitInventory(eventId, selectedPastovykle.id, itemId, quantity, notes)
                                        }
                                    )
                                }
                                item {
                                    AllocationSummaryCard(allocations = allocations)
                                }
                                item {
                                    PastovykleMembersCard(
                                        members = members,
                                        candidateMembers = state.candidateMembers,
                                        isWorking = state.isWorking || readOnly,
                                        onAdd = { userId -> viewModel.addPastovykleMember(eventId, selectedPastovykle.id, userId) },
                                        onRemove = { memberId -> viewModel.removePastovykleMember(eventId, selectedPastovykle.id, memberId) }
                                    )
                                }
                                item {
                                    IssueToMemberCard(
                                        inventory = inventory,
                                        members = members,
                                        isWorking = state.isWorking || readOnly,
                                        onIssue = { itemId, recipientUserId, quantity, notes ->
                                            viewModel.issueToMember(
                                                eventId,
                                                selectedPastovykle.id,
                                                itemId,
                                                recipientUserId,
                                                quantity,
                                                notes
                                            )
                                        }
                                    )
                                }
                                item {
                                    PastovykleInventoryCard(
                                        inventory = inventory,
                                        members = members,
                                        isWorking = state.isWorking || readOnly,
                                        onReturn = { row ->
                                            viewModel.markInventoryReturned(eventId, selectedPastovykle.id, row)
                                        }
                                    )
                                }
                                item {
                                    PastovykleRequestsCard(
                                        title = "Vienetas parūpins",
                                        subtitle = "Daiktai, už kuriuos atsakingas pats vienetas.",
                                        requests = requests.filter { it.provider == "UNIT" },
                                        members = state.candidateMembers,
                                        isWorking = state.isWorking || readOnly,
                                        emptyText = "Vienetui priskirtų poreikių nėra.",
                                        actionText = "Pažymėti parūpinta",
                                        onSwitchProvider = { requestId ->
                                            viewModel.switchRequestProvider(eventId, selectedPastovykle.id, requestId, "UKVEDYS")
                                        },
                                        onUpdate = { requestId, provider, dueDate, responsibleUserId, notes ->
                                            viewModel.updatePastovykleRequest(
                                                eventId, selectedPastovykle.id, requestId,
                                                provider, dueDate, responsibleUserId, notes
                                            )
                                        },
                                        onSelfProvide = { requestId ->
                                            viewModel.selfProvidePastovykleRequest(
                                                eventId,
                                                selectedPastovykle.id,
                                                requestId,
                                                "Pasirūpinta savo jėgomis"
                                            )
                                        }
                                    )
                                }
                                item {
                                    PastovykleRequestsCard(
                                        title = "Prašymai ūkvedžiui",
                                        subtitle = "Tik tai, ką turi parūpinti renginio ūkvedys.",
                                        requests = requests.filter { it.provider == "UKVEDYS" },
                                        members = state.candidateMembers,
                                        isWorking = state.isWorking || readOnly,
                                        emptyText = "Prašymų ūkvedžiui nėra.",
                                        actionText = "Pasirūpinome patys",
                                        onSwitchProvider = { requestId ->
                                            viewModel.switchRequestProvider(eventId, selectedPastovykle.id, requestId, "UNIT")
                                        },
                                        onUpdate = { requestId, provider, dueDate, responsibleUserId, notes ->
                                            viewModel.updatePastovykleRequest(
                                                eventId, selectedPastovykle.id, requestId,
                                                provider, dueDate, responsibleUserId, notes
                                            )
                                        },
                                        onSelfProvide = { requestId ->
                                            viewModel.selfProvidePastovykleRequest(
                                                eventId,
                                                selectedPastovykle.id,
                                                requestId,
                                                "Pasirūpinta savo jėgomis"
                                            )
                                        }
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

@Composable
private fun RequestNeedCard(
    items: List<ItemDto>,
    members: List<MemberDto>,
    isWorking: Boolean,
    onCreateRequest: (String?, String?, String, String, String, String?, String?) -> Unit
) {
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    var customItemName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf("UNIT") }
    var dueDate by remember { mutableStateOf("") }
    var responsibleUserId by remember { mutableStateOf<String?>(null) }

    EventDetailSection(
        title = "Ko reikia pastovyklei",
        subtitle = "Pasirink, ar daiktą parūpins vienetas, ar renginio ūkvedys."
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { provider = "UNIT" },
                modifier = Modifier.weight(1f),
                enabled = !isWorking
            ) {
                Text(if (provider == "UNIT") "✓ Vienetas" else "Vienetas")
            }
            OutlinedButton(
                onClick = { provider = "UKVEDYS" },
                modifier = Modifier.weight(1f),
                enabled = !isWorking
            ) {
                Text(if (provider == "UKVEDYS") "✓ Ūkvedys" else "Ūkvedys")
            }
        }
        DropdownField(
            label = "Daiktas",
            value = items.firstOrNull { it.id == selectedItemId }?.name ?: "Pasirinkti",
            options = items.map { it.id to "${it.name} (${it.quantity})" },
            onSelect = { selectedItemId = it }
        )
        SkautaiTextField(
            value = customItemName,
            onValueChange = { customItemName = it },
            label = "Arba laisvas poreikis",
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        SkautaiTextField(
            value = quantity,
            onValueChange = { quantity = it.filter(Char::isDigit) },
            label = "Kiekis",
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        SkautaiTextField(
            value = dueDate,
            onValueChange = { dueDate = it },
            label = "Terminas (YYYY-MM-DD)",
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        DropdownField(
            label = "Atsakingas žmogus",
            value = members.firstOrNull { it.userId == responsibleUserId }?.fullName() ?: "Nepasirinkta",
            options = listOf("" to "Nepasirinkta") + members.map { it.userId to it.fullName() },
            onSelect = { responsibleUserId = it.ifBlank { null } }
        )
        SkautaiTextField(
            value = notes,
            onValueChange = { notes = it },
            label = "Pastabos",
            modifier = Modifier.fillMaxWidth()
        )
        EventPrimaryButton(
            text = if (provider == "UNIT") "Pridėti prie vieneto plano" else "Siųsti prašymą ūkvedžiui",
            onClick = {
                onCreateRequest(
                    selectedItemId,
                    customItemName.ifBlank { null },
                    quantity,
                    notes,
                    provider,
                    dueDate.takeIf { it.isNotBlank() }?.let { "${it}T23:59:59Z" },
                    responsibleUserId
                )
                quantity = ""
                notes = ""
                customItemName = ""
                dueDate = ""
                responsibleUserId = null
            },
            enabled = !isWorking && (selectedItemId != null || customItemName.isNotBlank())
        )
    }
}

@Composable
private fun BringFromUnitCard(
    unitItems: List<ItemDto>,
    isWorking: Boolean,
    onAssign: (String, String, String) -> Unit
) {
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    var quantity by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    EventDetailSection(
        title = "Atsivešiu iš savo vieneto",
        subtitle = "Pažymėk, ką pastovyklė pasirūpins savo jėgomis."
    ) {
        DropdownField(
            label = "Vieneto daiktas",
            value = unitItems.firstOrNull { it.id == selectedItemId }?.name ?: "Pasirinkti",
            options = unitItems.map { it.id to "${it.name} (${it.quantity})" },
            onSelect = { selectedItemId = it }
        )
        SkautaiTextField(
            value = quantity,
            onValueChange = { quantity = it.filter(Char::isDigit) },
            label = "Kiekis",
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        SkautaiTextField(
            value = notes,
            onValueChange = { notes = it },
            label = "Pastabos",
            modifier = Modifier.fillMaxWidth()
        )
        EventPrimaryButton(
            text = "Pažymėti, kad atsivešiu",
            onClick = {
                selectedItemId?.let { onAssign(it, quantity, notes) }
                quantity = ""
                notes = ""
            },
            enabled = !isWorking && selectedItemId != null
        )
    }
}

@Composable
private fun AllocationSummaryCard(allocations: List<EventInventoryAllocationDto>) {
    EventDetailSection(
        title = "Jau paskirta man",
        subtitle = "Ūkvedžio suplanuotas inventoriaus paskirstymas."
    ) {
        if (allocations.isEmpty()) {
            EmptyStateText("Ūkvedys dar nesuplanuavo paskirstymo.")
        } else {
            allocations.forEach { allocation ->
                CompactInventoryRow(
                    title = allocation.bucketName,
                    subtitle = allocation.notes,
                    trailing = "${allocation.quantity} vnt."
                )
            }
        }
    }
}

@Composable
private fun PastovykleMembersCard(
    members: List<PastovykleMemberDto>,
    candidateMembers: List<MemberDto>,
    isWorking: Boolean,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    val memberUserIds = members.map { it.userId }.toSet()
    val addOptions = candidateMembers.filter { it.userId !in memberUserIds }
    var selectedUserId by remember(addOptions) { mutableStateOf(addOptions.firstOrNull()?.userId) }

    EventDetailSection(
        title = "Vaikai",
        subtitle = "Pastovyklės nariai, kuriems galima išduoti inventorių."
    ) {
        if (members.isEmpty()) {
            EmptyStateText("Vaikų sąrašas dar tuščias.")
        } else {
            members.forEach { member ->
                CompactInventoryRow(
                    title = member.userName,
                    subtitle = "Aktyvus narys",
                    trailing = ""
                )
                OutlinedButton(
                    onClick = { onRemove(member.id) },
                    enabled = !isWorking,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pašalinti")
                }
            }
        }
        if (addOptions.isNotEmpty()) {
            DropdownField(
                label = "Pridėti vaiką",
                value = addOptions.firstOrNull { it.userId == selectedUserId }?.fullName() ?: "Pasirinkti",
                options = addOptions.map { it.userId to it.fullName() },
                onSelect = { selectedUserId = it }
            )
            EventPrimaryButton(
                text = "Pridėti",
                onClick = { selectedUserId?.let(onAdd) },
                enabled = !isWorking && selectedUserId != null
            )
        }
    }
}

@Composable
private fun IssueToMemberCard(
    inventory: List<PastovykleInventoryDto>,
    members: List<PastovykleMemberDto>,
    isWorking: Boolean,
    onIssue: (String, String, String, String) -> Unit
) {
    val availableRows = inventory
        .filter { it.recipientUserId == null && it.quantityAssigned > it.quantityReturned }
        .distinctBy { it.itemId }
    var selectedItemId by remember(availableRows) { mutableStateOf(availableRows.firstOrNull()?.itemId) }
    var selectedMemberId by remember(members) { mutableStateOf(members.firstOrNull()?.userId) }
    var quantity by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    EventDetailSection(
        title = "Išduoti vaikui",
        subtitle = "Pasirink daiktą iš pastovyklės inventoriaus ir gavėją."
    ) {
        if (availableRows.isEmpty()) {
            EmptyStateText("Nėra laisvo pastovyklės inventoriaus išdavimui.")
            return@EventDetailSection
        }
        if (members.isEmpty()) {
            EmptyStateText("Nėra narių, kuriems galima išduoti inventorių.")
            return@EventDetailSection
        }
        DropdownField(
            label = "Daiktas",
            value = availableRows.firstOrNull { it.itemId == selectedItemId }?.itemName ?: "Pasirinkti",
            options = availableRows.map { row ->
                row.itemId to "${row.itemName} (${row.quantityAssigned - row.quantityReturned})"
            },
            onSelect = { selectedItemId = it }
        )
        DropdownField(
            label = "Gavėjas",
            value = members.firstOrNull { it.userId == selectedMemberId }?.userName ?: "Pasirinkti",
            options = members.map { it.userId to it.userName },
            onSelect = { selectedMemberId = it }
        )
        SkautaiTextField(
            value = quantity,
            onValueChange = { quantity = it.filter(Char::isDigit) },
            label = "Kiekis",
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        SkautaiTextField(
            value = notes,
            onValueChange = { notes = it },
            label = "Pastabos",
            modifier = Modifier.fillMaxWidth()
        )
        EventPrimaryButton(
            text = "Išduoti",
            onClick = {
                val itemId = selectedItemId
                val memberId = selectedMemberId
                if (itemId != null && memberId != null) {
                    onIssue(itemId, memberId, quantity, notes)
                    quantity = ""
                    notes = ""
                }
            },
            enabled = !isWorking && selectedItemId != null && selectedMemberId != null
        )
    }
}

@Composable
private fun PastovykleInventoryCard(
    inventory: List<PastovykleInventoryDto>,
    members: List<PastovykleMemberDto>,
    isWorking: Boolean,
    onReturn: (PastovykleInventoryDto) -> Unit
) {
    EventDetailSection(
        title = "Mano pastovyklės inventorius",
        subtitle = "Faktiškai pastovyklei priskirti ir dar negrąžinti daiktai."
    ) {
        if (inventory.isEmpty()) {
            EmptyStateText("Inventorius dar nepriskirtas.")
        } else {
            inventory.forEach { row ->
                val recipientName = row.recipientUserId
                    ?.let { recipientId -> members.firstOrNull { it.userId == recipientId }?.userName }
                CompactInventoryRow(
                    title = row.itemName,
                    subtitle = listOfNotNull(
                        recipientName,
                        "Grąžinta ${row.quantityReturned} iš ${row.quantityAssigned}"
                    ).joinToString(" · "),
                    trailing = "${row.quantityAssigned - row.quantityReturned}"
                )
                if (row.recipientUserId != null && row.quantityReturned < row.quantityAssigned) {
                    OutlinedButton(
                        onClick = { onReturn(row) },
                        enabled = !isWorking,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Žymėti grąžintą")
                    }
                }
            }
        }
    }
}

@Composable
private fun PastovykleRequestsCard(
    title: String,
    subtitle: String,
    requests: List<EventInventoryRequestDto>,
    members: List<MemberDto>,
    isWorking: Boolean,
    emptyText: String,
    actionText: String,
    onSwitchProvider: (String) -> Unit,
    onUpdate: (String, String, String, String?, String) -> Unit,
    onSelfProvide: (String) -> Unit
) {
    var editingRequestId by remember { mutableStateOf<String?>(null) }
    var editProvider by remember { mutableStateOf("UNIT") }
    var editDueDate by remember { mutableStateOf("") }
    var editResponsibleUserId by remember { mutableStateOf<String?>(null) }
    var editNotes by remember { mutableStateOf("") }
    EventDetailSection(
        title = title,
        subtitle = subtitle
    ) {
        if (requests.isEmpty()) {
            EmptyStateText(emptyText)
        } else {
            requests.forEach { request ->
                SkautaiCard(
                    modifier = Modifier.fillMaxWidth(),
                    tonal = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = request.itemName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${request.quantity} vnt.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            RequestStatusPill(request.status)
                        }
                        request.notes?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = listOfNotNull(
                                request.responsibleUserName?.let { "Atsakingas: $it" },
                                request.dueAt?.take(10)?.let { "Terminas: $it" }
                            ).joinToString(" | ").ifBlank { "Atsakingas ir terminas nenustatyti" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (editingRequestId == request.id) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { editProvider = "UNIT" },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isWorking
                                ) {
                                    Text(if (editProvider == "UNIT") "✓ Vienetas" else "Vienetas")
                                }
                                OutlinedButton(
                                    onClick = { editProvider = "UKVEDYS" },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isWorking
                                ) {
                                    Text(if (editProvider == "UKVEDYS") "✓ Ūkvedys" else "Ūkvedys")
                                }
                            }
                            SkautaiTextField(
                                value = editDueDate,
                                onValueChange = { editDueDate = it },
                                label = "Terminas (YYYY-MM-DD)",
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            DropdownField(
                                label = "Atsakingas žmogus",
                                value = members.firstOrNull { it.userId == editResponsibleUserId }?.fullName() ?: "Nepasirinkta",
                                options = listOf("" to "Nepasirinkta") + members.map { it.userId to it.fullName() },
                                onSelect = { editResponsibleUserId = it.ifBlank { null } }
                            )
                            SkautaiTextField(
                                value = editNotes,
                                onValueChange = { editNotes = it },
                                label = "Pastabos",
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { editingRequestId = null },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Atšaukti")
                                }
                                EventPrimaryButton(
                                    text = "Išsaugoti",
                                    onClick = {
                                        onUpdate(
                                            request.id,
                                            editProvider,
                                            editDueDate,
                                            editResponsibleUserId,
                                            editNotes
                                        )
                                        editingRequestId = null
                                    },
                                    enabled = !isWorking,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    editingRequestId = request.id
                                    editProvider = request.provider
                                    editDueDate = request.dueAt?.take(10).orEmpty()
                                    editResponsibleUserId = request.responsibleUserId
                                    editNotes = request.notes.orEmpty()
                                },
                                enabled = !isWorking,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Redaguoti")
                            }
                        }
                        if (request.status in listOf("PENDING", "APPROVED")) {
                            OutlinedButton(
                                onClick = { onSwitchProvider(request.id) },
                                enabled = !isWorking,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (request.provider == "UNIT") "Perduoti ūkvedžiui" else "Perimti vienetui")
                            }
                        }
                        if (request.status in listOf("PENDING", "APPROVED")) {
                            EventPrimaryButton(
                                text = actionText,
                                onClick = { onSelfProvide(request.id) },
                                enabled = !isWorking
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactInventoryRow(
    title: String,
    subtitle: String?,
    trailing: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Text(trailing, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
    HorizontalDivider()
}

@Composable
private fun RequestStatusPill(status: String) {
    val (label, tone) = when (status) {
        "PENDING" -> "Laukia" to SkautaiStatusTone.Warning
        "APPROVED" -> "Patvirtinta" to SkautaiStatusTone.Info
        "FULFILLED" -> "Įvykdyta" to SkautaiStatusTone.Success
        "REJECTED" -> "Atmesta" to SkautaiStatusTone.Danger
        "SELF_PROVIDED" -> "Savo jėgomis" to SkautaiStatusTone.Neutral
        else -> codeLabel(status) to SkautaiStatusTone.Neutral
    }
    SkautaiStatusPill(label = label, tone = tone)
}
