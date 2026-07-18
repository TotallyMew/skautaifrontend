package lt.skautai.android.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.EventPurchaseReconciliationCandidateDto
import lt.skautai.android.data.remote.EventReconciliationPurchaseLineDto
import lt.skautai.android.data.remote.EventReconciliationReturnLineDto
import lt.skautai.android.ui.common.ItemCheckResult
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.SkautaiStatusTone
import lt.skautai.android.ui.common.buildItemCheckSummary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventReconciliationScreen(
    eventId: String,
    onBack: () -> Unit,
    viewModel: EventReconciliationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingReturnDecision by remember { mutableStateOf<Pair<EventReconciliationReturnLineDto, String>?>(null) }
    var pendingPurchaseDecision by remember { mutableStateOf<Pair<EventReconciliationPurchaseLineDto, String>?>(null) }

    LaunchedEffect(eventId) { viewModel.load(eventId) }
    LaunchedEffect((state as? EventReconciliationUiState.Success)?.error) {
        (state as? EventReconciliationUiState.Success)?.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    EventScreenScaffold(
        title = "Inventoriaus suvedimas",
        onBack = onBack,
        snackbarHostState = snackbarHostState
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val current = state) {
                EventReconciliationUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is EventReconciliationUiState.Error -> SkautaiErrorState(
                    message = current.message,
                    onRetry = { viewModel.load(eventId) },
                    modifier = Modifier.align(Alignment.Center)
                )
                is EventReconciliationUiState.Success -> {
                    val reconciliation = current.reconciliation
                    val openReturns = reconciliation.openReturns.orEmpty()
                    val returnedToEventStorage = reconciliation.returnedToEventStorage.orEmpty()
                    val unresolvedPurchases = reconciliation.unresolvedPurchases.orEmpty()
                    val listState = rememberLazyListState()
                    val coroutineScope = rememberCoroutineScope()
                    var showUncheckedOnly by rememberSaveable { mutableStateOf(false) }
                    val visibleReturns = remember(openReturns, showUncheckedOnly) {
                        if (showUncheckedOnly) {
                            openReturns.filter { it.auditLog.isEmpty() && it.returnDecision == null }
                        } else {
                            openReturns
                        }
                    }
                    val checkSummary = remember(reconciliation) {
                        buildItemCheckSummary(
                            total = openReturns.size + returnedToEventStorage.size,
                            results = returnedToEventStorage.map { it.toItemCheckResult() } +
                                openReturns.map { it.toItemCheckResult() }
                        )
                    }
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            EventDetailHero(
                                event = current.event,
                                subtitle = "Atviri grąžinimai: ${openReturns.size} · pirkimai sprendimui: ${unresolvedPurchases.size}",
                                metrics = listOf(
                                    "Negrąžinta" to openReturns.sumOf { it.remainingQuantity }.toString(),
                                    "Pirkimai" to unresolvedPurchases.size.toString(),
                                    "Būsena" to if (reconciliation.canComplete) "Paruošta" else "Tikrinama"
                                )
                            )
                        }

                        item {
                            ReconciliationCheckSummaryCard(
                                summary = checkSummary,
                                openReturnCount = openReturns.sumOf { it.remainingQuantity },
                                hasOpenReturns = openReturns.isNotEmpty(),
                                actionLabel = if (returnedToEventStorage.isEmpty()) {
                                    "Pradėti inventorizaciją"
                                } else {
                                    "Tęsti inventorizaciją"
                                },
                                onOpenReturns = {
                                    coroutineScope.launch { listState.animateScrollToItem(2) }
                                },
                                showUncheckedOnly = showUncheckedOnly,
                                isRefreshing = current.isWorking,
                                onToggleUncheckedOnly = { showUncheckedOnly = !showUncheckedOnly },
                                onRefresh = { viewModel.load(eventId) }
                            )
                        }

                        item {
                            EventReconciliationReportCard(
                                openReturns = openReturns,
                                returnedRows = returnedToEventStorage,
                                unresolvedPurchases = unresolvedPurchases
                            )
                        }

                        item {
                            ReconciliationReturnSection(
                                title = "Negrąžinti daiktai",
                                rows = visibleReturns,
                                totalOpenRows = openReturns.size,
                                showUncheckedOnly = showUncheckedOnly,
                                isWorking = current.isWorking || current.event.status == "COMPLETED",
                                onOpen = { row, decision ->
                                    pendingReturnDecision = row to decision
                                }
                            )
                        }

                        item {
                            ReconciliationCompletedSection(
                                title = "Grąžinimų suvestinė",
                                rows = returnedToEventStorage
                            )
                        }

                        item {
                            ReconciliationPurchaseSection(
                                title = "Pirkimai sprendimui",
                                rows = unresolvedPurchases,
                                isWorking = current.isWorking || current.event.status == "COMPLETED",
                                onDecision = { row, decision ->
                                    if (decision == "INCREASE_EXISTING_ITEM") {
                                        viewModel.loadPurchaseCandidates(eventId, row.purchaseItemId)
                                    }
                                    pendingPurchaseDecision = row to decision
                                }
                            )
                        }

                        if (current.event.status == "WRAP_UP") {
                            item {
                                Button(
                                    onClick = { viewModel.complete(eventId) },
                                    enabled = reconciliation.canComplete && !current.isWorking,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Pažymėti renginį užbaigtu")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingPurchaseDecision?.let { (row, decision) ->
        val current = state as? EventReconciliationUiState.Success
        ReconcilePurchaseQuantityDialog(
            row = row,
            decision = decision,
            candidates = current?.purchaseCandidates?.get(row.purchaseItemId).orEmpty(),
            candidatesLoading = current?.candidateLoadingPurchaseItemId == row.purchaseItemId,
            onDismiss = { pendingPurchaseDecision = null },
            onConfirm = { quantity, existingItemId ->
                pendingPurchaseDecision = null
                viewModel.reconcilePurchase(
                    eventId = eventId,
                    purchaseItemId = row.purchaseItemId,
                    decision = decision,
                    quantity = quantity,
                    existingItemId = if (decision == "INCREASE_EXISTING_ITEM") existingItemId else null
                )
            }
        )
    }

    pendingReturnDecision?.let { (row, decision) ->
        ReconcileReturnQuantityDialog(
            row = row,
            initialDecision = decision,
            onDismiss = { pendingReturnDecision = null },
            onConfirm = { selectedDecision, quantity, returnToMode, note ->
                pendingReturnDecision = null
                viewModel.reconcileReturn(
                    eventId = eventId,
                    custodyId = row.custodyId,
                    decision = selectedDecision,
                    quantity = quantity,
                    returnToMode = returnToMode,
                    returnLocationNote = note,
                    notes = note
                )
            }
        )
    }
}

@Composable
private fun ReconciliationReturnSection(
    title: String,
    rows: List<EventReconciliationReturnLineDto>,
    totalOpenRows: Int,
    showUncheckedOnly: Boolean,
    isWorking: Boolean,
    onOpen: (EventReconciliationReturnLineDto, String) -> Unit
) {
    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (showUncheckedOnly) {
                            "Rodomi tik dar nesuvesti: ${rows.size}/$totalOpenRows"
                        } else {
                            "Atviri grąžinimai: $totalOpenRows"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (totalOpenRows > 0) {
                    SkautaiStatusPill(label = "${rows.sumOf { it.remainingQuantity }} vnt.", tone = SkautaiStatusTone.Warning)
                }
            }
            if (rows.isEmpty()) {
                Text(
                    text = if (showUncheckedOnly && totalOpenRows > 0) {
                        "Visi atviri grąžinimai jau turi bent vieną suvedimo įrašą."
                    } else {
                        "Atvirų grąžinimų nėra."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                rows.forEach { row ->
                    ReconciliationLine(
                        title = row.itemName,
                        subtitle = row.currentHolderSummary ?: listOfNotNull(row.pastovykleName, row.holderUserName).joinToString(" · ").ifBlank { "Renginio sandėlis" },
                        trailing = "${row.remainingQuantity}/${row.quantity} vnt."
                    )
                    row.sourcePickupSummary?.let {
                        Text("Originali vieta: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DecisionButtons(
                        enabled = !isWorking,
                        decisions = listOf(
                            "RETURNED" to "Grįžo",
                            "DAMAGED" to "Sugadinta",
                            "MISSING" to "Dingo",
                            "CONSUMED" to "Sunaudota"
                        ),
                        onDecision = { onOpen(row, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReconciliationCompletedSection(
    title: String,
    rows: List<EventReconciliationReturnLineDto>
) {
    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (rows.isEmpty()) {
                Text("Užbaigtų grąžinimų dar nėra.", style = MaterialTheme.typography.bodySmall)
            } else {
                rows.forEachIndexed { index, row ->
                    ReconciliationLine(
                        title = row.itemName,
                        subtitle = listOfNotNull(
                            row.returnDecision?.let(::returnDecisionLabel),
                            row.returnCondition?.let(::returnConditionLabel),
                            row.returnedToSummary
                        ).joinToString(" · ").ifBlank { row.status },
                        trailing = if (row.isReturned) "Užbaigta" else row.status
                    )
                    row.currentHolderSummary?.let {
                        Text("Buvo pas: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    row.sourcePickupSummary?.let {
                        Text("Paimta iš: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!row.notes.isNullOrBlank()) {
                        Text(row.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (index != rows.lastIndex) HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ReconciliationPurchaseSection(
    title: String,
    rows: List<EventReconciliationPurchaseLineDto>,
    isWorking: Boolean,
    onDecision: (EventReconciliationPurchaseLineDto, String) -> Unit
) {
    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (rows.isEmpty()) {
                Text("Pirkimų sprendimui nėra.", style = MaterialTheme.typography.bodySmall)
            } else {
                rows.forEachIndexed { index, row ->
                    ReconciliationLine(
                        title = row.itemName,
                        subtitle = if (row.invoiceFileUrl == null) "Sąskaita nepridėta" else "Sąskaita pridėta",
                        trailing = "${row.purchasedQuantity} vnt."
                    )
                    val decisions = buildList {
                        if (row.itemId != null) add("INCREASE_EXISTING_ITEM" to "Papildyti")
                        if (row.itemId == null) add("ADD_NEW_ITEM" to "Naujas")
                        add("CONSUMED" to "Sunaudota")
                        add("IGNORE" to "Ignoruoti")
                    }
                    DecisionButtons(
                        enabled = !isWorking,
                        decisions = decisions,
                        onDecision = { onDecision(row, it) }
                    )
                    if (index != rows.lastIndex) HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ReconcileReturnQuantityDialog(
    row: EventReconciliationReturnLineDto,
    initialDecision: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String?, String?) -> Unit
) {
    var selectedDecision by rememberSaveable(row.custodyId, initialDecision) { mutableStateOf(initialDecision) }
    var quantityText by rememberSaveable(row.custodyId, initialDecision) { mutableStateOf(row.remainingQuantity.toString()) }
    var returnToMode by rememberSaveable(row.custodyId, initialDecision) {
        mutableStateOf(if (initialDecision in listOf("RETURNED", "DAMAGED")) "ORIGINAL_SOURCE" else null)
    }
    var note by rememberSaveable(row.custodyId, initialDecision) { mutableStateOf("") }
    val quantity = quantityText.toIntOrNull()
    val displayQuantity = quantity ?: 0
    val destinationRequired = selectedDecision in listOf("RETURNED", "DAMAGED")
    val decisions = listOf(
        "RETURNED" to "Grįžo",
        "DAMAGED" to "Sugadinta",
        "MISSING" to "Dingo",
        "CONSUMED" to "Sunaudota"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(row.itemName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Likutis suvesti: ${row.remainingQuantity} vnt.", style = MaterialTheme.typography.bodyMedium)
                decisions.chunked(2).forEach { rowDecisions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowDecisions.forEach { (decision, label) ->
                            FilterChip(
                                selected = selectedDecision == decision,
                                onClick = {
                                    selectedDecision = decision
                                    if (decision in listOf("RETURNED", "DAMAGED") && returnToMode == null) {
                                        returnToMode = "ORIGINAL_SOURCE"
                                    }
                                    if (decision !in listOf("RETURNED", "DAMAGED")) {
                                        returnToMode = null
                                    }
                                },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (decision) {
                                        "RETURNED" -> MaterialTheme.colorScheme.primaryContainer
                                        "DAMAGED" -> MaterialTheme.colorScheme.secondaryContainer
                                        "MISSING" -> MaterialTheme.colorScheme.errorContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Kiekis",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { quantityText = (displayQuantity - 1).coerceAtLeast(1).toString() },
                            enabled = displayQuantity > 1,
                            modifier = Modifier.heightIn(min = 56.dp)
                        ) {
                            Text("-")
                        }
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { quantityText = it.filter(Char::isDigit) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { quantityText = (displayQuantity + 1).coerceAtMost(row.remainingQuantity).toString() },
                            enabled = displayQuantity < row.remainingQuantity,
                            modifier = Modifier.heightIn(min = 56.dp)
                        ) {
                            Text("+")
                        }
                    }
                }

                if (destinationRequired) {
                    ReturnDestinationSelector(
                        value = returnToMode ?: "ORIGINAL_SOURCE",
                        onValueChange = { returnToMode = it }
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Pastaba / vieta") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = quantity != null && quantity in 1..row.remainingQuantity,
                onClick = {
                    onConfirm(
                        selectedDecision,
                        quantity ?: 0,
                        if (destinationRequired) returnToMode else null,
                        note.takeIf { it.isNotBlank() }
                    )
                }
            ) {
                Text("Suvesti")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Atšaukti") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReturnDestinationSelector(value: String, onValueChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        "ORIGINAL_SOURCE" to "I originalu saltini",
        "EVENT_STORAGE" to "Į renginio sandėlį",
        "OTHER_LOCATION" to "I kita vieta"
    )
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == value }?.second ?: value,
            onValueChange = {},
            readOnly = true,
            label = { Text("Kur grįžo") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (mode, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onValueChange(mode)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ReconcilePurchaseQuantityDialog(
    row: EventReconciliationPurchaseLineDto,
    decision: String,
    candidates: List<EventPurchaseReconciliationCandidateDto>,
    candidatesLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int, String?) -> Unit
) {
    var quantityText by remember(row.purchaseItemId, decision) { mutableStateOf(row.purchasedQuantity.toString()) }
    var selectedCandidateId by remember(row.purchaseItemId, candidates) {
        mutableStateOf(candidates.firstOrNull { it.recommended }?.itemId ?: candidates.firstOrNull()?.itemId ?: row.itemId)
    }
    var error by remember { mutableStateOf<String?>(null) }
    val decisionLabel = when (decision) {
        "INCREASE_EXISTING_ITEM" -> "Papildyti"
        "ADD_NEW_ITEM" -> "Naujas įrašas"
        "CONSUMED" -> "Sunaudota"
        "IGNORE" -> "Ignoruoti"
        else -> decision
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(decisionLabel) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(row.itemName, fontWeight = FontWeight.SemiBold)
                Text("Likęs kiekis: ${row.purchasedQuantity} vnt.", style = MaterialTheme.typography.bodySmall)
                if (decision == "INCREASE_EXISTING_ITEM") {
                    CandidateDropdown(
                        candidates = candidates,
                        selectedCandidateId = selectedCandidateId,
                        loading = candidatesLoading,
                        onSelected = {
                            selectedCandidateId = it
                            error = null
                        }
                    )
                }
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = {
                        quantityText = it.filter(Char::isDigit)
                        error = null
                    },
                    label = { Text("Kiekis") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val quantity = quantityText.toIntOrNull()
                when {
                    quantity == null || quantity < 1 -> error = "Kiekis turi būti bent 1"
                    quantity > row.purchasedQuantity -> error = "Negali viršyti likusio kiekio"
                    decision == "INCREASE_EXISTING_ITEM" && selectedCandidateId == null -> error = "Pasirinkite daikta"
                    else -> onConfirm(quantity, selectedCandidateId)
                }
            }) { Text("Patvirtinti") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Atšaukti") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CandidateDropdown(
    candidates: List<EventPurchaseReconciliationCandidateDto>,
    selectedCandidateId: String?,
    loading: Boolean,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = candidates.firstOrNull { it.itemId == selectedCandidateId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (!loading && candidates.isNotEmpty()) expanded = it }
    ) {
        OutlinedTextField(
            value = when {
                loading -> "Kraunama..."
                selected != null -> selected.name
                candidates.isEmpty() -> "Tinkamų daiktų nerasta"
                else -> "Pasirinkite daikta"
            },
            onValueChange = {},
            readOnly = true,
            enabled = !loading && candidates.isNotEmpty(),
            label = { Text("Papildomas daiktas") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            candidates.forEach { candidate ->
                DropdownMenuItem(
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = candidate.name + if (candidate.recommended) " · rekomenduojama" else "",
                                fontWeight = if (candidate.recommended) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Text(
                                text = listOfNotNull(
                                    "${candidate.quantity} vnt.",
                                    candidate.custodianName
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onSelected(candidate.itemId)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun DecisionButtons(
    enabled: Boolean,
    decisions: List<Pair<String, String>>,
    onDecision: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        decisions.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (decision, label) ->
                    OutlinedButton(
                        onClick = { onDecision(decision) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text(label, maxLines = 1)
                    }
                }
                if (row.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EventReconciliationReportCard(
    openReturns: List<EventReconciliationReturnLineDto>,
    returnedRows: List<EventReconciliationReturnLineDto>,
    unresolvedPurchases: List<EventReconciliationPurchaseLineDto>
) {
    val returnedQuantity = returnedRows
        .filter { it.returnDecision == "RETURNED" }
        .sumOf { it.reconciledQuantity }
    val damagedQuantity = returnedRows
        .filter { it.returnDecision == "DAMAGED" }
        .sumOf { it.reconciledQuantity }
    val missingQuantity = returnedRows
        .filter { it.returnDecision == "MISSING" }
        .sumOf { it.reconciledQuantity }
    val consumedQuantity = returnedRows
        .filter { it.returnDecision == "CONSUMED" }
        .sumOf { it.reconciledQuantity }
    val openQuantity = openReturns.sumOf { it.remainingQuantity }
    val incidentRows = returnedRows.filter {
        it.returnDecision in listOf("DAMAGED", "MISSING", "CONSUMED") ||
            it.auditLog.any { audit -> audit.actualLocationNote?.isNotBlank() == true || audit.notes?.isNotBlank() == true }
    }

    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Kas nutiko po renginio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "Greita ataskaita is grąžinimų, inventorizacijos ir pirkimų suvedimo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (returnedQuantity > 0) SkautaiStatusPill("Grizo: $returnedQuantity", SkautaiStatusTone.Success)
                if (damagedQuantity > 0) SkautaiStatusPill("Sugadinta: $damagedQuantity", SkautaiStatusTone.Info)
                if (missingQuantity > 0) SkautaiStatusPill("Dingo: $missingQuantity", SkautaiStatusTone.Danger)
                if (consumedQuantity > 0) SkautaiStatusPill("Sunaudota: $consumedQuantity", SkautaiStatusTone.Neutral)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (openQuantity > 0) SkautaiStatusPill("Dar negrizo: $openQuantity", SkautaiStatusTone.Warning)
                if (unresolvedPurchases.isNotEmpty()) SkautaiStatusPill("Pirkimai: ${unresolvedPurchases.size}", SkautaiStatusTone.Warning)
                if (openQuantity == 0 && unresolvedPurchases.isEmpty()) {
                    SkautaiStatusPill("Suvedimas uzbaigtas", SkautaiStatusTone.Success)
                }
            }
            if (incidentRows.isNotEmpty()) {
                HorizontalDivider()
                incidentRows.take(6).forEach { row ->
                    val latestAudit = row.auditLog.lastOrNull()
                    ReconciliationLine(
                        title = row.itemName,
                        subtitle = listOfNotNull(
                            row.pastovykleName ?: row.holderUserName,
                            row.returnDecision?.let(::returnDecisionLabel),
                            latestAudit?.actualLocationNote?.takeIf { it.isNotBlank() },
                            latestAudit?.notes?.takeIf { it.isNotBlank() },
                            row.notes?.takeIf { it.isNotBlank() }
                        ).joinToString(" · "),
                        trailing = "${row.reconciledQuantity} vnt."
                    )
                }
                if (incidentRows.size > 6) {
                    Text(
                        "Dar ${incidentRows.size - 6} įrašai žemiau suvestinės sąrašuose.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ReconciliationLine(title: String, subtitle: String, trailing: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SkautaiStatusPill(label = trailing, tone = SkautaiStatusTone.Warning)
    }
}

@Composable
private fun ReconciliationCheckSummaryCard(
    summary: lt.skautai.android.ui.common.ItemCheckSummary,
    openReturnCount: Int,
    hasOpenReturns: Boolean,
    actionLabel: String,
    onOpenReturns: () -> Unit,
    showUncheckedOnly: Boolean,
    isRefreshing: Boolean,
    onToggleUncheckedOnly: () -> Unit,
    onRefresh: () -> Unit
) {
    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Bendra inventorizacijos patikra", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${summary.total - summary.unchecked}/${summary.total} eil. jau suvestos, dar laukia: ${summary.unchecked}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (summary.found > 0) {
                    SkautaiStatusPill(label = "Grizo: ${summary.found}", tone = SkautaiStatusTone.Success)
                }
                if (summary.damaged > 0) {
                    SkautaiStatusPill(label = "Sugadinta: ${summary.damaged}", tone = SkautaiStatusTone.Info)
                }
                if (summary.missing > 0) {
                    SkautaiStatusPill(label = "Dingo: ${summary.missing}", tone = SkautaiStatusTone.Danger)
                }
                if (summary.consumed > 0) {
                    SkautaiStatusPill(label = "Sunaudota: ${summary.consumed}", tone = SkautaiStatusTone.Neutral)
                }
                if (summary.unchecked > 0 || openReturnCount > 0) {
                    SkautaiStatusPill(label = "Laukia: ${summary.unchecked}", tone = SkautaiStatusTone.Warning)
                }
            }
            if (hasOpenReturns) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenReturns,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(actionLabel)
                    }
                    OutlinedButton(
                        onClick = onRefresh,
                        enabled = !isRefreshing,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(if (isRefreshing) "Atnaujinama..." else "Atnaujinti")
                    }
                }
                FilterChip(
                    selected = showUncheckedOnly,
                    onClick = onToggleUncheckedOnly,
                    label = { Text("Tik nesuvesti") }
                )
                if (summary.unchecked > 0 || openReturnCount > 0) {
                    Text(
                        text = "Renginį galėsi užbaigti, kai grąžinimai ir pirkimai bus suvesti.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "Visi grąžinimai jau suvesti arba nėra ką inventorizuoti.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun EventReconciliationReturnLineDto.toItemCheckResult(): ItemCheckResult? = when (returnDecision) {
    "RETURNED" -> ItemCheckResult.FOUND
    "DAMAGED" -> ItemCheckResult.DAMAGED
    "MISSING" -> ItemCheckResult.MISSING
    "CONSUMED" -> ItemCheckResult.CONSUMED
    else -> null
}

private fun returnDecisionLabel(value: String): String = when (value) {
    "RETURNED" -> "Grįžo"
    "DAMAGED" -> "Grįžo sugadinta"
    "MISSING" -> "Negrįžo"
    "CONSUMED" -> "Sunaudota"
    else -> value
}

private fun returnConditionLabel(value: String): String = when (value) {
    "GOOD" -> "Būklė gera"
    "DAMAGED" -> "Būklė sugadinta"
    "MISSING" -> "Būklė nežinoma"
    "CONSUMED" -> "Sunaudota"
    else -> "Būklė: $value"
}
