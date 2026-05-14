package lt.skautai.android.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
        InventoryTemplateEditorSheet(
            editor = editor,
            inventoryItems = uiState.inventoryItems,
            isSaving = uiState.isSaving,
            onDismiss = viewModel::closeEditor,
            onNameChange = viewModel::onNameChange,
            onEventTypeChange = viewModel::onEventTypeChange,
            onItemChange = viewModel::onItemChange,
            onAddItem = viewModel::addItemRow,
            onRemoveItem = viewModel::removeItemRow,
            onSave = viewModel::saveTemplate
        )
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                SkautaiCard(modifier = Modifier.fillMaxWidth(), tonal = MaterialTheme.colorScheme.surfaceBright) {
                    Column(
                        modifier = Modifier.padding(16.dp),
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
                    }
                }
            }
            items(editor.items.size, key = { index -> "template_sheet_item_$index" }) { index ->
                InventoryTemplateItemEditor(
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                SkautaiCard(modifier = Modifier.fillMaxWidth(), tonal = MaterialTheme.colorScheme.surfaceBright) {
                    Column(
                        modifier = Modifier.padding(16.dp),
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
                    }
                }
            }
            items(editor.items.size, key = { index -> "template_sheet_item_$index" }) { index ->
                InventoryTemplateItemEditor(
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
                    InventoryTemplateItemEditor(
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
    SkautaiCard(modifier = Modifier.fillMaxWidth(), tonal = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
