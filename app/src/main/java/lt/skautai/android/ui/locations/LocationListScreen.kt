package lt.skautai.android.ui.locations

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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import lt.skautai.android.ui.common.SkautaiSectionHeader
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.util.TokenManager

@Composable
fun LocationListScreen(
    onLocationClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    refreshSignal: Boolean,
    onRefreshHandled: () -> Unit,
    viewModel: LocationListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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

    val displayed = uiState.filteredLocations
    val publicRoots = displayed.filter { it.visibility == "PUBLIC" }
    val unitRoots = displayed.filter { it.visibility == "UNIT" && it.ownerUnitId == uiState.activeUnitId }
    val privateRoots = displayed.filter { it.visibility == "PRIVATE" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LocationHeroCard(
                totalCount = uiState.locations.size,
                filteredCount = displayed.size,
                onCreateClick = onCreateClick
            )
        }

        item {
            SkautaiSearchBar(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchChange,
                placeholder = "Ieskoti pagal pavadinima, adresa ar kelia",
                leadingIcon = Icons.Default.Search
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SkautaiChip(
                    label = "Viesos ${publicRoots.size}",
                    selected = false,
                    onClick = {}
                )
                SkautaiChip(
                    label = "Vieneto ${unitRoots.size}",
                    selected = false,
                    onClick = {}
                )
                SkautaiChip(
                    label = "Mano ${privateRoots.size}",
                    selected = false,
                    onClick = {}
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
                    title = "Lokaciju dar nera",
                    subtitle = "Sukurk pirma viesa, vieneto arba asmenine vieta ir pradek formuoti aiskesni saugojimo zemelapi.",
                    icon = Icons.Default.Place,
                    actionLabel = "Prideti lokacija",
                    onAction = onCreateClick
                )
            }
            return@LazyColumn
        }

        if (displayed.isEmpty()) {
            item {
                SkautaiEmptyState(
                    title = "Nieko nerasta",
                    subtitle = "Pabandyk kita paieskos fraze arba atidaryk lokaciju medi placiau.",
                    icon = Icons.Default.Search
                )
            }
            return@LazyColumn
        }

        locationSection(
            title = "Viesos lokacijos",
            subtitle = "Bendros vietos, kurias gali matyti visi.",
            icon = Icons.Default.Public,
            rootCandidates = publicRoots,
            expandedIds = uiState.expandedIds,
            onToggle = viewModel::toggleExpanded,
            onLocationClick = onLocationClick
        )
        locationSection(
            title = "Mano vieneto lokacijos",
            subtitle = "Vietos, priskirtos aktyviam vienetui.",
            icon = Icons.Default.GroupWork,
            rootCandidates = unitRoots,
            expandedIds = uiState.expandedIds,
            onToggle = viewModel::toggleExpanded,
            onLocationClick = onLocationClick
        )
        locationSection(
            title = "Asmenines lokacijos",
            subtitle = "Privacios vietos tavo asmeniniam naudojimui.",
            icon = Icons.Default.Person,
            rootCandidates = privateRoots,
            expandedIds = uiState.expandedIds,
            onToggle = viewModel::toggleExpanded,
            onLocationClick = onLocationClick
        )
    }
}

@Composable
private fun LocationHeroCard(
    totalCount: Int,
    filteredCount: Int,
    onCreateClick: () -> Unit
) {
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
                        text = "Lokaciju katalogas",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Viesos, vieneto ir asmenines vietos viename mediui patogiame sarase.",
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
                    label = "$totalCount is viso",
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                SkautaiStatusPill(
                    label = "$filteredCount rodoma",
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Button(onClick = onCreateClick, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Prideti lokacija")
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
    onLocationClick: (String) -> Unit
) {
    item(key = "section_$title") {
        SkautaiCard(
            modifier = Modifier.fillMaxWidth(),
            tonal = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SkautaiStatusPill(
                        label = rootCandidates.size.toString(),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }

    if (rootCandidates.isEmpty()) {
        item(key = "empty_$title") {
            Text(
                text = "Siame skyriuje lokaciju dar nera.",
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val nodesByParent = rootCandidates.groupBy { it.parentLocationId }
    val roots = rootCandidates
        .filter { it.parentLocationId == null }
        .sortedBy { it.fullPath.lowercase() }

    items(flattenLocations(roots, nodesByParent, expandedIds), key = { it.location.id }) { node ->
        LocationRow(
            node = node,
            expanded = node.location.id in expandedIds,
            onToggle = onToggle,
            onLocationClick = onLocationClick
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLocationClick(location.id) }
            .padding(start = (node.depth * 18).dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (location.hasChildren) {
            IconButton(onClick = { onToggle(location.id) }, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Sutraukti" else "Isskleisti"
                )
            }
        } else {
            Surface(
                modifier = Modifier.size(36.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (location.hasChildren) {
                    SkautaiStatusPill(
                        label = "Saka",
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = location.fullPath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (address != null) {
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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

data class LocationListUiState(
    val isLoading: Boolean = true,
    val locations: List<LocationDto> = emptyList(),
    val expandedIds: Set<String> = emptySet(),
    val activeUnitId: String? = null,
    val searchQuery: String = "",
    val error: String? = null,
    val isEmpty: Boolean = false
) {
    val filteredLocations: List<LocationDto>
        get() = if (searchQuery.isBlank()) locations
        else {
            val q = searchQuery.trim().lowercase()
            locations.filter {
                it.name.lowercase().contains(q) ||
                    it.fullPath.lowercase().contains(q) ||
                    it.address?.lowercase()?.contains(q) == true
            }
        }
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
            locationRepository.refreshLocations()
                .onFailure { error ->
                    if (_uiState.value.locations.isEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Nepavyko gauti lokaciju",
                            isEmpty = true
                        )
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
}
