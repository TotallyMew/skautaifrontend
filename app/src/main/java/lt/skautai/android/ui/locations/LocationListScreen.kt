package lt.skautai.android.ui.locations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.LocationDto
import lt.skautai.android.data.repository.LocationRepository
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiChip
import lt.skautai.android.ui.common.SkautaiEmptyState
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiSearchBar
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.skautaiHeroPillStyle
import lt.skautai.android.util.TokenManager

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LocationListScreen(
    onLocationClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    refreshSignal: Boolean,
    onRefreshHandled: () -> Unit,
    viewModel: LocationListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullRefreshState(uiState.isRefreshing, viewModel::refresh)

    LaunchedEffect(refreshSignal) {
        if (refreshSignal) {
            viewModel.refresh()
            onRefreshHandled()
        }
    }

    if (uiState.isLoading && uiState.locations.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (uiState.error != null && uiState.locations.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SkautaiErrorState(
                message = uiState.error!!,
                onRetry = viewModel::refresh
            )
        }
        return
    }

    val allLocations = uiState.locations
    val displayed = uiState.filteredLocations
    val chipScrollState = rememberScrollState()
    val publicAccent = MaterialTheme.colorScheme.tertiaryContainer
    val unitAccent = MaterialTheme.colorScheme.primaryContainer
    val privateAccent = MaterialTheme.colorScheme.secondaryContainer
    val publicLocations = displayed.filter { it.visibility == "PUBLIC" }
    val unitLocations = displayed.filter { it.visibility == "UNIT" && it.ownerUnitId == uiState.activeUnitId }
    val privateLocations = displayed.filter { it.visibility == "PRIVATE" }
    val publicRootCount = allLocations.count { it.visibility == "PUBLIC" && it.parentLocationId == null }
    val unitRootCount = allLocations.count {
        it.visibility == "UNIT" &&
            it.ownerUnitId == uiState.activeUnitId &&
            it.parentLocationId == null
    }
    val privateRootCount = allLocations.count { it.visibility == "PRIVATE" && it.parentLocationId == null }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 136.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                LocationHeroCard(
                    totalCount = allLocations.size,
                    filteredCount = displayed.size,
                    filter = uiState.filter,
                    searchQuery = uiState.searchQuery
                )
            }

        item {
            SkautaiSearchBar(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchChange,
                placeholder = "Ieškoti pagal pavadinimą, adresą ar tėvinę vietą",
                leadingIcon = Icons.Default.Search,
                trailingIcon = if (uiState.hasActiveSearchOrFilter) Icons.Default.Tune else null,
                onTrailingIconClick = if (uiState.hasActiveSearchOrFilter) viewModel::clearFilters else null
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chipScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SkautaiChip(
                    label = "Visos ${publicRootCount + unitRootCount + privateRootCount}",
                    selected = uiState.filter == LocationFilter.All,
                    onClick = { viewModel.onFilterChange(LocationFilter.All) }
                )
                SkautaiChip(
                    label = "Viešos $publicRootCount",
                    selected = uiState.filter == LocationFilter.Public,
                    onClick = { viewModel.onFilterChange(LocationFilter.Public) }
                )
                SkautaiChip(
                    label = "Vieneto $unitRootCount",
                    selected = uiState.filter == LocationFilter.Unit,
                    onClick = { viewModel.onFilterChange(LocationFilter.Unit) }
                )
                SkautaiChip(
                    label = "Mano $privateRootCount",
                    selected = uiState.filter == LocationFilter.Private,
                    onClick = { viewModel.onFilterChange(LocationFilter.Private) }
                )
            }
        }

        uiState.error?.let { error ->
            item {
                SkautaiErrorState(
                    message = error,
                    onRetry = viewModel::refresh
                )
            }
        }

        if (uiState.isEmpty) {
            item {
                SkautaiEmptyState(
                    title = "Lokacijų dar nėra",
                    subtitle = "Sukurk pirmą viešą, vieneto arba asmeninę vietą ir pradėk formuoti aiškesnį saugojimo žemėlapį.",
                    icon = Icons.Default.Place,
                    actionLabel = "Pridėti lokaciją",
                    onAction = onCreateClick
                )
            }
            return@LazyColumn
        }

        if (displayed.isEmpty()) {
            item {
                SkautaiEmptyState(
                    title = "Lokacijų nerasta",
                    subtitle = "Pabandyk kitą paiešką arba nuimk aktyvų filtrą, kad vėl matytum visą medį.",
                    icon = Icons.Default.Search,
                    actionLabel = if (uiState.hasActiveSearchOrFilter) "Išvalyti filtrus" else null,
                    onAction = if (uiState.hasActiveSearchOrFilter) viewModel::clearFilters else null
                )
            }
            return@LazyColumn
        }

        locationSection(
            title = "Viešos lokacijos",
            subtitle = "Bendros vietos, kurias gali matyti visi nariai.",
            icon = Icons.Default.Public,
            rootCandidates = publicLocations,
            expandedIds = uiState.expandedIds,
            onToggle = viewModel::toggleExpanded,
            onLocationClick = onLocationClick,
            accentColor = publicAccent
        )
        locationSection(
            title = "Mano vieneto lokacijos",
            subtitle = "Aktyviam vienetui priskirtos vietos ir jų šakos.",
            icon = Icons.Default.GroupWork,
            rootCandidates = unitLocations,
            expandedIds = uiState.expandedIds,
            onToggle = viewModel::toggleExpanded,
            onLocationClick = onLocationClick,
            accentColor = unitAccent
        )
        locationSection(
            title = "Asmeninės lokacijos",
            subtitle = "Privačios vietos tavo asmeniniam naudojimui.",
            icon = Icons.Default.Person,
            rootCandidates = privateLocations,
            expandedIds = uiState.expandedIds,
            onToggle = viewModel::toggleExpanded,
            onLocationClick = onLocationClick,
            accentColor = privateAccent
        )
        }
        PullRefreshIndicator(
            refreshing = uiState.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun LocationHeroCard(
    totalCount: Int,
    filteredCount: Int,
    filter: LocationFilter,
    searchQuery: String
) {
    val heroPillStyle = skautaiHeroPillStyle()
    SkautaiCard(
        modifier = Modifier.fillMaxWidth(),
        tonal = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Lokacijų katalogas",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Peržvelk visą medį, filtruok pagal matomumą ir greičiau surask konkrečią šaką.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SkautaiStatusPill(
                    label = "$totalCount iš viso",
                    containerColor = heroPillStyle.containerColor,
                    contentColor = heroPillStyle.contentColor
                )
                SkautaiStatusPill(
                    label = "$filteredCount rodoma",
                    containerColor = heroPillStyle.containerColor,
                    contentColor = heroPillStyle.contentColor
                )
                if (filter != LocationFilter.All || searchQuery.isNotBlank()) {
                    SkautaiStatusPill(
                        label = activeScopeLabel(filter, searchQuery),
                        containerColor = heroPillStyle.containerColor,
                        contentColor = heroPillStyle.contentColor
                    )
                }
            }
        }
    }
}

