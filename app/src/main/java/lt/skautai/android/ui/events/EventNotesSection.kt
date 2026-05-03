package lt.skautai.android.ui.events

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import lt.skautai.android.data.remote.EventPurchaseDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.SkautaiStatusTone

@Composable
fun PurchasesCard(
    eventNotes: String?,
    purchases: List<EventPurchaseDto>,
    canManage: Boolean,
    isWorking: Boolean,
    onCompletePurchase: (String, Double?) -> Unit,
    onAttachInvoice: (String, Uri) -> Unit,
    onDownloadInvoice: (String) -> Unit
) {
    var invoiceTargetPurchaseId by remember { mutableStateOf<String?>(null) }
    var completingPurchase by remember { mutableStateOf<EventPurchaseDto?>(null) }
    var totalAmountInput by remember { mutableStateOf("") }
    val spent = purchases.sumOf { it.totalAmount ?: 0.0 }
    val budget = parseBudget(eventNotes)
    val invoicePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val purchaseId = invoiceTargetPurchaseId
        if (uri != null && purchaseId != null) {
            onAttachInvoice(purchaseId, uri)
        }
        invoiceTargetPurchaseId = null
    }

    completingPurchase?.let { purchase ->
        AlertDialog(
            onDismissRequest = {
                completingPurchase = null
                totalAmountInput = ""
            },
            title = { Text("Pažymėti nupirkta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Jei žinote galutinę sumą, įrašykite ją dabar.")
                    OutlinedTextField(
                        value = totalAmountInput,
                        onValueChange = { value ->
                            totalAmountInput = value.filter { it.isDigit() || it == '.' || it == ',' }
                        },
                        label = { Text("Bendra suma (EUR)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = eventFormFieldColors()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isWorking,
                    onClick = {
                        val totalAmount = totalAmountInput.replace(',', '.').toDoubleOrNull()
                        onCompletePurchase(purchase.id, totalAmount)
                        completingPurchase = null
                        totalAmountInput = ""
                    }
                ) {
                    Text("Išsaugoti")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        completingPurchase = null
                        totalAmountInput = ""
                    }
                ) {
                    Text("Atšaukti")
                }
            }
        )
    }

    SkautaiCard(modifier = Modifier.fillMaxWidth(), tonal = MaterialTheme.colorScheme.surfaceBright) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Pirkimai", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            BudgetProgress(spent = spent, budget = budget)
            if (purchases.isEmpty()) {
                EmptyStateText("Pirkimų dar nėra. Pažymėk trūkstamus daiktus ūkvedžio skiltyje ir sukurk pirkimą.")
            }
            purchases.forEach { purchase ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    EventInfoRow("Būsena", purchaseStatusLabel(purchase.status))
                    purchase.totalAmount?.let { EventInfoRow("Suma", String.format("%.2f EUR", it)) }
                    purchase.invoiceFileUrl?.let { EventInfoRow("Sąskaita", invoiceTypeLabel(it)) }
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
                            onClick = {
                                completingPurchase = purchase
                                totalAmountInput = purchase.totalAmount?.toString().orEmpty()
                            },
                            enabled = canManage && !isWorking,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pažymėti nupirkta")
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
            "Išleista ${String.format("%.2f", spent)} EUR / Biudžetas ${String.format("%.2f", budget)} EUR"
        } else {
            "Išleista ${String.format("%.2f", spent)} EUR"
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

fun invoiceTypeLabel(url: String): String = when (url.substringAfterLast('.', "").lowercase()) {
    "pdf" -> "PDF sąskaita"
    "jpg", "jpeg", "png" -> "Sąskaitos nuotrauka"
    else -> "Sąskaitos failas"
}

fun purchaseStatusLabel(status: String): String = when (status) {
    "DRAFT" -> "Ruošiama"
    "PURCHASED" -> "Nupirkta"
    "ADDED_TO_INVENTORY" -> "Pridėta į inventorių"
    "CANCELLED" -> "Atšaukta"
    else -> status
}

fun purchaseStatusLabelPublic(status: String): String = purchaseStatusLabel(status)

@Composable
fun PurchaseStatusPill(status: String) {
    SkautaiStatusPill(label = purchaseStatusLabel(status), tone = purchaseStatusTone(status))
}

fun purchaseStatusTone(status: String): SkautaiStatusTone = when (status) {
    "DRAFT" -> SkautaiStatusTone.Info
    "PURCHASED" -> SkautaiStatusTone.Warning
    "ADDED_TO_INVENTORY" -> SkautaiStatusTone.Success
    "CANCELLED" -> SkautaiStatusTone.Danger
    else -> SkautaiStatusTone.Neutral
}

@Composable
private fun purchaseCardTone(status: String) = when (status) {
    "DRAFT" -> MaterialTheme.colorScheme.surfaceContainerLow
    "PURCHASED" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.32f)
    "ADDED_TO_INVENTORY" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f)
    "CANCELLED" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
    else -> MaterialTheme.colorScheme.surfaceContainerLow
}

