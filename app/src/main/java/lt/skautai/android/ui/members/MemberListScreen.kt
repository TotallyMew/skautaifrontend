package lt.skautai.android.ui.members

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiEmptyState
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiSearchBar
import lt.skautai.android.ui.theme.ScoutPalette

private const val AllFilter = "Visi"
private const val LeadersFilter = "Vadovai"
private const val NoUnitLabel = "Be vieneto"
private val MemberAccent = ScoutPalette.Forest
private val MemberAvatarTone = ScoutPalette.MossSoft

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MemberListScreen(
    onMemberClick: (String) -> Unit,
    onInviteClick: () -> Unit,
    canInvite: Boolean = false,
    viewModel: MemberListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(AllFilter) }
    val listState = rememberLazyListState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadMembers()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is MemberListUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is MemberListUiState.Error -> {
                SkautaiErrorState(
                    message = state.message,
                    onRetry = viewModel::loadMembers,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is MemberListUiState.Success -> {
                if (state.members.isEmpty()) {
                    SkautaiEmptyState(
                        title = "Nariu dar nera",
                        subtitle = "Cia matysi savo vieneto arba tunto narius pagal turimas teises.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    val unitFilters = remember(state.members) {
                        state.members
                            .flatMap { it.safeUnitAssignments().map { assignment -> assignment.organizationalUnitName } }
                            .distinct()
                            .sorted()
                    }
                    val filters = remember(unitFilters) { listOf(AllFilter, LeadersFilter) + unitFilters }
                    if (selectedFilter !in filters) selectedFilter = AllFilter

                    val filteredMembers = remember(state.members, query, selectedFilter) {
                        state.members
                            .filter { member -> member.matchesFilter(selectedFilter) }
                            .filter { member -> member.matchesQuery(query) }
                            .sortedWith(compareBy<MemberDto> { it.primaryUnitName() }.thenBy { it.displayName() })
                    }
                    val groupedMembers = remember(filteredMembers) {
                        filteredMembers.groupBy { it.primaryUnitName() }
                    }
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 132.dp)
                        ) {
                            item {
                                MemberToolbar(
                                    count = state.members.size,
                                    query = query,
                                    onQueryChange = { query = it },
                                    filters = filters,
                                    selectedFilter = selectedFilter,
                                    onFilterSelected = { selectedFilter = it }
                                )
                            }

                            if (filteredMembers.isEmpty()) {
                                item {
                                    SkautaiEmptyState(
                                        title = "Nieko nerasta",
                                        subtitle = "Pabandyk ieskoti pagal varda, el. pasta, telefona, pareigas ar vieneta."
                                    )
                                }
                            }

                            groupedMembers.forEach { (unitName, members) ->
                                stickyHeader {
                                    UnitStickyHeader(title = unitName)
                                }
                                items(members, key = { it.userId }) { member ->
                                    MemberRow(
                                        member = member,
                                        onClick = { onMemberClick(member.userId) }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }

                if (canInvite) {
                    FloatingActionButton(
                        onClick = onInviteClick,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Pakviesti nari")
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberToolbar(
    count: Int,
    query: String,
    onQueryChange: (String) -> Unit,
    filters: List<String>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    SkautaiCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        tonal = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Groups, contentDescription = null)
                Column {
                    Text(
                        text = "$count nariai",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Kontaktai, vienetai ir vadovai vienoje vietoje.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            SkautaiSearchBar(
                value = query,
                onValueChange = onQueryChange,
                placeholder = "Ieskoti nario",
                leadingIcon = Icons.Default.Search
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters, key = { it }) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { onFilterSelected(filter) },
                        label = { Text(filter, maxLines = 1) }
                    )
                }
            }
        }
    }
}

@Composable
private fun UnitStickyHeader(title: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = 12.dp, bottom = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MemberAccent,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MemberRow(
    member: MemberDto,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val phone = member.phone?.takeIf { it.isNotBlank() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = CircleShape,
            color = MemberAvatarTone,
            contentColor = MemberAccent
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = member.initials(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = member.displayName(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = member.contextSubtitle(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = member.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Clip
            )
        }
        if (phone != null) {
            IconButton(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))
                    )
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Skambinti",
                    tint = MemberAccent
                )
            }
        }
    }
}

private fun MemberDto.matchesQuery(query: String): Boolean {
    val trimmed = query.trim()
    if (trimmed.isBlank()) return true

    val searchable = listOf(
        name,
        surname,
        email,
        phone.orEmpty(),
        primaryUnitName(),
        leadershipRoles.firstOrNull()?.roleName.orEmpty(),
        ranks.firstOrNull()?.roleName.orEmpty()
    ).joinToString(" ").lowercase()

    return searchable.contains(trimmed.lowercase())
}

private fun MemberDto.matchesFilter(filter: String): Boolean = when (filter) {
    AllFilter -> true
    LeadersFilter -> leadershipRoles.isNotEmpty()
    else -> safeUnitAssignments().any { it.organizationalUnitName == filter } ||
        leadershipRoles.any { it.organizationalUnitName == filter }
}

private fun MemberDto.primaryUnitName(): String =
    safeUnitAssignments().firstOrNull()?.organizationalUnitName
        ?: leadershipRoles.firstOrNull()?.organizationalUnitName
        ?: NoUnitLabel

private fun MemberDto.safeUnitAssignments() = unitAssignments.orEmpty()

private fun MemberDto.contextSubtitle(): String {
    val role = leadershipRoles.firstOrNull()?.roleName ?: ranks.firstOrNull()?.roleName
    return role ?: ""
}

private fun MemberDto.initials(): String {
    val parts = displayName().split(" ").filter { it.isNotBlank() }
    return parts.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
        .ifBlank { "?" }
}

private fun MemberDto.displayName(): String {
    val parts = listOf(name, surname)
        .map { it.trim().toDisplayNamePart() }
        .filter { it.isNotBlank() }

    return parts.joinToString(" ").ifBlank { email.substringBefore("@").toDisplayNamePart() }
}

private fun String.toDisplayNamePart(): String = trim()
    .lowercase()
    .split(" ")
    .filter { it.isNotBlank() }
    .joinToString(" ") { word -> word.toTitleCaseWord() }

private fun String.toTitleCaseWord(): String = split("-")
    .joinToString("-") { hyphenPart ->
        hyphenPart.split("'").joinToString("'") { part ->
            part.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }
        }
    }
