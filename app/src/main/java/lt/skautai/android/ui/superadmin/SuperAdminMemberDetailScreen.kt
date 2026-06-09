package lt.skautai.android.ui.superadmin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.MemberLeadershipRoleDto
import lt.skautai.android.data.remote.MemberRankDto
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.remote.RoleDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiTextField
import lt.skautai.android.ui.common.codeLabel
import lt.skautai.android.ui.members.displayRoleName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminMemberDetailScreen(
    tuntasId: String,
    userId: String,
    onBack: () -> Unit,
    viewModel: SuperAdminMemberDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingRoleRemoval by remember { mutableStateOf<MemberLeadershipRoleDto?>(null) }
    var pendingRankRemoval by remember { mutableStateOf<MemberRankDto?>(null) }

    LaunchedEffect(tuntasId, userId) {
        viewModel.load(tuntasId, userId)
    }

    LaunchedEffect(uiState.error, uiState.actionSuccess) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.actionSuccess?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    if (uiState.showAssignRoleDialog) {
        AssignSuperAdminRoleDialog(
            roles = uiState.roles.filter { it.roleType == "LEADERSHIP" },
            units = uiState.units,
            selectedRoleId = uiState.selectedRoleId,
            selectedUnitId = uiState.selectedUnitId,
            isSaving = uiState.isSaving,
            onRoleSelected = viewModel::onRoleSelected,
            onUnitSelected = viewModel::onUnitSelected,
            onConfirm = viewModel::assignLeadershipRole,
            onDismiss = viewModel::closeAssignRoleDialog
        )
    }

    if (uiState.showAssignRankDialog) {
        AssignSuperAdminRankDialog(
            roles = uiState.roles.filter { it.roleType == "RANK" },
            selectedRoleId = uiState.selectedRankRoleId,
            isSaving = uiState.isSaving,
            onRoleSelected = viewModel::onRankSelected,
            onConfirm = viewModel::assignRank,
            onDismiss = viewModel::closeAssignRankDialog
        )
    }

    if (uiState.showEditRoleDialog) {
        EditSuperAdminRoleDialog(
            units = uiState.units,
            selectedUnitId = uiState.selectedUnitId,
            selectedTermStatus = uiState.selectedTermStatus,
            startsAt = uiState.startsAt,
            expiresAt = uiState.expiresAt,
            isSaving = uiState.isSaving,
            onUnitSelected = viewModel::onUnitSelected,
            onTermStatusSelected = viewModel::onTermStatusSelected,
            onStartsAtChanged = viewModel::onStartsAtChanged,
            onExpiresAtChanged = viewModel::onExpiresAtChanged,
            onConfirm = viewModel::updateLeadershipRole,
            onDismiss = viewModel::closeEditRoleDialog
        )
    }

    pendingRoleRemoval?.let { role ->
        AlertDialog(
            onDismissRequest = { pendingRoleRemoval = null },
            title = { Text("Šalinti pareigas?") },
            text = { Text("Pareigos ${displayRoleName(role.roleName)} bus pašalintos.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRoleRemoval = null
                        viewModel.removeLeadershipRole(role.id)
                    },
                    enabled = !uiState.isSaving
                ) {
                    Text("Šalinti", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRoleRemoval = null }) { Text("Atšaukti") }
            }
        )
    }

    pendingRankRemoval?.let { rank ->
        AlertDialog(
            onDismissRequest = { pendingRankRemoval = null },
            title = { Text("Šalinti laipsnį?") },
            text = { Text("Laipsnis ${displayRoleName(rank.roleName)} bus pašalintas iš nario.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRankRemoval = null
                        viewModel.removeRank(rank.id)
                    },
                    enabled = !uiState.isSaving
                ) {
                    Text("Šalinti", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRankRemoval = null }) { Text("Atšaukti") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nario informacija") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Grįžti")
                    }
                }
            )
        },
        snackbarHost = { SkautaiErrorSnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        when {
            uiState.isLoading && uiState.member == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.member == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Narys nerastas", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MemberHeaderCard(member = uiState.member!!)
                    MemberUnitAssignmentsBlock(member = uiState.member!!)
                    RoleBlock(
                        roles = uiState.member!!.leadershipRoles,
                        isSaving = uiState.isSaving,
                        onAdd = viewModel::openAssignRoleDialog,
                        onEdit = viewModel::openEditRoleDialog,
                        onRemove = { role -> pendingRoleRemoval = role }
                    )
                    RankBlock(
                        ranks = uiState.member!!.ranks,
                        isSaving = uiState.isSaving,
                        onAdd = viewModel::openAssignRankDialog,
                        onRemove = { rank -> pendingRankRemoval = rank }
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberHeaderCard(member: MemberDto) {
    SectionCard(title = "Narys") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${member.name} ${member.surname}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(member.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
            member.phone?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MemberUnitAssignmentsBlock(member: MemberDto) {
    SectionCard(title = "Vienetai") {
        if (member.unitAssignments.isNullOrEmpty()) {
            Text("Vienetų nėra", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                member.unitAssignments.forEachIndexed { index, assignment ->
                    Text(
                        "${assignment.organizationalUnitName} (${assignmentTypeLabel(assignment.assignmentType)})",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (index != member.unitAssignments.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleBlock(
    roles: List<MemberLeadershipRoleDto>,
    isSaving: Boolean,
    onAdd: () -> Unit,
    onEdit: (MemberLeadershipRoleDto) -> Unit,
    onRemove: (MemberLeadershipRoleDto) -> Unit
) {
    SectionCard {
        SectionHeader(title = "Pareigos", actionLabel = "Pridėti", enabled = !isSaving, onAction = onAdd)
        if (roles.isEmpty()) {
            Text("Pareigų nėra", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                roles.forEachIndexed { index, role ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(displayRoleName(role.roleName), fontWeight = FontWeight.Medium)
                            role.organizationalUnitName?.let {
                                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                buildString {
                                    append("Būsena: ${termStatusLabel(role.termStatus)}")
                                    role.startsAt?.let { append(" / nuo ${it.take(10)}") }
                                    role.expiresAt?.let { append(" / iki ${it.take(10)}") }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(onClick = { onEdit(role) }, enabled = !isSaving) {
                                Icon(Icons.Default.Edit, contentDescription = "Redaguoti")
                            }
                            IconButton(onClick = { onRemove(role) }, enabled = !isSaving) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Šalinti",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    if (index != roles.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RankBlock(
    ranks: List<MemberRankDto>,
    isSaving: Boolean,
    onAdd: () -> Unit,
    onRemove: (MemberRankDto) -> Unit
) {
    SectionCard {
        SectionHeader(title = "Laipsniai", actionLabel = "Pridėti", enabled = !isSaving, onAction = onAdd)
        if (ranks.isEmpty()) {
            Text("Laipsnių nėra", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ranks.forEachIndexed { index, rank ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(displayRoleName(rank.roleName), fontWeight = FontWeight.Medium)
                            Text(
                                rank.assignedAt.take(10),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onRemove(rank) }, enabled = !isSaving) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Šalinti",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    if (index != ranks.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String,
    enabled: Boolean,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onAction, enabled = enabled) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text(actionLabel)
        }
    }
}

@Composable
private fun SectionCard(
    title: String? = null,
    content: @Composable () -> Unit
) {
    SkautaiCard(
        modifier = Modifier.fillMaxWidth(),
        tonal = MaterialTheme.colorScheme.surfaceBright
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            title?.let {
                Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

private fun termStatusLabel(status: String): String = when (status) {
    "ACTIVE" -> "Aktyvus"
    "COMPLETED" -> "Baigtas"
    "RESIGNED" -> "Atsistatydinta"
    else -> codeLabel(status)
}

private fun assignmentTypeLabel(type: String): String = when (type) {
    "MEMBER" -> "Narys"
    else -> codeLabel(type)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignSuperAdminRoleDialog(
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
                        value = selectedRole?.name?.let(::displayRoleName) ?: "Pasirinkite pareigas",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pareigos") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(roleExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                        roles.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(displayRoleName(role.name)) },
                                onClick = {
                                    onRoleSelected(role.id)
                                    roleExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = it }) {
                    OutlinedTextField(
                        value = selectedUnit?.name ?: "Vienetas (neprivaloma)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vienetas") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
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
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = selectedRoleId.isNotBlank() && !isSaving) {
                Text("Priskirti")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Atšaukti")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignSuperAdminRankDialog(
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
                    value = selectedRole?.name?.let(::displayRoleName) ?: "Pasirinkite laipsnį",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Laipsnis") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    roles.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(displayRoleName(role.name)) },
                            onClick = {
                                onRoleSelected(role.id)
                                expanded = false
                            }
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
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Atšaukti")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSuperAdminRoleDialog(
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
                        value = termStatusLabel(selectedTermStatus),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Būsena") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(statusExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                        statuses.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(termStatusLabel(status)) },
                                onClick = {
                                    onTermStatusSelected(status)
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = it }) {
                    OutlinedTextField(
                        value = selectedUnit?.name ?: "Vienetas (neprivaloma)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vienetas") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
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

                SkautaiTextField(
                    value = startsAt,
                    onValueChange = onStartsAtChanged,
                    label = "Pradžia (YYYY-MM-DD)",
                    modifier = Modifier.fillMaxWidth()
                )

                SkautaiTextField(
                    value = expiresAt,
                    onValueChange = onExpiresAtChanged,
                    label = "Pabaiga (YYYY-MM-DD)",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isSaving) {
                Text("Išsaugoti")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Atšaukti")
            }
        }
    )
}
