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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.EventReconciliationPurchaseLineDto
import lt.skautai.android.data.remote.EventReconciliationReturnLineDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.SkautaiStatusTone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventReconciliationScreen(
    eventId: String,
    onBack: () -> Unit,
    viewModel: EventReconciliationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            EventDetailHero(
                                event = current.event,
                                subtitle = "Atviri grąžinimai: ${reconciliation.openReturns.size} · pirkimai sprendimui: ${reconciliation.unresolvedPurchases.size}",
                                metrics = listOf(
                                    "Negrąžinta" to reconciliation.openReturns.sumOf { it.remainingQuantity }.toString(),
                                    "Pirkimai" to reconciliation.unresolvedPurchases.size.toString(),
                                    "Būsena" to if (reconciliation.canComplete) "Paruošta" else "Tikrinama"
                                )
                            )
                        }

                        item {
                            ReconciliationReturnSection(
                                title = "Negrąžinti daiktai",
                                rows = reconciliation.openReturns,
                                isWorking = current.isWorking || current.event.status == "COMPLETED",
                                onDecision = { row, decision ->
                                    viewModel.reconcileReturn(eventId, row.custodyId, decision, row.remainingQuantity)
                                }
                            )
                        }

                        item {
                            ReconciliationPurchaseSection(
                                title = "Pirkimai sprendimui",
                                rows = reconciliation.unresolvedPurchases,
                                isWorking = current.isWorking || current.event.status == "COMPLETED",
                                onDecision = { row, decision ->
                                    viewModel.reconcilePurchase(
                                        eventId = eventId,
                                        purchaseItemId = row.purchaseItemId,
                                        decision = decision,
                                        quantity = row.purchasedQuantity,
                                        existingItemId = if (decision == "INCREASE_EXISTING_ITEM") row.itemId else null
                                    )
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
}

@Composable
private fun ReconciliationReturnSection(
    title: String,
    rows: List<EventReconciliationReturnLineDto>,
    isWorking: Boolean,
    onDecision: (EventReconciliationReturnLineDto, String) -> Unit
) {
    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (rows.isEmpty()) {
                Text("Atvirų grąžinimų nėra.", style = MaterialTheme.typography.bodySmall)
            } else {
                rows.forEachIndexed { index, row ->
                    ReconciliationLine(
                        title = row.itemName,
                        subtitle = listOfNotNull(row.pastovykleName, row.holderUserName).joinToString(" · ").ifBlank { "Renginio sandėlis" },
                        trailing = "${row.remainingQuantity}/${row.quantity} vnt."
                    )
                    DecisionButtons(
                        enabled = !isWorking,
                        decisions = listOf(
                            "RETURNED" to "Grįžo",
                            "DAMAGED" to "Sugadinta",
                            "MISSING" to "Dingo",
                            "CONSUMED" to "Sunaudota"
                        ),
                        onDecision = { onDecision(row, it) }
                    )
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
private fun DecisionButtons(
    enabled: Boolean,
    decisions: List<Pair<String, String>>,
    onDecision: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        decisions.forEach { (decision, label) ->
            OutlinedButton(
                onClick = { onDecision(decision) },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            ) {
                Text(label)
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
