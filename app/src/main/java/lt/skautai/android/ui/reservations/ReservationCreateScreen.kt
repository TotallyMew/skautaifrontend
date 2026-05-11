package lt.skautai.android.ui.reservations

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.LocationDto
import lt.skautai.android.ui.common.RemoteImage
import lt.skautai.android.ui.common.SkautaiInlineErrorBanner
import lt.skautai.android.ui.common.SkautaiTextField
import lt.skautai.android.ui.locations.LocationPickerField

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReservationCreateScreen(
    onBack: () -> Unit,
    viewModel: ReservationCreateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val filteredItems = remember(uiState.items, uiState.searchQuery) {
        val query = uiState.searchQuery.trim().lowercase()
        if (query.isBlank()) {
            uiState.items
        } else {
            uiState.items.filter { item ->
                item.name.lowercase().contains(query) ||
                    item.type.lowercase().contains(query) ||
                    item.category.lowercase().contains(query) ||
                    (item.description?.lowercase()?.contains(query) == true)
            }
        }
    }
    val selectedQuantities = remember(uiState.selectedItems) {
        uiState.selectedItems.associate { it.itemId to it.quantity }
    }
    val sharedItems = remember(filteredItems) { filteredItems.filter { it.custodianId == null } }
    val unitItems = remember(filteredItems, uiState.activeOrgUnitId) {
        filteredItems.filter { it.custodianId == uiState.activeOrgUnitId && it.custodianId != null }
    }
    val density = LocalDensity.current
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0
    val datesSelected = uiState.startDate.isNotBlank() && uiState.endDate.isNotBlank()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onBack()
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbarMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nauja rezervacija") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atgal")
                    }
                }
            )
        },
        snackbarHost = { lt.skautai.android.ui.common.SkautaiErrorSnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (!isKeyboardVisible) {
                Surface(shadowElevation = 8.dp) {
                    Column(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Pasirinkta daiktų: ${uiState.selectedItems.sumOf { it.quantity }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = viewModel::createReservation,
                            enabled = !uiState.isSaving && datesSelected,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(if (datesSelected) "Sukurti rezervaciją" else "Pasirinkite datas")
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.isLoadingItems) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Rezervuok esamą inventorių",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Pasirink datas ir daiktus, kuriuos nori užsakyti tam laikotarpiui.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                uiState.formError?.let { message ->
                    item {
                        SkautaiInlineErrorBanner(message = message)
                    }
                }

                item {
                    SkautaiTextField(
                        value = uiState.title,
                        onValueChange = viewModel::onTitleChange,
                        label = "Rezervacijos pavadinimas",
                        isError = uiState.titleError != null,
                        supportingText = uiState.titleError,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    DatePickerField(
                        label = "Pradžios data",
                        value = uiState.startDate,
                        onDateSelected = viewModel::onStartDateChange,
                        errorText = uiState.startDateError
                    )
                }

                item {
                    DatePickerField(
                        label = "Pabaigos data",
                        value = uiState.endDate,
                        onDateSelected = viewModel::onEndDateChange,
                        errorText = uiState.endDateError
                    )
                }

                if (uiState.isLoadingAvailability) {
                    item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                }

                item {
                    ReservationLocationFields(
                        locations = uiState.locations,
                        activeOrgUnitId = uiState.activeOrgUnitId,
                        hasSelectedUnitInventory = uiState.hasSelectedUnitInventory(),
                        pickupLocationId = uiState.pickupLocationId,
                        returnLocationId = uiState.returnLocationId,
                        onPickupLocationChange = viewModel::onPickupLocationChange,
                        onReturnLocationChange = viewModel::onReturnLocationChange
                    )
                }

                item {
                    SkautaiTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        label = "Ieškoti daikto",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    SkautaiTextField(
                        value = uiState.notes,
                        onValueChange = viewModel::onNotesChange,
                        label = "Pastabos",
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )
                }

                if (uiState.selectedItems.isNotEmpty()) {
                    item {
                        SelectedItemsSummary(
                            selectedItems = uiState.selectedItems,
                            allItems = uiState.items
                        )
                    }
                }

                stickyHeader {
                    ReservationSectionHeader(
                        title = "Tunto inventorius",
                        subtitle = "Tvirtina inventorininkas arba tuntininkas"
                    )
                }

                if (sharedItems.isEmpty()) {
                    item {
                        EmptyInventorySection(
                            message = if (uiState.searchQuery.isBlank()) {
                                "Tunto inventorius tuščias"
                            } else {
                                "Tunto inventoriuje nieko nerasta pagal paiešką"
                            }
                        )
                    }
                }

                items(sharedItems, key = { "shared-${it.id}" }) { item ->
                    ReservationItemCard(
                        item = item,
                        selectedQuantity = selectedQuantities[item.id] ?: 0,
                        availableQuantity = uiState.availabilityByItemId[item.id] ?: item.quantity,
                        onIncrease = { viewModel.increaseItem(item.id) },
                        onDecrease = { viewModel.decreaseItem(item.id) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                stickyHeader {
                    ReservationSectionHeader(
                        title = "Tavo vieneto inventorius",
                        subtitle = "Tvirtina vieneto vadovas"
                    )
                }

                if (unitItems.isEmpty()) {
                    item {
                        EmptyInventorySection(
                            message = if (uiState.searchQuery.isBlank()) {
                                "Vieneto inventorius tuščias"
                            } else {
                                "Vieneto inventoriuje nieko nerasta pagal paiešką"
                            }
                        )
                    }
                }

                items(unitItems, key = { "unit-${it.id}" }) { item ->
                    ReservationItemCard(
                        item = item,
                        selectedQuantity = selectedQuantities[item.id] ?: 0,
                        availableQuantity = uiState.availabilityByItemId[item.id] ?: item.quantity,
                        onIncrease = { viewModel.increaseItem(item.id) },
                        onDecrease = { viewModel.decreaseItem(item.id) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun ReservationLocationFields(
    locations: List<LocationDto>,
    activeOrgUnitId: String?,
    hasSelectedUnitInventory: Boolean,
    pickupLocationId: String?,
    returnLocationId: String?,
    onPickupLocationChange: (String?) -> Unit,
    onReturnLocationChange: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LocationPickerField(
            label = "Atsiėmimo vieta",
            locations = locations,
            selectedId = pickupLocationId,
            onSelected = { onPickupLocationChange(it?.id) },
            filter = { location ->
                reservationLocationAllowed(location, activeOrgUnitId, hasSelectedUnitInventory)
            }
        )
        LocationPickerField(
            label = "Grąžinimo vieta",
            locations = locations,
            selectedId = returnLocationId,
            onSelected = { onReturnLocationChange(it?.id) },
            filter = { location ->
                reservationLocationAllowed(location, activeOrgUnitId, hasSelectedUnitInventory)
            }
        )
    }
}

private fun reservationLocationAllowed(
    location: LocationDto,
    activeOrgUnitId: String?,
    hasSelectedUnitInventory: Boolean
): Boolean = when (location.visibility) {
    "UNIT" -> hasSelectedUnitInventory && activeOrgUnitId == location.ownerUnitId
    "PRIVATE" -> false
    else -> true
}

private fun ReservationCreateUiState.hasSelectedUnitInventory(): Boolean {
    val selectedIds = selectedItems.map { it.itemId }.toSet()
    return items.any { it.id in selectedIds && it.custodianId != null }
}

@Composable
private fun EmptyInventorySection(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SelectedItemsSummary(
    selectedItems: List<ReservationDraftItem>,
    allItems: List<ItemDto>
) {
    val itemsById = allItems.associateBy { it.id }
    val sharedItems = selectedItems.filter { itemsById[it.itemId]?.custodianId == null }
    val unitItems = selectedItems.filter { itemsById[it.itemId]?.custodianId != null }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Rezervacijos krepšelis",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            BasketGroup(title = "Tunto inventorius", items = sharedItems)
            BasketGroup(title = "Tavo vieneto inventorius", items = unitItems)
        }
    }
}

@Composable
private fun BasketGroup(
    title: String,
    items: List<ReservationDraftItem>
) {
    if (items.isEmpty()) return
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    items.sortedBy { it.itemName.lowercase() }.forEach { item ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.itemName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "x${item.quantity}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ReservationSectionHeader(
    title: String,
    subtitle: String
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    label: String,
    value: String,
    onDateSelected: (String) -> Unit,
    errorText: String? = null
) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        placeholder = { Text("Pasirinkite datą") },
        isError = errorText != null,
        supportingText = errorText?.let { message -> { Text(message) } },
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            TextButton(onClick = { showPicker = true }) {
                Text("Rinktis")
            }
        }
    )

    if (showPicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateSelected(millis.toIsoDateString())
                        }
                        showPicker = false
                    }
                ) {
                    Text("Gerai")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Atšaukti")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ReservationItemCard(
    item: ItemDto,
    selectedQuantity: Int,
    availableQuantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    val remainingQuantity = (availableQuantity - selectedQuantity).coerceAtLeast(0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.photoUrl.isNullOrBlank()) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = item.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            RemoteImage(
                imageUrl = item.photoUrl,
                contentDescription = item.name,
                modifier = Modifier.size(42.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = "Laisva: $remainingQuantity / ${item.quantity}",
                style = MaterialTheme.typography.bodySmall,
                color = if (remainingQuantity > 0) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
            item.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = onDecrease,
                enabled = selectedQuantity > 0,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Mažinti")
            }
            Text(
                text = "$selectedQuantity",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            FilledIconButton(
                onClick = onIncrease,
                enabled = availableQuantity > selectedQuantity,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Didinti")
            }
        }
    }
}

private fun Long.toIsoDateString(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return formatter.format(Date(this))
}
