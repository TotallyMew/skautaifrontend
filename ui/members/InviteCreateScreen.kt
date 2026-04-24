package lt.skautai.android.ui.members

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.remote.RoleDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteCreateScreen(
    onBack: () -> Unit,
    viewModel: InviteCreateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pakviesti narį") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atgal"
                        )
                    }
                }
            )
        },
        snackbarHost = { lt.skautai.android.ui.common.SkautaiErrorSnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoadingRoles -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.isSuccess && uiState.generatedCode != null -> {
                    InviteSuccessContent(
                        code = uiState.generatedCode!!,
                        roleName = uiState.generatedRoleName ?: "",
                        expiresAt = uiState.expiresAt ?: "",
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(uiState.generatedCode!!))
                        },
                        onBack = onBack
                    )
                }

                else -> {
                    InviteFormContent(
                        uiState = uiState,
                        onRoleSelected = viewModel::onRoleSelected,
                        onOrgUnitSelected = viewModel::onOrgUnitSelected,
                        onSubmit = viewModel::createInvitation
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InviteFormContent(
    uiState: InviteCreateUiState,
    onRoleSelected: (String) -> Unit,
    onOrgUnitSelected: (String?) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RoleDropdown(
            roles = uiState.roles,
            selectedRoleId = uiState.selectedRoleId,
            onRoleSelected = onRoleSelected
        )

        if (!uiState.lockedOrgUnitName.isNullOrBlank()) {
            OutlinedTextField(
                value = uiState.lockedOrgUnitName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Draugovė") },
                supportingText = { Text("Pakvietimas bus priskirtas jūsų vienetui") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (uiState.canChooseOrgUnit && uiState.selectedRoleType == "LEADERSHIP" && uiState.orgUnits.isNotEmpty()) {
            OrgUnitDropdown(
                orgUnits = uiState.orgUnits,
                selectedOrgUnitId = uiState.selectedOrgUnitId,
                onOrgUnitSelected = onOrgUnitSelected
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onSubmit,
            enabled = !uiState.isSaving && uiState.selectedRoleId.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text("Sukurti pakvietimą")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleDropdown(
    roles: List<RoleDto>,
    selectedRoleId: String,
    onRoleSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedRole = roles.find { it.id == selectedRoleId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedRole?.name ?: "Pasirinkite rolę",
            onValueChange = {},
            readOnly = true,
            label = { Text("Rolė") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            roles.forEach { role ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(role.name)
                            Text(
                                text = if (role.roleType == "LEADERSHIP") "Pareigos" else "Laipsnis",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onRoleSelected(role.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrgUnitDropdown(
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

@Composable
private fun InviteSuccessContent(
    code: String,
    roleName: String,
    expiresAt: String,
    onCopy: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Pakvietimas sukurtas",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Rolė: $roleName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Galioja iki: ${expiresAt.take(10)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onCopy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Kopijuoti kodą")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grįžti")
        }
    }
}
