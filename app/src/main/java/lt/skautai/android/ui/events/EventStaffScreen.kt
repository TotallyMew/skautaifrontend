package lt.skautai.android.ui.events

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.EventRoleDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiStatusPill

private enum class StaffPanelMode { Slot, Extra }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventStaffScreen(
    eventId: String,
    onBack: () -> Unit,
    onOpenPastovyklės: (String) -> Unit,
    viewModel: EventStaffViewModel = hiltViewModel()
) {
    val onOpenPastovyklEs = onOpenPastovyklės
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var activeSlot by remember { mutableStateOf<EventStaffSlotUiModel?>(null) }
    var pendingRemovalSlot by remember { mutableStateOf<EventStaffSlotUiModel?>(null) }
    var panelMode by remember { mutableStateOf(StaffPanelMode.Slot) }
    var isCoreSectionExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(eventId) { viewModel.load(eventId) }

    LaunchedEffect((uiState as? EventStaffUiState.Success)?.error) {
        (uiState as? EventStaffUiState.Success)?.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val state = uiState
    val readOnly = (state as? EventStaffUiState.Success)?.event?.status?.let(::isEventReadOnlyStatus) == true
    val canManage = !readOnly && (
        "events.manage" in permissions ||
            (state as? EventStaffUiState.Success)?.event?.eventRoles
                ?.filter { it.userId == state.currentUserId }
                ?.any { it.role == "VIRSININKAS" } == true
        )

    if (canManage && state is EventStaffUiState.Success && panelMode == StaffPanelMode.Slot && activeSlot != null) {
        ModalBottomSheet(onDismissRequest = { activeSlot = null }) {
            SlotAssignmentPanel(
                title = activeSlot!!.title,
                subtitle = activeSlot!!.subtitle,
                members = eligibleStaffMembersForSlot(state, activeSlot!!),
                isWorking = state.isWorking,
                onAssign = { userId ->
                    viewModel.assignToSlot(eventId, activeSlot!!, userId)
                    activeSlot = null
                }
            )
        }
    }

    if (canManage && state is EventStaffUiState.Success && panelMode == StaffPanelMode.Extra) {
        ModalBottomSheet(onDismissRequest = { panelMode = StaffPanelMode.Slot }) {
            ExtraRoleAssignmentPanel(
                members = eligibleAdditionalRoleMembers(state),
                isWorking = state.isWorking,
                onAssign = { userId, role ->
                    viewModel.assignAdditionalRole(eventId, userId, role)
                    panelMode = StaffPanelMode.Slot
                }
            )
        }
    }

    if (pendingRemovalSlot != null) {
        AlertDialog(
            onDismissRequest = { pendingRemovalSlot = null },
            title = { Text("Nuimti nuo pareigu?") },
            text = {
                Text(
                    pendingRemovalSlot?.assignedUserName?.let { assignedUser ->
                        "$assignedUser bus nuimtas nuo \"${pendingRemovalSlot?.title}\"."
                    } ?: "Sis slotas bus isvalytas."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRemovalSlot?.let { viewModel.removeFromSlot(eventId, it) }
                        pendingRemovalSlot = null
                    }
                ) {
                    Text("Nuimti")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemovalSlot = null }) {
                    Text("Atsaukti")
                }
            }
        )
    }

    EventScreenScaffold(
        title = "Stabas",
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            if (canManage) {
                ExtendedFloatingActionButton(
                    onClick = {
                        panelMode = StaffPanelMode.Extra
                        activeSlot = null
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Papildoma pareiga")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state) {
                is EventStaffUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is EventStaffUiState.Error -> SkautaiErrorState(
                    message = state.message,
                    onRetry = { viewModel.load(eventId) },
                    modifier = Modifier.align(Alignment.Center)
                )
                is EventStaffUiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            EventDetailHero(
                                event = state.event,
                                subtitle = "Stabas / ${state.event.eventRoles.size} nariai",
                                metrics = listOf(
                                    "Pagrindines" to state.coreSlots.count { it.assignedUserName != null }.toString(),
                                    "Programa" to state.programSlots.count { it.assignedUserName != null }.toString(),
                                    "PastovyklEs" to state.pastovykleSlots.count { it.assignedUserName != null }.toString()
                                )
                            )
                        }
                        item {
                            EventDetailSection(
                                title = "Pagrindines pareigos",
                                subtitle = "Renginio valdymo komanda ir atsakomybes.",
                                actionLabel = if (isCoreSectionExpanded) "Suskleisti" else "Rodyti",
                                onAction = { isCoreSectionExpanded = !isCoreSectionExpanded }
                            ) {
                                if (isCoreSectionExpanded) {
                                    state.coreSlots.forEach { slot ->
                                        StaffSlotCard(
                                            slot = slot,
                                            canManage = canManage,
                                            isWorking = state.isWorking,
                                            isSelected = activeSlot?.id == slot.id && panelMode == StaffPanelMode.Slot,
                                            onSelect = {
                                                panelMode = StaffPanelMode.Slot
                                                activeSlot = slot
                                            },
                                            onRemove = { pendingRemovalSlot = slot },
                                            onOpenPastovyklEs = { onOpenPastovyklEs(eventId) }
                                        )
                                    }
                                } else {
                                    EmptyStateText("Skyrius suskleistas, kad greiciau pasiektum pastovyklas.")
                                }
                            }
                        }
                        item {
                            EventDetailSection(
                                title = "Programa",
                                subtitle = "Programos atsakingi pagal amziaus grupes."
                            ) {
                                state.programSlots.forEach { slot ->
                                    StaffSlotCard(
                                        slot = slot,
                                        canManage = canManage,
                                        isWorking = state.isWorking,
                                        isSelected = activeSlot?.id == slot.id && panelMode == StaffPanelMode.Slot,
                                        onSelect = {
                                            panelMode = StaffPanelMode.Slot
                                            activeSlot = slot
                                        },
                                        onRemove = { pendingRemovalSlot = slot },
                                        onOpenPastovyklEs = { onOpenPastovyklEs(eventId) }
                                    )
                                }
                            }
                        }
                        item {
                            EventDetailSection(
                                title = "Pastovykliu vadovai",
                                subtitle = "Vadovu slotai atsiranda is atskiro pastovyklu puslapio.",
                                actionLabel = "Tvarkyti",
                                onAction = { onOpenPastovyklEs(eventId) }
                            ) {
                                if (state.pastovykleSlots.isEmpty()) {
                                    EmptyStateText("Pastovyklu dar nera. Sukurk jas atskirame puslapyje.")
                                } else {
                                    state.pastovykleSlots.forEach { slot ->
                                        StaffSlotCard(
                                            slot = slot,
                                            canManage = canManage,
                                            isWorking = state.isWorking,
                                            isSelected = activeSlot?.id == slot.id && panelMode == StaffPanelMode.Slot,
                                            onSelect = {
                                                panelMode = StaffPanelMode.Slot
                                                activeSlot = slot
                                            },
                                            onRemove = { pendingRemovalSlot = slot },
                                            onOpenPastovyklEs = { onOpenPastovyklEs(eventId) }
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            EventDetailSection(
                                title = "Papildomos pareigos",
                                subtitle = "Papildomi vadovai ir savanoriai, kurie nepriklauso fiksuotiems slotams."
                            ) {
                                if (state.additionalRoles.isEmpty()) {
                                    EmptyStateText("Papildomu stabo pareigu dar nera.")
                                } else {
                                    state.additionalRoles.forEach { role ->
                                        AdditionalRoleRow(
                                            role = role,
                                            canManage = canManage,
                                            isWorking = state.isWorking,
                                            onRemove = { viewModel.removeRole(eventId, role.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun eligibleStaffMembers(members: List<MemberDto>): List<MemberDto> {
    val vadovasOrHigherRanks = setOf(
        "Vadovas",
        "Tuntininkas",
        "Tuntininko pavaduotojas",
        "Inventorininkas",
        "Draugininkas",
        "Pirmininkas",
        "Pavaduotojas"
    )
    return members.filter { member ->
        member.ranks.any { it.roleName in vadovasOrHigherRanks } ||
            member.leadershipRoles.any { it.termStatus == "ACTIVE" && it.leftAt == null }
    }
}

private fun eligibleStaffMembersForSlot(
    state: EventStaffUiState.Success,
    slot: EventStaffSlotUiModel
): List<MemberDto> {
    return eligibleStaffMembers(state.members).filter { member ->
        !memberHasAnotherStaffRole(member.userId, state.event, excludingSlot = slot) &&
            memberEligibleForPastovykleAgeGroup(member, slot.pastovykleAgeGroup)
    }
}

private fun eligibleAdditionalRoleMembers(state: EventStaffUiState.Success): List<MemberDto> {
    return eligibleStaffMembers(state.members).filter { member ->
        !memberHasAnotherStaffRole(member.userId, state.event)
    }
}

@Composable
private fun StaffSlotCard(
    slot: EventStaffSlotUiModel,
    canManage: Boolean,
    isWorking: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
    onOpenPastovyklEs: () -> Unit
) {
    val isAssigned = slot.assignedUserName != null
    val isPastovykleLeader = slot.role == "PASTOVYKLE_LEADER"
    val pastovyklePalette = if (isPastovykleLeader) pastovykleAgeGroupPalette(slot.pastovykleAgeGroup) else null
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        !isAssigned -> MaterialTheme.colorScheme.surfaceBright
        isPastovykleLeader -> pastovyklePalette?.cardTone ?: MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        !isAssigned -> MaterialTheme.colorScheme.outline
        isPastovykleLeader -> pastovyklePalette?.accent ?: MaterialTheme.colorScheme.outlineVariant
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canManage && !slot.isLocked, onClick = onSelect),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = slot.title,
                        style = if (isPastovykleLeader) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = slot.subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isSelected) {
                    Text(
                        "Parenkama",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (isPastovykleLeader) {
                SkautaiStatusPill(
                    label = pastovykleAgeGroupLabel(slot.pastovykleAgeGroup),
                    containerColor = pastovyklePalette?.iconTone ?: MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = pastovyklePalette?.accent ?: MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            if (isAssigned) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = slot.assignedUserName.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (canManage && !slot.isLocked) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onSelect, enabled = !isWorking) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Keisti"
                                )
                            }
                            IconButton(onClick = onRemove, enabled = !isWorking) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Nuimti",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAddAlt1,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Dar nepriskirta",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pasirink vadova siam slotui.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isPastovykleLeader) {
                OutlinedButton(
                    onClick = onOpenPastovyklEs,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tvarkyti pastovykle")
                }
            }

            if (canManage && !slot.isLocked) {
                if (!isAssigned) {
                    Button(
                        onClick = onSelect,
                        enabled = !isWorking,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Priskirti")
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotAssignmentPanel(
    title: String,
    subtitle: String,
    members: List<MemberDto>,
    isWorking: Boolean,
    onAssign: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedUserId by remember { mutableStateOf<String?>(null) }
    val filteredMembers = remember(members, searchQuery) {
        val query = searchQuery.trim()
        members
            .sortedBy { it.fullName().lowercase() }
            .filter { member ->
                query.isBlank() ||
                    member.fullName().contains(query, ignoreCase = true) ||
                    member.email.contains(query, ignoreCase = true)
            }
    }

    EventDetailSection(
        title = "Priskirti: $title",
        subtitle = subtitle
    ) {
        EventFormEyebrow("Pasirinkimas")
        EventFormSupportText("Pirma rask tinkama zmogu, tada patvirtink vienu pagrindiniu veiksmu apacioje.")
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Ieskoti zmogaus") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = eventFormFieldColors()
        )
        SkautaiCard(modifier = Modifier.fillMaxWidth(), tonal = MaterialTheme.colorScheme.surfaceContainerLow) {
            LazyColumn(
                modifier = Modifier.heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(10.dp)
            ) {
                if (filteredMembers.isEmpty()) {
                    item {
                        EmptyStateText("Pagal sia paieska nariu nerasta.")
                    }
                } else {
                    items(filteredMembers, key = { it.userId }) { member ->
                        MemberSelectionRow(
                            name = member.fullName(),
                            email = member.email,
                            selected = member.userId == selectedUserId,
                            onClick = { selectedUserId = member.userId }
                        )
                    }
                }
            }
        }
        EventPrimaryButton(
            text = "Priskirti",
            onClick = { selectedUserId?.let(onAssign) },
            enabled = !isWorking && selectedUserId != null
        )
    }
}

@Composable
private fun ExtraRoleAssignmentPanel(
    members: List<MemberDto>,
    isWorking: Boolean,
    onAssign: (String, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedUserId by remember { mutableStateOf<String?>(null) }
    var selectedRole by remember { mutableStateOf("VADOVAS") }
    val filteredMembers = remember(members, searchQuery) {
        val query = searchQuery.trim()
        members.sortedBy { it.fullName().lowercase() }
            .filter { member ->
                query.isBlank() ||
                    member.fullName().contains(query, ignoreCase = true) ||
                    member.email.contains(query, ignoreCase = true)
            }
    }

    EventDetailSection(
        title = "Prideti papildoma pareiga",
        subtitle = "Naudok tik papildomoms rolems, kurios neturi atskiro sloto."
    ) {
        EventFormEyebrow("Papildoma role")
        EventFormSupportText("Pirma pasirink pareiga, tada konkretu zmogu. Galutinis veiksmas turi buti tik vienas.")
        DropdownField(
            label = "Pareiga",
            value = eventRoleLabel(selectedRole),
            options = listOf("VADOVAS" to "Vadovas", "SAVANORIS" to "Savanoris"),
            onSelect = { selectedRole = it }
        )
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Ieskoti zmogaus") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = eventFormFieldColors()
        )
        SkautaiCard(modifier = Modifier.fillMaxWidth(), tonal = MaterialTheme.colorScheme.surfaceContainerLow) {
            LazyColumn(
                modifier = Modifier.heightIn(max = 240.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(10.dp)
            ) {
                if (filteredMembers.isEmpty()) {
                    item {
                        EmptyStateText("Pagal sia paieska nariu nerasta.")
                    }
                } else {
                    items(filteredMembers, key = { it.userId }) { member ->
                        MemberSelectionRow(
                            name = member.fullName(),
                            email = member.email,
                            selected = member.userId == selectedUserId,
                            onClick = { selectedUserId = member.userId }
                        )
                    }
                }
            }
        }
        EventPrimaryButton(
            text = "Prideti pareiga",
            onClick = { selectedUserId?.let { onAssign(it, selectedRole) } },
            enabled = !isWorking && selectedUserId != null
        )
    }
}

@Composable
private fun MemberSelectionRow(
    name: String,
    email: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = email,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
            )
        }
    }
}

@Composable
private fun AdditionalRoleRow(
    role: EventRoleDto,
    canManage: Boolean,
    isWorking: Boolean,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(role.userName ?: role.userId, fontWeight = FontWeight.SemiBold)
            Text(
                buildString {
                    append(eventRoleLabel(role.role))
                    role.targetGroup?.let { append(" / $it") }
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (canManage && role.role != "VIRSININKAS") {
            TextButton(onClick = onRemove, enabled = !isWorking) {
                Text("Salinti")
            }
        }
    }
    HorizontalDivider()
}

private fun eventRoleLabel(role: String): String = when (role) {
    "VIRSININKAS" -> "Virsininkas"
    "KOMENDANTAS" -> "Komendantas"
    "UKVEDYS" -> "Ukvedys"
    "PROGRAMERIS" -> "Programeris"
    "MAISTININKAS" -> "Maistininkas"
    "VADOVAS" -> "Vadovas"
    "SAVANORIS" -> "Savanoris"
    "PASTOVYKLE_LEADER" -> "PastovyklEs vadovas"
    else -> role
}
