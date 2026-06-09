package lt.skautai.android.ui.superadmin

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
import androidx.compose.material.icons.filled.Search
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
                val pendingTuntai = uiState.tuntai.filter { it.status == "PENDING" }
                val activeTuntai = uiState.tuntai.filter { it.status == "ACTIVE" }
                val inactiveTuntai = uiState.tuntai.filter { it.status == "REJECTED" || it.status == "SUSPENDED" }
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
                        UnitsSection(units = uiState.units)
                        MembersSection(
                            members = uiState.filteredMembers,
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
                label = "Atmesti / sustabdyti",
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
                    title = "Atmesti / sustabdyti",
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
                                append(unitTypeLabel(unit.type))
                                unit.subtype?.takeIf { it.isNotBlank() }?.let {
                                    append(" / ")
                                    append(subtypeLabel(it))
                                }
                                append(" / ")
                                append("${unit.memberCount} nariai")
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (index != units.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MembersSection(
    members: List<MemberDto>,
    totalMembers: Int,
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    onMemberSelected: (String) -> Unit
) {
    SectionCard(title = "Nariai") {
        SkautaiTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            label = "Ieškoti nario",
            placeholder = "Vardas, pavardė, el. paštas arba vienetas",
            singleLine = true,
            leadingIcon = Icons.Default.Search,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            if (searchQuery.isBlank()) {
                "Rodoma narių: $totalMembers"
            } else {
                "Rasta: ${members.size} iš $totalMembers"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (members.isEmpty()) {
            Text(
                if (searchQuery.isBlank()) "Narių nėra" else "Pagal paiešką narių nerasta",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                members.forEachIndexed { index, member ->
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
                        modifier = Modifier
                            .padding(12.dp),
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
                        val unitsText = member.unitAssignments.orEmpty()
                            .joinToString { it.organizationalUnitName }
                            .ifBlank { null }
                        val summary = memberSummary(member, unitsText)
                        Text(
                            summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = selectionStyle.supportingColor
                        )
                    }
                    }
                    if (index != members.lastIndex) HorizontalDivider()
                }
            }
        }
    }
}

private fun memberSummary(member: MemberDto, unitsText: String?): String {
    val parts = buildList {
        unitsText?.let { add(it) }
        member.leadershipRoles.forEach { add(displayRoleName(it.roleName)) }
        member.ranks.forEach { add(displayRoleName(it.roleName)) }
    }
    return parts.joinToString(" • ").ifBlank { "Narystės informacija nepateikta" }
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


