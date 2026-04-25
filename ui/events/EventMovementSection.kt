package lt.skautai.android.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lt.skautai.android.data.remote.EventInventoryCustodyDto
import lt.skautai.android.data.remote.EventInventoryItemDto
import lt.skautai.android.data.remote.EventInventoryMovementDto
import lt.skautai.android.data.remote.EventInventoryPlanDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.PastovykleDto

private enum class MovementDialogMode { Checkout, Assign, Request }

@Composable
fun EventMovementCard(
    inventoryPlan: EventInventoryPlanDto?,
    pastovykles: List<PastovykleDto>,
    members: List<MemberDto>,
    custody: List<EventInventoryCustodyDto>,
    movements: List<EventInventoryMovementDto>,
    canManage: Boolean,
    isWorking: Boolean,
    onCreatePastovykle: (String, String?, String) -> Unit,
    onCreateMovement: (String, String, String, String?, String?, String?, String) -> Unit
) {
    var dialogMode by remember { mutableStateOf<MovementDialogMode?>(null) }
    var showPastovykleDialog by remember { mutableStateOf(false) }

    dialogMode?.let { mode ->
        MovementDialog(
            mode = mode,
            items = inventoryPlan?.items.orEmpty(),
            pastovykles = pastovykles,
            members = members,
            canManage = canManage,
            isWorking = isWorking,
            onDismiss = { dialogMode = null },
            onSubmit = { itemId, quantity, pastovykleId, toUserId, notes ->
                val movementType = when (mode) {
                    MovementDialogMode.Assign -> "ASSIGN_TO_PASTOVYKLE"
                    MovementDialogMode.Request -> "PASTOVYKLE_REQUEST"
                    MovementDialogMode.Checkout -> "CHECKOUT_TO_PERSON"
                }
                onCreateMovement(movementType, itemId, quantity, pastovykleId, toUserId, null, notes)
                dialogMode = null
            }
        )
    }

    if (showPastovykleDialog) {
        PastovykleDialog(
            members = members,
            isWorking = isWorking,
            onDismiss = { showPastovykleDialog = false },
            onSubmit = { name, responsibleUserId, notes ->
                onCreatePastovykle(name, responsibleUserId, notes)
                showPastovykleDialog = false
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Gyvas inventorius", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "${custody.count { it.status == "OPEN" }} aktyvus judejimai / ${movements.size} istorijoje",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { dialogMode = MovementDialogMode.Checkout }, enabled = !isWorking, modifier = Modifier.weight(1f)) {
                    Text("Pasiimti")
                }
                Button(onClick = { dialogMode = MovementDialogMode.Request }, enabled = !isWorking, modifier = Modifier.weight(1f)) {
                    Text("Prasyti")
                }
            }
            if (canManage) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { dialogMode = MovementDialogMode.Assign }, enabled = !isWorking, modifier = Modifier.weight(1f)) {
                        Text("Priskirti")
                    }
                    Button(onClick = { showPastovykleDialog = true }, enabled = !isWorking, modifier = Modifier.weight(1f)) {
                        Text("Pastovykle")
                    }
                }
            }
            HorizontalDivider()

            CustodySection(
                title = "Pas zmones",
                custody = custody.filter { it.status == "OPEN" && it.holderUserId != null },
                empty = "Niekas dar nera pasieme daiktu.",
                isWorking = isWorking,
                onReturn = { row ->
                    onCreateMovement(
                        if (row.pastovykleId != null) "RETURN_TO_PASTOVYKLE" else "RETURN_TO_EVENT_STORAGE",
                        row.eventInventoryItemId,
                        row.remainingQuantity.toString(),
                        row.pastovykleId,
                        null,
                        row.id,
                        ""
                    )
                }
            )
            CustodySection(
                title = "Pastovykles",
                custody = custody.filter { it.status == "OPEN" && it.holderUserId == null && it.pastovykleId != null },
                empty = "Pastovyklems dar nepriskirta inventoriaus.",
                isWorking = isWorking,
                onReturn = { row ->
                    onCreateMovement("RETURN_TO_EVENT_STORAGE", row.eventInventoryItemId, row.remainingQuantity.toString(), null, null, row.id, "")
                }
            )
            EventListSection(title = "Istorija", subtitle = "${movements.size} irasu") {
                if (movements.isEmpty()) {
                    EmptyStateText("Judejimo istorija tuscia.")
                } else {
                    movements.take(12).forEach { movement ->
                        MovementHistoryRow(movement)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustodySection(
    title: String,
    custody: List<EventInventoryCustodyDto>,
    empty: String,
    isWorking: Boolean,
    onReturn: (EventInventoryCustodyDto) -> Unit
) {
    EventListSection(title = title, subtitle = "${custody.size} irasu") {
        if (custody.isEmpty()) {
            EmptyStateText(empty)
        } else {
            custody.forEach { row ->
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(row.itemName, fontWeight = FontWeight.SemiBold)
                    Text(
                        buildString {
                            row.pastovykleName?.let { append("$it / ") }
                            row.holderUserName?.let { append(it) }
                            if (isBlank()) append("Renginio inventorius")
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EventMetricPill("${row.remainingQuantity}/${row.quantity}")
                        TextButton(
                            onClick = { onReturn(row) },
                            enabled = !isWorking && row.remainingQuantity > 0,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Grazinti")
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun MovementHistoryRow(movement: EventInventoryMovementDto) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("${movementLabel(movement.movementType)}: ${movement.itemName}", fontWeight = FontWeight.SemiBold)
        Text(
            listOfNotNull(
                movement.toPastovykleName,
                movement.toUserName,
                "kiekis ${movement.quantity}",
                movement.performedByUserName
            ).joinToString(" / "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MovementDialog(
    mode: MovementDialogMode,
    items: List<EventInventoryItemDto>,
    pastovykles: List<PastovykleDto>,
    members: List<MemberDto>,
    canManage: Boolean,
    isWorking: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String?, String?, String) -> Unit
) {
    var itemId by remember { mutableStateOf(items.firstOrNull()?.id.orEmpty()) }
    var quantity by remember { mutableStateOf("1") }
    var pastovykleId by remember { mutableStateOf<String?>(pastovykles.firstOrNull()?.id) }
    var toUserId by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(when (mode) {
            MovementDialogMode.Assign -> "Priskirti pastovyklei"
            MovementDialogMode.Request -> "Prasyti daiktu"
            MovementDialogMode.Checkout -> "Pasiimti daikta"
        }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField(
                    label = "Daiktas",
                    value = items.firstOrNull { it.id == itemId }?.name ?: "Pasirinkti",
                    options = items.map { it.id to it.name },
                    onSelect = { itemId = it }
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filter(Char::isDigit) },
                    label = { Text("Kiekis") },
                    singleLine = true
                )
                if (mode != MovementDialogMode.Checkout || pastovykles.isNotEmpty()) {
                    DropdownField(
                        label = "Pastovykle",
                        value = pastovykles.firstOrNull { it.id == pastovykleId }?.name ?: "Renginio sandelis",
                        options = listOf("" to "Renginio sandelis") + pastovykles.map { it.id to it.name },
                        onSelect = { pastovykleId = it.ifBlank { null } }
                    )
                }
                if (mode == MovementDialogMode.Checkout && canManage) {
                    DropdownField(
                        label = "Kam",
                        value = members.firstOrNull { it.userId == toUserId }?.fullName() ?: "Sau",
                        options = listOf("" to "Sau") + members.map { it.userId to it.fullName() },
                        onSelect = { toUserId = it.ifBlank { null } }
                    )
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Pastabos") })
            }
        },
        confirmButton = {
            TextButton(enabled = !isWorking && itemId.isNotBlank(), onClick = { onSubmit(itemId, quantity, pastovykleId, toUserId, notes) }) {
                Text("Issaugoti")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Atsaukti") } }
    )
}

@Composable
private fun PastovykleDialog(
    members: List<MemberDto>,
    isWorking: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String?, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var responsibleUserId by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nauja pastovykle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Pavadinimas") })
                DropdownField(
                    label = "Vadovas",
                    value = members.firstOrNull { it.userId == responsibleUserId }?.fullName() ?: "Nepasirinkta",
                    options = members.map { it.userId to it.fullName() },
                    onSelect = { responsibleUserId = it }
                )
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Pastabos") })
            }
        },
        confirmButton = {
            TextButton(enabled = !isWorking, onClick = { onSubmit(name, responsibleUserId, notes) }) {
                Text("Sukurti")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Atsaukti") } }
    )
}

private fun movementLabel(type: String): String = when (type) {
    "PASTOVYKLE_REQUEST" -> "Prasymas"
    "ASSIGN_TO_PASTOVYKLE" -> "Priskirta"
    "CHECKOUT_TO_PERSON" -> "Pasiimta"
    "RETURN_TO_PASTOVYKLE" -> "Grazinta pastovyklei"
    "RETURN_TO_EVENT_STORAGE" -> "Grazinta i sandeli"
    "TRANSFER" -> "Perduota"
    else -> type
}
