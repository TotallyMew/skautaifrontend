package lt.skautai.android.ui.members

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.*
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiErrorState

@Composable
fun MemberDetailScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: MemberDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    var pendingStepDownRole by remember { mutableStateOf<MemberLeadershipRoleDto?>(null) }
    var pendingRoleRemoval by remember { mutableStateOf<MemberLeadershipRoleDto?>(null) }
    var pendingRankRemoval by remember { mutableStateOf<MemberRankDto?>(null) }
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
            title = { Text("Šalinti iš tunto?") },
            text = { Text("Narys bus pašalintas iš tunto. Taip pat bus uždarytos jo pareigos ir vienetų narystės šiame tunte.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.removeMember(userId) },
                    enabled = !uiState.isSaving
                ) { Text("Šalinti iš tunto", color = MaterialTheme.colorScheme.error) }
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

    if (uiState.showEditRoleDialog) {
        EditRoleDialog(
            units = uiState.availableUnits,
            selectedUnitId = uiState.selectedUnitId,
            selectedTermStatus = uiState.selectedTermStatus,
            startsAt = uiState.startsAt,
            expiresAt = uiState.expiresAt,
            isSaving = uiState.isSaving,
            onUnitSelected = viewModel::onRoleUnitSelected,
            onTermStatusSelected = viewModel::onTermStatusSelected,
            onStartsAtChanged = viewModel::onStartsAtChanged,
            onExpiresAtChanged = viewModel::onExpiresAtChanged,
            onConfirm = { viewModel.updateLeadershipRole(userId) },
            onDismiss = viewModel::hideEditRoleDialog
        )
    }

    if (uiState.showMoveMemberDialog) {
        MoveMemberDialog(
            units = uiState.availableUnits,
            selectedUnitId = uiState.selectedMoveUnitId,
            isSaving = uiState.isSaving,
            onUnitSelected = viewModel::onMoveUnitSelected,
            onConfirm = { viewModel.moveMember(userId) },
            onDismiss = viewModel::hideMoveMemberDialog
        )
    }

    pendingStepDownRole?.let { role ->
        AlertDialog(
            onDismissRequest = { pendingStepDownRole = null },
            title = { Text("Atsistatydinti is pareigu?") },
            text = {
                Text(
                    buildString {
                        append("Bus uzdarytos pareigos ")
                        append(role.roleName)
                        role.organizationalUnitName?.let {
                            append(" vienete ")
                            append(it)
                        }
                        append(".")
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingStepDownRole = null
                        viewModel.stepDownLeadershipRole(userId, role.id)
                    },
                    enabled = !uiState.isSaving
                ) {
                    Text("Atsistatydinti", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingStepDownRole = null }) { Text("Atsaukti") }
            }
        )
    }

    pendingRoleRemoval?.let { role ->
        AlertDialog(
            onDismissRequest = { pendingRoleRemoval = null },
            title = { Text("Salinti pareigas?") },
            text = {
                Text(
                    buildString {
                        append("Pareigos ")
                        append(role.roleName)
                        role.organizationalUnitName?.let {
                            append(" vienete ")
                            append(it)
                        }
                        append(" bus pasalintos.")
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRoleRemoval = null
                        viewModel.removeLeadershipRole(userId, role.id)
                    },
                    enabled = !uiState.isSaving
                ) {
                    Text("Salinti", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRoleRemoval = null }) { Text("Atsaukti") }
            }
        )
    }

    pendingRankRemoval?.let { rank ->
        AlertDialog(
            onDismissRequest = { pendingRankRemoval = null },
            title = { Text("Salinti laipsni?") },
            text = { Text("Laipsnis ${rank.roleName} bus pasalintas is sio nario.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRankRemoval = null
                        viewModel.removeRank(userId, rank.id)
                    },
                    enabled = !uiState.isSaving
                ) {
                    Text("Salinti", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRankRemoval = null }) { Text("Atsaukti") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SkautaiErrorSnackbarHost(hostState = snackbarHostState) }
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
                        Icon(Icons.Default.PersonRemove, contentDescription = "Šalinti iš tunto",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SkautaiErrorState(
                        message = uiState.error!!,
                        onRetry = { viewModel.loadMember(userId) }
                    )
                }
                uiState.member != null -> MemberDetailContent(
                    member = uiState.member!!,
                    isSaving = uiState.isSaving,
                    isCurrentUser = currentUserId == userId,
                    canManageRoles = "roles.assign" in permissions,
                    canMoveMember = "unit.members.manage:ALL" in permissions,
                    onMoveMember = viewModel::openMoveMemberDialog,
                    onAssignRole = viewModel::openAssignRoleDialog,
                    onEditRole = viewModel::openEditRoleDialog,
                    onStepDownRole = { role -> pendingStepDownRole = role },
                    onRemoveRole = { role -> pendingRoleRemoval = role },
                    onAssignRank = viewModel::openAssignRankDialog,
                    onRemoveRank = { rank -> pendingRankRemoval = rank }
                )
            }
        }
    }
}

