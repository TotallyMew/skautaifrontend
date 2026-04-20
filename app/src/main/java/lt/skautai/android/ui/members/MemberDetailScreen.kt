package lt.skautai.android.ui.members

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.*

@Composable
fun MemberDetailScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: MemberDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userId) { viewModel.loadMember(userId) }

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) onBack()
    }

    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionError()
        }
    }

    if (uiState.showRemoveMemberDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideRemoveMemberDialog,
            title = { Text("Šalinti narį?") },
            text = { Text("Narys bus pašalintas iš tunto. Šis veiksmas negrįžtamas.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.removeMember(userId) },
                    enabled = !uiState.isSaving
                ) { Text("Šalinti", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideRemoveMemberDialog) { Text("Atšaukti") }
            }
        )
    }

    if (uiState.showAssignRoleDialog) {
        AssignRoleDialog(
            roles = uiState.leadershipRoles,
            units = uiState.availableUnits,
            selectedRoleId = uiState.selectedRoleId,
            selectedUnitId = uiState.selectedUnitId,
            isSaving = uiState.isSaving,
            onRoleSelected = viewModel::onRoleSelected,
            onUnitSelected = viewModel::onRoleUnitSelected,
            onConfirm = { viewModel.assignLeadershipRole(userId) },
            onDismiss = viewModel::hideAssignRoleDialog
        )
    }

    if (uiState.showAssignRankDialog) {
        AssignRankDialog(
            roles = uiState.rankRoles,
            selectedRoleId = uiState.selectedRankRoleId,
            isSaving = uiState.isSaving,
            onRoleSelected = viewModel::onRankRoleSelected,
            onConfirm = { viewModel.assignRank(userId) },
            onDismiss = viewModel::hideAssignRankDialog
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Grįžti")
                }
                if (uiState.member != null && "members.remove" in permissions) {
                    IconButton(onClick = viewModel::showRemoveMemberDialog) {
                        Icon(Icons.Default.PersonRemove, contentDescription = "Šalinti narį",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadMember(userId) }) { Text("Bandyti dar kartą") }
                    }
                }
                uiState.member != null -> MemberDetailContent(
                    member = uiState.member!!,
                    isSaving = uiState.isSaving,
                    canManageRoles = "roles.assign" in permissions,
                    onAssignRole = viewModel::openAssignRoleDialog,
                    onRemoveRole = { assignmentId -> viewModel.removeLeadershipRole(userId, assignmentId) },
                    onAssignRank = viewModel::openAssignRankDialog,
                    onRemoveRank = { rankId -> viewModel.removeRank(userId, rankId) }
                )
            }
        }
    }
}

@Composable
private fun MemberDetailContent(
    member: MemberDto,
    isSaving: Boolean,
    canManageRoles: Boolean,
    onAssignRole: () -> Unit,
    onRemoveRole: (String) -> Unit,
    onAssignRank: () -> Unit,
    onRemoveRank: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "${member.name} ${member.surname}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        MemberInfoSection(member = member)

        MemberRolesSection(
            roles = member.leadershipRoles,
            isSaving = isSaving,
            canManageRoles = canManageRoles,
            onAssignRole = onAssignRole,
            onRemoveRole = onRemoveRole
        )

        MemberRanksSection(
            ranks = member.ranks,
            isSaving = isSaving,
            canManageRoles = canManageRoles,
            onAssignRank = onAssignRank,
            onRemoveRank = onRemoveRank
        )
    }
}

@Composable
private fun MemberInfoSection(member: MemberDto) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Informacija", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            MemberInfoRow("El. paštas", member.email)
            member.phone?.let { MemberInfoRow("Telefonas", it) }
            MemberInfoRow("Prisijungė", member.joinedAt.take(10))
        }
    }
}

@Composable
private fun MemberRolesSection(
    roles: List<MemberLeadershipRoleDto>,
    isSaving: Boolean,
    canManageRoles: Boolean,
    onAssignRole: () -> Unit,
    onRemoveRole: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pareigos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (canManageRoles) {
                    TextButton(onClick = onAssignRole, enabled = !isSaving) { Text("+ Pridėti") }
                }
            }
            HorizontalDivider()
            if (roles.isEmpty()) {
                Text("Pareigų nėra", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium)
            } else {
                roles.forEach { role ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(role.roleName, style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium)
                            role.organizationalUnitName?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = if (role.termStatus == "ACTIVE") "Aktyvus" else role.termStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (role.termStatus == "ACTIVE") MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (canManageRoles) {
                            IconButton(onClick = { onRemoveRole(role.id) }, enabled = !isSaving) {
                                Icon(Icons.Default.Delete, contentDescription = "Šalinti pareigas",
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    if (role != roles.last()) HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun MemberRanksSection(
    ranks: List<MemberRankDto>,
    isSaving: Boolean,
    canManageRoles: Boolean,
    onAssignRank: () -> Unit,
    onRemoveRank: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Laipsniai", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (canManageRoles) {
                    TextButton(onClick = onAssignRank, enabled = !isSaving) { Text("+ Pridėti") }
                }
            }
            HorizontalDivider()
            if (ranks.isEmpty()) {
                Text("Laipsnių nėra", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium)
            } else {
                ranks.forEach { rank ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(rank.roleName, style = MaterialTheme.typography.bodyMedium)
                        if (canManageRoles) {
                            IconButton(onClick = { onRemoveRank(rank.id) }, enabled = !isSaving) {
                                Icon(Icons.Default.Delete, contentDescription = "Šalinti laipsnį",
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    if (rank != ranks.last()) HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignRoleDialog(
    roles: List<RoleDto>,
    units: List<OrganizationalUnitDto>,
    selectedRoleId: String,
    selectedUnitId: String?,
    isSaving: Boolean,
    onRoleSelected: (String) -> Unit,
    onUnitSelected: (String?) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var roleExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }
    val selectedRole = roles.find { it.id == selectedRoleId }
    val selectedUnit = units.find { it.id == selectedUnitId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Priskirti pareigas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(expanded = roleExpanded, onExpandedChange = { roleExpanded = it }) {
                    OutlinedTextField(
                        value = selectedRole?.name ?: "Pasirinkite pareigas",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pareigos") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(roleExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                        roles.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.name) },
                                onClick = { onRoleSelected(role.id); roleExpanded = false }
                            )
                        }
                    }
                }

                if (units.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = it }) {
                        OutlinedTextField(
                            value = selectedUnit?.name ?: "Vienetas (neprivaloma)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Vienetas") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Nepriskirtas") },
                                onClick = { onUnitSelected(null); unitExpanded = false }
                            )
                            units.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit.name) },
                                    onClick = { onUnitSelected(unit.id); unitExpanded = false }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = selectedRoleId.isNotBlank() && !isSaving) {
                Text("Priskirti")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Atšaukti") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignRankDialog(
    roles: List<RoleDto>,
    selectedRoleId: String,
    isSaving: Boolean,
    onRoleSelected: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedRole = roles.find { it.id == selectedRoleId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Priskirti laipsnį") },
        text = {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = selectedRole?.name ?: "Pasirinkite laipsnį",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Laipsnis") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    roles.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role.name) },
                            onClick = { onRoleSelected(role.id); expanded = false }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = selectedRoleId.isNotBlank() && !isSaving) {
                Text("Priskirti")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Atšaukti") } }
    )
}

@Composable
private fun MemberInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
