package lt.skautai.android.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiChip
import lt.skautai.android.ui.common.SkautaiEmptyState
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.SkautaiStatusTone
import lt.skautai.android.ui.common.SkautaiSummaryCard

@Composable
fun EventListScreen(
    onEventClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    viewModel: EventListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadEvents()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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

                    if (state.events.isEmpty()) {
                        SkautaiEmptyState(
                            title = "Renginiu nera",
                            subtitle = "Cia matysi stovyklas, sueigas ir kitus vieneto renginius, kai tik jie bus sukurti.",
                            icon = Icons.Default.CalendarMonth,
                            actionLabel = "Kurti rengini",
                            onAction = onCreateClick,
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

                FloatingActionButton(
                    onClick = onCreateClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Naujas renginys")
                }
            }
        }
    }
}

@Composable
private fun EventListOverview(events: List<EventDto>, activeFilter: String?) {
    SkautaiSummaryCard(
        eyebrow = "Renginiu kalendorius",
        title = activeFilter?.let(::eventTypeLabel) ?: "Visi renginiai",
        subtitle = "Greita aktyviu, planuojamu ir uzbaigtu renginiu apzvalga.",
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
    LazyRow(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
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

@Composable
fun EventCard(
    event: EventDto,
    onClick: () -> Unit
) {
    SkautaiCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        tonal = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${event.startDate.take(10)} - ${event.endDate.take(10)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                EventStatusChip(status = event.status)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SkautaiStatusPill(label = eventTypeLabel(event.type), tone = SkautaiStatusTone.Info)
                event.stovyklaDetails?.actualParticipants?.let {
                    SkautaiStatusPill(label = "$it dalyviai", tone = SkautaiStatusTone.Neutral)
                }
            }
            event.notes?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            event.inventorySummary?.let { summary ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventMetricMini(
                        icon = Icons.Default.Groups,
                        label = "Planas",
                        value = summary.totalPlannedQuantity.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    EventMetricMini(
                        icon = Icons.Default.CalendarMonth,
                        label = "Pirkti",
                        value = summary.itemsNeedingPurchase.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
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
                .padding(10.dp),
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
    val tone = when (status) {
        "PLANNING" -> SkautaiStatusTone.Warning
        "ACTIVE" -> SkautaiStatusTone.Success
        "COMPLETED" -> SkautaiStatusTone.Neutral
        "CANCELLED" -> SkautaiStatusTone.Danger
        else -> SkautaiStatusTone.Neutral
    }
    val label = when (status) {
        "PLANNING" -> "Planuojamas"
        "ACTIVE" -> "Aktyvus"
        "COMPLETED" -> "Ivykdytas"
        "CANCELLED" -> "Atsauktas"
        else -> status
    }
    SkautaiStatusPill(label = label, tone = tone)
}
