package lt.skautai.android.ui.units

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.MemberRankDto
import lt.skautai.android.data.remote.UnitMembershipDto
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.members.displayRoleName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitDetailScreen(
    unitId: String,
    onBack: () -> Unit,
    onEditClick: (String) -> Unit,
    viewModel: UnitDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    var memberPendingRemoval by remember { mutableStateOf<UnitMembershipDto?>(null) }
    val canManageUnit = "organizational_units.manage" in permissions
    val canManageMembers = "unit.members.manage:ALL" in permissions ||
        ("unit.members.manage:OWN_UNIT" in permissions && uiState.canCurrentUserManageThisUnit)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(unitId) { viewModel.loadUnit(unitId) }

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) onBack()
    }

    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionError()
        }
    }

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideDeleteDialog,
            title = { Text("Trinti vienetÄ…?") },
            text = { Text("Å is veiksmas negrÄ¯Å¾tamas. Vienetas bus iÅ¡trintas.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteUnit(unitId) },
                    enabled = !uiState.isSaving
                ) { Text("Trinti", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideDeleteDialog) { Text("AtÅ¡aukti") }
            }
        )
    }

    if (uiState.showLeaveDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideLeaveDialog,
            title = { Text("Palikti vieneta?") },
            text = { Text("Paliksi si vieneta, bet liksi tunto nariu. Su siuo vienetu susieti nario priskyrimai bus uzdaryti.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.leaveUnit(unitId) },
                    enabled = !uiState.isSaving
                ) { Text("Palikti", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideLeaveDialog) { Text("Atsaukti") }
            }
        )
    }

    if (uiState.showAssignMemberDialog) {
        AssignMemberDialog(
            members = uiState.availableTuntasMembers,
            selectedMemberId = uiState.selectedMemberId,
            selectedAssignmentType = uiState.selectedAssignmentType,
            isSaving = uiState.isSaving,
            onMemberSelected = viewModel::onMemberSelected,
            onAssignmentTypeSelected = viewModel::onAssignmentTypeSelected,
            onConfirm = { viewModel.assignMember(unitId) },
            onDismiss = viewModel::hideAssignMemberDialog
        )
    }

    memberPendingRemoval?.let { membership ->
        AlertDialog(
            onDismissRequest = { memberPendingRemoval = null },
            title = { Text("Salinti nari is vieneto?") },
            text = {
                Text(
                    "Narys ${membership.userName} ${membership.userSurname} bus pasalintas is sio vieneto."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        memberPendingRemoval = null
                        viewModel.removeUnitMember(unitId, membership.userId)
                    },
                    enabled = !uiState.isSaving
                ) {
                    Text("Salinti", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberPendingRemoval = null }) { Text("Atsaukti") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.unit?.name ?: "Vienetas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atgal")
                    }
                },
                actions = {
                    if (uiState.unit != null && canManageUnit) {
                        IconButton(onClick = { onEditClick(unitId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Redaguoti")
                        }
                        IconButton(onClick = viewModel::showDeleteDialog) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Trinti",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    if (uiState.unit != null && uiState.canCurrentUserLeaveThisUnit) {
                        IconButton(onClick = viewModel::showLeaveDialog) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Palikti vieneta")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.unit != null && canManageMembers) {
                FloatingActionButton(onClick = viewModel::openAssignMemberDialog) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Priskirti narÄ¯")
                }
            }
        },
        snackbarHost = { SkautaiErrorSnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            uiState.error != null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                SkautaiErrorState(
                    message = uiState.error!!,
                    onRetry = { viewModel.loadUnit(unitId) }
                )
            }

            uiState.unit != null -> {
                val unit = uiState.unit!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Informacija",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                HorizontalDivider()
                                DetailRow("Tipas", unitTypeLabel(unit.type))
                                unit.acceptedRankName?.let { DetailRow("Priimamas laipsnis", it) }
                                DetailRow("Sukurta", unit.createdAt.take(10))
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Nariai (${uiState.members.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (uiState.members.isEmpty()) {
                        item {
                            Text(
                                "NariÅ³ nÄ—ra",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(uiState.members) { membership ->
                            UnitMemberCard(
                                membership = membership,
                                member = uiState.memberDetails[membership.userId],
                                canManageMembers = canManageMembers,
                                onRemove = { memberPendingRemoval = membership },
                                isRemoving = uiState.isSaving
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitMemberCard(
    membership: UnitMembershipDto,
    member: MemberDto?,
    canManageMembers: Boolean,
    onRemove: () -> Unit,
    isRemoving: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${membership.userName} ${membership.userSurname}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = resolveUnitMemberRoleLabel(membership, member),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (canManageMembers) {
                IconButton(onClick = onRemove, enabled = !isRemoving) {
                    Icon(
                        Icons.Default.PersonRemove,
                        contentDescription = "Å alinti narÄ¯",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignMemberDialog(
    members: List<MemberDto>,
    selectedMemberId: String,
    selectedAssignmentType: String,
    isSaving: Boolean,
    onMemberSelected: (String) -> Unit,
    onAssignmentTypeSelected: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var memberExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }
    val assignmentTypes = listOf("MEMBER" to "Narys", "VADOVO_PADEJEJAS" to "Vadovo padÄ—jÄ—jas")
    val selectedMember = members.find { it.userId == selectedMemberId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Priskirti narÄ¯") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (members.isEmpty()) {
                    Text(
                        text = "Visi tunto nariai jau yra siame vienete.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                ExposedDropdownMenuBox(expanded = memberExpanded, onExpandedChange = { memberExpanded = it }) {
                    OutlinedTextField(
                        value = selectedMember?.let { "${it.name} ${it.surname}" } ?: "Pasirinkite narÄ¯",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Narys") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(memberExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = memberExpanded, onDismissRequest = { memberExpanded = false }) {
                        members.forEach { m ->
                            DropdownMenuItem(
                                text = { Text("${m.name} ${m.surname}") },
                                onClick = { onMemberSelected(m.userId); memberExpanded = false }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                    OutlinedTextField(
                        value = assignmentTypes.find { it.first == selectedAssignmentType }?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipas") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        assignmentTypes.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { onAssignmentTypeSelected(key); typeExpanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = members.isNotEmpty() && selectedMemberId.isNotBlank() && !isSaving
            ) {
                Text("Priskirti")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("AtÅ¡aukti") }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

private fun resolveUnitMemberRoleLabel(
    membership: UnitMembershipDto,
    member: MemberDto?
): String {
    val activeLeadershipRole = member?.leadershipRoles
        .orEmpty()
        .firstOrNull { role ->
            role.termStatus == "ACTIVE" && role.organizationalUnitId == membership.organizationalUnitId
        }
    if (activeLeadershipRole != null) {
        return displayRoleName(activeLeadershipRole.roleName)
    }

    val memberRank = resolveMemberRankForUnit(membership, member)
    if (memberRank != null) {
        return displayRoleName(memberRank.roleName)
    }

    return when (membership.assignmentType) {
        "MEMBER" -> "Narys"
        "VADOVO_PADEJEJAS" -> "Vadovo padÄ—jÄ—jas"
        else -> membership.assignmentType
    }
}

private fun resolveMemberRankForUnit(
    membership: UnitMembershipDto,
    member: MemberDto?
): MemberRankDto? {
    if (member == null) return null

    val hasMembershipInUnit = member.unitAssignments.orEmpty()
        .any { it.organizationalUnitId == membership.organizationalUnitId }
    if (!hasMembershipInUnit) return null

    return member.ranks.firstOrNull()
}
