package lt.skautai.android.ui.members

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.*
import lt.skautai.android.ui.common.MetadataRow
import lt.skautai.android.ui.common.codeLabel
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiConfirmDialog
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiSectionHeader
import lt.skautai.android.ui.common.SkautaiSurfaceRole
import lt.skautai.android.ui.common.SkautaiTextField
import lt.skautai.android.ui.common.skautaiDividerTone
import lt.skautai.android.ui.common.skautaiOverlayTone

@Composable
fun MemberDetailScreen(
    userId: String,
    onBack: () -> Unit,
    onUnitClick: (String) -> Unit = {},
    onInventoryClick: (String) -> Unit = {},
    viewModel: MemberDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
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
        SkautaiConfirmDialog(
            title = "Šalinti iš tunto?",
            message = "Narys bus pašalintas iš tunto. Taip pat bus uždarytos jo pareigos ir vienetų narystės šiame tunte.",
            confirmText = "Šalinti iš tunto",
            dismissText = "Atšaukti",
            isDanger = true,
            enabled = !uiState.isSaving,
            onConfirm = { viewModel.removeMember(userId) },
            onDismiss = viewModel::hideRemoveMemberDialog
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

    if (uiState.showTransferTuntininkasDialog) {
        TransferTuntininkasDialog(
            members = uiState.availableSuccessors,
            selectedSuccessorUserId = uiState.selectedSuccessorUserId,
            isSaving = uiState.isSaving,
            onSuccessorSelected = viewModel::onSuccessorSelected,
            onConfirm = { viewModel.transferTuntininkas(userId) },
            onDismiss = viewModel::hideTransferTuntininkasDialog
        )
    }

    pendingStepDownRole?.let { role ->
        SkautaiConfirmDialog(
            title = "Atsistatydinti iš pareigų?",
            message = buildString {
                append("Bus uždarytos pareigos ")
                append(displayRoleName(role.roleName))
                role.organizationalUnitName?.let {
                    append(" vienete ")
                    append(it)
                }
                append(".")
            },
            confirmText = when {
                isTuntininkasRoleName(role.roleName) -> "Perleisti pareigas"
                isPrincipalUnitLeaderRoleName(role.roleName) -> "Pateikti prašymą"
                else -> "Atsistatydinti"
            },
            dismissText = "Atšaukti",
            isDanger = true,
            enabled = !uiState.isSaving,
            onConfirm = {
                pendingStepDownRole = null
                if (isTuntininkasRoleName(role.roleName)) {
                    viewModel.openTransferTuntininkasDialog(userId, role.id)
                } else if (isPrincipalUnitLeaderRoleName(role.roleName)) {
                    viewModel.requestLeadershipResignation(role.id)
                } else {
                    viewModel.stepDownLeadershipRole(userId, role.id)
                }
            },
            onDismiss = { pendingStepDownRole = null }
        )
    }

    pendingRoleRemoval?.let { role ->
        SkautaiConfirmDialog(
            title = "Šalinti pareigas?",
            message = buildString {
                append("Pareigos ")
                append(displayRoleName(role.roleName))
                role.organizationalUnitName?.let {
                    append(" vienete ")
                    append(it)
                }
                append(" bus pašalintos.")
            },
            confirmText = "Šalinti",
            dismissText = "Atšaukti",
            isDanger = true,
            enabled = !uiState.isSaving,
            onConfirm = {
                pendingRoleRemoval = null
                viewModel.removeLeadershipRole(userId, role.id)
            },
            onDismiss = { pendingRoleRemoval = null }
        )
    }

    pendingRankRemoval?.let { rank ->
        SkautaiConfirmDialog(
            title = "Šalinti laipsnį?",
            message = "Laipsnis ${displayRoleName(rank.roleName)} bus pašalintas iš šio nario.",
            confirmText = "Šalinti",
            dismissText = "Atšaukti",
            isDanger = true,
            enabled = !uiState.isSaving,
            onConfirm = {
                pendingRankRemoval = null
                viewModel.removeRank(userId, rank.id)
            },
            onDismiss = { pendingRankRemoval = null }
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
                    activeReservationsCount = uiState.activeReservationsCount,
                    activeRequestsCount = uiState.activeRequestsCount,
                    onMoveMember = viewModel::openMoveMemberDialog,
                    onUnitClick = onUnitClick,
                    onInventoryClick = { onInventoryClick(userId) },
                    onCall = { phone ->
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                    },
                    onEmail = { email ->
                        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
                    },
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
    activeReservationsCount: Int,
    activeRequestsCount: Int,
    onMoveMember: () -> Unit,
    onUnitClick: (String) -> Unit,
    onInventoryClick: () -> Unit,
    onCall: (String) -> Unit,
    onEmail: (String) -> Unit,
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
        MemberProfileCard(
            member = member,
            onCall = onCall,
            onEmail = onEmail,
            onInventoryClick = onInventoryClick,
            activeReservationsCount = activeReservationsCount,
            activeRequestsCount = activeRequestsCount
        )

        MemberActivitySummarySection(
            activeReservationsCount = activeReservationsCount,
            activeRequestsCount = activeRequestsCount
        )

        MemberOrganizationCard(
            assignments = member.unitAssignments.orEmpty(),
            roles = member.leadershipRoles,
            ranks = member.ranks,
            history = member.leadershipRoleHistory,
            canMoveMember = canMoveMember,
            isSaving = isSaving,
            isCurrentUser = isCurrentUser,
            canManageRoles = canManageRoles,
            onMoveMember = onMoveMember,
            onUnitClick = onUnitClick,
            onAssignRole = onAssignRole,
            onEditRole = onEditRole,
            onStepDownRole = onStepDownRole,
            onRemoveRole = onRemoveRole,
            onAssignRank = onAssignRank,
            onRemoveRank = onRemoveRank
        )
    }
}

@Composable
private fun MemberProfileCard(
    member: MemberDto,
    onCall: (String) -> Unit,
    onEmail: (String) -> Unit,
    onInventoryClick: () -> Unit,
    activeReservationsCount: Int,
    activeRequestsCount: Int
) {
    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "${member.name} ${member.surname}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = member.primaryMemberSubtitle(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CompactMemberMetric("Vienetai", member.unitAssignments.orEmpty().size.toString(), Modifier.weight(1f))
                CompactMemberMetric(
                    "Pareigos",
                    member.leadershipRoles.count { it.termStatus == "ACTIVE" }.toString(),
                    Modifier.weight(1f)
                )
                CompactMemberMetric("Aktyvu", (activeReservationsCount + activeRequestsCount).toString(), Modifier.weight(1f))
            }
            HorizontalDivider(color = skautaiDividerTone())
            MetadataRow("Prisijungė", member.joinedAt.take(10))
            if (member.email.isNotBlank()) {
                DetailListRow(
                    icon = Icons.Default.Email,
                    title = "El. paštas",
                    subtitle = member.email,
                    onClick = { onEmail(member.email) }
                )
            }
            member.phone?.let {
                DetailListRow(
                    icon = Icons.Default.Phone,
                    title = "Telefonas",
                    subtitle = it,
                    onClick = { onCall(it) }
                )
            }
            OutlinedButton(
                onClick = onInventoryClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Rodyti nario inventorių")
            }
        }
    }
}

