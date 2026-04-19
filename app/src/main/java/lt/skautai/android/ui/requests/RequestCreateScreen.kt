package lt.skautai.android.ui.requests

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.OrganizationalUnitDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestCreateScreen(
    onBack: () -> Unit,
    viewModel: RequestCreateViewModel = hiltViewModel()
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoadingItems) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RequestItemDropdown(
                        items = uiState.items,
                        selectedItemId = uiState.selectedItemId,
                        onItemSelected = viewModel::onItemSelected
                    )

                    if (uiState.orgUnits.isNotEmpty()) {
                        RequestOrgUnitDropdown(
                            orgUnits = uiState.orgUnits,
                            selectedOrgUnitId = uiState.selectedOrgUnitId,
                            onOrgUnitSelected = viewModel::onOrgUnitSelected
                        )
                    }

                    OutlinedTextField(
                        value = uiState.quantity,
                        onValueChange = viewModel::onQuantityChange,
                        label = { Text("Kiekis *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = uiState.startDate,
                        onValueChange = viewModel::onStartDateChange,
                        label = { Text("Pradžios data (YYYY-MM-DD) *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = uiState.endDate,
                        onValueChange = viewModel::onEndDateChange,
                        label = { Text("Pabaigos data (YYYY-MM-DD) *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = uiState.notes,
                        onValueChange = viewModel::onNotesChange,
                        label = { Text("Pastabos") },
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestItemDropdown(
    items: List<ItemDto>,
    selectedItemId: String,
    onItemSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedItem = items.find { it.id == selectedItemId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedItem?.name ?: "Pasirinkite daiktą",
            onValueChange = {},
            readOnly = true,
            label = { Text("Daiktas *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(item.name)
                            Text(
                                text = "Kiekis: ${item.quantity}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
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
private fun RequestOrgUnitDropdown(
    orgUnits: List<OrganizationalUnitDto>,
    selectedOrgUnitId: String?,
    onOrgUnitSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedUnit = orgUnits.find { it.id == selectedOrgUnitId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedUnit?.name ?: "Pasirinkite draugovę (neprivaloma)",
            onValueChange = {},
            readOnly = true,
            label = { Text("Draugovė") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Nepriskirta") },
                onClick = {
                    onOrgUnitSelected(null)
                    expanded = false
                }
            )
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