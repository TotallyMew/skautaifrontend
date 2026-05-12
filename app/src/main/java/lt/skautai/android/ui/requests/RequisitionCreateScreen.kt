package lt.skautai.android.ui.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.ui.common.SkautaiTextField
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun RequisitionCreateScreen(
    onBack: () -> Unit,
    viewModel: RequisitionCreateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onBack()
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
                title = { Text("Naujas prašymas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atgal")
                    }
                }
            )
        },
        snackbarHost = { lt.skautai.android.ui.common.SkautaiErrorSnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        if (uiState.isLoadingUnits) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Pirkimo arba papildymo prašymas",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Aprašyk trūkstamą inventorių. Vieneto vadovas galės pats patvirtinti arba perduoti inventorininkui.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                SkautaiTextField(
                    value = uiState.itemName,
                    onValueChange = viewModel::onItemNameChange,
                    label = if (uiState.requestType == "RESTOCK_EXISTING") "Daiktas papildymui *" else "Norimas daiktas *",
                    placeholder = if (uiState.requestType == "RESTOCK_EXISTING") "Pasirink iš sąrašo žemiau" else "pvz. Palapinė 4 asmenims",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = uiState.requestType != "RESTOCK_EXISTING"
                )

                RequisitionTypeSelector(
                    selectedType = uiState.requestType,
                    onTypeSelected = viewModel::onRequestTypeChange
                )

                if (uiState.requestType == "RESTOCK_EXISTING") {
                    RequisitionExistingItemDropdown(
                        items = uiState.existingItems,
                        selectedItemId = uiState.existingItemId,
                        onItemSelected = viewModel::onExistingItemSelected
                    )
                } else {
                    SkautaiTextField(
                        value = uiState.itemDescription,
                        onValueChange = viewModel::onItemDescriptionChange,
                        label = "Paaiškinimas",
                        placeholder = "Kuo tiksliau aprašyk, ko reikia",
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }

                SkautaiTextField(
                    value = uiState.quantity,
                    onValueChange = viewModel::onQuantityChange,
                    label = "Kiekis *",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                RequisitionDatePickerField(
                    value = uiState.neededByDate,
                    onDateSelected = viewModel::onNeededByDateChange
                )

                RequisitionOrgUnitDropdown(
                    canRequestForTuntas = uiState.canRequestForTuntas,
                    orgUnits = uiState.orgUnits,
                    selectedOrgUnitId = uiState.selectedOrgUnitId,
                    selectedOrgUnitName = uiState.selectedOrgUnitName,
                    onOrgUnitSelected = viewModel::onOrgUnitSelected
                )

                SkautaiTextField(
                    value = uiState.notes,
                    onValueChange = viewModel::onNotesChange,
                    label = "Pagrindimas / pastabos",
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = viewModel::createRequest,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text("Pateikti prašymą")
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequisitionTypeSelector(
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        "NEW_ITEM" to "Naujas daiktas",
        "RESTOCK_EXISTING" to "Papildyti esamą daiktą"
    )
    val selectedLabel = options.firstOrNull { it.first == selectedType }?.second ?: "Pasirinkti"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Prašymo tipas") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.second) },
                    onClick = {
                        onTypeSelected(option.first)
                        expanded = false
                    }
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequisitionExistingItemDropdown(
    items: List<lt.skautai.android.data.remote.ItemDto>,
    selectedItemId: String?,
    onItemSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedItem = items.firstOrNull { it.id == selectedItemId }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedItem?.let { "${it.name} (${it.quantity})" } ?: "Pasirinkti daiktą",
            onValueChange = {},
            readOnly = true,
            label = { Text("Esamas daiktas") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.sortedBy { it.name.lowercase() }.forEach { item ->
                DropdownMenuItem(
                    text = { Text("${item.name} (${item.quantity} vnt.)") },
                    onClick = {
                        onItemSelected(item.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequisitionDatePickerField(
    value: String,
    onDateSelected: (String) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text("Reikalinga iki") },
        placeholder = { Text("Pasirinkite datą") },
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

private fun Long.toIsoDateString(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return formatter.format(Date(this))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequisitionOrgUnitDropdown(
    canRequestForTuntas: Boolean,
    orgUnits: List<OrganizationalUnitDto>,
    selectedOrgUnitId: String?,
    selectedOrgUnitName: String?,
    onOrgUnitSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedUnit = orgUnits.find { it.id == selectedOrgUnitId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedUnit?.name ?: selectedOrgUnitName ?: "Tuntui",
            onValueChange = {},
            readOnly = true,
            label = { Text("Kam teikiamas prašymas") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (canRequestForTuntas) {
                DropdownMenuItem(
                    text = { Text("Tuntui") },
                    onClick = {
                        onOrgUnitSelected(null)
                        expanded = false
                    }
                )
            }
            orgUnits.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.name) },
                    onClick = {
                        onOrgUnitSelected(unit.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
