package lt.skautai.android.ui.inventory

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.LocationDto
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.ui.common.RemoteImage
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiInlineErrorBanner
import lt.skautai.android.ui.common.SkautaiSectionHeader
import lt.skautai.android.ui.locations.LocationPickerField
import lt.skautai.android.ui.common.itemConditionLabel
import lt.skautai.android.ui.common.inventoryCategoryLabel
import lt.skautai.android.ui.common.inventoryTypeLabel
import java.time.Instant
import java.time.ZoneOffset

private const val STEP_CONTEXT = 0
private const val STEP_INFO = 1
private const val STEP_REVIEW = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryAddEditScreen(
    itemId: String?,
    mode: String?,
    navController: NavController,
    viewModel: InventoryAddEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var currentStep by remember(itemId) { mutableIntStateOf(STEP_CONTEXT) }
    var saveImmediately by remember(itemId) { mutableStateOf(true) }
    var saveAndAddAnother by remember(itemId) { mutableStateOf(false) }
    val isCreateFlow = itemId == null

    LaunchedEffect(Unit) {
        viewModel.init(itemId, mode)
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            if (saveAndAddAnother && isCreateFlow) {
                snackbarHostState.showSnackbar("Išsaugota. Galite pridėti kitą daiktą.")
                viewModel.prepareNextItem()
                currentStep = STEP_INFO
            } else {
                viewModel.clearSuccess()
                navController.popBackStack()
            }
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbarMessage()
        }
    }

    uiState.duplicateCandidate?.let { duplicate ->
        DuplicateItemDialog(
            item = duplicate,
            onDismiss = viewModel::dismissDuplicateDialog,
            onAddToExisting = viewModel::addToExistingDuplicate,
            onCreateNew = viewModel::createNewDuplicateRecord
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isCreateFlow) createTitle(uiState.mode) else "Redaguoti inventorių") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(
                        onClick = {
                            if (isCreateFlow && currentStep > STEP_CONTEXT) currentStep -= 1
                            else navController.popBackStack()
                        }
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atgal"
                        )
                    }
                }
            )
        },
        snackbarHost = { lt.skautai.android.ui.common.SkautaiErrorSnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    uiState.formError?.let { message ->
                        SkautaiInlineErrorBanner(message = message)
                    }

                    if (isCreateFlow) {
                        StepHeader(currentStep = currentStep)
                    }

                    when {
                        !isCreateFlow -> {
                            ContextStep(uiState = uiState, viewModel = viewModel)
                            ItemInfoStep(uiState = uiState, viewModel = viewModel, isEditing = true)
                        }
                        currentStep == STEP_CONTEXT -> ContextStep(uiState = uiState, viewModel = viewModel)
                        currentStep == STEP_INFO -> ItemInfoStep(uiState = uiState, viewModel = viewModel, isEditing = false)
                        else -> ReviewStep(uiState = uiState)
                    }

                    if (isCreateFlow) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (currentStep > STEP_CONTEXT) {
                                OutlinedButton(
                                    onClick = { currentStep -= 1 },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Atgal")
                                }
                            }
                            Button(
                                onClick = {
                                    when (currentStep) {
                                        STEP_CONTEXT -> if (validateContextStep(uiState, viewModel)) currentStep = STEP_INFO
                                        STEP_INFO -> if (validateInfoStep(uiState, viewModel)) {
                                            if (saveImmediately) viewModel.save(null) else currentStep = STEP_REVIEW
                                        }
                                        else -> viewModel.save(null)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isSaving && !uiState.isUploadingPhoto
                            ) {
                                if (uiState.isSaving) {
                                    CircularProgressIndicator()
                                } else {
                                    Text(
                                        when (currentStep) {
                                            STEP_CONTEXT -> "Toliau"
                                            STEP_INFO -> if (saveImmediately) submitLabel(uiState) else "Peržiūrėti"
                                            else -> submitLabel(uiState)
                                        }
                                    )
                                }
                            }
                        }
                        if (currentStep == STEP_INFO) {
                            SaveFlowOptions(
                                saveImmediately = saveImmediately,
                                onSaveImmediatelyChange = { saveImmediately = it },
                                saveAndAddAnother = saveAndAddAnother,
                                onSaveAndAddAnotherChange = { saveAndAddAnother = it }
                            )
                        }
                        if (currentStep == STEP_REVIEW) {
                            OutlinedButton(
                                onClick = {
                                    saveAndAddAnother = true
                                    viewModel.save(null)
                                },
                                enabled = !uiState.isSaving && !uiState.isUploadingPhoto,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (uiState.mode == "SHARED" && !uiState.canCreateSharedDirectly) "Pateikti ir pridėti kitą" else "Išsaugoti ir pridėti kitą")
                            }
                        }
                    } else {
                        Button(
                            onClick = { viewModel.save(itemId) },
                            enabled = !uiState.isSaving && !uiState.isUploadingPhoto,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isSaving) CircularProgressIndicator() else Text("Išsaugoti pakeitimus")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateItemDialog(
    item: ItemDto,
    onDismiss: () -> Unit,
    onAddToExisting: () -> Unit,
    onCreateNew: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rastas toks pats daiktas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Jau yra įrašas su tokiu pačiu pavadinimu. Ką norite daryti?")
                Text(
                    text = buildString {
                        append(item.name)
                        append(" • ")
                        append(item.quantity)
                        append(" vnt.")
                        item.locationName?.takeIf { it.isNotBlank() }?.let {
                            append(" • ")
                            append(it)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAddToExisting) {
                Text("Pridėti prie esamo")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("Atšaukti")
                }
                TextButton(onClick = onCreateNew) {
                    Text("Sukurti naują")
                }
            }
        }
    )
}

