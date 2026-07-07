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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.EventExtraCostDto
import lt.skautai.android.data.remote.EventFinanceDto
import lt.skautai.android.data.remote.EventPurchaseDto
import lt.skautai.android.ui.common.SkautaiChip
import lt.skautai.android.ui.common.SkautaiErrorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventPurchasesScreen(
    eventId: String,
    onBack: () -> Unit,
    onOpenReconciliation: (String) -> Unit,
    viewModel: EventPurchasesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(eventId) { viewModel.load(eventId) }

    LaunchedEffect((uiState as? EventPurchasesUiState.Success)?.error) {
        (uiState as? EventPurchasesUiState.Success)?.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val state = uiState
    val canInventory = "events.inventory.distribute:ALL" in permissions ||
        (state as? EventPurchasesUiState.Success)?.event?.eventRoles
            ?.any { it.userId == state.currentUserId && it.role in setOf("VIRSININKAS", "KOMENDANTAS", "UKVEDYS") } == true
    val canFinance = canInventory ||
        (state as? EventPurchasesUiState.Success)?.event?.eventRoles
            ?.any { it.userId == state.currentUserId && it.role == "FINANSININKAS" } == true

    EventScreenScaffold(
        title = "Pirkimai",
        onBack = onBack,
        snackbarHostState = snackbarHostState
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state) {
                is EventPurchasesUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is EventPurchasesUiState.Error -> SkautaiErrorState(
                    message = state.message,
                    onRetry = { viewModel.load(eventId) },
                    modifier = Modifier.align(Alignment.Center)
                )
                is EventPurchasesUiState.Success -> {
                    val readOnly = isEventReadOnlyStatus(state.event.status)
                    var searchQuery by remember { mutableStateOf("") }
                    var statusFilter by remember { mutableStateOf<String?>(null) }
                    var expandedIds by rememberSaveable { mutableStateOf(emptySet<String>()) }
                    val statusOptions = listOf(
                        null to "Visi",
                        "DRAFT" to "RuoÅ¡iami",
                        "PURCHASED" to "Nupirkta",
                        "ADDED_TO_INVENTORY" to "PridÄ—ta Ä¯ inventoriÅ³",
                        "CANCELLED" to "AtÅ¡aukta"
                    )
                    val filtered = remember(state.purchases, searchQuery, statusFilter) {
                        state.purchases.filter { purchase ->
                            val matchesStatus = statusFilter == null || purchase.status == statusFilter
                            val matchesSearch = searchQuery.isBlank() ||
                                purchase.notes?.contains(searchQuery, ignoreCase = true) == true ||
                                purchaseStatusLabel(purchase.status).contains(searchQuery, ignoreCase = true) ||
                                purchase.items.any { it.itemName.contains(searchQuery, ignoreCase = true) }
                            matchesStatus && matchesSearch
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                EventDetailHero(
                                    event = state.event,
                                    subtitle = "Pirkimai Â· ${state.purchases.size} Ä¯raÅ¡ai",
                                    metrics = purchaseHeroMetrics(state.purchases)
                                )
                            }
                            item {
                                EventDetailSection(
                                    title = "PirkimÅ³ suvestinÄ—",
                                    subtitle = "Suma, statusai ir sÄ…skaitÅ³ bÅ«sena vienoje vietoje"
                                ) {
                                    EventDetailMetricRow(metrics = purchaseDashboardMetrics(state.purchases))
                                    EventDetailSearchBar(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = "IeÅ¡koti pirkimuose",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(statusOptions, key = { it.first ?: "all" }) { (value, label) ->
                                            SkautaiChip(
                                                label = label,
                                                selected = statusFilter == value,
                                                onClick = { statusFilter = value }
                                            )
                                        }
                                    }
                                }
                            }
                            state.finance?.let { finance ->
                                item {
                                    FinanceDashboardSection(
                                        finance = finance,
                                        canManage = canFinance && !readOnly,
                                        isWorking = state.isWorking,
                                        onUpdateBudget = { amount -> viewModel.updateBudget(eventId, amount) },
                                        onAddCost = { category, label, quantity, unit, unitPrice, totalAmount, notes ->
                                            viewModel.addExtraCost(eventId, category, label, quantity, unit, unitPrice, totalAmount, notes)
                                        },
                                        onDeleteCost = { costId -> viewModel.deleteExtraCost(eventId, costId) }
                                    )
                                }
                            }
                            if (filtered.isEmpty()) {
                                item {
                                    EmptyStateText(
                                        if (state.purchases.isEmpty()) {
                                            "PirkimÅ³ dar nÄ—ra. PaÅ¾ymÄ—k trÅ«kstamus daiktus ÅªkvedÅ¾io skiltyje ir sukurk pirkimÄ…."
                                        } else {
                                            "Nerasta pirkimÅ³ pagal pasirinktÄ… filtrÄ…."
                                        }
                                    )
                                }
                            }
                            items(filtered, key = { it.id }) { purchase ->
                                PurchaseRowCard(
                                    purchase = purchase,
                                    expanded = purchase.id in expandedIds,
                                    canManage = canFinance && !readOnly,
                                    canReconcile = canInventory && !readOnly,
                                    isWorking = state.isWorking,
                                    onToggle = {
                                        expandedIds = if (purchase.id in expandedIds) {
                                            expandedIds - purchase.id
                                        } else {
                                            expandedIds + purchase.id
                                        }
                                    },
                                    onCompletePurchase = { totalAmount ->
                                        viewModel.completePurchase(eventId, purchase.id, totalAmount)
                                    },
                                    onUpdateAmount = { totalAmount ->
                                        viewModel.updatePurchaseAmount(eventId, purchase.id, totalAmount)
                                    },
                                    onAttachInvoices = { uris ->
                                        viewModel.attachInvoices(eventId, purchase.id, uris)
                                    },
                                    onDownloadInvoice = { invoiceId, invoiceFileUrl ->
                                        viewModel.downloadInvoice(eventId, purchase.id, invoiceId, invoiceFileUrl)
                                    },
                                    onAddToInventory = {
                                        onOpenReconciliation(eventId)
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

@Composable
private fun FinanceDashboardSection(
    finance: EventFinanceDto,
    canManage: Boolean,
    isWorking: Boolean,
    onUpdateBudget: (Double?) -> Unit,
    onAddCost: (String, String, Double?, String?, Double?, Double?, String?) -> Unit,
    onDeleteCost: (String) -> Unit
) {
    var budgetInput by rememberSaveable(finance.summary.inventoryBudgetAmount) {
        mutableStateOf(finance.summary.inventoryBudgetAmount?.toString().orEmpty())
    }
    var category by rememberSaveable { mutableStateOf("FIREWOOD") }
    var label by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf("") }
    var unitPrice by rememberSaveable { mutableStateOf("") }
    var totalAmount by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    val categories = listOf("FIREWOOD" to "Malkos", "TOILETS" to "Tualetai", "OTHER" to "Kita")

    EventDetailSection(
        title = "Biudžetas ir papildomos išlaidos",
        subtitle = if (finance.summary.overBudget) "Biudžetas viršytas" else "Pirkimai, malkos, tualetai ir kitos išlaidos"
    ) {
        EventDetailMetricRow(
            metrics = listOf(
                "Biudžetas" to finance.summary.inventoryBudgetAmount.moneyOrDash(),
                "Išleista" to finance.summary.spentTotal.money(),
                "Likutis" to finance.summary.remainingAmount.moneyOrDash(),
                "Papildoma" to finance.summary.extraCostTotal.money()
            )
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = budgetInput,
                onValueChange = { budgetInput = it.moneyInput() },
                label = { Text("Biudžetas EUR") },
                singleLine = true,
                enabled = canManage && !isWorking,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { onUpdateBudget(budgetInput.toAmountOrNull()) },
                enabled = canManage && !isWorking
            ) {
                Text("Išsaugoti")
            }
        }

        if (canManage) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories, key = { it.first }) { (value, title) ->
                    SkautaiChip(label = title, selected = category == value, onClick = { category = value })
                }
            }
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Išlaidos pavadinimas") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.moneyInput() },
                    label = { Text("Kiekis") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Vnt.") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = unitPrice,
                    onValueChange = { unitPrice = it.moneyInput() },
                    label = { Text("Vnt. kaina") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = totalAmount,
                    onValueChange = { totalAmount = it.moneyInput() },
                    label = { Text("Suma") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Pastabos") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    onAddCost(
                        category,
                        label.trim(),
                        quantity.toAmountOrNull(),
                        unit.trim().ifBlank { null },
                        unitPrice.toAmountOrNull(),
                        totalAmount.toAmountOrNull(),
                        notes.trim().ifBlank { null }
                    )
                    label = ""
                    quantity = ""
                    unit = ""
                    unitPrice = ""
                    totalAmount = ""
                    notes = ""
                },
                enabled = !isWorking && label.isNotBlank() && (totalAmount.isNotBlank() || (quantity.isNotBlank() && unitPrice.isNotBlank())),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Pridėti išlaidas")
            }
        }

        if (finance.extraCosts.isEmpty()) {
            EmptyStateText("Papildomų išlaidų dar nėra.")
        } else {
            finance.extraCosts.forEach { cost ->
                ExtraCostRow(cost = cost, canManage = canManage && !isWorking, onDelete = { onDeleteCost(cost.id) })
            }
        }
    }
}

@Composable
private fun ExtraCostRow(
    cost: EventExtraCostDto,
    canManage: Boolean,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(cost.label, style = MaterialTheme.typography.bodyLarge)
            Text(
                listOfNotNull(extraCostCategoryLabel(cost.category), cost.quantity?.let { "${it.cleanNumber()} ${cost.unit.orEmpty()}".trim() })
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(cost.totalAmount.money(), style = MaterialTheme.typography.bodyMedium)
        if (canManage) {
            TextButton(onClick = onDelete) { Text("Šalinti") }
        }
    }
}

private fun purchaseHeroMetrics(purchases: List<EventPurchaseDto>): List<Pair<String, String>> = listOf(
    "RuoÅ¡iami" to purchases.count { it.status == "DRAFT" }.toString(),
    "Nupirkta" to purchases.count { it.status == "PURCHASED" }.toString(),
    "Suvesta" to purchases.count { it.status == "ADDED_TO_INVENTORY" }.toString(),
    "Suma" to String.format("%.2f EUR", purchases.sumOf { it.totalAmount ?: 0.0 })
)

private fun purchaseDashboardMetrics(purchases: List<EventPurchaseDto>): List<Pair<String, String>> = listOf(
    "Visi" to purchases.size.toString(),
    "Suvedime" to purchases.count { it.status == "PURCHASED" }.toString(),
    "Inventoriuje" to purchases.count { it.status == "ADDED_TO_INVENTORY" }.toString(),
    "Su sąskaita" to purchases.count { it.invoiceFileUrl != null || it.invoices.isNotEmpty() }.toString()
)

private fun String.moneyInput(): String = filter { it.isDigit() || it == '.' || it == ',' }

private fun String.toAmountOrNull(): Double? = replace(',', '.').toDoubleOrNull()

private fun Double.money(): String = String.format("%.2f EUR", this)

private fun Double?.moneyOrDash(): String = this?.money() ?: "-"

private fun Double.cleanNumber(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()

private fun extraCostCategoryLabel(category: String): String = when (category) {
    "FIREWOOD" -> "Malkos"
    "TOILETS" -> "Tualetai"
    else -> "Kita"
}