@Composable
private fun CompactMemberMetric(label: String, value: String, modifier: Modifier = Modifier) {
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
private fun MemberOrganizationCard(
    assignments: List<MemberUnitAssignmentDto>,
    roles: List<MemberLeadershipRoleDto>,
    ranks: List<MemberRankDto>,
    history: List<MemberLeadershipRoleDto>,
    canMoveMember: Boolean,
    isSaving: Boolean,
    isCurrentUser: Boolean,
    canManageRoles: Boolean,
    onMoveMember: () -> Unit,
    onUnitClick: (String) -> Unit,
    onAssignRole: () -> Unit,
    onEditRole: (MemberLeadershipRoleDto) -> Unit,
    onStepDownRole: (MemberLeadershipRoleDto) -> Unit,
    onRemoveRole: (MemberLeadershipRoleDto) -> Unit,
    onAssignRank: () -> Unit,
    onRemoveRank: (MemberRankDto) -> Unit
) {
    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Organizacija", style = MaterialTheme.typography.titleLarge)

            SkautaiSectionHeader(
                title = "Vienetai",
                subtitle = "${assignments.size} aktyvūs priskyrimai",
                actionLabel = if (canMoveMember) "Perkelti" else null,
                actionIcon = Icons.Default.SwapHoriz,
                onAction = if (canMoveMember && !isSaving) onMoveMember else null
            )
            if (assignments.isEmpty()) {
                DetailEmptyRow(Icons.Default.Groups, "Aktyvių vienetų nėra")
            } else {
                assignments.forEach { assignment ->
                    DetailListRow(
                        icon = Icons.Default.Groups,
                        title = assignment.organizationalUnitName,
                        subtitle = assignmentTypeLabel(assignment.assignmentType),
                        trailing = assignment.joinedAt.take(10),
                        onClick = { onUnitClick(assignment.organizationalUnitId) }
                    )
                }
            }

            HorizontalDivider(color = skautaiDividerTone())

            SkautaiSectionHeader(
                title = "Pareigos",
                subtitle = "${roles.count { it.termStatus == "ACTIVE" }} aktyvios",
                actionLabel = if (canManageRoles) "Pridėti" else null,
                actionIcon = Icons.Default.Add,
                onAction = if (canManageRoles && !isSaving) onAssignRole else null
            )
            if (roles.isEmpty()) {
                DetailEmptyRow(Icons.Default.Groups, "Pareigų nėra")
            } else {
                roles.forEach { role ->
                    MemberRoleRow(
                        role = role,
                        isSaving = isSaving,
                        isCurrentUser = isCurrentUser,
                        canManageRoles = canManageRoles,
                        onEditRole = onEditRole,
                        onStepDownRole = onStepDownRole,
                        onRemoveRole = onRemoveRole
                    )
                }
            }

            HorizontalDivider(color = skautaiDividerTone())

            SkautaiSectionHeader(
                title = "Laipsniai",
                subtitle = "${ranks.size} priskirti",
                actionLabel = if (canManageRoles) "Pridėti" else null,
                actionIcon = Icons.Default.Add,
                onAction = if (canManageRoles && !isSaving) onAssignRank else null
            )
            if (ranks.isEmpty()) {
                DetailEmptyRow(Icons.Default.Groups, "Laipsnių nėra")
            } else {
                ranks.forEach { rank ->
                    MemberRankRow(
                        rank = rank,
                        isSaving = isSaving,
                        canManageRoles = canManageRoles,
                        onRemoveRank = onRemoveRank
                    )
                }
            }

            if (history.isNotEmpty()) {
                HorizontalDivider(color = skautaiDividerTone())
                SkautaiSectionHeader(
                    title = "Pareigų istorija",
                    subtitle = "${history.size} įrašai"
                )
                history.forEach { role -> MemberHistoryRow(role) }
            }
        }
    }
}

