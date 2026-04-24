package lt.skautai.android.ui.units

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val UNIT_TYPES = listOf(
    "VILKU_DRAUGOVE",
    "SKAUTU_DRAUGOVE",
    "PATYRUSIU_SKAUTU_DRAUGOVE",
    "GILDIJA",
    "VYR_SKAUTU_VIENETAS",
    "VYR_SKAUCIU_VIENETAS"
)

private val UNIT_SUBTYPES = listOf("DRAUGOVE", "BURELIS")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitCreateScreen(
    onBack: () -> Unit,
    viewModel: UnitCreateViewModel = hiltViewModel()
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
                title = { Text("Naujas vienetas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atgal")
                    }
                }
            )
        },
        snackbarHost = { lt.skautai.android.ui.common.SkautaiErrorSnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Pavadinimas *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            UnitTypeDropdown(
                selectedType = uiState.type,
                onTypeSelected = viewModel::onTypeChange
            )

            if (uiState.type.startsWith("VYR_")) {
                UnitSubTypeDropdown(
                    selectedSubType = uiState.subType,
                    onSubTypeSelected = viewModel::onSubTypeChange
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = viewModel::createUnit,
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
                    Text("Sukurti vienetą")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitTypeDropdown(
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = if (selectedType.isBlank()) "Pasirinkite tipą" else unitTypeLabel(selectedType),
            onValueChange = {},
            readOnly = true,
            label = { Text("Tipas *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            UNIT_TYPES.forEach { type ->
                DropdownMenuItem(
                    text = { Text(unitTypeLabel(type)) },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitSubTypeDropdown(
    selectedSubType: String?,
    onSubTypeSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedSubType?.let { subtypeLabel(it) } ?: "Pasirinkite potipį (neprivaloma)",
            onValueChange = {},
            readOnly = true,
            label = { Text("Potipis") },
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
                text = { Text("Nepriskirtas") },
                onClick = {
                    onSubTypeSelected(null)
                    expanded = false
                }
            )
            UNIT_SUBTYPES.forEach { subtype ->
                DropdownMenuItem(
                    text = { Text(subtypeLabel(subtype)) },
                    onClick = {
                        onSubTypeSelected(subtype)
                        expanded = false
                    }
                )
            }
        }
    }
}

