package lt.skautai.android.ui.events

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.PastovykleDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiEmptyState
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.SkautaiStatusTone
import lt.skautai.android.ui.theme.ScoutUnitColors
import lt.skautai.android.ui.theme.ScoutUnitPalette

private data class PastovykleAgeGroupOption(
    val code: String,
    val label: String,
    val palette: ScoutUnitPalette
)

private val PastovykleAgeGroups = listOf(
    PastovykleAgeGroupOption("VILKAI", "Vilkai", ScoutUnitColors.Vilkai),
    PastovykleAgeGroupOption("SKAUTAI", "Skautai", ScoutUnitColors.Skautai),
    PastovykleAgeGroupOption("PATYRE_SKAUTAI", "Patyrę skautai", ScoutUnitColors.PatyreSkautai),
    PastovykleAgeGroupOption("VYR_SKAUTAI", "Vyr. skautai", ScoutUnitColors.VyrSkautai),
    PastovykleAgeGroupOption("VYR_SKAUTES", "Vyr. skautės", ScoutUnitColors.VyrSkautes)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventPastovyklėsScreen(
    eventId: String,
    onBack: () -> Unit,
    viewModel: EventPastovyklėsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var editingPastovykle by remember { mutableStateOf<PastovykleDto?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deletingPastovykle by remember { mutableStateOf<PastovykleDto?>(null) }

    LaunchedEffect(eventId) { viewModel.load(eventId) }

    LaunchedEffect((uiState as? EventPastovyklėsUiState.Success)?.error) {
        (uiState as? EventPastovyklėsUiState.Success)?.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val state = uiState
    val readOnly = (state as? EventPastovyklėsUiState.Success)?.event?.status?.let(::isEventReadOnlyStatus) == true
    val canManage = !readOnly && ("events.manage" in permissions ||
        (state as? EventPastovyklėsUiState.Success)?.event?.eventRoles
            ?.any { it.userId == state.currentUserId && it.role == "VIRSININKAS" } == true
        )

    if (state is EventPastovyklėsUiState.Success && showEditor && canManage) {
        PastovykleEditorScreen(
            pastovykle = editingPastovykle,
            event = state.event,
            members = eligibleStaffMembers(state.members),
            isWorking = state.isWorking,
            snackbarHostState = snackbarHostState,
            onBack = { showEditor = false },
            onSave = { name, ageGroup, responsibleUserId, notes ->
                viewModel.savePastovykle(
                    eventId = eventId,
                    pastovykleId = editingPastovykle?.id,
                    name = name,
                    ageGroup = ageGroup,
                    responsibleUserId = responsibleUserId,
                    notes = notes
                )
                showEditor = false
            }
        )
        return
    }

    if (!readOnly) deletingPastovykle?.let { pastovykle ->
        AlertDialog(
            onDismissRequest = { deletingPastovykle = null },
            title = { Text("Ištrinti pastovyklę?") },
            text = { Text("Pastovyklė \"${pastovykle.name}\" bus pašalinta iš šio renginio.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingPastovykle = null
                        viewModel.deletePastovykle(eventId, pastovykle)
                    }
                ) {
                    Text("Ištrinti", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPastovykle = null }) { Text("Uždaryti") }
            }
        )
    }

    EventScreenScaffold(
        title = "Pastovyklės",
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            if (canManage) {
                FloatingActionButton(
                    onClick = {
                        editingPastovykle = null
                        showEditor = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Pridėti pastovyklę")
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
                EventPastovyklėsUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is EventPastovyklėsUiState.Error -> SkautaiErrorState(
                    message = state.message,
                    onRetry = { viewModel.load(eventId) },
                    modifier = Modifier.align(Alignment.Center)
                )
                is EventPastovyklėsUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            EventDetailHero(
                                event = state.event,
                                subtitle = "Pastovyklės / ${state.pastovykles.size} grupės",
                                metrics = listOf(
                                    "Viso" to state.pastovykles.size.toString(),
                                    "Su vadovu" to state.pastovykles.count { it.responsibleUserId != null }.toString(),
                                    "Be vadovo" to state.pastovykles.count { it.responsibleUserId == null }.toString()
                                )
                            )
                        }
                        item {
                            EventDetailSection(
                                title = "Pastovyklių sąrašas",
                                subtitle = "Kurk grupes, priskirk vadovus ir laikyk struktūrą aiškią."
                            ) {
                                if (state.pastovykles.isEmpty()) {
                                    SkautaiEmptyState(
                                        title = "Pastovyklių nėra",
                                        subtitle = if (canManage) "Pradėk nuo pirmos pastovyklės sukūrimo." else "Renginio vadovas dar nesukūrė pastovyklių.",
                                        icon = Icons.Default.Groups,
                                        actionLabel = if (canManage) "Sukurti" else null,
                                        onAction = if (canManage) {
                                            {
                                                editingPastovykle = null
                                                showEditor = true
                                            }
                                        } else null
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        state.pastovykles.forEach { pastovykle ->
                                            PastovykleRow(
                                                pastovykle = pastovykle,
                                                leaderName = state.members.firstOrNull { it.userId == pastovykle.responsibleUserId }?.fullName(),
                                                canManage = canManage,
                                                isWorking = state.isWorking,
                                                onEdit = {
                                                    editingPastovykle = pastovykle
                                                    showEditor = true
                                                },
                                                onDelete = { deletingPastovykle = pastovykle }
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

@Composable
private fun PastovykleRow(
    pastovykle: PastovykleDto,
    leaderName: String?,
    canManage: Boolean,
    isWorking: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = pastovykleAgeGroupPalette(pastovykle.ageGroup)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (canManage && !isWorking) Modifier.clickable(onClick = onEdit) else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = palette.cardTone),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = palette.iconTone
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = palette.accent)
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = pastovykle.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = leaderName ?: "Vadovas nepriskirtas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                pastovykle.notes?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkautaiStatusPill(
                        label = pastovykleAgeGroupLabel(pastovykle.ageGroup),
                        containerColor = palette.iconTone,
                        contentColor = palette.accent
                    )
                }
            }
            if (canManage) {
                Row(
                    modifier = Modifier.widthIn(min = 88.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEdit,
                        enabled = !isWorking,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Keisti", tint = palette.accent)
                    }
                    IconButton(
                        onClick = onDelete,
                        enabled = !isWorking,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Ištrinti",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = palette.accent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PastovykleEditorScreen(
    pastovykle: PastovykleDto?,
    event: EventDto,
    members: List<MemberDto>,
    isWorking: Boolean,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSave: (String, String?, String?, String?) -> Unit
) {
    var name by remember(pastovykle) { mutableStateOf(pastovykle?.name.orEmpty()) }
    var ageGroup by remember(pastovykle) { mutableStateOf(normalizePastovykleAgeGroupCode(pastovykle?.ageGroup).orEmpty()) }
    var notes by remember(pastovykle) { mutableStateOf(pastovykle?.notes.orEmpty()) }
    var selectedLeaderId by remember(pastovykle) { mutableStateOf(pastovykle?.responsibleUserId) }
    var searchQuery by remember(pastovykle) { mutableStateOf("") }
    val filteredMembers = remember(members, searchQuery, ageGroup, pastovykle, event) {
        val query = searchQuery.trim()
        eligiblePastovykleLeaderMembers(members, event, pastovykle, ageGroup)
            .sortedBy { it.fullName().lowercase() }
            .filter { member ->
                query.isBlank() ||
                    member.fullName().contains(query, ignoreCase = true) ||
                    member.email.contains(query, ignoreCase = true)
            }
    }
    val canSave = name.isNotBlank() && !isWorking

    EventScreenScaffold(
        title = if (pastovykle == null) "Nauja pastovyklė" else "Redaguoti pastovyklę",
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        actions = {
            TextButton(
                onClick = {
                    onSave(
                        name.trim(),
                        ageGroup.takeUnless { it.isBlank() },
                        selectedLeaderId,
                        notes.takeUnless { it.isBlank() }?.trim()
                    )
                },
                enabled = canSave
            ) {
                Text("Išsaugoti")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                EventDetailSection(
                    title = "Pastovyklės duomenys",
                    subtitle = "Pavadinimas ir amžiaus grupė bus matomi pastovyklių sąraše."
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Pavadinimas *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = eventFormFieldColors()
                    )
                    DropdownField(
                        label = "Amžiaus grupė",
                        value = pastovykleAgeGroupLabel(ageGroup),
                        options = PastovykleAgeGroups.map { it.code to it.label },
                        onSelect = { ageGroup = it }
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Pastabos") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        colors = eventFormFieldColors()
                    )
                }
            }

            item {
                EventDetailSection(
                    title = "Pagrindinis vadovas",
                    subtitle = "Priskyrus vadovą, jo renginio pareiga bus sinchronizuota automatiškai."
                ) {
                    val selectedName = members.firstOrNull { it.userId == selectedLeaderId }?.fullName()
                    SkautaiStatusPill(
                        label = selectedName ?: "Vadovas nepriskirtas",
                        tone = if (selectedLeaderId == null) SkautaiStatusTone.Warning else SkautaiStatusTone.Success
                    )
                    EventDetailSearchBar(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Ieškoti vadovo"
                    )
                    SkautaiCard(modifier = Modifier.fillMaxWidth(), tonal = MaterialTheme.colorScheme.surfaceContainerLow) {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            item {
                                TextButton(
                                    onClick = { selectedLeaderId = null },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Nepriskirti vadovo")
                                }
                            }
                            items(filteredMembers, key = { it.userId }) { member ->
                                TextButton(
                                    onClick = { selectedLeaderId = member.userId },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = member.fullName(),
                                            color = if (member.userId == selectedLeaderId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (member.userId == selectedLeaderId) FontWeight.SemiBold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = member.email,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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

private fun eligiblePastovykleLeaderMembers(
    members: List<MemberDto>,
    event: EventDto,
    pastovykle: PastovykleDto?,
    ageGroup: String?
): List<MemberDto> {
    val currentLeaderRoleId = event.eventRoles.firstOrNull {
        it.role == "PASTOVYKLE_LEADER" && it.userId == pastovykle?.responsibleUserId
    }?.id
    val currentSlot = EventStaffSlotUiModel(
        id = pastovykle?.id ?: "new_pastovykle",
        title = pastovykle?.name.orEmpty(),
        subtitle = "Pastovyklės pagrindinis vadovas",
        role = "PASTOVYKLE_LEADER",
        pastovykleId = pastovykle?.id,
        pastovykleAgeGroup = ageGroup,
        assignedUserId = pastovykle?.responsibleUserId,
        linkedRoleId = currentLeaderRoleId
    )
    return members.filter { member ->
        !memberHasAnotherStaffRole(member.userId, event, excludingSlot = currentSlot) &&
            memberEligibleForPastovykleAgeGroup(member, ageGroup)
    }
}

internal fun normalizePastovykleAgeGroupCode(ageGroup: String?): String? = when (ageGroup?.trim()) {
    "VILKAI", "Vilkai" -> "VILKAI"
    "SKAUTAI", "Skautai" -> "SKAUTAI"
    "PATYRE_SKAUTAI", "Patyrę skautai", "Patyre skautai" -> "PATYRE_SKAUTAI"
    "VYR_SKAUTAI", "Vyr. skautai", "Vyr skautai" -> "VYR_SKAUTAI"
    "VYR_SKAUTES", "Vyr. skautės", "Vyr skautes" -> "VYR_SKAUTES"
    "MIXED" -> "MIXED"
    else -> null
}

internal fun pastovykleAgeGroupLabel(ageGroup: String?): String {
    val code = normalizePastovykleAgeGroupCode(ageGroup)
    return when (code) {
        "VILKAI" -> "Vilkai"
        "SKAUTAI" -> "Skautai"
        "PATYRE_SKAUTAI" -> "Patyrę skautai"
        "VYR_SKAUTAI" -> "Vyr. skautai"
        "VYR_SKAUTES" -> "Vyr. skautės"
        "MIXED" -> "Mišri"
        else -> "Be amžiaus grupės"
    }
}

internal fun pastovykleAgeGroupPalette(ageGroup: String?): ScoutUnitPalette = when (normalizePastovykleAgeGroupCode(ageGroup)) {
    "VILKAI" -> ScoutUnitColors.Vilkai
    "SKAUTAI" -> ScoutUnitColors.Skautai
    "PATYRE_SKAUTAI" -> ScoutUnitColors.PatyreSkautai
    "VYR_SKAUTAI" -> ScoutUnitColors.VyrSkautai
    "VYR_SKAUTES" -> ScoutUnitColors.VyrSkautes
    "MIXED" -> ScoutUnitColors.Default
    else -> ScoutUnitColors.Default
}
