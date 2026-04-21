package lt.skautai.android.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import lt.skautai.android.data.remote.LocationDto
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiSectionHeader
import lt.skautai.android.ui.common.inventoryCategoryLabel
import lt.skautai.android.ui.common.inventoryTypeLabel

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
    val isCreateFlow = itemId == null

    LaunchedEffect(Unit) {
        viewModel.init(itemId, mode)
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) navController.popBackStack()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isCreateFlow) createTitle(uiState.mode) else "Redaguoti inventoriu") },
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                                        STEP_INFO -> if (validateInfoStep(uiState, viewModel)) currentStep = STEP_REVIEW
                                        else -> viewModel.save(null)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isSaving
                            ) {
                                if (uiState.isSaving) {
                                    CircularProgressIndicator()
                                } else {
                                    Text(
                                        when (currentStep) {
                                            STEP_CONTEXT -> "Toliau"
                                            STEP_INFO -> "Perziureti"
                                            else -> "Issaugoti"
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = { viewModel.save(itemId) },
                            enabled = !uiState.isSaving,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isSaving) CircularProgressIndicator() else Text("Issaugoti pakeitimus")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepHeader(currentStep: Int) {
    val labels = listOf(
        "1. Kontekstas ir laikymas",
        "2. Daikto informacija",
        "3. Perziura"
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
            onSelected = viewModel::onCategoryChange
        )

        ReadOnlyInfo(
            label = "Kilme",
            value = originLabelForMode(uiState.mode)
        )

        if (uiState.mode == "UNIT_OWN") {
            SkautaiCard(
                modifier = Modifier.fillMaxWidth(),
                tonal = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = "Jei vienetas nori gauti daikta is bendro tunto inventoriaus, nekurk jo ranka. Naudok paemimo is tunto prasyma, kad inventorininkas patvirtintu perdavima.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        LocationDropdown(
            locations = uiState.locations,
            selectedId = uiState.selectedLocationId,
            onSelected = viewModel::onLocationChange
        )

        OutlinedTextField(
            value = uiState.temporaryStorageLabel,
            onValueChange = viewModel::onTemporaryStorageLabelChange,
            label = { Text("Vienkartine laikymo vieta") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        if (uiState.mode == "UNIT_OWN" || uiState.selectedOrgUnitId.isNotBlank()) {
            CustodianUnitDropdown(
                units = uiState.orgUnits,
                selectedId = uiState.selectedOrgUnitId,
                onSelected = viewModel::onOrgUnitChange,
                enabled = uiState.mode != "UNIT_OWN"
            )
        }

        SkautaiCard(
            modifier = Modifier.fillMaxWidth(),
            tonal = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Text(
                text = approvalMessage(uiState),
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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

        OutlinedTextField(
            value = uiState.name,
            onValueChange = viewModel::onNameChange,
            label = { Text("Pavadinimas *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = uiState.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text("Aprasymas") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )

        OutlinedTextField(
            value = uiState.quantity,
            onValueChange = viewModel::onQuantityChange,
            label = { Text("Kiekis *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        if (isEditing) {
            DropdownField(
                label = "Bukle",
                selected = uiState.condition,
                options = listOf(
                    "GOOD" to "Gera",
                    "DAMAGED" to "Pazeista",
                    "WRITTEN_OFF" to "Nurasyta"
                ),
                onSelected = viewModel::onConditionChange
            )
        }

        OutlinedTextField(
            value = uiState.notes,
            onValueChange = viewModel::onNotesChange,
            label = { Text("Pastabos") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )

        OutlinedTextField(
            value = uiState.purchaseDate,
            onValueChange = viewModel::onPurchaseDateChange,
            label = { Text("Pirkimo data (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
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
        SkautaiSectionHeader(title = "Perziura")
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
                    ReviewRow("Vienkartine vieta", it)
                }
                ReviewRow("Atsakingas vienetas", selectedUnit?.name ?: "Bendras tunto saugojimas")
                ReviewRow("Kilme", originLabelForMode(uiState.mode))
                ReviewRow("Kiekis", uiState.quantity.ifBlank { "1" })
            }
        }
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
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: selected
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
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
private fun LocationDropdown(
    locations: List<LocationDto>,
    selectedId: String,
    onSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = if (selectedId.isEmpty()) "Lokacija nepasirinkta" else locations.firstOrNull { it.id == selectedId }?.name ?: "Lokacija nepasirinkta"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Permanent lokacija") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Lokacija nepasirinkta") },
                onClick = {
                    onSelected(null)
                    expanded = false
                }
            )
            locations.forEach { location ->
                DropdownMenuItem(
                    text = { Text(location.name) },
                    onClick = {
                        onSelected(location.id)
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
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = units.firstOrNull { it.id == selectedId }?.name ?: "Pasirinkti vieneta"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Atsakingas vienetas") },
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
    "CAMPING" to "Camping",
    "TOOLS" to "Tools",
    "COOKING" to "Cooking",
    "FIRST_AID" to "First aid",
    "UNIFORMS" to "Uniforms",
    "BOOKS" to "Books",
    "PERSONAL_LOANS" to "Personal loans"
)

private fun createTitle(mode: String): String = when (mode) {
    "UNIT_OWN" -> "Naujas vieneto daiktas"
    "PERSONAL" -> "Mano siulomas skolinti"
    else -> "Prideti i tunto inventoriu"
}

private fun contextTitle(mode: String): String = when (mode) {
    "UNIT_OWN" -> "Aktyvaus vieneto inventorius"
    "PERSONAL" -> "Mano siulomas skolinti"
    else -> "Bendras tunto inventorius"
}

private fun contextDescription(mode: String): String = when (mode) {
    "UNIT_OWN" -> "Cia kuriamas naujas tavo vieneto daiktas. Jei reikia paimti is bendro inventoriaus, naudok atskira prasymo krepseli."
    "PERSONAL" -> "Sis daiktas iskart atsiras kaip tavo siulomas skolinti inventorius."
    else -> "Tai bendro tunto sandelio inventorius."
}

private fun originLabelForMode(mode: String): String = when (mode) {
    "SHARED" -> "Bendro tunto inventorius"
    "PERSONAL" -> "Asmeninis daiktas skolinimui"
    "UNIT_OWN" -> "Naujas vieneto daiktas"
    else -> "Sukurtas naujai"
}

private fun approvalMessage(uiState: InventoryAddEditUiState): String = when (uiState.mode) {
    "UNIT_OWN" -> "Daiktas bus sukurtas tavo aktyviam vienetui kaip jo nuosavas inventorius."
    "PERSONAL" -> "Daiktas bus iskart matomas kaip tavo siulomas skolinti inventorius."
    else -> "Bendro tunto inventorius gali reikalauti aukstesnio lygio patvirtinimo."
}

private fun validateContextStep(
    uiState: InventoryAddEditUiState,
    viewModel: InventoryAddEditViewModel
): Boolean {
    if (uiState.category.isBlank()) {
        viewModel.showValidationError("Pasirink inventoriaus kategorija")
        return false
    }
    if (uiState.mode == "UNIT_OWN" && uiState.selectedOrgUnitId.isBlank()) {
        viewModel.showValidationError("Aktyviam vienetui reikia pasirinkto vieneto")
        return false
    }
    return true
}

private fun validateInfoStep(
    uiState: InventoryAddEditUiState,
    viewModel: InventoryAddEditViewModel
): Boolean {
    if (uiState.name.isBlank()) {
        viewModel.showValidationError("Pavadinimas privalomas")
        return false
    }
    if (uiState.quantity.toIntOrNull() == null || uiState.quantity.toInt() < 1) {
        viewModel.showValidationError("Kiekis turi buti teigiamas skaicius")
        return false
    }
    return true
}
