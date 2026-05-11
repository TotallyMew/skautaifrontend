package lt.skautai.android.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.EventInventoryAllocationDto
import lt.skautai.android.data.remote.EventInventoryBucketDto
import lt.skautai.android.data.remote.EventInventoryItemDto
import lt.skautai.android.data.remote.EventInventoryPlanDto
import lt.skautai.android.data.remote.EventInventoryRequestDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.PastovykleDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiConfirmDialog
import lt.skautai.android.ui.common.SkautaiSectionHeader
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.SkautaiStatusTone
import lt.skautai.android.ui.common.SkautaiTextField
import lt.skautai.android.ui.common.inventoryCategoryLabel

private enum class NeedEntryMode { Inventory, Manual }

@Composable
fun NeedsCard(
    inventoryPlan: EventInventoryPlanDto?,
    isWorking: Boolean,
    onOpenInventoryPicker: () -> Unit,
    onCreateManualNeeds: (List<ManualEventNeedInput>) -> Unit,
    onManualValidationError: (String) -> Unit = {}
) {
    var entryMode by remember { mutableStateOf(NeedEntryMode.Inventory) }
    var showHelp by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var selectedBucketId by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var manualNeeds by remember { mutableStateOf<List<ManualEventNeedInput>>(emptyList()) }
    val buckets = inventoryPlan?.buckets.orEmpty()
    val currentQuantity = quantity.toIntOrNull()
    val currentNeed = ManualEventNeedInput(
        name = name.trim(),
        quantity = currentQuantity ?: 0,
        bucketId = selectedBucketId,
        notes = notes.trim()
    )
    val canAddManualNeed = currentNeed.name.isNotBlank() && currentNeed.quantity > 0

    fun clearManualForm() {
        name = ""
        quantity = ""
        notes = ""
    }

    fun addCurrentManualNeed() {
        if (!canAddManualNeed) return
        manualNeeds = manualNeeds + currentNeed
        clearManualForm()
    }

    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EventFormEyebrow("Greitas veiksmas")
            Text("Pridėti poreikį", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EventFormSupportText("Pasirink, ar poreikis bus iš sandėlio, ar rankinis.")
                IconButton(onClick = { showHelp = !showHelp }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = if (showHelp) "Paslėpti pagalbą" else "Rodyti pagalbą"
                    )
                }
            }
            if (showHelp) {
                EventContextBanner(
                    title = "Kaip kurti poreikį",
                    subtitle = "Iš inventoriaus rinkis jau sandėlyje esantiems daiktams. Rankinis įvedimas tinka naujam pirkiniui, paslaugai arba daiktui, kurio kataloge dar nėra."
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    EventModeChip(
                        selected = entryMode == NeedEntryMode.Inventory,
                        text = "Iš inventoriaus",
                        onClick = { entryMode = NeedEntryMode.Inventory },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    EventModeChip(
                        selected = entryMode == NeedEntryMode.Manual,
                        text = "Ne iš inventoriaus",
                        onClick = { entryMode = NeedEntryMode.Manual },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            HorizontalDivider()
            when (entryMode) {
                NeedEntryMode.Inventory -> {
                    EventListSection(
                        title = "Pridėti iš inventoriaus",
                        subtitle = "Greitas pasirinkimas, kai daiktas jau yra sandelyje."
                    ) {
                        OutlinedButton(
                            onClick = onOpenInventoryPicker,
                            enabled = !isWorking,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Pridėti iš inventoriaus")
                        }
                    }
                }

                NeedEntryMode.Manual -> {
                    EventListSection(
                        title = "Poreikis ne iš inventoriaus",
                        subtitle = "Naujas daiktas arba paslauga, kurios dar nėra sandėlyje."
                    ) {
                        SkautaiTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Pavadinimas",
                            placeholder = "Pvz. dujų balionas, tentas, virvė",
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            SkautaiTextField(
                                value = quantity,
                                onValueChange = { quantity = it.filter(Char::isDigit) },
                                label = "Kiekis",
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
                        SkautaiTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = "Pastabos neprivaloma",
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (manualNeeds.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Paruošta pridėti (${manualNeeds.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                manualNeeds.forEachIndexed { index, need ->
                                    ManualNeedDraftRow(
                                        need = need,
                                        bucketName = buckets.firstOrNull { it.id == need.bucketId }?.name,
                                        onRemove = {
                                            manualNeeds = manualNeeds.filterIndexed { itemIndex, _ -> itemIndex != index }
                                        }
                                    )
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                if (canAddManualNeed) {
                                    addCurrentManualNeed()
                                } else {
                                    onManualValidationError("Įveskite pavadinimą ir teigiamą kiekį.")
                                }
                            },
                            enabled = !isWorking,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Įtraukti į sąrašą")
                        }
                        Button(
                            onClick = {
                                val needsToCreate = if (manualNeeds.isEmpty() && canAddManualNeed) {
                                    listOf(currentNeed)
                                } else {
                                    manualNeeds
                                }
                                onCreateManualNeeds(needsToCreate)
                                manualNeeds = emptyList()
                                clearManualForm()
                            },
                            enabled = !isWorking,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Pridėti poreikį")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualNeedDraftRow(
    need: ManualEventNeedInput,
    bucketName: String?,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(need.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull("${need.quantity} vnt.", bucketName).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (need.notes.isNotBlank()) {
                Text(
                    need.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, contentDescription = "Pašalinti")
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
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("-")
                }
                Text(quantity.toString(), modifier = Modifier.widthIn(min = 24.dp), fontWeight = FontWeight.SemiBold)
                OutlinedButton(
                    onClick = { onQuantityChange(quantity + 1) },
                    modifier = Modifier.size(48.dp),
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
    isWorking: Boolean,
    onCreateNeedsBulk: (Map<String, Int>, String?, String?, String) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var selectedBucketId by remember { mutableStateOf<String?>(null) }
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
        Text("Pasirinkti iš inventoriaus", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        SkautaiTextField(
            value = search,
            onValueChange = { search = it },
            label = "Paieška inventoriuje",
            leadingIcon = Icons.Default.Search,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        DropdownField(
            label = "Paskirtis",
            value = buckets.firstOrNull { it.id == selectedBucketId }?.name ?: "Pasirinkti",
            options = buckets.map { it.id to it.name },
            onSelect = { selectedBucketId = it }
        )
        SkautaiTextField(
            value = notes,
            onValueChange = { notes = it },
            label = "Pastabos visiems pažymėtiems",
            modifier = Modifier.fillMaxWidth()
        )
        SkautaiCard(modifier = Modifier.fillMaxWidth(), tonal = MaterialTheme.colorScheme.surfaceContainerLow) {
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
                onCreateNeedsBulk(selectedQuantities, selectedBucketId, null, notes)
                selectedQuantities = emptyMap()
                notes = ""
            },
            enabled = !isWorking && selectedQuantities.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Pridėti pasirinktus (${selectedQuantities.size})")
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
    pastovykles: List<PastovykleDto>,
    pastovykleRequestsById: Map<String, List<EventInventoryRequestDto>>,
    canManage: Boolean,
    isWorking: Boolean,
    onCreatePurchase: (Set<String>) -> Unit,
    onCreateBucket: (String, String, String?, String) -> Unit,
    onUpdateBucket: (String, String, String, String?, String) -> Unit,
    onDeleteBucket: (String) -> Unit,
    onCreateAllocation: (String, String, String, String) -> Unit,
    onUpdateAllocation: (String, String, String) -> Unit,
    onDeleteAllocation: (String) -> Unit,
    onApproveRequest: (String, String) -> Unit,
    onRejectRequest: (String, String) -> Unit,
    onFulfillRequest: (String, String) -> Unit
) {
    var selected by remember { mutableStateOf(setOf<String>()) }
    var returnStates by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var bucketName by remember { mutableStateOf("") }
    var bucketType by remember { mutableStateOf("OTHER") }
    var bucketPastovykleId by remember { mutableStateOf<String?>(null) }
    var allocationItemId by remember { mutableStateOf<String?>(null) }
    var allocationBucketId by remember { mutableStateOf<String?>(null) }
    var allocationQuantity by remember { mutableStateOf("") }
    var pendingBucketDeletion by remember { mutableStateOf<EventInventoryBucketDto?>(null) }
    var pendingAllocationDeletion by remember { mutableStateOf<EventInventoryAllocationDto?>(null) }
    var pendingRequestRejection by remember { mutableStateOf<EventInventoryRequestDto?>(null) }
    val shortageItems = inventoryPlan?.items.orEmpty().filter { it.shortageQuantity > 0 }
    val planItems = inventoryPlan?.items.orEmpty()
    val buckets = inventoryPlan?.buckets.orEmpty()
    val allocations = inventoryPlan?.allocations.orEmpty()
    val allRequests = pastovykleRequestsById.values.flatten().sortedByDescending { it.createdAt }
    val returnMode = eventStatus == "COMPLETED"

    pendingBucketDeletion?.let { bucket ->
        SkautaiConfirmDialog(
            title = "Trinti paskirtį?",
            message = "Paskirtis ${bucket.name} bus ištrinta.",
            confirmText = "Trinti",
            dismissText = "Atšaukti",
            isDanger = true,
            onConfirm = {
                pendingBucketDeletion = null
                onDeleteBucket(bucket.id)
            },
            onDismiss = { pendingBucketDeletion = null }
        )
    }

    pendingAllocationDeletion?.let { allocation ->
        SkautaiConfirmDialog(
            title = "Trinti paskirstymą?",
            message = "Paskirstymas ${allocation.bucketName} bus ištrintas.",
            confirmText = "Trinti",
            dismissText = "Atšaukti",
            isDanger = true,
            onConfirm = {
                pendingAllocationDeletion = null
                onDeleteAllocation(allocation.id)
            },
            onDismiss = { pendingAllocationDeletion = null }
        )
    }

    pendingRequestRejection?.let { request ->
        SkautaiConfirmDialog(
            title = "Atmesti prašymą?",
            message = "${request.pastovykleName}: ${request.itemName} x${request.quantity}",
            confirmText = "Atmesti",
            dismissText = "Atšaukti",
            isDanger = true,
            onConfirm = {
                pendingRequestRejection = null
                onRejectRequest(request.pastovykleId, request.id)
            },
            onDismiss = { pendingRequestRejection = null }
        )
    }

    SkautaiCard(modifier = Modifier.fillMaxWidth(), tonal = MaterialTheme.colorScheme.surfaceBright) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (returnMode) "Grąžinimas" else "Ūkvedžio suvestinė",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            when {
                returnMode && planItems.isEmpty() -> {
                    EmptyStateText("Nėra ką grąžinti. Poreikių planas tuščias.")
                }

                returnMode -> {
                    ReturnInventoryList(
                        items = planItems,
                        returnStates = returnStates,
                        onReturnStatesChange = { returnStates = it }
                    )
                }

                shortageItems.isEmpty() -> {
                    EmptyStateText("Trūkstamų daiktų nėra. Kai poreikiuose atsiras trūkumų, čia juos pažymėsi pirkimui.")
                }

                else -> {
                    ShortageInventoryList(
                        items = shortageItems,
                        selected = selected,
                        onSelectedChange = { selected = it }
                    )
                }
            }
            if (!returnMode) {
                HorizontalDivider()
                Text("Paskirtys", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Atskirk inventorių pagal paskirtį ar pastovykles.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                if (canManage) {
                    SkautaiTextField(
                        value = bucketName,
                        onValueChange = { bucketName = it },
                        label = "Nauja paskirtis",
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownField(
                        label = "Tipas",
                        value = bucketType,
                        options = listOf(
                            "PROGRAM" to "Programa",
                            "KITCHEN" to "Virtuvė",
                            "ADMIN" to "Administracija",
                            "MEDICAL" to "Medicina",
                            "PASTOVYKLE" to "Pastovyklė",
                            "OTHER" to "Kita"
                        ),
                        onSelect = { bucketType = it ?: "OTHER" }
                    )
                    if (bucketType == "PASTOVYKLE") {
                        DropdownField(
                            label = "Pastovyklė",
                            value = pastovykles.firstOrNull { it.id == bucketPastovykleId }?.name ?: "Pasirinkti",
                            options = pastovykles.map { it.id to it.name },
                            onSelect = { bucketPastovykleId = it }
                        )
                    }
                    EventPrimaryButton(
                        text = "Pridėti paskirtį",
                        onClick = {
                            onCreateBucket(bucketName, bucketType, bucketPastovykleId, "")
                            bucketName = ""
                        },
                        enabled = !isWorking
                    )
                }
                buckets.forEach { bucket ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(bucket.name, fontWeight = FontWeight.SemiBold)
                            Text(bucket.type, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (canManage) {
                            TextButton(onClick = { pendingBucketDeletion = bucket }) { Text("Trinti") }
                        }
                    }
                }

                HorizontalDivider()
                Text("Paskirstymai", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Priskirk inventoriaus kiekį paskirtims ir stebėk, kam jis atitenka.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                if (canManage) {
                    DropdownField(
                        label = "Daiktas",
                        value = planItems.firstOrNull { it.id == allocationItemId }?.name ?: "Pasirinkti",
                        options = planItems.map { it.id to it.name },
                        onSelect = { allocationItemId = it }
                    )
                    DropdownField(
                        label = "Paskirtis",
                        value = buckets.firstOrNull { it.id == allocationBucketId }?.name ?: "Pasirinkti",
                        options = buckets.map { it.id to it.name },
                        onSelect = { allocationBucketId = it }
                    )
                    SkautaiTextField(
                        value = allocationQuantity,
                        onValueChange = { allocationQuantity = it.filter(Char::isDigit) },
                        label = "Kiekis",
                        modifier = Modifier.fillMaxWidth()
                    )
                    EventPrimaryButton(
                        text = "Pridėti paskirstymą",
                        onClick = {
                            if (allocationItemId != null && allocationBucketId != null) {
                                onCreateAllocation(allocationItemId!!, allocationBucketId!!, allocationQuantity, "")
                            }
                        },
                        enabled = !isWorking
                    )
                }
                allocations.forEach { allocation ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(allocation.bucketName, fontWeight = FontWeight.SemiBold)
                            Text("${allocation.quantity} vnt.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (canManage) {
                            TextButton(onClick = { pendingAllocationDeletion = allocation }) { Text("Trinti") }
                        }
                    }
                }

                HorizontalDivider()
                Text("Pastovyklių prašymai", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Matosi, kas prašė, kiek prašė ir kokia prašymo būsena.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                if (allRequests.isEmpty()) {
                    EmptyStateText("Prašymų iš pastovyklės dar nėra.")
                } else {
                    allRequests.forEach { request ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${request.pastovykleName}: ${request.itemName} x${request.quantity}", fontWeight = FontWeight.SemiBold)
                            Text(request.status, color = MaterialTheme.colorScheme.primary)
                            if (canManage && request.status in listOf("PENDING", "APPROVED")) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { onApproveRequest(request.pastovykleId, request.id) }) { Text("Patvirtinti") }
                                    TextButton(onClick = { onFulfillRequest(request.pastovykleId, request.id) }) { Text("Įvykdyti") }
                                    TextButton(onClick = { pendingRequestRejection = request }) {
                                        Text("Atmesti", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Button(
                onClick = { onCreatePurchase(selected) },
                enabled = !returnMode && canManage && !isWorking && selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sukurti pirkimą iš pažymėtų (${selected.size})")
            }
        }
    }
}

private enum class UkvedysOverviewTab(val title: String) {
    Purchase("Pirkimai"),
    Buckets("Paskirtys"),
    Requests("Pastovyklių prašymai")
}

@Composable
private fun UkvedysOverviewTabs(
    selectedTab: UkvedysOverviewTab,
    onTabSelected: (UkvedysOverviewTab) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            UkvedysOverviewTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(tab) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ) {
                    Text(
                        text = tab.title,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        textAlign = TextAlign.Center,
                        style = if (selected) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
fun UkvedysTabsCard(
    eventStatus: String,
    inventoryPlan: EventInventoryPlanDto?,
    pastovykles: List<PastovykleDto>,
    pastovykleRequestsById: Map<String, List<EventInventoryRequestDto>>,
    activePurchaseItemIds: Set<String> = emptySet(),
    canManage: Boolean,
    isWorking: Boolean,
    onCreatePurchase: (Set<String>) -> Unit,
    onCreateBucket: (String, String, String?, String) -> Unit,
    onDeleteBucket: (String) -> Unit,
    onCreateAllocation: (String, String, String, String) -> Unit,
    onDeleteAllocation: (String) -> Unit,
    onApproveRequest: (String, String) -> Unit,
    onRejectRequest: (String, String) -> Unit,
    onFulfillRequest: (String, String) -> Unit
) {
    var selected by remember { mutableStateOf(setOf<String>()) }
    var returnStates by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var bucketName by remember { mutableStateOf("") }
    var bucketType by remember { mutableStateOf("OTHER") }
    var bucketPastovykleId by remember { mutableStateOf<String?>(null) }
    var allocationItemId by remember { mutableStateOf<String?>(null) }
    var allocationBucketId by remember { mutableStateOf<String?>(null) }
    var allocationQuantity by remember { mutableStateOf("") }
    var pendingBucketDeletion by remember { mutableStateOf<EventInventoryBucketDto?>(null) }
    var pendingAllocationDeletion by remember { mutableStateOf<EventInventoryAllocationDto?>(null) }
    var selectedTab by remember { mutableStateOf(UkvedysOverviewTab.Purchase) }
    var purchaseSearch by remember { mutableStateOf("") }
    var bucketSearch by remember { mutableStateOf("") }
    var allocationSearch by remember { mutableStateOf("") }
    var requestSearch by remember { mutableStateOf("") }
    val shortageItems = inventoryPlan?.items.orEmpty().filter { it.shortageQuantity > 0 }
    val planItems = inventoryPlan?.items.orEmpty()
    val buckets = inventoryPlan?.buckets.orEmpty()
    val allocations = inventoryPlan?.allocations.orEmpty()
    val allRequests = pastovykleRequestsById.values.flatten().sortedByDescending { it.createdAt }
    val returnMode = eventStatus == "COMPLETED"
    val filteredShortageItems = remember(shortageItems, purchaseSearch) {
        shortageItems.filterPlanItems(purchaseSearch)
    }
    val filteredBuckets = remember(buckets, bucketSearch) {
        val query = bucketSearch.trim()
        buckets
            .sortedWith(compareBy<EventInventoryBucketDto>({ it.type }, { it.name.lowercase() }))
            .filter {
                query.isBlank() ||
                    it.name.contains(query, ignoreCase = true) ||
                    it.type.contains(query, ignoreCase = true) ||
                    it.pastovykleName.orEmpty().contains(query, ignoreCase = true)
            }
    }
    val filteredAllocations = remember(allocations, allocationSearch) {
        val query = allocationSearch.trim()
        allocations
            .sortedWith(compareBy<EventInventoryAllocationDto>({ it.bucketName.lowercase() }, { it.quantity }))
            .filter {
                query.isBlank() ||
                    it.bucketName.contains(query, ignoreCase = true) ||
                    it.notes.orEmpty().contains(query, ignoreCase = true)
            }
    }
    val filteredRequests = remember(allRequests, requestSearch) {
        val query = requestSearch.trim()
        allRequests.filter {
            query.isBlank() ||
                it.itemName.contains(query, ignoreCase = true) ||
                it.pastovykleName.contains(query, ignoreCase = true) ||
                it.requestedByName.orEmpty().contains(query, ignoreCase = true) ||
                requestStatusLabel(it.status).contains(query, ignoreCase = true)
        }
    }

    pendingBucketDeletion?.let { bucket ->
        SkautaiConfirmDialog(
            title = "Trinti paskirtį?",
            message = "Paskirtis „${bucket.name}“ bus ištrinta.",
            confirmText = "Trinti",
            dismissText = "Atšaukti",
            isDanger = true,
            onConfirm = {
                pendingBucketDeletion = null
                onDeleteBucket(bucket.id)
            },
            onDismiss = { pendingBucketDeletion = null }
        )
    }

    pendingAllocationDeletion?.let { allocation ->
        SkautaiConfirmDialog(
            title = "Trinti paskirstymą?",
            message = "Paskirstymas „${allocation.bucketName}“ bus ištrintas.",
            confirmText = "Trinti",
            dismissText = "Atšaukti",
            isDanger = true,
            onConfirm = {
                pendingAllocationDeletion = null
                onDeleteAllocation(allocation.id)
            },
            onDismiss = { pendingAllocationDeletion = null }
        )
    }

    SkautaiCard(
        modifier = Modifier.fillMaxWidth(),
        tonal = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SkautaiSectionHeader(
                title = if (returnMode) "Grąžinimas" else "Ūkvedžio darbas",
                subtitle = "${planItems.size} plano eil. · ${shortageItems.size} trūksta · ${allRequests.size} praš."
            )
            EventDetailMetricRow(
                metrics = listOf(
                    "Plano eil." to planItems.size.toString(),
                    "Trūksta" to shortageItems.sumOf { it.shortageQuantity }.toString(),
                    "Paskirstyta" to allocations.sumOf { it.quantity }.toString()
                )
            )

            if (returnMode) {
                if (planItems.isEmpty()) {
                    EmptyStateText("Nėra ką grąžinti. Poreikių planas tuščias.")
                } else {
                    ReturnInventoryList(
                        items = planItems,
                        returnStates = returnStates,
                        onReturnStatesChange = { returnStates = it }
                    )
                }
            } else {
                UkvedysOverviewTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )

                when (selectedTab) {
                    UkvedysOverviewTab.Purchase -> {
                        EventDetailSection(
                            title = "Ką reikia nupirkti",
                            subtitle = "${filteredShortageItems.size} iš ${shortageItems.size} trūkumo eilučių"
                        ) {
                            EventDetailSearchBar(
                                value = purchaseSearch,
                                onValueChange = { purchaseSearch = it },
                                placeholder = "Ieškoti pagal daiktą, paskirtį ar atsakingą"
                            )
                            if (shortageItems.isEmpty()) {
                                EmptyStateText("Trūkstamų daiktų nėra. Kai plane atsiras trūkumų, čia juos pažymėsi pirkimui.")
                            } else if (filteredShortageItems.isEmpty()) {
                                EmptyStateText("Pagal šią paiešką trūkumų nerasta.")
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { selected = selected + filteredShortageItems.filter { it.id !in activePurchaseItemIds }.map { it.id } },
                                        enabled = canManage && !isWorking
                                    ) {
                                        Text("Pažymėti matomus")
                                    }
                                    TextButton(
                                        onClick = { selected = selected - filteredShortageItems.map { it.id }.toSet() },
                                        enabled = selected.isNotEmpty()
                                    ) {
                                        Text("Nuimti")
                                    }
                                }
                                ShortageInventoryList(
                                    items = filteredShortageItems,
                                    selected = selected,
                                    disabledItemIds = activePurchaseItemIds,
                                    onSelectedChange = { selected = it }
                                )
                            }
                        }
                        Button(
                            onClick = { onCreatePurchase(selected) },
                            enabled = canManage && !isWorking && selected.any { it !in activePurchaseItemIds },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sukurti pirkimą iš pažymėtų (${selected.size})")
                        }
                    }

                    UkvedysOverviewTab.Buckets -> {
                        EventDetailSection(
                            title = "Paskirtys",
                            subtitle = "Paskirtys leidžia inventorių atskirti pagal programą, virtuvę ar pastovyklę."
                        ) {
                            if (canManage) {
                                SkautaiTextField(
                                    value = bucketName,
                                    onValueChange = { bucketName = it },
                                    label = "Nauja paskirtis",
                                    modifier = Modifier.fillMaxWidth()
                                )
                                DropdownField(
                                    label = "Tipas",
                                    value = bucketTypeLabel(bucketType),
                                    options = listOf(
                                        "PROGRAM" to "Programa",
                                        "KITCHEN" to "Virtuvė",
                                        "ADMIN" to "Administracija",
                                        "MEDICAL" to "Medicina",
                                        "PASTOVYKLE" to "Pastovyklė",
                                        "OTHER" to "Kita"
                                    ),
                                    onSelect = { bucketType = it }
                                )
                                if (bucketType == "PASTOVYKLE") {
                                    DropdownField(
                                        label = "Pastovyklė",
                                        value = pastovykles.firstOrNull { it.id == bucketPastovykleId }?.name ?: "Pasirinkti",
                                        options = pastovykles.map { it.id to it.name },
                                        onSelect = { bucketPastovykleId = it }
                                    )
                                }
                                EventPrimaryButton(
                                    text = "Pridėti paskirtį",
                                    onClick = {
                                        onCreateBucket(bucketName, bucketType, bucketPastovykleId, "")
                                        bucketName = ""
                                        bucketPastovykleId = null
                                    },
                                    enabled = !isWorking
                                )
                            }

                            EventDetailSearchBar(
                                value = bucketSearch,
                                onValueChange = { bucketSearch = it },
                                placeholder = "Ieškoti paskirtyse"
                            )
                            if (buckets.isEmpty()) {
                                EmptyStateText("Paskirčių dar nėra.")
                            } else if (filteredBuckets.isEmpty()) {
                                EmptyStateText("Pagal šią paiešką paskirčių nerasta.")
                            } else {
                                BucketLazyList(
                                    buckets = filteredBuckets,
                                    canManage = canManage,
                                    onDeleteBucket = { pendingBucketDeletion = it }
                                )
                            }
                        }

                        EventDetailSection(
                            title = "Paskirstymai",
                            subtitle = "${filteredAllocations.size} iš ${allocations.size} paskirstymų"
                        ) {
                            if (canManage) {
                                DropdownField(
                                    label = "Daiktas",
                                    value = planItems.firstOrNull { it.id == allocationItemId }?.name ?: "Pasirinkti",
                                    options = planItems.map { it.id to it.name },
                                    onSelect = { allocationItemId = it }
                                )
                                DropdownField(
                                    label = "Paskirtis",
                                    value = buckets.firstOrNull { it.id == allocationBucketId }?.name ?: "Pasirinkti",
                                    options = buckets.map { it.id to it.name },
                                    onSelect = { allocationBucketId = it }
                                )
                                SkautaiTextField(
                                    value = allocationQuantity,
                                    onValueChange = { allocationQuantity = it.filter(Char::isDigit) },
                                    label = "Kiekis",
                                    modifier = Modifier.fillMaxWidth()
                                )
                                EventPrimaryButton(
                                    text = "Pridėti paskirstymą",
                                    onClick = {
                                        if (allocationItemId != null && allocationBucketId != null) {
                                            onCreateAllocation(allocationItemId!!, allocationBucketId!!, allocationQuantity, "")
                                        }
                                    },
                                    enabled = !isWorking
                                )
                            }

                            EventDetailSearchBar(
                                value = allocationSearch,
                                onValueChange = { allocationSearch = it },
                                placeholder = "Ieškoti paskirstymuose"
                            )
                            if (allocations.isEmpty()) {
                                EmptyStateText("Paskirstymų dar nėra.")
                            } else if (filteredAllocations.isEmpty()) {
                                EmptyStateText("Pagal šią paiešką paskirstymų nerasta.")
                            } else {
                                AllocationLazyList(
                                    allocations = filteredAllocations,
                                    canManage = canManage,
                                    onDeleteAllocation = { pendingAllocationDeletion = it }
                                )
                            }
                        }
                    }

                    UkvedysOverviewTab.Requests -> {
                        EventDetailSection(
                            title = "Pastovyklių prašymai",
                            subtitle = "${filteredRequests.size} iš ${allRequests.size} prašymų"
                        ) {
                            EventDetailSearchBar(
                                value = requestSearch,
                                onValueChange = { requestSearch = it },
                                placeholder = "Ieškoti pagal pastovyklę, daiktą ar statusą"
                            )
                            if (allRequests.isEmpty()) {
                                EmptyStateText("Prašymų iš pastovyklių dar nėra.")
                            } else if (filteredRequests.isEmpty()) {
                                EmptyStateText("Pagal šią paiešką prašymų nerasta.")
                            } else {
                                RequestsLazyList(
                                    requests = filteredRequests,
                                    canManage = canManage,
                                    onApproveRequest = onApproveRequest,
                                    onRejectRequest = onRejectRequest,
                                    onFulfillRequest = onFulfillRequest
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun List<EventInventoryItemDto>.filterPlanItems(query: String): List<EventInventoryItemDto> {
    val normalized = query.trim()
    return sortedWith(compareBy<EventInventoryItemDto>({ it.bucketName ?: "Be paskirties" }, { it.name.lowercase() }))
        .filter {
            normalized.isBlank() ||
                it.name.contains(normalized, ignoreCase = true) ||
                it.bucketName.orEmpty().contains(normalized, ignoreCase = true) ||
                it.responsibleUserName.orEmpty().contains(normalized, ignoreCase = true) ||
                it.notes.orEmpty().contains(normalized, ignoreCase = true)
        }
}

@Composable
private fun BucketLazyList(
    buckets: List<EventInventoryBucketDto>,
    canManage: Boolean,
    onDeleteBucket: (EventInventoryBucketDto) -> Unit
) {
    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
        items(buckets, key = { it.id }) { bucket ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(bucket.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(bucketTypeLabel(bucket.type), bucket.pastovykleName).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (canManage) {
                    TextButton(onClick = { onDeleteBucket(bucket) }) { Text("Trinti") }
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun AllocationLazyList(
    allocations: List<EventInventoryAllocationDto>,
    canManage: Boolean,
    onDeleteAllocation: (EventInventoryAllocationDto) -> Unit
) {
    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
        items(allocations, key = { it.id }) { allocation ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(allocation.bucketName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        allocation.notes ?: "Pastabų nėra",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                EventMetricPill("${allocation.quantity} vnt.", EventMetricTone.Neutral)
                if (canManage) {
                    TextButton(onClick = { onDeleteAllocation(allocation) }) { Text("Trinti") }
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun RequestsLazyList(
    requests: List<EventInventoryRequestDto>,
    canManage: Boolean,
    onApproveRequest: (String, String) -> Unit,
    onRejectRequest: (String, String) -> Unit,
    onFulfillRequest: (String, String) -> Unit
) {
    var pendingRequestRejection by remember { mutableStateOf<EventInventoryRequestDto?>(null) }

    pendingRequestRejection?.let { request ->
        SkautaiConfirmDialog(
            title = "Atmesti prašymą?",
            message = "${request.pastovykleName}: ${request.itemName} x${request.quantity}",
            confirmText = "Atmesti",
            dismissText = "Atšaukti",
            isDanger = true,
            onConfirm = {
                pendingRequestRejection = null
                onRejectRequest(request.pastovykleId, request.id)
            },
            onDismiss = { pendingRequestRejection = null }
        )
    }

    LazyColumn(modifier = Modifier.heightIn(max = 520.dp)) {
        items(requests, key = { it.id }) { request ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            "${request.itemName} × ${request.quantity}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            listOfNotNull(request.pastovykleName, request.requestedByName).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    RequestStatusPill(status = request.status)
                }
                if (!request.notes.isNullOrBlank()) {
                    Text(
                        request.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (canManage && request.status in listOf("PENDING", "APPROVED")) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (request.status == "PENDING") {
                            TextButton(onClick = { onApproveRequest(request.pastovykleId, request.id) }) { Text("Patvirtinti") }
                        }
                        TextButton(onClick = { onFulfillRequest(request.pastovykleId, request.id) }) { Text("Įvykdyti") }
                        TextButton(onClick = { pendingRequestRejection = request }) {
                            Text("Atmesti", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun RequestStatusPill(status: String) {
    val tone = when (status) {
        "PENDING" -> SkautaiStatusTone.Warning
        "APPROVED" -> SkautaiStatusTone.Info
        "FULFILLED" -> SkautaiStatusTone.Success
        "REJECTED" -> SkautaiStatusTone.Danger
        else -> SkautaiStatusTone.Neutral
    }
    SkautaiStatusPill(label = requestStatusLabel(status), tone = tone)
}

private fun requestStatusLabel(status: String): String = when (status) {
    "PENDING" -> "Laukia"
    "APPROVED" -> "Patvirtinta"
    "FULFILLED" -> "Įvykdyta"
    "REJECTED" -> "Atmesta"
    "SELF_PROVIDED" -> "Savo jėgomis"
    else -> status
}

private fun bucketTypeLabel(type: String): String = when (type) {
    "PROGRAM" -> "Programa"
    "KITCHEN" -> "Virtuvė"
    "ADMIN" -> "Administracija"
    "MEDICAL" -> "Medicina"
    "PASTOVYKLE" -> "Pastovyklė"
    "OTHER" -> "Kita"
    else -> type
}

@Composable
private fun ReturnInventoryList(
    items: List<EventInventoryItemDto>,
    returnStates: Map<String, String>,
    onReturnStatesChange: (Map<String, String>) -> Unit
) {
    EventListSection(
        title = "Grąžinimo sąrašas",
        subtitle = "${items.size} eil. / ${items.sumOf { it.plannedQuantity }} vnt."
    ) {
        val sortedItems = remember(items) {
            items.sortedWith(compareBy<EventInventoryItemDto>({ it.bucketName ?: "Be paskirties" }, { it.name.lowercase() }))
        }
        LazyColumn(modifier = Modifier.heightIn(max = 560.dp)) {
            var lastBucket: String? = null
            sortedItems.forEach { item ->
                val bucketName = item.bucketName ?: "Be paskirties"
                if (bucketName != lastBucket) {
                    val count = sortedItems.count { (it.bucketName ?: "Be paskirties") == bucketName }
                    item(key = "return_bucket_$bucketName") {
                        EventListGroupHeader(bucketName, count)
                    }
                    lastBucket = bucketName
                }
                item(key = item.id) {
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
}

@Composable
private fun ShortageInventoryList(
    items: List<EventInventoryItemDto>,
    selected: Set<String>,
    disabledItemIds: Set<String> = emptySet(),
    onSelectedChange: (Set<String>) -> Unit
) {
    val sortedItems = remember(items) {
        items.sortedWith(compareBy<EventInventoryItemDto>({ it.bucketName ?: "Be paskirties" }, { it.name.lowercase() }))
    }
    LazyColumn(modifier = Modifier.heightIn(max = 520.dp)) {
        var lastBucket: String? = null
        sortedItems.forEach { item ->
            val bucketName = item.bucketName ?: "Be paskirties"
            if (bucketName != lastBucket) {
                val count = sortedItems.count { (it.bucketName ?: "Be paskirties") == bucketName }
                item(key = "shortage_bucket_$bucketName") {
                    EventListGroupHeader(bucketName, count)
                }
                lastBucket = bucketName
            }
            item(key = item.id) {
                val disabled = item.id in disabledItemIds
                EventInventoryListRow(
                    item = item,
                    leading = {
                        Checkbox(
                            checked = item.id in selected,
                            enabled = !disabled,
                            onCheckedChange = { checked ->
                                onSelectedChange(if (checked) selected + item.id else selected - item.id)
                            }
                        )
                    },
                    trailing = {
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            EventMetricPill("${item.availableQuantity}/${item.plannedQuantity}", EventMetricTone.Neutral)
                            if (disabled) {
                                EventMetricPill("Jau pirkime", EventMetricTone.Good)
                            } else {
                                EventMetricPill("Pirkti ${item.shortageQuantity}", EventMetricTone.Warning)
                            }
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
    inventoryPlan: EventInventoryPlanDto?
) {
    SkautaiCard(modifier = Modifier.fillMaxWidth(), tonal = MaterialTheme.colorScheme.surfaceBright) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Inventoriaus planas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            event.inventorySummary?.let { summary ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventMetricPill("Planuota ${summary.totalPlannedQuantity}", EventMetricTone.Neutral)
                    EventMetricPill("Turima ${summary.totalAvailableQuantity}", EventMetricTone.Neutral)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventMetricPill("Trūksta ${summary.totalShortageQuantity}", EventMetricTone.Warning)
                    EventMetricPill("Paskirstyta ${summary.totalAllocatedQuantity}", EventMetricTone.Good)
                }
            }
            if (inventoryPlan == null || inventoryPlan.items.isEmpty()) {
                Text("Planas dar tuščias", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun EditNeedDialog(
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
                SkautaiTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Daiktas",
                    modifier = Modifier.fillMaxWidth()
                )
                SkautaiTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filter(Char::isDigit) },
                    label = "Kiekis",
                    modifier = Modifier.fillMaxWidth()
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
                SkautaiTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Pastabos",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(enabled = !isWorking, onClick = { onSave(name, quantity, bucketId, responsibleUserId, notes) }) {
                Text("Išsaugoti")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Atšaukti")
            }
        }
    )
}
