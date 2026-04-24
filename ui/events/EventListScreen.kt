package lt.skautai.android.ui.events

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import lt.skautai.android.ui.common.SkautaiChip
import lt.skautai.android.ui.common.SkautaiEmptyState
import lt.skautai.android.ui.common.SkautaiErrorState

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
                    EventTypeFilterRow(
                        activeFilter = state.activeFilter,
                        onFilterSelected = { viewModel.setTypeFilter(it) }
                    )

                    if (state.events.isEmpty()) {
                        SkautaiEmptyState(
                            title = "Renginiu nera",
                            subtitle = "Cia matysi stovyklas, sueigas ir kitus vieneto renginius.",
                            icon = Icons.Default.CalendarMonth,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
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
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                EventStatusChip(status = event.status)
            }
            Spacer(modifier = Modifier.height(4.dp))
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EventTypeChip(type = event.type)
                Text(
                    text = "${event.startDate.take(10)} - ${event.endDate.take(10)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EventTypeChip(type: String) {
    val label = when (type) {
        "STOVYKLA" -> "Stovykla"
        "SUEIGA" -> "Sueiga"
        "RENGINYS" -> "Renginys"
        else -> type
    }
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun EventStatusChip(status: String) {
    val (label, color) = when (status) {
        "PLANNING" -> "Planuojamas" to MaterialTheme.colorScheme.tertiary
        "ACTIVE" -> "Aktyvus" to MaterialTheme.colorScheme.primary
        "COMPLETED" -> "Ivykdytas" to MaterialTheme.colorScheme.onSurfaceVariant
        "CANCELLED" -> "Atsauktas" to MaterialTheme.colorScheme.error
        else -> status to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = color
    )
}
