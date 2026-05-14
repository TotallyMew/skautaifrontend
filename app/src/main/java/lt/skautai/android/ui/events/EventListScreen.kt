package lt.skautai.android.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiChip
import lt.skautai.android.ui.common.SkautaiEmptyState
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.SkautaiStatusTone
import lt.skautai.android.ui.common.SkautaiSummaryCard
import lt.skautai.android.ui.common.eventStatusTone

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun EventListScreen(
    onEventClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    viewModel: EventListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullRefreshState(isRefreshing, viewModel::loadEvents)
    val canCreate = "events.create" in permissions

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        when (val state = uiState) {
            is EventListUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is EventListUiState.Error -> {
                SkautaiErrorState(
                    message = state.message,
                    onRetry = viewModel::loadEvents,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is EventListUiState.Success -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    EventListOverview(events = state.events, activeFilter = state.activeFilter)
                    EventTypeFilterRow(
                        activeFilter = state.activeFilter,
                        onFilterSelected = { viewModel.setTypeFilter(it) }
                    )
                    if (canCreate) {
                        EventTemplatesEntry(onClick = onTemplatesClick)
                    }

                    if (state.events.isEmpty()) {
                        SkautaiEmptyState(
                            title = "Renginių nėra",
                            subtitle = "Čia matysi stovyklas, sueigas ir kitus vieneto renginius, kai tik jie bus sukurti.",
                            icon = Icons.Default.CalendarMonth,
                            actionLabel = if (canCreate) "Kurti renginį" else null,
                            onAction = if (canCreate) onCreateClick else null,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
                        ) {
                            items(state.events, key = { it.id }) { event ->
                                EventCard(
                                    event = event,
                                    onClick = { onEventClick(event.id) }
                                )
                            }
                        }
                    }
                }

                if (canCreate && state.events.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = onCreateClick,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Naujas renginys")
                    }
                }
            }
        }
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun EventTemplatesEntry(onClick: () -> Unit) {
    SkautaiCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = onClick,
        tonal = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Inventoriaus šablonai",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Sukurti ir redaguoti renginių inventoriaus šablonus",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EventListOverview(events: List<EventDto>, activeFilter: String?) {
    SkautaiSummaryCard(
        eyebrow = "Renginių kalendorius",
        title = activeFilter?.let(::eventTypeLabel) ?: "Visi renginiai",
        subtitle = "Greita aktyvių, planuojamų ir užbaigtų renginių apžvalga.",
        metrics = listOf(
            "Visi" to events.size.toString(),
            "Aktyvus" to events.count { it.status == "ACTIVE" }.toString(),
            "Planuojami" to events.count { it.status == "PLANNING" }.toString()
        ),
        foresty = true,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun EventTypeFilterRow(
    activeFilter: String?,
    onFilterSelected: (String?) -> Unit
) {
    val filters = listOf(
        null to "Visi",
        "STOVYKLA" to "Stovykla",
        "SUEIGA" to "Sueiga",
        "RENGINYS" to "Renginys"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large
    ) {
        LazyRow(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { (type, label) ->
                SkautaiChip(
                    label = label,
                    selected = activeFilter == type,
                    onClick = { onFilterSelected(type) }
                )
            }
        }
    }
}

@Composable
fun EventCard(
    event: EventDto,
    onClick: () -> Unit
) {
    SkautaiCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        tonal = MaterialTheme.colorScheme.surfaceBright
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EventAvailable,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = event.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${event.startDate.take(10)} - ${event.endDate.take(10)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    EventStatusChip(status = event.status)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkautaiStatusPill(label = eventTypeLabel(event.type), tone = SkautaiStatusTone.Info)
                    event.inventorySummary?.let { summary ->
                        SkautaiStatusPill(
                            label = "${summary.totalAvailableQuantity}/${summary.totalPlannedQuantity} aprupinta",
                            tone = if (summary.totalShortageQuantity > 0) SkautaiStatusTone.Warning else SkautaiStatusTone.Success
                        )
                    }
                }
                event.notes?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EventMetricMini(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    SkautaiCard(modifier = modifier, tonal = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun EventStatusChip(status: String) {
    val label = when (status) {
        "PLANNING" -> "Planuojamas"
        "ACTIVE" -> "Vyksta"
        "WRAP_UP" -> "Suvedimas"
        "COMPLETED" -> "Įvykdytas"
        "CANCELLED" -> "Atšauktas"
        else -> status
    }
    SkautaiStatusPill(label = label, tone = eventStatusTone(status))
}