@Composable
private fun MemberDetailContent(
    member: MemberDto,
    isSaving: Boolean,
    isCurrentUser: Boolean,
    canManageRoles: Boolean,
    canMoveMember: Boolean,
    onMoveMember: () -> Unit,
    onAssignRole: () -> Unit,
    onEditRole: (MemberLeadershipRoleDto) -> Unit,
    onStepDownRole: (MemberLeadershipRoleDto) -> Unit,
    onRemoveRole: (MemberLeadershipRoleDto) -> Unit,
    onAssignRank: () -> Unit,
    onRemoveRank: (MemberRankDto) -> Unit
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

        if (canMoveMember) {
            Button(
                onClick = onMoveMember,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Perkelti i kita vieneta")
            }
        }

        MemberUnitsSection(assignments = member.unitAssignments.orEmpty())

        MemberRolesSection(
            roles = member.leadershipRoles,
            isSaving = isSaving,
            isCurrentUser = isCurrentUser,
            canManageRoles = canManageRoles,
            onAssignRole = onAssignRole,
            onEditRole = onEditRole,
            onStepDownRole = onStepDownRole,
            onRemoveRole = onRemoveRole
        )

        MemberLeadershipHistorySection(history = member.leadershipRoleHistory)

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
private fun MemberUnitsSection(assignments: List<MemberUnitAssignmentDto>) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Vienetai", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            if (assignments.isEmpty()) {
                Text(
                    "Aktyviu vienetu nera",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                assignments.forEach { assignment ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = assignment.organizationalUnitName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = when (assignment.assignmentType) {
                                "MEMBER" -> "Narys"
                                "VADOVO_PADEJEJAS" -> "Vadovo padejejas"
                                else -> assignment.assignmentType
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (assignment != assignments.last()) HorizontalDivider()
                }
            }
        }
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
    isCurrentUser: Boolean,
    canManageRoles: Boolean,
    onAssignRole: () -> Unit,
    onEditRole: (MemberLeadershipRoleDto) -> Unit,
    onStepDownRole: (MemberLeadershipRoleDto) -> Unit,
    onRemoveRole: (MemberLeadershipRoleDto) -> Unit
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isCurrentUser && role.termStatus == "ACTIVE") {
                                TextButton(onClick = { onStepDownRole(role) }, enabled = !isSaving) {
                                    Text("Atsistatydinti")
                                }
                            }
                            if (canManageRoles) {
                                IconButton(onClick = { onEditRole(role) }, enabled = !isSaving) {
                                    Icon(Icons.Default.Edit, contentDescription = "Redaguoti pareigas")
                                }
                                IconButton(onClick = { onRemoveRole(role) }, enabled = !isSaving) {
                                    Icon(Icons.Default.Delete, contentDescription = "Šalinti pareigas",
                                        tint = MaterialTheme.colorScheme.error)
                                }
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
private fun MemberLeadershipHistorySection(history: List<MemberLeadershipRoleDto>) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Pareigu istorija", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            if (history.isEmpty()) {
                Text(
                    "Buvusiu pareigu nera",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                history.forEach { role ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(role.roleName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        role.organizationalUnitName?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val endedAt = role.leftAt?.take(10)
                        Text(
                            text = listOfNotNull(role.termStatus, endedAt).joinToString(" - "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (role != history.last()) HorizontalDivider()
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
    onRemoveRank: (MemberRankDto) -> Unit
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
                            IconButton(onClick = { onRemoveRank(rank) }, enabled = !isSaving) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveMemberDialog(
    units: List<OrganizationalUnitDto>,
    selectedUnitId: String,
    isSaving: Boolean,
    onUnitSelected: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedUnit = units.find { it.id == selectedUnitId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Perkelti nari") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Bus pakeista tik pagrindine nario naryste to paties vieneto tipo ribose. Pareigos ir laipsniai nebus keiciami.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedUnit?.name ?: "Pasirinkite vieneta",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Naujas vienetas") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        units.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.name) },
                                onClick = {
                                    onUnitSelected(unit.id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = selectedUnitId.isNotBlank() && !isSaving) {
                Text("Perkelti")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Atsaukti") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditRoleDialog(
    units: List<OrganizationalUnitDto>,
    selectedUnitId: String?,
    selectedTermStatus: String,
    startsAt: String,
    expiresAt: String,
    isSaving: Boolean,
    onUnitSelected: (String?) -> Unit,
    onTermStatusSelected: (String) -> Unit,
    onStartsAtChanged: (String) -> Unit,
    onExpiresAtChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var unitExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    val selectedUnit = units.find { it.id == selectedUnitId }
    val statuses = listOf("ACTIVE", "COMPLETED", "RESIGNED")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Redaguoti pareigas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }) {
                    OutlinedTextField(
                        value = selectedTermStatus,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Statusas") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(statusExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                        statuses.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status) },
                                onClick = {
                                    onTermStatusSelected(status)
                                    statusExpanded = false
                                }
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
                                onClick = {
                                    onUnitSelected(null)
                                    unitExpanded = false
                                }
                            )
                            units.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit.name) },
                                    onClick = {
                                        onUnitSelected(unit.id)
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = startsAt,
                    onValueChange = onStartsAtChanged,
                    label = { Text("Pradzia (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = expiresAt,
                    onValueChange = onExpiresAtChanged,
                    label = { Text("Pabaiga (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isSaving) {
                Text("Issaugoti")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Atsaukti") } }
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
