package lt.skautai.android.ui.units

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Tag
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
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.remote.UnitMembershipDto
import lt.skautai.android.ui.common.MetadataRow
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiConfirmDialog
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiSectionHeader
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.SkautaiSummaryCard
import lt.skautai.android.ui.common.isUnitLeader
import lt.skautai.android.ui.members.displayRoleName
import lt.skautai.android.util.canManageUnits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitDetailScreen(
    unitId: String,
    onBack: () -> Unit,
    onEditClick: (String) -> Unit,
    onMemberClick: (String) -> Unit = {},
    viewModel: UnitDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    var memberPendingRemoval by remember { mutableStateOf<UnitMembershipDto?>(null) }
    val canManageUnit = permissions.canManageUnits()
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
        SkautaiConfirmDialog(
            title = "Trinti vienetą?",
            message = "Šis veiksmas negrįžtamas. Vienetas bus ištrintas.",
            confirmText = "Trinti",
            dismissText = "Atšaukti",
            isDanger = true,
            enabled = !uiState.isSaving,
            onConfirm = { viewModel.deleteUnit(unitId) },
            onDismiss = viewModel::hideDeleteDialog
        )
    }

    if (uiState.showLeaveDialog) {
        SkautaiConfirmDialog(
            title = "Palikti vienetą?",
            message = "Paliksi šį vienetą, bet liksi tunto nariu. Su šiuo vienetu susieti nario priskyrimai bus uždaryti.",
            confirmText = "Palikti",
            dismissText = "Atšaukti",
            isDanger = true,
            enabled = !uiState.isSaving,
            onConfirm = { viewModel.leaveUnit(unitId) },
            onDismiss = viewModel::hideLeaveDialog
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
        SkautaiConfirmDialog(
            title = "Šalinti narį iš vieneto?",
            message = "Narys ${membership.userName} ${membership.userSurname} bus pašalintas iš šio vieneto.",
            confirmText = "Šalinti",
            dismissText = "Atšaukti",
            isDanger = true,
            enabled = !uiState.isSaving,
            onConfirm = {
                memberPendingRemoval = null
                viewModel.removeUnitMember(unitId, membership.userId)
            },
            onDismiss = { memberPendingRemoval = null }
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
                            Icon(Icons.Default.ExitToApp, contentDescription = "Palikti vienetą")
                        }
                    }
                }
            )
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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        UnitProfileCard(
                            unit = unit,
                            memberCount = uiState.members.size,
                            activeReservationsCount = uiState.activeReservationsCount,
                            activeRequestsCount = uiState.activeRequestsCount
                        )
                    }

                    val leaders = uiState.members.filter { membership ->
                        isUnitLeader(membership, uiState.memberDetails[membership.userId])
                    }
                    val regularMembers = uiState.members.filterNot { membership ->
                        isUnitLeader(membership, uiState.memberDetails[membership.userId])
                    }

                    item {
                        SkautaiSectionHeader(
                            title = "Vadovai",
                            subtitle = "${leaders.size} aktyvios vadovavimo narystės"
                        )
                    }
                    if (leaders.isEmpty()) {
                        item { EmptyMembersCard("Vadovų nėra") }
                    } else {
                        items(leaders, key = { "leader-${it.userId}" }) { membership ->
                            UnitMemberCard(
                                membership = membership,
                                member = uiState.memberDetails[membership.userId],
                                canManageMembers = canManageMembers,
                                onOpen = { onMemberClick(membership.userId) },
                                onRemove = { memberPendingRemoval = membership },
                                isRemoving = uiState.isSaving
                            )
                        }
                    }

                    item {
                        SkautaiSectionHeader(
                            title = "Nariai",
                            subtitle = "${regularMembers.size} aktyvūs priskyrimai",
                            actionLabel = if (canManageMembers) "Priskirti" else null,
                            actionIcon = Icons.Default.Add,
                            onAction = if (canManageMembers && !uiState.isSaving) viewModel::openAssignMemberDialog else null
                        )
                    }
                    if (regularMembers.isEmpty()) {
                        item { EmptyMembersCard("Narių nėra") }
                    } else {
                        items(regularMembers, key = { "member-${it.userId}" }) { membership ->
                            UnitMemberCard(
                                membership = membership,
                                member = uiState.memberDetails[membership.userId],
                                canManageMembers = canManageMembers,
                                onOpen = { onMemberClick(membership.userId) },
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
private fun UnitProfileCard(
    unit: OrganizationalUnitDto,
    memberCount: Int,
    activeReservationsCount: Int,
    activeRequestsCount: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(unit.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            listOfNotNull(
                unitTypeLabel(unit.type),
                unit.subtype?.let(::subtypeLabel),
                unit.acceptedRankName?.let { "Priima: $it" }
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CompactMetric("Nariai", memberCount.toString(), Modifier.weight(1f))
            CompactMetric("Rez.", activeReservationsCount.toString(), Modifier.weight(1f))
            CompactMetric("Praš.", activeRequestsCount.toString(), Modifier.weight(1f))
        }
        UnitMetadataCard(unit = unit)
    }
}

@Composable
private fun UnitMetadataCard(unit: OrganizationalUnitDto) {
    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Metaduomenys", style = MaterialTheme.typography.titleLarge)
            MetadataRow("Tipas", unitTypeLabel(unit.type))
            unit.subtype?.let { MetadataRow("Struktūra", subtypeLabel(it)) }
            unit.acceptedRankName?.let { MetadataRow("Priimamas laipsnis", it) }
            MetadataRow("Inventorius", "${unit.itemCount} daiktai")
            MetadataRow("Sukurta", unit.createdAt.take(10))
        }
    }
}

@Composable
private fun CompactMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun UnitInfoCard(unit: lt.skautai.android.data.remote.OrganizationalUnitDto) {
    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SkautaiSectionHeader(title = "Informacija")
            UnitDetailListRow(
                icon = Icons.Default.AccountTree,
                title = "Tipas",
                subtitle = unitTypeLabel(unit.type)
            )
            unit.subtype?.let {
                UnitDetailListRow(
                    icon = Icons.Default.AccountTree,
                    title = "Struktūra",
                    subtitle = subtypeLabel(it)
                )
            }
            unit.acceptedRankName?.let {
                UnitDetailListRow(
                    icon = Icons.Default.PersonAdd,
                    title = "Priimamas laipsnis",
                    subtitle = it
                )
            }
            UnitDetailListRow(
                icon = Icons.Default.Inventory2,
                title = "Inventorius",
                subtitle = "${unit.itemCount} daiktai"
            )
            MetadataRow("Sukurta", unit.createdAt.take(10))
        }
    }
}