private fun LazyListScope.locationSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    rootCandidates: List<LocationDto>,
    expandedIds: Set<String>,
    onToggle: (String) -> Unit,
    onLocationClick: (String) -> Unit,
    accentColor: Color
) {
    val nodesByParent = rootCandidates.groupBy { it.parentLocationId }
    val roots = rootCandidates
        .filter { it.parentLocationId == null }
        .sortedBy { it.fullPath.lowercase() }

    item(key = "section_$title") {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = accentColor,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SkautaiStatusPill(
                        label = roots.size.toString(),
                        containerColor = accentColor,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }

    if (rootCandidates.isEmpty()) {
        item(key = "empty_$title") {
            Text(
                text = "Šiame skyriuje lokacijų dar nėra.",
                modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    items(flattenLocations(roots, nodesByParent, expandedIds), key = { it.location.id }) { node ->
        LocationRow(
            node = node,
            expanded = node.location.id in expandedIds,
            onToggle = onToggle,
            onLocationClick = onLocationClick
        )
    }
}

@Composable
private fun LocationRow(
    node: VisibleLocationNode,
    expanded: Boolean,
    onToggle: (String) -> Unit,
    onLocationClick: (String) -> Unit
) {
    val location = node.location
    val address = location.address?.takeIf { it.isNotBlank() }
    val parentTrail = parentTrail(location.fullPath, location.name)
    val rowTone = if (location.hasChildren) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surfaceBright
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TreeGuides(depth = node.depth)
        SkautaiCard(
            modifier = Modifier.fillMaxWidth(),
            tonal = rowTone,
            onClick = { onLocationClick(location.id) }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (location.hasChildren) {
                    IconButton(onClick = { onToggle(location.id) }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Sutraukti" else "Išskleisti"
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(34.dp))
                }

                Surface(
                    modifier = Modifier.size(48.dp),
                    color = locationIconContainer(node.depth, location.hasChildren),
                    contentColor = locationIconContent(node.depth, location.hasChildren),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = locationIcon(node.depth, location.hasChildren),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = location.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (location.hasChildren) {
                            SkautaiStatusPill(
                                label = "Šaka",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    if (parentTrail != null) {
                        Text(
                            text = parentTrail,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = rowSubtitle(location, address),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TreeGuides(depth: Int) {
    if (depth == 0) {
        Spacer(modifier = Modifier.width(4.dp))
        return
    }

    Row(
        modifier = Modifier
            .width((depth * 14 + 8).dp)
            .height(70.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(depth) {
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                )
            }
        }
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
        )
    }
}

private data class VisibleLocationNode(
    val location: LocationDto,
    val depth: Int
)

private fun flattenLocations(
    roots: List<LocationDto>,
    nodesByParent: Map<String?, List<LocationDto>>,
    expandedIds: Set<String>
): List<VisibleLocationNode> {
    val result = mutableListOf<VisibleLocationNode>()

    fun append(node: LocationDto, depth: Int) {
        result += VisibleLocationNode(node, depth)
        if (node.id in expandedIds) {
            nodesByParent[node.id]
                .orEmpty()
                .sortedBy { it.name.lowercase() }
                .forEach { append(it, depth + 1) }
        }
    }

    roots.forEach { append(it, 0) }
    return result
}

private fun parentTrail(fullPath: String, currentName: String): String? {
    val segments = fullPath.split("/").map { it.trim() }.filter { it.isNotEmpty() }
    if (segments.isEmpty()) return null
    val withoutCurrent = if (segments.lastOrNull() == currentName) segments.dropLast(1) else segments
    return withoutCurrent.takeIf { it.isNotEmpty() }?.joinToString(" / ")
}

private fun rowSubtitle(location: LocationDto, address: String?): String {
    val parts = buildList {
        add(visibilityLabel(location.visibility))
        location.ownerUnitName?.takeIf { location.visibility == "UNIT" }?.let(::add)
        address?.let(::add)
    }
    return parts.joinToString(" • ")
}

@Composable
private fun locationIcon(depth: Int, hasChildren: Boolean): ImageVector = when {
    depth == 0 && hasChildren -> Icons.Default.Business
    hasChildren -> Icons.Default.Folder
    else -> Icons.Default.Place
}

@Composable
private fun locationIconContainer(depth: Int, hasChildren: Boolean): Color = when {
    depth == 0 && hasChildren -> MaterialTheme.colorScheme.primaryContainer
    hasChildren -> MaterialTheme.colorScheme.tertiaryContainer
    else -> MaterialTheme.colorScheme.secondaryContainer
}

@Composable
private fun locationIconContent(depth: Int, hasChildren: Boolean): Color = when {
    depth == 0 && hasChildren -> MaterialTheme.colorScheme.onPrimaryContainer
    hasChildren -> MaterialTheme.colorScheme.onTertiaryContainer
    else -> MaterialTheme.colorScheme.onSecondaryContainer
}

private fun visibilityLabel(value: String): String = when (value) {
    "PRIVATE" -> "Asmeninė"
    "UNIT" -> "Vieneto"
    else -> "Vieša"
}

private fun activeScopeLabel(filter: LocationFilter, searchQuery: String): String = when {
    searchQuery.isNotBlank() && filter != LocationFilter.All -> "Paieška + ${filter.label}"
    searchQuery.isNotBlank() -> "Paieška aktyvi"
    else -> filter.label
}

enum class LocationFilter(val label: String) {
    All("Visos"),
    Public("Viešos"),
    Unit("Vieneto"),
    Private("Asmeninės")
}

data class LocationListUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val locations: List<LocationDto> = emptyList(),
    val expandedIds: Set<String> = emptySet(),
    val activeUnitId: String? = null,
    val searchQuery: String = "",
    val filter: LocationFilter = LocationFilter.All,
    val error: String? = null,
    val isEmpty: Boolean = false
) {
    val filteredLocations: List<LocationDto>
        get() {
            val filteredByScope = when (filter) {
                LocationFilter.All -> locations
                LocationFilter.Public -> locations.filter { it.visibility == "PUBLIC" }
                LocationFilter.Unit -> locations.filter {
                    it.visibility == "UNIT" && it.ownerUnitId == activeUnitId
                }
                LocationFilter.Private -> locations.filter { it.visibility == "PRIVATE" }
            }

            if (searchQuery.isBlank()) return filteredByScope

            val q = searchQuery.trim().lowercase()
            return filteredByScope.filter {
                it.name.lowercase().contains(q) ||
                    it.fullPath.lowercase().contains(q) ||
                    it.address?.lowercase()?.contains(q) == true
            }
        }

    val hasActiveSearchOrFilter: Boolean
        get() = searchQuery.isNotBlank() || filter != LocationFilter.All
}

@HiltViewModel
class LocationListViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val tokenManager: TokenManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(LocationListUiState())
    val uiState: StateFlow<LocationListUiState> = _uiState.asStateFlow()

    init {
        observeCachedLocations()
        refresh()
    }

    private fun observeCachedLocations() {
        viewModelScope.launch {
            val activeUnitId = tokenManager.activeOrgUnitId.first()
            locationRepository.observeLocations().collect { locations ->
                val prevExpanded = _uiState.value.expandedIds
                val rootIds = locations.filter { it.parentLocationId == null }.map { it.id }.toSet()
                val expandedIds = if (prevExpanded.isEmpty()) rootIds else prevExpanded
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    locations = locations,
                    expandedIds = expandedIds,
                    activeUnitId = activeUnitId,
                    isEmpty = locations.isEmpty(),
                    error = if (locations.isNotEmpty()) null else _uiState.value.error
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val refreshOnly = _uiState.value.locations.isNotEmpty()
            if (refreshOnly) {
                _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            }
            try {
                locationRepository.refreshLocations()
                    .onFailure { error ->
                        if (_uiState.value.locations.isEmpty()) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = error.message ?: "Nepavyko gauti lokacijų",
                                isEmpty = true
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                error = error.message ?: "Nepavyko gauti lokacijų"
                            )
                        }
                    }
            } finally {
                if (refreshOnly) {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                }
            }
        }
    }

    fun toggleExpanded(id: String) {
        val expanded = _uiState.value.expandedIds.toMutableSet()
        if (!expanded.add(id)) expanded.remove(id)
        _uiState.value = _uiState.value.copy(expandedIds = expanded)
    }

    fun onSearchChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onFilterChange(filter: LocationFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun clearFilters() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            filter = LocationFilter.All
        )
    }
}
