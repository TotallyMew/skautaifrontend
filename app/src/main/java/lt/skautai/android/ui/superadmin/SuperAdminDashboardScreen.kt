package lt.skautai.android.ui.superadmin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.remote.TuntasDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.codeLabel
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiSelectableCard
import lt.skautai.android.ui.common.SkautaiTextField
import lt.skautai.android.ui.common.skautaiSelectionStyle
import lt.skautai.android.ui.members.displayRoleName
import lt.skautai.android.ui.units.subtypeLabel
import lt.skautai.android.ui.units.unitTypeLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminDashboardScreen(
    onMemberClick: (tuntasId: String, userId: String) -> Unit = { _, _ -> },
    onLogout: () -> Unit = {},
    viewModel: SuperAdminDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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

    BackHandler(onBack = onLogout)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Superadministratorius") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Atsijungti"
                        )
                    }
                }
            )
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
                val pendingTuntai = uiState.tuntai.filter { it.status == "PENDING" }
                val activeTuntai = uiState.tuntai.filter { it.status == "ACTIVE" }
                val inactiveTuntai = uiState.tuntai.filter { it.status == "REJECTED" }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TuntaiOverviewSection(
                        activeCount = activeTuntai.size,
                        pendingCount = pendingTuntai.size,
                        inactiveCount = inactiveTuntai.size
                    )

                    PendingTuntaiSection(
                        tuntai = pendingTuntai,
                        isSaving = uiState.isSaving,
                        onApprove = viewModel::approveTuntas,
                        onReject = viewModel::rejectTuntas
                    )

                    TuntasSelectorCard(
                        activeTuntai = activeTuntai,
                        pendingTuntai = pendingTuntai,
                        otherTuntai = inactiveTuntai,
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
                        UnitMemberDirectorySection(
                            units = uiState.units,
                            members = uiState.members,
                            totalMembers = uiState.members.size,
                            searchQuery = uiState.memberSearchQuery,
                            onSearchChanged = viewModel::onMemberSearchChanged,
                            onMemberSelected = { userId ->
                                uiState.selectedTuntasId?.let { tuntasId -> onMemberClick(tuntasId, userId) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TuntaiOverviewSection(
    activeCount: Int,
    pendingCount: Int,
    inactiveCount: Int
) {
    SectionCard(title = "Tuntų suvestinė") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TuntasCountBlock(
                label = "Patvirtinti",
                count = activeCount,
                modifier = Modifier.weight(1f)
            )
            TuntasCountBlock(
                label = "Laukia patvirtinimo",
                count = pendingCount,
                modifier = Modifier.weight(1f)
            )
            TuntasCountBlock(
                label = "Atmesti",
                count = inactiveCount,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TuntasCountBlock(
    label: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(count.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Text(
            "Šie tuntai dar nepatvirtinti. Jie gali egzistuoti kartu su jau patvirtintais tuntais.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
    activeTuntai: List<TuntasDto>,
    pendingTuntai: List<TuntasDto>,
    otherTuntai: List<TuntasDto>,
    selectedTuntasId: String?,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val tuntai = activeTuntai + pendingTuntai + otherTuntai
    val selectedTuntas = tuntai.find { it.id == selectedTuntasId }

    SectionCard(title = "Pasirinktas tuntas valdymui") {
        Text(
            "Pasirinkite tuntą, kurio vienetus ir narius norite tvarkyti.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selectedTuntas?.let { "${it.name} (${statusText(it.status)})" } ?: "Pasirinkite tuntą",
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
                TuntasDropdownGroup(
                    title = "Patvirtinti",
                    tuntai = activeTuntai,
                    onSelected = onSelected,
                    onClose = { expanded = false }
                )
                TuntasDropdownGroup(
                    title = "Laukia patvirtinimo",
                    tuntai = pendingTuntai,
                    onSelected = onSelected,
                    onClose = { expanded = false }
                )
                TuntasDropdownGroup(
                    title = "Atmesti",
                    tuntai = otherTuntai,
                    onSelected = onSelected,
                    onClose = { expanded = false }
                )
            }
        }
    }
}

@Composable
private fun TuntasDropdownGroup(
    title: String,
    tuntai: List<TuntasDto>,
    onSelected: (String) -> Unit,
    onClose: () -> Unit
) {
    if (tuntai.isEmpty()) return
    DropdownMenuItem(
        text = {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        },
        enabled = false,
        onClick = {}
    )
    tuntai.forEach { tuntas ->
        DropdownMenuItem(
            text = { Text("${tuntas.name} (${statusText(tuntas.status)})") },
            onClick = {
                onSelected(tuntas.id)
                onClose()
            }
        )
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
        if (tuntas.status == "PENDING") {
            Text(
                "Šis tuntas dar laukia patvirtinimo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
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
private fun UnitMemberDirectorySection(
    units: List<OrganizationalUnitDto>,
    members: List<MemberDto>,
    totalMembers: Int,
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    onMemberSelected: (String) -> Unit
) {
    var expandedUnitIds by remember { mutableStateOf(emptySet<String>()) }
    val query = searchQuery.trim()
    val normalizedQuery = query.lowercase()
    val unassignedMembers = members.filter { it.unitAssignments.isNullOrEmpty() }
    val visibleGroups = units.mapNotNull { unit ->
        val unitMembers = members.filter { member ->
            member.unitAssignments.orEmpty().any { it.organizationalUnitId == unit.id }
        }
        val unitMatches = normalizedQuery.isBlank() || unit.matchesSearch(normalizedQuery)
        val matchingMembers = if (normalizedQuery.isBlank() || unitMatches) {
            unitMembers
        } else {
            unitMembers.filter { it.matchesSearch(normalizedQuery) }
        }
        if (matchingMembers.isNotEmpty() || unitMatches) UnitMemberGroup(unit, matchingMembers) else null
    }
    val visibleUnassignedMembers = when {
        normalizedQuery.isBlank() -> unassignedMembers
        "be vieneto".contains(normalizedQuery) -> unassignedMembers
        else -> unassignedMembers.filter { it.matchesSearch(normalizedQuery) }
    }
    val visibleMemberCount = (visibleGroups.flatMap { it.members } + visibleUnassignedMembers)
        .distinctBy { it.userId }
        .size

    SectionCard(title = "Vienetai ir nariai") {
        SkautaiTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            label = "Ieškoti nario",
            placeholder = "Vienetas, vardas, pavardė, el. paštas arba pareigos",
            singleLine = true,
            leadingIcon = Icons.Default.Search,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            if (query.isBlank()) {
                "Vienetai suskleisti. Rodoma narių: $totalMembers"
            } else {
                "Rasta narių: $visibleMemberCount iš $totalMembers"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (visibleGroups.isEmpty() && visibleUnassignedMembers.isEmpty()) {
            Text(
                if (query.isBlank()) "Vienetų ir narių nėra" else "Pagal paiešką rezultatų nerasta",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                visibleGroups.forEachIndexed { index, group ->
                    val isExpanded = query.isNotBlank() || group.unit.id in expandedUnitIds
                    UnitGroupRow(
                        unit = group.unit,
                        memberCount = group.members.size,
                        expanded = isExpanded,
                        onToggle = {
                            expandedUnitIds = if (group.unit.id in expandedUnitIds) {
                                expandedUnitIds - group.unit.id
                            } else {
                                expandedUnitIds + group.unit.id
                            }
                        }
                    )
                    if (isExpanded) {
                        if (group.members.isEmpty()) {
                            Text(
                                "Šiame vienete narių nerasta",
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                group.members.forEach { member ->
                                    MemberListRow(member = member, onMemberSelected = onMemberSelected)
                                }
                            }
                        }
                    }
                    if (index != visibleGroups.lastIndex || visibleUnassignedMembers.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    }
                }

                if (visibleUnassignedMembers.isNotEmpty()) {
                    val unassignedExpanded = query.isNotBlank() || UNASSIGNED_UNIT_KEY in expandedUnitIds
                    UnitGroupHeader(
                        title = "Be vieneto",
                        subtitle = "${visibleUnassignedMembers.size} nariai",
                        expanded = unassignedExpanded,
                        onToggle = {
                            expandedUnitIds = if (UNASSIGNED_UNIT_KEY in expandedUnitIds) {
                                expandedUnitIds - UNASSIGNED_UNIT_KEY
                            } else {
                                expandedUnitIds + UNASSIGNED_UNIT_KEY
                            }
                        }
                    )
                    if (unassignedExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            visibleUnassignedMembers.forEach { member ->
                                MemberListRow(member = member, onMemberSelected = onMemberSelected)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitGroupRow(
    unit: OrganizationalUnitDto,
    memberCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    UnitGroupHeader(
        title = unit.name,
        subtitle = buildString {
            append(unitTypeLabel(unit.type))
            unit.subtype?.takeIf { it.isNotBlank() }?.let {
                append(" / ")
                append(subtypeLabel(it))
            }
            append(" / ")
            append("$memberCount nariai")
        },
        expanded = expanded,
        onToggle = onToggle
    )
}

@Composable
private fun UnitGroupHeader(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Suskleisti" else "Išskleisti"
            )
        }
    }
}

@Composable
private fun MemberListRow(
    member: MemberDto,
    onMemberSelected: (String) -> Unit
) {
    val selectionStyle = skautaiSelectionStyle(
        selected = false,
        idleContainer = MaterialTheme.colorScheme.surfaceBright
    )
    SkautaiSelectableCard(
        selected = false,
        onClick = { onMemberSelected(member.userId) },
        modifier = Modifier.fillMaxWidth(),
        style = selectionStyle
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                "${member.name} ${member.surname}",
                fontWeight = FontWeight.Medium,
                color = selectionStyle.titleColor
            )
            Text(
                member.email,
                style = MaterialTheme.typography.bodySmall,
                color = selectionStyle.supportingColor
            )
            Text(
                memberSummary(member),
                style = MaterialTheme.typography.bodySmall,
                color = selectionStyle.supportingColor
            )
        }
    }
}

private data class UnitMemberGroup(
    val unit: OrganizationalUnitDto,
    val members: List<MemberDto>
)

private const val UNASSIGNED_UNIT_KEY = "unassigned"

private fun OrganizationalUnitDto.matchesSearch(query: String): Boolean =
    listOfNotNull(
        name,
        type,
        unitTypeLabel(type),
        subtype,
        subtype?.let(::subtypeLabel)
    ).any { it.lowercase().contains(query) }

private fun MemberDto.matchesSearch(query: String): Boolean {
    val units = unitAssignments.orEmpty().joinToString(" ") { it.organizationalUnitName }
    val roles = leadershipRoles.joinToString(" ") { displayRoleName(it.roleName) }
    val ranks = ranks.joinToString(" ") { displayRoleName(it.roleName) }
    return listOf(name, surname, email, units, roles, ranks)
        .any { it.lowercase().contains(query) }
}

private fun memberSummary(member: MemberDto): String {
    val parts = buildList {
        member.leadershipRoles.forEach { add(displayRoleName(it.roleName)) }
        member.ranks.forEach { add(displayRoleName(it.roleName)) }
    }
    return parts.joinToString(" • ").ifBlank { "Pareigų ar laipsnių nėra" }
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
    SkautaiCard(
        modifier = Modifier.fillMaxWidth(),
        tonal = MaterialTheme.colorScheme.surfaceBright
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
        else -> codeLabel(status) to MaterialTheme.colorScheme.onSurfaceVariant
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
    else -> codeLabel(status)
}


