package lt.skautai.android.ui.events

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.InventoryTemplateDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiChip
import lt.skautai.android.ui.common.SkautaiConfirmDialog
import lt.skautai.android.ui.common.SkautaiEmptyState
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiPrimaryButton
import lt.skautai.android.ui.common.SkautaiSectionHeader
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.SkautaiStatusTone
import lt.skautai.android.ui.common.SkautaiTextField
import lt.skautai.android.ui.common.inventoryCategoryLabel

@Composable
fun InventoryTemplateScreen(
    onBack: () -> Unit,
    viewModel: InventoryTemplateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    uiState.editor?.let { editor ->
        InventoryTemplateEditorFullScreen(
            editor = editor,
            inventoryItems = uiState.inventoryItems,
            isSaving = uiState.isSaving,
            onDismiss = viewModel::closeEditor,
            onNameChange = viewModel::onNameChange,
            onEventTypeChange = viewModel::onEventTypeChange,
            onUpsertItem = viewModel::upsertItemRow,
            onRemoveItem = viewModel::removeItemRow,
            onSave = viewModel::saveTemplate
        )
        return
    }

    uiState.deleteTarget?.let { target ->
        SkautaiConfirmDialog(
            title = "Ištrinti šabloną",
            message = "\"${target.name}\" bus pašalintas iš šablonų sąrašo.",
            confirmText = "Ištrinti",
            dismissText = "Uždaryti",
            isDanger = true,
            onConfirm = viewModel::deleteTemplate,
            onDismiss = viewModel::dismissDelete
        )
    }

    EventScreenScaffold(
        title = "Inventoriaus šablonai",
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::startCreate) {
                Icon(Icons.Default.Add, contentDescription = "Naujas šablonas")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                InventoryTemplateFilterRow(
                    activeFilter = uiState.filterEventType,
                    onFilterSelected = viewModel::setFilter
                )
            }

            when {
                uiState.isLoading -> {
                    item { CircularProgressIndicator(modifier = Modifier.padding(24.dp)) }
                }

                uiState.error != null && uiState.templates.isEmpty() -> {
                    item {
                        SkautaiErrorState(
                            message = uiState.error.orEmpty(),
                            onRetry = viewModel::loadTemplates,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                uiState.templates.isEmpty() -> {
                    item {
                        SkautaiEmptyState(
                            title = "Šablonų dar nėra",
                            subtitle = "Susikurk šabloną iš daiktų sąrašo ir naudok jį kuriant renginį.",
                            icon = Icons.Default.Inventory2,
                            actionLabel = "Kurti šabloną",
                            onAction = viewModel::startCreate,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                else -> {
                    items(uiState.templates, key = { it.id }) { template ->
                        InventoryTemplateCard(
                            template = template,
                            onEdit = { viewModel.startEdit(template) },
                            onDelete = { viewModel.requestDelete(template) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryTemplateFilterRow(
    activeFilter: String?,
    onFilterSelected: (String?) -> Unit
) {
    val filters = listOf(
        null to "Visi",
        "STOVYKLA" to "Stovykla",
        "SUEIGA" to "Sueiga",
        "RENGINYS" to "Renginys"
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filters) { (type, label) ->
            SkautaiChip(
                label = label,
                selected = activeFilter == type,
                onClick = { onFilterSelected(type) }
            )
        }
    }
}

@Composable
private fun InventoryTemplateCard(
    template: InventoryTemplateDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    SkautaiCard(modifier = Modifier.fillMaxWidth(), tonal = MaterialTheme.colorScheme.surfaceBright) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = template.createdByUserName?.let { "Sukūrė $it" } ?: "Tunto šablonas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Redaguoti")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Ištrinti")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SkautaiStatusPill(label = eventTypeLabel(template.eventType ?: "RENGINYS"), tone = SkautaiStatusTone.Info)
                SkautaiStatusPill(label = "${template.items.size} eilutės", tone = SkautaiStatusTone.Neutral)
            }
        }
    }
}

@Composable
private fun InventoryTemplateEditorFullScreen(
    editor: InventoryTemplateEditorState,
    inventoryItems: List<ItemDto>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onEventTypeChange: (String) -> Unit,
    onUpsertItem: (Int?, InventoryTemplateEditorItem) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onSave: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val categories = remember(inventoryItems) {
        inventoryItems.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val filteredItems = remember(inventoryItems, query, selectedCategory) {
        inventoryItems.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.name.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true) ||
                item.type.contains(query, ignoreCase = true)
            val matchesCategory = selectedCategory == null || item.category == selectedCategory
            matchesQuery && matchesCategory
        }
    }
    val selectedRows = remember(editor.items) {
        editor.items
            .mapIndexed { index, item -> index to item }
            .filter { (_, item) -> item.itemName.isNotBlank() }
    }

    BackHandler(enabled = !isSaving, onBack = onDismiss)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = CenterVertically
        ) {
            IconButton(onClick = onDismiss, enabled = !isSaving) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Grįžti")
            }
            Text(
                text = if (editor.templateId == null) "Naujas šablonas" else "Redaguoti šabloną",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onSave, enabled = !isSaving) {
                Text(if (isSaving) "Saugoma..." else "Išsaugoti")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CompactTemplateTextField(
                        value = editor.name,
                        onValueChange = onNameChange,
                        label = "Pavadinimas",
                        placeholder = "Šablono pavadinimas",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    InventoryTemplateEventTypeDropdownCompact(
                        selected = editor.eventType,
                        onSelected = onEventTypeChange
                    )
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        label = { Text("Ieškoti daiktų") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (categories.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                SkautaiChip(
                                    label = "Visi",
                                    selected = selectedCategory == null,
                                    onClick = { selectedCategory = null }
                                )
                            }
                            items(categories, key = { it }) { category ->
                                SkautaiChip(
                                    label = templateCategoryLabel(category),
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category }
                                )
                            }
                        }
                    }
                    Text(
                        text = "${selectedRows.size} pasirinkta · ${inventoryItems.size} daiktai kataloge",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (selectedRows.isNotEmpty()) {
                item {
                    Text(
                        text = "Pasirinkti daiktai",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(selectedRows, key = { (index, _) -> "selected_template_item_$index" }) { (index, item) ->
                    InventoryTemplateItemSummaryCard(
                        index = index,
                        item = item,
                        canRemove = true,
                        onEdit = {},
                        onRemove = { onRemoveItem(index) }
                    )
                }
            }

            item {
                Text(
                    text = "Inventorius",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (filteredItems.isEmpty()) {
                item {
                    SkautaiEmptyState(
                        title = "Daiktų nerasta",
                        subtitle = "Pabandyk pakeisti paiešką arba kategoriją.",
                        icon = Icons.Default.Search
                    )
                }
            } else {
                items(filteredItems, key = { it.id }) { item ->
                    val existingIndex = editor.items.indexOfFirst { it.itemId == item.id }
                    val existing = editor.items.getOrNull(existingIndex)
                    val quantity = existing?.quantity?.toIntOrNull() ?: 0
                    InventoryTemplateCatalogItem(
                        item = item,
                        quantity = quantity,
                        onDecrease = {
                            if (existingIndex >= 0 && existing != null) {
                                if (quantity <= 1) {
                                    onRemoveItem(existingIndex)
                                } else {
                                    onUpsertItem(existingIndex, existing.copy(quantity = (quantity - 1).toString()))
                                }
                            }
                        },
                        onIncrease = {
                            if (existingIndex >= 0 && existing != null) {
                                onUpsertItem(existingIndex, existing.copy(quantity = (quantity + 1).toString()))
                            } else {
                                onUpsertItem(
                                    null,
                                    InventoryTemplateEditorItem(
                                        itemId = item.id,
                                        itemName = item.name,
                                        quantity = "1",
                                        category = item.category
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InventoryTemplateCatalogItem(
    item: ItemDto,
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOf(
                        templateCategoryLabel(item.category),
                        "${item.quantity} vnt. turima"
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TemplateQuantityStepper(
                quantity = quantity,
                onDecrease = onDecrease,
                onIncrease = onIncrease
            )
        }
    }
}

@Composable
private fun TemplateQuantityStepper(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (quantity > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = CenterVertically
        ) {
            IconButton(
                onClick = onDecrease,
                enabled = quantity > 0,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Sumažinti", modifier = Modifier.size(18.dp))
            }
            Text(
                text = quantity.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(2.dp))
            IconButton(onClick = onIncrease, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Pridėti", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryTemplateEditorSheetLegacy(
    editor: InventoryTemplateEditorState,
    inventoryItems: List<ItemDto>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onEventTypeChange: (String) -> Unit,
    onItemChange: (Int, InventoryTemplateEditorItem) -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onSave: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SkautaiSectionHeader(
                        title = if (editor.templateId == null) "Naujas šablonas" else "Redaguoti šabloną",
                        subtitle = "${editor.items.size} eilutės · ${inventoryItems.size} daiktai kataloge"
                    )
                    IconButton(onClick = onDismiss, enabled = !isSaving) {
                        Icon(Icons.Default.Close, contentDescription = "Uždaryti")
                    }
                }
            }
            item {
                SkautaiCard(modifier = Modifier.fillMaxWidth(), tonal = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompactTemplateTextField(
                            value = editor.name,
                            onValueChange = onNameChange,
                            label = "Pavadinimas",
                            placeholder = "Šablono pavadinimas",
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        InventoryTemplateEventTypeDropdownCompact(
                            selected = editor.eventType,
                            onSelected = onEventTypeChange
                        )
                    }
                }
            }
            items(editor.items.size, key = { index -> "template_sheet_item_$index" }) { index ->
                InventoryTemplateItemEditorModern(
                    index = index,
                    item = editor.items[index],
                    inventoryItems = inventoryItems,
                    canRemove = editor.items.size > 1,
                    onChange = { onItemChange(index, it) },
                    onRemove = { onRemoveItem(index) }
                )
            }
            item {
                OutlinedButton(onClick = onAddItem, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Pridėti eilutę")
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    ) {
                        Text("Uždaryti")
                    }
                    SkautaiPrimaryButton(
                        text = if (isSaving) "Saugoma..." else "Išsaugoti",
                        onClick = onSave,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryTemplateEditorSheet(
    editor: InventoryTemplateEditorState,
    inventoryItems: List<ItemDto>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onEventTypeChange: (String) -> Unit,
    onItemChange: (Int, InventoryTemplateEditorItem) -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onSave: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SkautaiSectionHeader(
                        title = if (editor.templateId == null) "Naujas šablonas" else "Redaguoti šabloną",
                        subtitle = "${editor.items.size} eilutės · ${inventoryItems.size} daiktai kataloge"
                    )
                    IconButton(onClick = onDismiss, enabled = !isSaving) {
                        Icon(Icons.Default.Close, contentDescription = "Uždaryti")
                    }
                }
            }
            item {
                SkautaiCard(modifier = Modifier.fillMaxWidth(), tonal = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SkautaiTextField(
                            value = editor.name,
                            onValueChange = onNameChange,
                            label = "Pavadinimas",
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        InventoryTemplateEventTypeDropdown(
                            selected = editor.eventType,
                            onSelected = onEventTypeChange
                        )
                    }
                }
            }
            items(editor.items.size, key = { index -> "template_sheet_item_$index" }) { index ->
                InventoryTemplateItemEditorModern(
                    index = index,
                    item = editor.items[index],
                    inventoryItems = inventoryItems,
                    canRemove = editor.items.size > 1,
                    onChange = { onItemChange(index, it) },
                    onRemove = { onRemoveItem(index) }
                )
            }
            item {
                OutlinedButton(onClick = onAddItem, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Pridėti eilutę")
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    ) {
                        Text("Uždaryti")
                    }
                    SkautaiPrimaryButton(
                        text = if (isSaving) "Saugoma..." else "Išsaugoti",
                        onClick = onSave,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryTemplateEditorSheetV2(
    editor: InventoryTemplateEditorState,
    inventoryItems: List<ItemDto>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onEventTypeChange: (String) -> Unit,
    onUpsertItem: (Int?, InventoryTemplateEditorItem) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onSave: () -> Unit
) {
    var editingIndex by remember(editor.templateId) { mutableStateOf<Int?>(null) }
    var editingItem by remember(editor.templateId) { mutableStateOf<InventoryTemplateEditorItem?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (editor.templateId == null) "Naujas šablonas" else "Redaguoti šabloną",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${editor.items.size} eilutės · ${inventoryItems.size} daiktai kataloge",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss, enabled = !isSaving) {
                    Icon(Icons.Default.Close, contentDescription = "Uždaryti")
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Šablono informacija",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CompactTemplateTextField(
                        value = editor.name,
                        onValueChange = onNameChange,
                        label = "Pavadinimas",
                        placeholder = "Šablono pavadinimas",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    InventoryTemplateEventTypeDropdownCompact(
                        selected = editor.eventType,
                        onSelected = onEventTypeChange
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "Šablono eilutės",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(editor.items.size, key = { index -> "template_sheet_v2_item_$index" }) { index ->
                    InventoryTemplateItemSummaryCard(
                        index = index,
                        item = editor.items[index],
                        canRemove = editor.items.size > 1,
                        onEdit = {
                            editingIndex = index
                            editingItem = editor.items[index]
                        },
                        onRemove = { onRemoveItem(index) }
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    editingIndex = null
                    editingItem = InventoryTemplateEditorItem()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Pridėti eilutę")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                ) {
                    Text("Uždaryti")
                }
                SkautaiPrimaryButton(
                    text = if (isSaving) "Saugoma..." else "Išsaugoti",
                    onClick = onSave,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    editingItem?.let { draft ->
        InventoryTemplateItemDialog(
            item = draft,
            inventoryItems = inventoryItems,
            onDismiss = {
                editingItem = null
                editingIndex = null
            },
            onSave = { saved ->
                onUpsertItem(editingIndex, saved)
                editingItem = null
                editingIndex = null
            }
        )
    }
}

@Composable
private fun InventoryTemplateEditorDialog(
    editor: InventoryTemplateEditorState,
    inventoryItems: List<ItemDto>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onEventTypeChange: (String) -> Unit,
    onItemChange: (Int, InventoryTemplateEditorItem) -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {},
        title = { Text(if (editor.templateId == null) "Naujas šablonas" else "Redaguoti šabloną") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SkautaiTextField(
                    value = editor.name,
                    onValueChange = onNameChange,
                    label = "Pavadinimas",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                InventoryTemplateEventTypeDropdown(
                    selected = editor.eventType,
                    onSelected = onEventTypeChange
                )
                editor.items.forEachIndexed { index, item ->
                    InventoryTemplateItemEditorModern(
                        index = index,
                        item = item,
                        inventoryItems = inventoryItems,
                        canRemove = editor.items.size > 1,
                        onChange = { onItemChange(index, it) },
                        onRemove = { onRemoveItem(index) }
                    )
                }
                OutlinedButton(onClick = onAddItem, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Pridėti eilutę")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    ) {
                        Text("Uždaryti")
                    }
                    SkautaiPrimaryButton(
                        text = if (isSaving) "Saugoma..." else "Išsaugoti",
                        onClick = onSave,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    )
}

@Composable
private fun InventoryTemplateItemEditor(
    index: Int,
    item: InventoryTemplateEditorItem,
    inventoryItems: List<ItemDto>,
    canRemove: Boolean,
    onChange: (InventoryTemplateEditorItem) -> Unit,
    onRemove: () -> Unit
) {
    SkautaiCard(modifier = Modifier.fillMaxWidth(), tonal = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${index + 1}. ${item.itemName.ifBlank { "Nauja eilutė" }}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (canRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, contentDescription = "Šalinti eilutę")
                    }
                }
            }
            InventoryTemplateItemPickerCompact(
                selectedItemId = item.itemId,
                items = inventoryItems,
                onSelected = { selected ->
                    onChange(
                        item.copy(
                            itemId = selected.id,
                            itemName = selected.name,
                            category = selected.category
                        )
                    )
                }
            )
            SkautaiTextField(
                value = item.itemName,
                onValueChange = { onChange(item.copy(itemId = null, itemName = it)) },
                label = "Daikto pavadinimas",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = item.quantity,
                    onValueChange = { value -> onChange(item.copy(quantity = value.filter { it.isDigit() }.take(4))) },
                    label = { Text("Kiekis") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = eventFormFieldColors(),
                    modifier = Modifier.weight(0.35f)
                )
                InventoryTemplateCategoryPicker(
                    selected = item.category,
                    onSelected = { onChange(item.copy(category = it)) },
                    modifier = Modifier.weight(0.65f)
                )
            }
            SkautaiTextField(
                value = item.notes,
                onValueChange = { onChange(item.copy(notes = it)) },
                label = "Pastabos",
                minLines = 1,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun InventoryTemplateItemEditorModern(
    index: Int,
    item: InventoryTemplateEditorItem,
    inventoryItems: List<ItemDto>,
    canRemove: Boolean,
    onChange: (InventoryTemplateEditorItem) -> Unit,
    onRemove: () -> Unit
) {
    SkautaiCard(modifier = Modifier.fillMaxWidth(), tonal = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${index + 1}. eilutė",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.itemName.ifBlank { "Nauja eilutė" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (canRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, contentDescription = "Šalinti eilutę")
                    }
                }
            }
            InventoryTemplateItemPicker(
                selectedItemId = item.itemId,
                items = inventoryItems,
                onSelected = { selected ->
                    onChange(
                        item.copy(
                            itemId = selected.id,
                            itemName = selected.name,
                            category = selected.category
                        )
                    )
                }
            )
            CompactTemplateTextField(
                value = item.itemName,
                onValueChange = { onChange(item.copy(itemId = null, itemName = it)) },
                label = "Daikto pavadinimas",
                placeholder = "Įrašyk daikto pavadinimą",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactTemplateTextField(
                    value = item.quantity,
                    onValueChange = { value -> onChange(item.copy(quantity = value.filter { it.isDigit() }.take(4))) },
                    label = "Kiekis",
                    placeholder = "0",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.3f)
                )
                InventoryTemplateCategoryPickerCompact(
                    selected = item.category,
                    onSelected = { onChange(item.copy(category = it)) },
                    modifier = Modifier.weight(0.7f)
                )
            }
            CompactTemplateTextField(
                value = item.notes,
                onValueChange = { onChange(item.copy(notes = it)) },
                label = "Pastabos",
                placeholder = "Papildoma informacija",
                minLines = 1,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun InventoryTemplateItemEditorCard(
    index: Int,
    item: InventoryTemplateEditorItem,
    inventoryItems: List<ItemDto>,
    canRemove: Boolean,
    onChange: (InventoryTemplateEditorItem) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${index + 1}. eilutė",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.itemName.ifBlank { "Nauja eilutė" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (canRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, contentDescription = "Šalinti eilutę")
                    }
                }
            }

            InventoryTemplateItemPickerCompact(
                selectedItemId = item.itemId,
                items = inventoryItems,
                onSelected = { selected ->
                    onChange(
                        item.copy(
                            itemId = selected.id,
                            itemName = selected.name,
                            category = selected.category
                        )
                    )
                }
            )

            CompactTemplateTextField(
                value = item.itemName,
                onValueChange = { onChange(item.copy(itemId = null, itemName = it)) },
                label = "Daikto pavadinimas",
                placeholder = "Įrašyk daikto pavadinimą",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CompactTemplateTextField(
                    value = item.quantity,
                    onValueChange = { value -> onChange(item.copy(quantity = value.filter { it.isDigit() }.take(4))) },
                    label = "Kiekis",
                    placeholder = "0",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.32f)
                )
                InventoryTemplateCategoryPickerCompact(
                    selected = item.category,
                    onSelected = { onChange(item.copy(category = it)) },
                    modifier = Modifier.weight(0.68f)
                )
            }

            CompactTemplateTextField(
                value = item.notes,
                onValueChange = { onChange(item.copy(notes = it)) },
                label = "Pastabos",
                placeholder = "Papildoma informacija",
                minLines = 1,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun InventoryTemplateItemSummaryCard(
    index: Int,
    item: InventoryTemplateEditorItem,
    canRemove: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${index + 1}. eilutė",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.itemName.ifBlank { "Nauja eilutė" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkautaiStatusPill(
                        label = "Kiekis ${item.quantity.ifBlank { "1" }}",
                        tone = SkautaiStatusTone.Neutral
                    )
                    SkautaiStatusPill(
                        label = item.category.takeIf { it.isNotBlank() }?.let(::templateCategoryLabel) ?: "Be kategorijos",
                        tone = SkautaiStatusTone.Info
                    )
                }
                item.notes.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Redaguoti")
                }
                if (canRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, contentDescription = "Šalinti eilutę")
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryTemplateItemDialog(
    item: InventoryTemplateEditorItem,
    inventoryItems: List<ItemDto>,
    onDismiss: () -> Unit,
    onSave: (InventoryTemplateEditorItem) -> Unit
) {
    var draft by remember(item) { mutableStateOf(item) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (item.itemName.isBlank()) "Pridėti eilutę" else "Redaguoti eilutę",
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InventoryTemplateItemPickerCompact(
                    selectedItemId = draft.itemId,
                    items = inventoryItems,
                    onSelected = { selected ->
                        draft = draft.copy(
                            itemId = selected.id,
                            itemName = selected.name,
                            category = selected.category
                        )
                    }
                )
                CompactTemplateTextField(
                    value = draft.itemName,
                    onValueChange = { draft = draft.copy(itemId = null, itemName = it) },
                    label = "Daikto pavadinimas",
                    placeholder = "Įrašyk daikto pavadinimą",
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CompactTemplateTextField(
                        value = draft.quantity,
                        onValueChange = { value -> draft = draft.copy(quantity = value.filter { it.isDigit() }.take(4)) },
                        label = "Kiekis",
                        placeholder = "0",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.32f)
                    )
                    InventoryTemplateCategoryPickerCompact(
                        selected = draft.category,
                        onSelected = { draft = draft.copy(category = it) },
                        modifier = Modifier.weight(0.68f)
                    )
                }
                CompactTemplateTextField(
                    value = draft.notes,
                    onValueChange = { draft = draft.copy(notes = it) },
                    label = "Pastabos",
                    placeholder = "Papildoma informacija",
                    minLines = 1,
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        draft.copy(
                            itemName = draft.itemName.trim(),
                            quantity = draft.quantity.ifBlank { "1" }
                        )
                    )
                },
                enabled = draft.itemName.trim().isNotBlank()
            ) {
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

@Composable
private fun CompactTemplateTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    readOnly: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = placeholder?.let { text -> { Text(text) } },
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            readOnly = readOnly,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            shape = MaterialTheme.shapes.medium,
            colors = compactTemplateFieldColors()
        )
    }
}

@Composable
private fun compactTemplateFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    cursorColor = MaterialTheme.colorScheme.primary
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryTemplateItemPickerCompact(
    selectedItemId: String?,
    items: List<ItemDto>,
    onSelected: (ItemDto) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember(selectedItemId) {
        mutableStateOf(items.firstOrNull { it.id == selectedItemId }?.name.orEmpty())
    }
    val filtered = remember(query, items) {
        val needle = query.trim().lowercase()
        items
            .filter { needle.isBlank() || it.name.lowercase().contains(needle) || it.category.lowercase().contains(needle) }
            .take(25)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        CompactTemplateTextField(
            value = query,
            onValueChange = {
                query = it
                expanded = true
            },
            label = "Tunto inventorius",
            placeholder = "Ieškok daikto",
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            filtered.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${option.name} · ${inventoryCategoryLabel(option.category)}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        query = option.name
                        onSelected(option)
                        expanded = false
                    }
                )
            }
            if (filtered.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Nerasta") },
                    onClick = { expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryTemplateCategoryPickerCompact(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val options = inventoryTemplateCategoryOptions()
    val isCustomMode = selected == "CUSTOM"
    val isKnown = selected.isBlank() || options.any { it.first == selected }
    var customCategory by remember(selected) {
        mutableStateOf(if (!isKnown && !isCustomMode) selected.toTemplateCategoryInput() else "")
    }
    val label = when {
        selected.isBlank() -> "Be kategorijos"
        isCustomMode -> "Kita kategorija"
        isKnown -> templateCategoryLabel(selected)
        else -> selected.toTemplateCategoryInput()
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            CompactTemplateTextField(
                value = label,
                onValueChange = {},
                label = "Kategorija",
                singleLine = true,
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Be kategorijos") },
                    onClick = {
                        onSelected("")
                        expanded = false
                    }
                )
                options.forEach { (value, optionLabel) ->
                    DropdownMenuItem(
                        text = { Text(optionLabel) },
                        onClick = {
                            onSelected(value)
                            expanded = false
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Kita kategorija") },
                    onClick = {
                        onSelected("CUSTOM")
                        expanded = false
                    }
                )
            }
        }

        if (!isKnown || selected == "CUSTOM") {
            CompactTemplateTextField(
                value = customCategory,
                onValueChange = {
                    customCategory = it
                    onSelected(it.take(100))
                },
                label = "Kita kategorija",
                placeholder = "Įrašyk kategoriją",
                singleLine = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryTemplateEventTypeDropdownCompact(
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("STOVYKLA", "SUEIGA", "RENGINYS")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        CompactTemplateTextField(
            value = eventTypeLabel(selected),
            onValueChange = {},
            label = "Renginio tipas",
            singleLine = true,
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(eventTypeLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryTemplateItemPicker(
    selectedItemId: String?,
    items: List<ItemDto>,
    onSelected: (ItemDto) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember(selectedItemId) {
        mutableStateOf(items.firstOrNull { it.id == selectedItemId }?.name.orEmpty())
    }
    val filtered = remember(query, items) {
        val needle = query.trim().lowercase()
        items
            .filter { needle.isBlank() || it.name.lowercase().contains(needle) || it.category.lowercase().contains(needle) }
            .take(25)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                expanded = true
            },
            label = { Text("Pasirinkti iš tunto inventoriaus") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = eventFormFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            filtered.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${option.name} · ${inventoryCategoryLabel(option.category)}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        query = option.name
                        onSelected(option)
                        expanded = false
                    }
                )
            }
            if (filtered.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Nerasta") },
                    onClick = { expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryTemplateCategoryPicker(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val options = inventoryTemplateCategoryOptions()
    val isCustomMode = selected == "CUSTOM"
    val isKnown = selected.isBlank() || options.any { it.first == selected }
    var customCategory by remember(selected) {
        mutableStateOf(if (!isKnown && !isCustomMode) selected.toTemplateCategoryInput() else "")
    }
    val label = when {
        selected.isBlank() -> "Be kategorijos"
        isCustomMode -> "Kita kategorija"
        isKnown -> templateCategoryLabel(selected)
        else -> selected.toTemplateCategoryInput()
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Kategorija") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                colors = eventFormFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Be kategorijos") },
                    onClick = {
                        onSelected("")
                        expanded = false
                    }
                )
                options.forEach { (value, optionLabel) ->
                    DropdownMenuItem(
                        text = { Text(optionLabel) },
                        onClick = {
                            onSelected(value)
                            expanded = false
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Kita kategorija") },
                    onClick = {
                        onSelected("CUSTOM")
                        expanded = false
                    }
                )
            }
        }

        if (!isKnown || selected == "CUSTOM") {
            SkautaiTextField(
                value = customCategory,
                onValueChange = {
                    customCategory = it
                    onSelected(it.take(100))
                },
                label = "Kita kategorija",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryTemplateEventTypeDropdown(
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("STOVYKLA", "SUEIGA", "RENGINYS")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = eventTypeLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text("Renginio tipas") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            colors = eventFormFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(eventTypeLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun inventoryTemplateCategoryOptions(): List<Pair<String, String>> = listOf(
    "CAMPING" to "Stovyklavimas",
    "TOOLS" to "Įrankiai",
    "COOKING" to "Maisto gamyba",
    "FIRST_AID" to "Pirmoji pagalba",
    "UNIFORMS" to "Uniformos",
    "BOOKS" to "Knygos",
    "PERSONAL_LOANS" to "Asmeninis skolinimas"
)

private fun templateCategoryLabel(category: String): String =
    if (inventoryTemplateCategoryOptions().any { it.first == category }) {
        inventoryCategoryLabel(category)
    } else {
        category.toTemplateCategoryInput()
    }

private fun String.toTemplateCategoryInput(): String =
    trim()
        .replace('_', ' ')
