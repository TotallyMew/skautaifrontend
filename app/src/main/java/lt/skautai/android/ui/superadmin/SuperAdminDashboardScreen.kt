package lt.skautai.android.ui.superadmin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
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
import lt.skautai.android.data.remote.TuntasDto
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiTextField
import lt.skautai.android.ui.members.displayRoleName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminDashboardScreen(
    viewModel: SuperAdminDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingRoleRemoval by remember { mutableStateOf<MemberLeadershipRoleDto?>(null) }
    var pendingRankRemoval by remember { mutableStateOf<MemberRankDto?>(null) }

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
            text = {
                Text(
                    buildString {
                        append("Pareigos ")
                        append(displayRoleName(role.roleName))
                        role.organizationalUnitName?.let {
                            append(" vienete ")
                            append(it)
                        }
                        append(" bus pašalintos.")
                    }
                )
            },
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
            TopAppBar(title = { Text("Superadministratorius") })
        },
        snackbarHost = { SkautaiErrorSnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        when {
            uiState.isLoadingTuntai && uiState.tuntai.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.tuntai.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tuntų nėra")
                }
            }

            else -> {
                val selectedTuntas = uiState.tuntai.find { it.id == uiState.selectedTuntasId }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PendingTuntaiSection(
                        tuntai = uiState.tuntai.filter { it.status == "PENDING" },
                        isSaving = uiState.isSaving,
                        onApprove = viewModel::approveTuntas,
                        onReject = viewModel::rejectTuntas
                    )

                    TuntasSelectorCard(
                        tuntai = uiState.tuntai,
                        selectedTuntasId = uiState.selectedTuntasId,
                        onSelected = viewModel::selectTuntas
                    )

                    selectedTuntas?.let { tuntas ->
                        SelectedTuntasCard(
                            tuntas = tuntas,
                            isSaving = uiState.isSaving,
                            onApprove = { viewModel.approveTuntas(tuntas.id) },
                            onReject = { viewModel.rejectTuntas(tuntas.id) }
                        )
                    }

                    if (uiState.isLoadingContext) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        UnitsSection(units = uiState.units)
                        MembersSection(
                            members = uiState.members,
                            selectedMemberId = uiState.selectedMemberId,
                            onMemberSelected = viewModel::selectMember
                        )
                        MemberDetailSection(
                            member = uiState.selectedMember,
                            isLoading = uiState.isLoadingMember,
                            isSaving = uiState.isSaving,
                            onAssignRole = viewModel::openAssignRoleDialog,
                            onEditRole = viewModel::openEditRoleDialog,
                            onRemoveRole = { role -> pendingRoleRemoval = role },
                            onAssignRank = viewModel::openAssignRankDialog,
                            onRemoveRank = { rank -> pendingRankRemoval = rank }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingTuntaiSection(
    tuntai: List<TuntasDto>,
    isSaving: Boolean,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    if (tuntai.isEmpty()) return

    SectionCard(title = "Laukiantys tvirtinimo") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            tuntai.forEach { tuntas ->
                TuntasSummaryRow(
                    tuntas = tuntas,
                    showActions = true,
                    isSaving = isSaving,
                    onApprove = { onApprove(tuntas.id) },
                    onReject = { onReject(tuntas.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TuntasSelectorCard(
    tuntai: List<TuntasDto>,
    selectedTuntasId: String?,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTuntas = tuntai.find { it.id == selectedTuntasId }

    SectionCard(title = "Pasirinktas tuntas") {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selectedTuntas?.name ?: "Pasirinkite tuntą",
                onValueChange = {},
                readOnly = true,
                label = { Text("Tuntas") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                tuntai.forEach { tuntas ->
                    DropdownMenuItem(
                        text = { Text("${tuntas.name} (${statusText(tuntas.status)})") },
                        onClick = {
                            onSelected(tuntas.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedTuntasCard(
    tuntas: TuntasDto,
    isSaving: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    SectionCard(title = "Tunto informacija") {
        TuntasSummaryRow(
            tuntas = tuntas,
            showActions = tuntas.status == "PENDING",
            isSaving = isSaving,
            onApprove = onApprove,
            onReject = onReject
        )
    }
}

@Composable
private fun UnitsSection(units: List<OrganizationalUnitDto>) {
    SectionCard(title = "Vienetai") {
        if (units.isEmpty()) {
            Text("Vienetų nėra", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                units.forEachIndexed { index, unit ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(unit.name, fontWeight = FontWeight.Medium)
                        Text(
                            buildString {
                                append(unit.type)
                                unit.subtype?.takeIf { it.isNotBlank() }?.let {
                                    append(" / ")
                                    append(it)
                                }
                                append(" / ")
                                append("${unit.memberCount} nariai")
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (index != units.lastIndex) HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun MembersSection(
    members: List<MemberDto>,
    selectedMemberId: String?,
    onMemberSelected: (String) -> Unit
) {
    SectionCard(title = "Nariai") {
        if (members.isEmpty()) {
            Text("Narių nėra", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                members.forEachIndexed { index, member ->
                    val isSelected = member.userId == selectedMemberId
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { onMemberSelected(member.userId) }
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("${member.name} ${member.surname}", fontWeight = FontWeight.Medium)
                        Text(
                            member.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val unitsText = member.unitAssignments.orEmpty()
                            .joinToString { it.organizationalUnitName }
                            .ifBlank { "Vienetų nėra" }
                        Text(
                            unitsText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (index != members.lastIndex) HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun MemberDetailSection(
    member: MemberDto?,
    isLoading: Boolean,
    isSaving: Boolean,
    onAssignRole: () -> Unit,
    onEditRole: (MemberLeadershipRoleDto) -> Unit,
    onRemoveRole: (MemberLeadershipRoleDto) -> Unit,
    onAssignRank: () -> Unit,
    onRemoveRank: (MemberRankDto) -> Unit
) {
    SectionCard(title = "Nario detalės") {
        when {
            isLoading -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            member == null -> Text(
                "Pasirinkite narį, kad matytumėte daugiau informacijos.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            else -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${member.name} ${member.surname}", style = MaterialTheme.typography.titleLarge)
                    Text(member.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    member.phone?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                MemberUnitAssignmentsBlock(member = member)

                RoleBlock(
                    roles = member.leadershipRoles,
                    isSaving = isSaving,
                    onAdd = onAssignRole,
                    onEdit = onEditRole,
                    onRemove = onRemoveRole
                )

                RankBlock(
                    ranks = member.ranks,
                    isSaving = isSaving,
                    onAdd = onAssignRank,
                    onRemove = onRemoveRank
                )
            }
        }
    }
}

@Composable
private fun MemberUnitAssignmentsBlock(member: MemberDto) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Vienetai", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (member.unitAssignments.isNullOrEmpty()) {
            Text("Vienetų nėra", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            member.unitAssignments.forEach { assignment ->
                Text(
                    "${assignment.organizationalUnitName} (${assignmentTypeLabel(assignment.assignmentType)})",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Pareigos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Button(onClick = onAdd, enabled = !isSaving) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pridėti")
            }
        }

        if (roles.isEmpty()) {
            Text("Pareigų nėra", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            roles.forEachIndexed { index, role ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        Row {
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
                    if (index != roles.lastIndex) HorizontalDivider()
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Laipsniai", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Button(onClick = onAdd, enabled = !isSaving) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pridėti")
            }
        }

        if (ranks.isEmpty()) {
            Text("Laipsnių nėra", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            ranks.forEachIndexed { index, rank ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
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
                if (index != ranks.lastIndex) HorizontalDivider()
            }
        }
    }
}

@Composable
private fun TuntasSummaryRow(
    tuntas: TuntasDto,
    showActions: Boolean,
    isSaving: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(tuntas.name, fontWeight = FontWeight.SemiBold)
                if (tuntas.krastas.isNotBlank()) {
                    Text(tuntas.krastas, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (tuntas.contactEmail.isNotBlank()) {
                    Text(
                        tuntas.contactEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            StatusChip(status = tuntas.status)
        }

        if (showActions) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onApprove,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Patvirtinti")
                }
                OutlinedButton(
                    onClick = onReject,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Atmesti")
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (text, color) = when (status) {
        "PENDING" -> "Laukiama" to MaterialTheme.colorScheme.tertiary
        "ACTIVE" -> "Aktyvus" to MaterialTheme.colorScheme.primary
        "REJECTED" -> "Atmestas" to MaterialTheme.colorScheme.error
        "SUSPENDED" -> "Sustabdytas" to MaterialTheme.colorScheme.error
        else -> status to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.SemiBold
    )
}

private fun statusText(status: String): String = when (status) {
    "PENDING" -> "Laukiama"
    "ACTIVE" -> "Aktyvus"
    "REJECTED" -> "Atmestas"
    "SUSPENDED" -> "Sustabdytas"
    else -> status
}

private fun termStatusLabel(status: String): String = when (status) {
    "ACTIVE" -> "Aktyvus"
    "COMPLETED" -> "Baigtas"
    "RESIGNED" -> "Atsistatydinta"
    else -> status
}

private fun assignmentTypeLabel(type: String): String = when (type) {
    "MEMBER" -> "Narys"
    else -> type
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
                    ExposedDropdownMenu(
                        expanded = roleExpanded,
                        onDismissRequest = { roleExpanded = false }
                    ) {
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
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
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
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
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
                    ExposedDropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
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
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
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