@Composable
private fun StepHeader(currentStep: Int) {
    val labels = listOf(
        "1. Kontekstas ir laikymas",
        "2. Daikto informacija",
        "3. Peržiūra"
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Naujo inventoriaus vedlys", style = MaterialTheme.typography.headlineSmall)
        labels.forEachIndexed { index, label ->
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (index == currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (index == currentStep) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ContextStep(
    uiState: InventoryAddEditUiState,
    viewModel: InventoryAddEditViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SkautaiCard(
            modifier = Modifier.fillMaxWidth(),
            tonal = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(contextTitle(uiState.mode), style = MaterialTheme.typography.titleMedium)
                Text(
                    contextDescription(uiState.mode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SkautaiSectionHeader(title = "Inventoriaus duomenys")

        ReadOnlyInfo(label = "Tipas", value = inventoryTypeLabel(uiState.type))

        DropdownField(
            label = "Inventoriaus kategorija",
            selected = uiState.category,
            options = inventoryCategoryOptions(),
            onSelected = viewModel::onCategoryChange,
            errorText = uiState.categoryError
        )

        if (uiState.mode == "UNIT_OWN") {
            SkautaiCard(
                modifier = Modifier.fillMaxWidth(),
                tonal = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = "Kuriamas naujas vieneto daiktas; paėmimui iš tunto naudok prašymą.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        if (uiState.canManageLocations) {
            LocationPickerField(
                label = "Nuolatinė lokacija",
                locations = uiState.locations,
                selectedId = uiState.selectedLocationId,
                onSelected = { viewModel.onLocationChange(it?.id) },
                filter = { location ->
                    when (uiState.mode) {
                        "PERSONAL" -> location.visibility == "PRIVATE"
                        "UNIT_OWN" -> location.visibility == "UNIT" && location.ownerUnitId == uiState.selectedOrgUnitId
                        else -> location.visibility == "PUBLIC"
                    }
                },
                onQuickCreate = if (uiState.mode == "PERSONAL") {
                    { name, _ -> viewModel.createPrivateLocation(name) }
                } else {
                    null
                },
                errorText = null
            )
        } else {
            Text(
                text = "Lokacija šiame sraute parenkama vadovo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = uiState.temporaryStorageLabel,
            onValueChange = viewModel::onTemporaryStorageLabelChange,
            label = { Text("Vienkartinė laikymo vieta") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        if (uiState.mode == "UNIT_OWN" || uiState.selectedOrgUnitId.isNotBlank()) {
            CustodianUnitDropdown(
                units = uiState.orgUnits,
                selectedId = uiState.selectedOrgUnitId,
                onSelected = viewModel::onOrgUnitChange,
                enabled = uiState.mode != "UNIT_OWN",
                errorText = uiState.orgUnitError
            )
        }

        Text(
            text = approvalMessage(uiState),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ItemInfoStep(
    uiState: InventoryAddEditUiState,
    viewModel: InventoryAddEditViewModel,
    isEditing: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SkautaiSectionHeader(title = "Daikto informacija")

        PhotoField(uiState = uiState, viewModel = viewModel)

        OutlinedTextField(
            value = uiState.name,
            onValueChange = viewModel::onNameChange,
            label = { Text("Pavadinimas *") },
            isError = uiState.nameError != null,
            supportingText = uiState.nameError?.let { message -> { Text(message) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = uiState.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text("Aprašymas") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )

        QuantityStepper(
            quantity = uiState.quantity,
            onQuantityChange = viewModel::onQuantityChange,
            errorText = uiState.quantityError
        )

        ConditionSelector(
            selected = uiState.condition,
            onSelected = viewModel::onConditionChange
        )

        OutlinedTextField(
            value = uiState.notes,
            onValueChange = viewModel::onNotesChange,
            label = { Text("Pastabos") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )

        PurchaseDateField(
            value = uiState.purchaseDate,
            onSelected = viewModel::onPurchaseDateSelected
        )

        OutlinedTextField(
            value = uiState.purchasePrice,
            onValueChange = viewModel::onPurchasePriceChange,
            label = { Text("Pirkimo kaina (EUR)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
    }
}

@Composable
private fun ReviewStep(uiState: InventoryAddEditUiState) {
    val selectedLocation = uiState.locations.firstOrNull { it.id == uiState.selectedLocationId }
    val selectedUnit = uiState.orgUnits.firstOrNull { it.id == uiState.selectedOrgUnitId }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SkautaiSectionHeader(title = "Peržiūra")
        SkautaiCard(
            modifier = Modifier.fillMaxWidth(),
            tonal = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(uiState.name.ifBlank { "Be pavadinimo" }, style = MaterialTheme.typography.headlineSmall)
                ReviewRow("Tipas", inventoryTypeLabel(uiState.type))
                ReviewRow("Kategorija", inventoryCategoryLabel(uiState.category))
                ReviewRow("Lokacija", selectedLocation?.name ?: "Nenurodyta")
                uiState.temporaryStorageLabel.takeIf { it.isNotBlank() }?.let {
                    ReviewRow("Vienkartinė vieta", it)
                }
                ReviewRow("Atsakingas vienetas", selectedUnit?.name ?: "Bendras tunto saugojimas")
                ReviewRow("Kilmė", originLabelForMode(uiState.mode))
                ReviewRow("Kiekis", uiState.quantity.ifBlank { "1" })
                ReviewRow("Būklė", itemConditionLabel(uiState.condition))
                uiState.photoUrl.takeIf { it.isNotBlank() }?.let {
                    ReviewRow("Nuotrauka", "Pridėta")
                }
            }
        }
    }
}

@Composable
private fun PhotoField(
    uiState: InventoryAddEditUiState,
    viewModel: InventoryAddEditViewModel
) {
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.uploadPhoto(it) }
    }

    SkautaiCard(
        modifier = Modifier.fillMaxWidth(),
        tonal = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.AddAPhoto,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Pridėti nuotrauką")
            }
            if (uiState.isUploadingPhoto) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(
                        text = "Nuotrauka įkeliama...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (uiState.photoUrl.isNotBlank()) {
                RemoteImage(
                    imageUrl = uiState.photoUrl,
                    contentDescription = "Pasirinkta nuotrauka",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.7f)
                )
                Text(
                    text = "Nuotrauka įkelta",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseDateField(
    value: String,
    onSelected: (String?) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = value.toEpochMillisOrNull()
    )

    OutlinedButton(
        onClick = { showPicker = true },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
    ) {
        Text(if (value.isBlank()) "Pasirinkti pirkimo datą" else "Pirkimo data: $value")
    }

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSelected(datePickerState.selectedDateMillis?.toIsoDate())
                        showPicker = false
                    }
                ) {
                    Text("Pasirinkti")
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
private fun QuantityStepper(
    quantity: String,
    onQuantityChange: (String) -> Unit,
    errorText: String?
) {
    val numeric = quantity.toIntOrNull() ?: 1
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = { onQuantityChange((numeric - 1).coerceAtLeast(1).toString()) },
            modifier = Modifier.size(width = 56.dp, height = 56.dp)
        ) {
            androidx.compose.material3.Icon(Icons.Default.Remove, contentDescription = "Mažinti")
        }
        OutlinedTextField(
            value = quantity,
            onValueChange = onQuantityChange,
            label = { Text("Kiekis *") },
            isError = errorText != null,
            supportingText = errorText?.let { message -> { Text(message) } },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Button(
            onClick = { onQuantityChange((numeric + 1).toString()) },
            modifier = Modifier.size(width = 56.dp, height = 56.dp)
        ) {
            androidx.compose.material3.Icon(Icons.Default.Add, contentDescription = "Didinti")
        }
    }
}

private fun Long.toIsoDate(): String =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate().toString()

private fun String.toEpochMillisOrNull(): Long? =
    runCatching {
        java.time.LocalDate.parse(this)
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()
    }.getOrNull()

@Composable
private fun ConditionSelector(
    selected: String,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Būklė",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            conditionOptions().forEach { (value, label) ->
                val isSelected = selected == value
                if (isSelected) {
                    Button(
                        onClick = { onSelected(value) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(label)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onSelected(value) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(label)
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveFlowOptions(
    saveImmediately: Boolean,
    onSaveImmediatelyChange: (Boolean) -> Unit,
    saveAndAddAnother: Boolean,
    onSaveAndAddAnotherChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CheckboxRow(
            checked = saveImmediately,
            onCheckedChange = onSaveImmediatelyChange,
            label = "Išsaugoti iškart"
        )
        CheckboxRow(
            checked = saveAndAddAnother,
            onCheckedChange = onSaveAndAddAnotherChange,
            label = "Išsaugoti ir pridėti kitą"
        )
    }
}

@Composable
private fun CheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ReadOnlyInfo(label: String, value: String) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
    errorText: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: selected
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            isError = errorText != null,
            supportingText = errorText?.let { message -> { Text(message) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, display) ->
                DropdownMenuItem(
                    text = { Text(display) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustodianUnitDropdown(
    units: List<OrganizationalUnitDto>,
    selectedId: String,
    onSelected: (String?) -> Unit,
    enabled: Boolean,
    errorText: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = units.firstOrNull { it.id == selectedId }?.name ?: "Pasirinkti vienetą"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Atsakingas vienetas") },
            isError = errorText != null,
            supportingText = errorText?.let { message -> { Text(message) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            units.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.name) },
                    onClick = {
                        onSelected(unit.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun inventoryCategoryOptions(): List<Pair<String, String>> = listOf(
    "CAMPING" to "Stovyklavimas",
    "TOOLS" to "Įrankiai",
    "COOKING" to "Maisto gamyba",
    "FIRST_AID" to "Pirmoji pagalba",
    "UNIFORMS" to "Uniformos",
    "BOOKS" to "Knygos",
    "PERSONAL_LOANS" to "Asmeninis skolinimas"
)

private fun conditionOptions(): List<Pair<String, String>> = listOf(
    "GOOD" to "Gera",
    "DAMAGED" to "Vidutinė",
    "WRITTEN_OFF" to "Bloga"
)

private fun createTitle(mode: String): String = when (mode) {
    "UNIT_OWN" -> "Naujas vieneto daiktas"
    "PERSONAL" -> "Mano siūlomas skolinti"
    else -> "Pridėti į tunto inventorių"
}

private fun submitLabel(uiState: InventoryAddEditUiState): String =
    if (uiState.mode == "SHARED" && !uiState.canCreateSharedDirectly) {
        "Pateikti patvirtinimui"
    } else {
        "Išsaugoti"
    }

private fun contextTitle(mode: String): String = when (mode) {
    "UNIT_OWN" -> "Aktyvaus vieneto inventorius"
    "PERSONAL" -> "Mano siūlomas skolinti"
    else -> "Bendras tunto inventorius"
}

private fun contextDescription(mode: String): String = when (mode) {
    "UNIT_OWN" -> "Naujas tavo aktyvaus vieneto inventoriaus įrašas."
    "PERSONAL" -> "Asmeninis daiktas, kurį gali skolinti kitiems."
    else -> "Bendro tunto sandėlio inventoriaus įrašas."
}

private fun originLabelForMode(mode: String): String = when (mode) {
    "SHARED" -> "Bendro tunto inventorius"
    "PERSONAL" -> "Asmeninis daiktas skolinimui"
    "UNIT_OWN" -> "Naujas vieneto daiktas"
    else -> "Sukurtas naujai"
}

private fun approvalMessage(uiState: InventoryAddEditUiState): String = when (uiState.mode) {
    "UNIT_OWN" -> "Daiktas bus sukurtas tavo aktyviam vienetui kaip jo nuosavas inventorius."
    "PERSONAL" -> "Daiktas bus iškart matomas kaip tavo siūlomas skolinti inventorius."
    else -> if (uiState.canCreateSharedDirectly) {
        "Daiktas bus iš karto įtrauktas į bendrą tunto inventorių."
    } else {
        "Daiktas bus pateiktas patvirtinimui. Iki patvirtinimo jis bus pažymėtas kaip laukiantis."
    }
}

private fun validateContextStep(
    uiState: InventoryAddEditUiState,
    viewModel: InventoryAddEditViewModel
): Boolean {
    if (uiState.category.isBlank()) {
        viewModel.showValidationError("Pasirinkite inventoriaus kategoriją.")
        return false
    }
    if (uiState.mode == "UNIT_OWN" && uiState.selectedOrgUnitId.isBlank()) {
        viewModel.showValidationError("Aktyviam vienetui reikia pasirinkto vieneto.")
        return false
    }
    return true
}

private fun validateInfoStep(
    uiState: InventoryAddEditUiState,
    viewModel: InventoryAddEditViewModel
): Boolean {
    if (uiState.name.isBlank()) {
        viewModel.showValidationError("Pavadinimas yra privalomas.")
        return false
    }
    if (uiState.quantity.toIntOrNull() == null || uiState.quantity.toInt() < 1) {
        viewModel.showValidationError("Kiekis turi būti teigiamas skaičius.")
        return false
    }
    return true
}
