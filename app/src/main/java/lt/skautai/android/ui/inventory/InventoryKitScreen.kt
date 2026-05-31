package lt.skautai.android.ui.inventory

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.InventoryKitDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.LocationDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiChip
import lt.skautai.android.ui.common.SkautaiEmptyState
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.SkautaiStatusTone
import lt.skautai.android.ui.common.itemConditionLabel
import lt.skautai.android.ui.common.itemConditionTone
import lt.skautai.android.ui.locations.LocationPickerField

@Composable
fun InventoryKitScreen(
    onItemClick: (String) -> Unit,
    viewModel: InventoryKitViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (val state = uiState) {
        InventoryKitUiState.Loading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is InventoryKitUiState.Error -> SkautaiErrorState(message = state.message, onRetry = viewModel::refresh)
        is InventoryKitUiState.Success -> InventoryKitContent(
            state = state,
            onSelectKit = viewModel::selectKit,
            onCreateKit = viewModel::createKit,
            onClearActionError = viewModel::clearActionError,
            onItemClick = onItemClick
        )
    }
}

@Composable
private fun InventoryKitContent(
    state: InventoryKitUiState.Success,
    onSelectKit: (String) -> Unit,
    onCreateKit: (String, String?, String?, String?, Map<String, Int>) -> Unit,
    onClearActionError: () -> Unit,
    onItemClick: (String) -> Unit
) {
    var isCreatingKit by remember { mutableStateOf(false) }
    var createRequestStarted by remember { mutableStateOf(false) }

    LaunchedEffect(state.isCreating, state.actionError) {
        if (state.isCreating) {
            createRequestStarted = true
        } else if (createRequestStarted && state.actionError == null) {
            createRequestStarted = false
            isCreatingKit = false
        }
    }

    if (isCreatingKit) {
        CreateInventoryKitScreen(
            availableItems = state.availableItems,
            locations = state.locations,
            isCreating = state.isCreating,
            actionError = state.actionError,
            onBack = {
                if (!state.isCreating) {
                    isCreatingKit = false
                    onClearActionError()
                }
            },
            onCreate = onCreateKit
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.kits.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SkautaiEmptyState(
                    title = "Komplektų nėra",
                    subtitle = "Komplektas yra daiktų rinkinys, pvz. žygio virtuvė ar palapinių rinkinys.",
                    icon = Icons.Default.Inventory2
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.kits, key = { it.id }) { kit ->
                            SkautaiChip(
                                label = kit.name,
                                selected = kit.id == state.selectedKit?.id,
                                onClick = { onSelectKit(kit.id) }
                            )
                        }
                    }
                }

                state.selectedKit?.let { kit ->
                    item { InventoryKitHeader(kit) }
                    if (kit.items.isEmpty()) {
                        item {
                            SkautaiEmptyState(
                                title = "Komplektas tuščias",
                                subtitle = "Pridėjus daiktus čia matysis kiekiai ir būklės.",
                                icon = Icons.Default.Inventory2
                            )
                        }
                    } else {
                        items(kit.items, key = { it.id }) { kitItem ->
                            SkautaiCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onItemClick(kitItem.itemId) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = kitItem.itemName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        SkautaiStatusPill(
                                            label = "${kitItem.quantity}/${kitItem.availableQuantity}",
                                            tone = if (kitItem.availableQuantity >= kitItem.quantity) SkautaiStatusTone.Success else SkautaiStatusTone.Warning
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SkautaiStatusPill(
                                            label = itemConditionLabel(kitItem.itemCondition),
                                            tone = itemConditionTone(kitItem.itemCondition)
                                        )
                                        SkautaiStatusPill(label = kitItem.itemStatus, tone = SkautaiStatusTone.Neutral)
                                    }
                                    kitItem.notes?.takeIf { it.isNotBlank() }?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { isCreatingKit = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Sukurti komplektą")
        }
    }
}

@Composable
private fun InventoryKitHeader(kit: InventoryKitDto) {
    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = kit.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            kit.description?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = listOfNotNull(
                    kit.locationPath ?: kit.locationName ?: kit.temporaryStorageLabel,
                    kit.custodianName,
                    kit.responsibleUserName
                ).joinToString(" · ").ifBlank { "Lokacija nenurodyta" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SkautaiStatusPill(label = "${kit.items.size} daiktai", tone = SkautaiStatusTone.Info)
        }
    }
}

@Composable
private fun CreateInventoryKitScreen(
    availableItems: List<ItemDto>,
    locations: List<LocationDto>,
    isCreating: Boolean,
    actionError: String?,
    onBack: () -> Unit,
    onCreate: (String, String?, String?, String?, Map<String, Int>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf<LocationDto?>(null) }
    var temporaryStorageLabel by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val selectedQuantities = remember { mutableStateMapOf<String, Int>() }
    val categories = remember(availableItems) {
        availableItems.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val filteredItems = remember(availableItems, query, selectedCategory) {
        availableItems.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.name.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true) ||
                item.type.contains(query, ignoreCase = true)
            val matchesCategory = selectedCategory == null || item.category == selectedCategory
            matchesQuery && matchesCategory
        }
    }

    BackHandler(enabled = !isCreating, onBack = onBack)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, enabled = !isCreating) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Grįžti")
            }
            Text(
                text = "Naujas komplektas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                enabled = !isCreating,
                onClick = {
                    onCreate(
                        name,
                        description,
                        selectedLocation?.id,
                        temporaryStorageLabel,
                        selectedQuantities.toMap()
                    )
                }
            ) {
                Text(if (isCreating) "Kuriama..." else "Sukurti")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Pavadinimas") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Aprašymas") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LocationPickerField(
                        label = "Komplekto lokacija",
                        locations = locations,
                        selectedId = selectedLocation?.id,
                        onSelected = { selectedLocation = it }
                    )
                    OutlinedTextField(
                        value = temporaryStorageLabel,
                        onValueChange = { temporaryStorageLabel = it },
                        label = { Text("Laikina vieta / lentyna") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
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
                                    label = category,
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category }
                                )
                            }
                        }
                    }
                    Text(
                        text = "${selectedQuantities.size} pasirinkta",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    actionError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
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
                    InventoryKitSelectableItem(
                        item = item,
                        quantity = selectedQuantities[item.id] ?: 0,
                        onDecrease = {
                            val current = selectedQuantities[item.id] ?: 0
                            if (current <= 1) {
                                selectedQuantities.remove(item.id)
                            } else {
                                selectedQuantities[item.id] = current - 1
                            }
                        },
                        onIncrease = {
                            val current = selectedQuantities[item.id] ?: 0
                            selectedQuantities[item.id] = (current + 1).coerceAtMost(item.quantity.coerceAtLeast(1))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InventoryKitSelectableItem(
    item: ItemDto,
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
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
                        item.category,
                        "${item.quantity} vnt.",
                        itemConditionLabel(item.condition)
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            QuantityStepper(
                quantity = quantity,
                onDecrease = onDecrease,
                onIncrease = onIncrease
            )
        }
    }
}

@Composable
private fun QuantityStepper(
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
            verticalAlignment = Alignment.CenterVertically
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
