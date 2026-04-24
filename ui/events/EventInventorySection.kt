package lt.skautai.android.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.EventInventoryBucketDto
import lt.skautai.android.data.remote.EventInventoryItemDto
import lt.skautai.android.data.remote.EventInventoryPlanDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.ui.common.inventoryCategoryLabel

@Composable
fun NeedsCard(
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
                    ) {
                        Text("Prideti poreiki")
                    }
                }
            }

            if (planItems.isEmpty()) {
                EmptyStateText("Dar nieko nepasirinkote. Pridekite daiktu is inventoriaus arba sukurkite nauja pirkiniu sarasa.")
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
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(checked = selected, onCheckedChange = onCheckedChange)
        Column(Modifier.weight(1f)) {
            Text(item.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                inventoryCategoryLabel(item.category),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${item.quantity}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            if (selected) {
                Text(
                    "pasirinkta $quantity",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
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
fun InventoryPickerSheet(
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
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
        Card(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.heightIn(max = 420.dp)
            ) {
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Text("$count daiktai", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess, contentDescription = null)
    }
}

@Composable
fun UkvedysCard(
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
            Text(
                if (returnMode) "Grazinimas" else "Ukvedzio suvestine",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
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
            ) {
                Text("Sukurti pirkima is pazymetu (${selected.size})")
            }
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
fun PlanCard(
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
                                                contentPadding = PaddingValues(0.dp)
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
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filter(Char::isDigit) },
                    label = { Text("Kiekis") }
                )
                DropdownField(
                    label = "Paskirtis",
                    value = buckets.firstOrNull { it.id == bucketId }?.name ?: "Pasirinkti",
                    options = buckets.map { it.id to it.name },
                    onSelect = { bucketId = it }
                )
                DropdownField(
                    label = "Atsakingas",
                    value = members.firstOrNull { it.userId == responsibleUserId }?.fullName() ?: "Nepasirinkta",
                    options = members.map { it.userId to it.fullName() },
                    onSelect = { responsibleUserId = it }
                )
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Pastabos") })
            }
        },
        confirmButton = {
            TextButton(enabled = !isWorking, onClick = { onSave(name, quantity, bucketId, responsibleUserId, notes) }) {
                Text("Issaugoti")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Atsaukti")
            }
        }
    )
}
