package lt.skautai.android.ui.events

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lt.skautai.android.data.remote.EventPurchaseDto

@Composable
fun PurchasesCard(
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
        if (uri != null && purchaseId != null) {
            onAttachInvoice(purchaseId, uri)
        }
        invoiceTargetPurchaseId = null
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Pirkimai", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            BudgetProgress(spent = spent, budget = budget)
            if (purchases.isEmpty()) {
                EmptyStateText("Pirkimu dar nera. Pazymek trukstamus daiktus Ukvedzio skiltyje ir sukurk pirkima.")
            }
            purchases.forEach { purchase ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    EventInfoRow("Busena", purchaseStatusLabel(purchase.status))
                    purchase.totalAmount?.let { EventInfoRow("Suma", String.format("%.2f EUR", it)) }
                    purchase.invoiceFileUrl?.let { EventInfoRow("Saskaita", invoiceTypeLabel(it)) }
                    purchase.items.forEach { item ->
                        EventInfoRow(item.itemName, "${item.purchasedQuantity} vnt.")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                invoiceTargetPurchaseId = purchase.id
                                invoicePicker.launch(arrayOf("application/pdf", "image/jpeg", "image/png"))
                            },
                            enabled = canManage && !isWorking,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Prisegti")
                        }
                        OutlinedButton(
                            onClick = { onDownloadInvoice(purchase.id) },
                            enabled = purchase.invoiceFileUrl != null && !isWorking,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Parsisiusti")
                        }
                    }
                    if (purchase.status == "DRAFT") {
                        OutlinedButton(
                            onClick = { onCompletePurchase(purchase.id) },
                            enabled = canManage && !isWorking,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pazymeti nupirkta")
                        }
                    }
                    if (purchase.status == "PURCHASED") {
                        Button(
                            onClick = { onAddPurchaseToInventory(purchase.id) },
                            enabled = canManage && !isWorking,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Prideti i inventoriu")
                        }
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

private fun invoiceTypeLabel(url: String): String = when (url.substringAfterLast('.', "").lowercase()) {
    "pdf" -> "PDF saskaita"
    "jpg", "jpeg", "png" -> "Saskaitos nuotrauka"
    else -> "Saskaitos failas"
}

private fun purchaseStatusLabel(status: String): String = when (status) {
    "DRAFT" -> "Ruosiama"
    "PURCHASED" -> "Nupirkta"
    "ADDED_TO_INVENTORY" -> "Prideta i inventoriu"
    "CANCELLED" -> "Atsaukta"
    else -> status
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
