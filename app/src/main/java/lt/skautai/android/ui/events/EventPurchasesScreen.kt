package lt.skautai.android.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
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
    val canInventory = "events.inventory.distribute" in permissions ||
        (state as? EventPurchasesUiState.Success)?.event?.eventRoles
            ?.any { it.role in setOf("VIRSININKAS", "KOMENDANTAS", "UKVEDYS") } == true

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
                        "DRAFT" to "Ruošiami",
                        "PURCHASED" to "Nupirkta",
                        "ADDED_TO_INVENTORY" to "Pridėta į inventorių",
                        "CANCELLED" to "Atšaukta"
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
                                    subtitle = "Pirkimai · ${state.purchases.size} įrašai",
                                    metrics = purchaseHeroMetrics(state.purchases)
                                )
                            }
                            item {
                                EventDetailSection(
                                    title = "Pirkimų suvestinė",
                                    subtitle = "Suma, statusai ir sąskaitų būsena vienoje vietoje"
                                ) {
                                    EventDetailMetricRow(metrics = purchaseDashboardMetrics(state.purchases))
                                    EventDetailSearchBar(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = "Ieškoti pirkimuose",
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
                            if (filtered.isEmpty()) {
                                item {
                                    EmptyStateText(
                                        if (state.purchases.isEmpty()) {
                                            "Pirkimų dar nėra. Pažymėk trūkstamus daiktus Ūkvedžio skiltyje ir sukurk pirkimą."
                                        } else {
                                            "Nerasta pirkimų pagal pasirinktą filtrą."
                                        }
                                    )
                                }
                            }
                            items(filtered, key = { it.id }) { purchase ->
                                PurchaseRowCard(
                                    purchase = purchase,
                                    expanded = purchase.id in expandedIds,
                                    canManage = canInventory && !readOnly,
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
                                    onAttachInvoice = { uri ->
                                        viewModel.attachInvoice(eventId, purchase.id, uri)
                                    },
                                    onDownloadInvoice = {
                                        viewModel.downloadInvoice(eventId, purchase.id)
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

private fun purchaseHeroMetrics(purchases: List<EventPurchaseDto>): List<Pair<String, String>> = listOf(
    "Ruošiami" to purchases.count { it.status == "DRAFT" }.toString(),
    "Nupirkta" to purchases.count { it.status == "PURCHASED" }.toString(),
    "Suvesta" to purchases.count { it.status == "ADDED_TO_INVENTORY" }.toString(),
    "Suma" to String.format("%.2f EUR", purchases.sumOf { it.totalAmount ?: 0.0 })
)

private fun purchaseDashboardMetrics(purchases: List<EventPurchaseDto>): List<Pair<String, String>> = listOf(
    "Visi" to purchases.size.toString(),
    "Suvedime" to purchases.count { it.status == "PURCHASED" }.toString(),
    "Inventoriuje" to purchases.count { it.status == "ADDED_TO_INVENTORY" }.toString(),
    "Su sąskaita" to purchases.count { it.invoiceFileUrl != null }.toString()
)