@Composable
private fun UnitActivitySummaryCard(
    activeReservationsCount: Int,
    activeRequestsCount: Int
) {
    SkautaiCard(modifier = Modifier.fillMaxWidth(), tonal = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SkautaiSectionHeader(
                title = "Aktyvumas",
                subtitle = "Vieneto rezervacijos ir prašymai"
            )
            MetadataRow("Aktyvios rezervacijos", activeReservationsCount.toString())
            MetadataRow("Aktyvūs prašymai", activeRequestsCount.toString())
        }
    }
}

@Composable
private fun EmptyMembersCard(text: String) {
    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(18.dp)
        )
    }
}

@Composable
private fun UnitMemberCard(
    membership: UnitMembershipDto,
    member: MemberDto?,
    canManageMembers: Boolean,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    isRemoving: Boolean
) {
    SkautaiCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen,
        tonal = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AccountTree,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "${membership.userName} ${membership.userSurname}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                SkautaiStatusPill(
                    label = resolveUnitMemberRoleLabel(membership, member),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Priskirtas ${membership.joinedAt.take(10)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (canManageMembers) {
                IconButton(onClick = onRemove, enabled = !isRemoving) {
                    Icon(
                        Icons.Default.PersonRemove,
                        contentDescription = "Šalinti narį",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun UnitDetailListRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    val assignmentTypes = listOf("MEMBER" to "Narys", "VADOVO_PADEJEJAS" to "Vadovo padėjėjas")
    val selectedMember = members.find { it.userId == selectedMemberId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Priskirti narį") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (members.isEmpty()) {
                    Text(
                        text = "Visi tunto nariai jau yra šiame vienete.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                ExposedDropdownMenuBox(expanded = memberExpanded, onExpandedChange = { memberExpanded = it }) {
                    OutlinedTextField(
                        value = selectedMember?.let { "${it.name} ${it.surname}" } ?: "Pasirinkite narį",
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
                Text(if (isSaving) "Priskiriama..." else "Priskirti")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Atšaukti") }
        }
    )
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
        "VADOVO_PADEJEJAS" -> "Vadovo padėjėjas"
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