@Composable
fun BudgetSummaryCard(eventNotes: String?, purchases: List<EventPurchaseDto>) {
    val spent = purchases.sumOf { it.totalAmount ?: 0.0 }
    val budget = parseBudget(eventNotes)
    SkautaiCard(modifier = Modifier.fillMaxWidth(), tonal = MaterialTheme.colorScheme.surfaceBright) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val title = if (budget != null && budget > 0.0) {
                "Išleista ${String.format("%.2f", spent)} EUR / Biudžetas ${String.format("%.2f", budget)} EUR"
            } else {
                "Išleista ${String.format("%.2f", spent)} EUR"
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
}

@Composable
fun PurchaseRowCard(
    purchase: EventPurchaseDto,
    expanded: Boolean,
    canManage: Boolean,
    isWorking: Boolean,
    onToggle: () -> Unit,
    onCompletePurchase: (Double?) -> Unit,
    onUpdateAmount: (Double?) -> Unit,
    onAttachInvoice: (Uri) -> Unit,
    onDownloadInvoice: () -> Unit,
    onAddToInventory: () -> Unit
) {
    var amountDialogMode by remember { mutableStateOf<PurchaseAmountDialogMode?>(null) }
    var totalAmountInput by remember { mutableStateOf("") }
    val invoicePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onAttachInvoice(uri)
    }

    amountDialogMode?.let { mode ->
        AlertDialog(
            onDismissRequest = {
                amountDialogMode = null
                totalAmountInput = ""
            },
            title = { Text(if (mode == PurchaseAmountDialogMode.Complete) "Pažymėti nupirkta" else "Redaguoti sumą") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Įrašykite bendrą sąskaitos ar faktūros sumą.")
                    OutlinedTextField(
                        value = totalAmountInput,
                        onValueChange = { totalAmountInput = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                        label = { Text("Bendra suma (EUR)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = eventFormFieldColors()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isWorking,
                    onClick = {
                        val totalAmount = totalAmountInput.replace(',', '.').toDoubleOrNull()
                        if (mode == PurchaseAmountDialogMode.Complete) onCompletePurchase(totalAmount) else onUpdateAmount(totalAmount)
                        amountDialogMode = null
                        totalAmountInput = ""
                    }
                ) { Text("Išsaugoti") }
            },
            dismissButton = {
                TextButton(onClick = {
                    amountDialogMode = null
                    totalAmountInput = ""
                }) { Text("Atšaukti") }
            }
        )
    }

    SkautaiCard(modifier = Modifier.fillMaxWidth(), tonal = purchaseCardTone(purchase.status)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PurchaseStatusPill(purchase.status)
                    Text(
                        purchase.items.joinToString(", ") { it.itemName },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EventMetricPill(purchase.totalAmount?.let { String.format("%.2f EUR", it) } ?: "Suma neįvesta")
                        EventMetricPill("${purchase.items.size} eil. / ${purchase.items.sumOf { it.purchasedQuantity }} vnt.")
                    }
                }
                TextButton(onClick = onToggle) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
            }
            if (expanded) {
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Sąskaita / faktūra", style = MaterialTheme.typography.labelLarge)
                        Text(
                            purchase.invoiceFileUrl?.let { invoiceTypeLabel(it) } ?: "Failas nepridėtas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SkautaiStatusPill(
                        label = if (purchase.invoiceFileUrl == null) "Trūksta" else "Prisegta",
                        tone = if (purchase.invoiceFileUrl == null) SkautaiStatusTone.Warning else SkautaiStatusTone.Success
                    )
                }
                purchase.items.forEach { item ->
                    EventInfoRow(item.itemName, "${item.purchasedQuantity} vnt.")
                }
                if (purchase.status == "PURCHASED" && purchase.items.any { !it.addedToInventory }) {
                    Text(
                        "Ne visi nupirkti daiktai dar perkelti į bendrą inventorių.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { invoicePicker.launch(arrayOf("application/pdf", "image/jpeg", "image/png")) },
                        enabled = canManage && !isWorking,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (purchase.invoiceFileUrl == null) "Prisegti sąskaitą" else "Pakeisti sąskaitą")
                    }
                    OutlinedButton(
                        onClick = onDownloadInvoice,
                        enabled = purchase.invoiceFileUrl != null && !isWorking,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Atsisiųsti")
                    }
                }
                OutlinedButton(
                    onClick = {
                        totalAmountInput = purchase.totalAmount?.toString().orEmpty()
                        amountDialogMode = PurchaseAmountDialogMode.Update
                    },
                    enabled = canManage && !isWorking && purchase.status in listOf("DRAFT", "PURCHASED"),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Redaguoti sumą") }
                if (purchase.status == "DRAFT") {
                    OutlinedButton(
                        onClick = {
                            totalAmountInput = purchase.totalAmount?.toString().orEmpty()
                            amountDialogMode = PurchaseAmountDialogMode.Complete
                        },
                        enabled = canManage && !isWorking,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Pažymėti nupirkta") }
                }
                if (purchase.status == "PURCHASED" && purchase.items.any { !it.addedToInventory }) {
                    Button(
                        onClick = onAddToInventory,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Atidaryti inventoriaus suvedimą")
                    }
                }
            }
        }
    }
}

private enum class PurchaseAmountDialogMode { Complete, Update }

private fun parseBudget(notes: String?): Double? {
    val text = notes ?: return null
    val regex = Regex("(biudžetas|biudzetas|budget)\\s*[:=]?\\s*(\\d+(?:[.,]\\d+)?)", RegexOption.IGNORE_CASE)
    return regex.find(text)
        ?.groupValues
        ?.getOrNull(2)
        ?.replace(',', '.')
        ?.toDoubleOrNull()
}