@Composable
private fun MemberActivitySummarySection(
    activeReservationsCount: Int,
    activeRequestsCount: Int
) {
    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SkautaiSectionHeader(
                title = "Aktyvumas",
                subtitle = "Rezervacijos ir prašymai, susieti su nariu"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ActivityMetric("Rezervacijos", activeReservationsCount.toString(), Modifier.weight(1f))
                ActivityMetric("Prašymai", activeRequestsCount.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ActivityMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MemberRoleRow(
    role: MemberLeadershipRoleDto,
    isSaving: Boolean,
    isCurrentUser: Boolean,
    canManageRoles: Boolean,
    onEditRole: (MemberLeadershipRoleDto) -> Unit,
    onStepDownRole: (MemberLeadershipRoleDto) -> Unit,
    onRemoveRole: (MemberLeadershipRoleDto) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(displayRoleName(role.roleName), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                role.organizationalUnitName?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = termStatusLabel(role.termStatus),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (role.termStatus == "ACTIVE") MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.widthIn(min = 88.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCurrentUser && role.termStatus == "ACTIVE") {
                    TextButton(onClick = { onStepDownRole(role) }, enabled = !isSaving) {
                        Text(
                            when {
                                isTuntininkasRoleName(role.roleName) -> "Perleisti"
                                isPrincipalUnitLeaderRoleName(role.roleName) -> "Prašyti atsistatydinti"
                                else -> "Atsistatydinti"
                            }
                        )
                    }
                }
                if (canManageRoles) {
                    IconButton(onClick = { onEditRole(role) }, enabled = !isSaving, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Redaguoti pareigas")
                    }
                    IconButton(onClick = { onRemoveRole(role) }, enabled = !isSaving, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Šalinti pareigas", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberRankRow(
    rank: MemberRankDto,
    isSaving: Boolean,
    canManageRoles: Boolean,
    onRemoveRank: (MemberRankDto) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(displayRoleName(rank.roleName), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (canManageRoles) {
                IconButton(onClick = { onRemoveRank(rank) }, enabled = !isSaving, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Šalinti laipsnį", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun MemberHistoryRow(role: MemberLeadershipRoleDto) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = skautaiOverlayTone(SkautaiSurfaceRole.Muted)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(displayRoleName(role.roleName), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
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
                        value = selectedRole?.name?.let(::displayRoleName) ?: "Pasirinkite pareigas",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pareigos") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(roleExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                        roles.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(displayRoleName(role.name)) },
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
                    value = selectedRole?.name?.let(::displayRoleName) ?: "Pasirinkite laipsnį",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Laipsnis") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    roles.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(displayRoleName(role.name)) },
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
        title = { Text("Perkelti narį") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Bus pakeista tik pagrindinė nario narystė to paties vieneto tipo ribose. Pareigos ir laipsniai nebus keičiami.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedUnit?.name ?: "Pasirinkite vienetą",
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Atšaukti") } }
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
                        value = termStatusLabel(selectedTermStatus),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Statusas") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(statusExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Atšaukti") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferTuntininkasDialog(
    members: List<MemberDto>,
    selectedSuccessorUserId: String,
    isSaving: Boolean,
    onSuccessorSelected: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedMember = members.find { it.userId == selectedSuccessorUserId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Perleisti tuntininko pareigas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Pasirinktam nariui bus paliktas tik Vadovo laipsnis, o visos kitos aktyvios vadovavimo pareigos bus nuimtos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedMember?.let { "${it.name} ${it.surname}" } ?: "Pasirinkite narį",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Naujas tuntininkas") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        members.forEach { member ->
                            DropdownMenuItem(
                                text = { Text("${member.name} ${member.surname}") },
                                onClick = {
                                    onSuccessorSelected(member.userId)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = selectedSuccessorUserId.isNotBlank() && !isSaving
            ) {
                Text("Perleisti")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Atšaukti") } }
    )
}


@Composable
private fun DetailListRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    trailing: String? = null,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
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
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            trailing?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DetailEmptyRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    DetailListRow(
        icon = icon,
        title = text,
        subtitle = "Įrašų šiame skyriuje nėra"
    )
}

private fun MemberDto.primaryMemberSubtitle(): String {
    val activeRole = leadershipRoles.firstOrNull { it.termStatus == "ACTIVE" }?.roleName?.let(::displayRoleName)
    val unit = unitAssignments.orEmpty().firstOrNull()?.organizationalUnitName
    return listOfNotNull(activeRole, unit, email.takeIf { it.isNotBlank() }).joinToString(" · ")
        .ifBlank { "Nario informacija" }
}

private fun assignmentTypeLabel(type: String): String = when (type) {
    "MEMBER" -> "Narys"
    "VADOVO_PADEJEJAS" -> "Vadovo padėjėjas"
    else -> codeLabel(type)
}

private fun isTuntininkasRoleName(roleName: String): Boolean = roleName == "Tuntininkas"

private fun isPrincipalUnitLeaderRoleName(roleName: String): Boolean = roleName in setOf(
    "Draugininkas",
    "Gildijos pirmininkas",
    "Vyr. skautu draugoves draugininkas",
    "Vyr. skautu burelio pirmininkas",
    "Vyr. skauciu draugoves draugininkas",
    "Vyr. skauciu burelio pirmininkas"
)

private fun termStatusLabel(status: String): String = when (status) {
    "ACTIVE" -> "Aktyvus"
    "COMPLETED" -> "Baigtas"
    "RESIGNED" -> "Atsistatydinta"
    else -> codeLabel(status)
}



