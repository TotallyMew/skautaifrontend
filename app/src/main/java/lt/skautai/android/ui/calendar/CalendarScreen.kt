package lt.skautai.android.ui.calendar

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiEmptyState
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiAlpha
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.SkautaiStatusTone
import lt.skautai.android.ui.common.eventStatusTone
import lt.skautai.android.ui.common.reservationStatusTone

@Composable
fun CalendarScreen(
    onReservationClick: (String) -> Unit,
    onEventClick: (String) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    when (val state = uiState) {
        CalendarUiState.Loading -> LoadingCalendar()
        is CalendarUiState.Error -> SkautaiErrorState(message = state.message, onRetry = viewModel::refresh)
        is CalendarUiState.Success -> {
            val allEntries = remember(state.events, state.reservations) {
                buildCalendarEntries(state.events, state.reservations)
            }
            val monthEntries = remember(allEntries, selectedMonth) {
                allEntries.filter { YearMonth.from(it.date) == selectedMonth }
            }
            CalendarContent(
                selectedMonth = selectedMonth,
                selectedDate = selectedDate,
                entries = monthEntries,
                onPreviousMonth = {
                    val nextMonth = selectedMonth.minusMonths(1)
                    selectedMonth = nextMonth
                    selectedDate = nextMonth.atDay(1)
                },
                onNextMonth = {
                    val nextMonth = selectedMonth.plusMonths(1)
                    selectedMonth = nextMonth
                    selectedDate = nextMonth.atDay(1)
                },
                onToday = {
                    selectedMonth = YearMonth.now()
                    selectedDate = LocalDate.now()
                },
                onSelectDate = { selectedDate = it },
                onReservationClick = onReservationClick,
                onEventClick = onEventClick
            )
        }
    }
}

@Composable
private fun LoadingCalendar() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CalendarContent(
    selectedMonth: YearMonth,
    selectedDate: LocalDate,
    entries: List<CalendarEntry>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onReservationClick: (String) -> Unit,
    onEventClick: (String) -> Unit
) {
    val entriesByDate = remember(entries) { entries.groupBy { it.date } }
    val selectedEntries = entriesByDate[selectedDate].orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CalendarHeader(
                selectedMonth = selectedMonth,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onToday = onToday
            )
        }

        item {
            CalendarMonthGrid(
                month = selectedMonth,
                selectedDate = selectedDate,
                entriesByDate = entriesByDate,
                onSelectDate = onSelectDate
            )
        }

        item {
            Text(
                text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (selectedEntries.isEmpty()) {
            item {
                SkautaiEmptyState(
                    title = "Nėra įrašų",
                    subtitle = "Pasirinktą dieną nėra renginių ar rezervacijų.",
                    icon = Icons.Default.CalendarMonth
                )
            }
        } else {
            items(selectedEntries.distinctBy { "${it.type}_${it.id}" }, key = { "${it.type}_${it.id}" }) { entry ->
                CalendarEntryCard(
                    entry = entry,
                    onClick = {
                        if (entry.type == CalendarEntryType.Reservation) {
                            onReservationClick(entry.id)
                        } else {
                            onEventClick(entry.id)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    selectedMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = selectedMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale("lt"))),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Ankstesnis mėnuo")
            }
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Kitas mėnuo")
            }
        }
        TextButton(onClick = onToday) {
            Text("Šiandien")
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    entriesByDate: Map<LocalDate, List<CalendarEntry>>,
    onSelectDate: (LocalDate) -> Unit
) {
    val weeks = remember(month) { month.calendarWeeks() }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Pr", "An", "Tr", "Kt", "Pn", "Št", "Sk").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = SkautaiAlpha.SubtleSupporting),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        weeks.forEach { week ->
            CalendarWeekRow(
                week = week,
                selectedDate = selectedDate,
                entriesByDate = entriesByDate,
                onSelectDate = onSelectDate
            )
        }
    }
}

@Composable
private fun CalendarWeekRow(
    week: List<LocalDate?>,
    selectedDate: LocalDate,
    entriesByDate: Map<LocalDate, List<CalendarEntry>>,
    onSelectDate: (LocalDate) -> Unit
) {
    val visibleEntries = remember(week, entriesByDate) { week.visibleWeekBlocks(entriesByDate) }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            week.forEach { date ->
                CalendarDayNumber(
                    date = date,
                    isSelected = date == selectedDate,
                    isToday = date == LocalDate.now(),
                    onClick = { if (date != null) onSelectDate(date) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        visibleEntries.forEach { block ->
            CalendarWeekEventBlock(week = week, block = block)
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun CalendarDayNumber(
    date: LocalDate?,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .clickable(enabled = date != null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val circleColor = when {
            isToday -> MaterialTheme.colorScheme.primary
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            else -> Color.Transparent
        }
        val textColor = when {
            date == null -> Color.Transparent
            isToday -> MaterialTheme.colorScheme.onPrimary
            isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(circleColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date?.dayOfMonth?.toString().orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor
            )
        }
    }
}

@Composable
private fun CalendarWeekEventBlock(
    week: List<LocalDate?>,
    block: WeekEventBlock
) {
    val startIndex = week.indexOfFirst { it == block.startDate }.coerceAtLeast(0)
    val endIndex = week.indexOfLast { it == block.endDate }.coerceAtLeast(startIndex)
    val span = (endIndex - startIndex + 1).coerceAtLeast(1)
    val trailing = (7 - startIndex - span).coerceAtLeast(0)
    Row(modifier = Modifier.fillMaxWidth()) {
        if (startIndex > 0) Spacer(modifier = Modifier.weight(startIndex.toFloat()))
        Text(
            text = block.entry.title,
            style = MaterialTheme.typography.labelSmall,
            color = block.entry.blockContentTone(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(span.toFloat())
                .background(block.entry.blockContainerTone(), RoundedCornerShape(4.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        )
        if (trailing > 0) Spacer(modifier = Modifier.weight(trailing.toFloat()))
    }
}

@Composable
private fun CalendarEntryCard(
    entry: CalendarEntry,
    onClick: () -> Unit
) {
    SkautaiCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                SkautaiStatusPill(
                    label = if (entry.type == CalendarEntryType.Reservation) "Rezervacija" else "Renginys",
                    tone = entry.tone
                )
            }
            Text(
                text = entry.periodLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SkautaiStatusPill(label = entry.status, tone = entry.statusTone)
        }
    }
}

private enum class CalendarEntryType {
    Event,
    Reservation
}

private data class CalendarEntry(
    val id: String,
    val type: CalendarEntryType,
    val title: String,
    val date: LocalDate,
    val periodLabel: String,
    val status: String,
    val statusTone: SkautaiStatusTone,
    val tone: SkautaiStatusTone
)

private data class WeekEventBlock(
    val entry: CalendarEntry,
    val startDate: LocalDate,
    val endDate: LocalDate
)

@Composable
private fun CalendarEntry.blockContainerTone(): Color =
    if (type == CalendarEntryType.Event) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }

@Composable
private fun CalendarEntry.blockContentTone(): Color =
    if (type == CalendarEntryType.Event) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }

private fun buildCalendarEntries(
    events: List<lt.skautai.android.data.remote.EventDto>,
    reservations: List<lt.skautai.android.data.remote.ReservationDto>
): List<CalendarEntry> {
    val eventEntries = events.flatMap { event ->
        datesBetween(event.startDate.toLocalDateOrNull(), event.endDate.toLocalDateOrNull()).map { date ->
            CalendarEntry(
                id = event.id,
                type = CalendarEntryType.Event,
                title = event.name,
                date = date,
                periodLabel = "${event.startDate.take(10)} - ${event.endDate.take(10)}",
                status = event.status,
                statusTone = eventStatusTone(event.status),
                tone = SkautaiStatusTone.Info
            )
        }
    }
    val reservationEntries = reservations.filter { it.eventId == null }.flatMap { reservation ->
        datesBetween(reservation.startDate.toLocalDateOrNull(), reservation.endDate.toLocalDateOrNull()).map { date ->
            CalendarEntry(
                id = reservation.id,
                type = CalendarEntryType.Reservation,
                title = reservation.title,
                date = date,
                periodLabel = "${reservation.startDate.take(10)} - ${reservation.endDate.take(10)}",
                status = reservation.status,
                statusTone = reservationStatusTone(reservation.status),
                tone = SkautaiStatusTone.Warning
            )
        }
    }
    return eventEntries + reservationEntries
}

private fun List<LocalDate?>.visibleWeekBlocks(entriesByDate: Map<LocalDate, List<CalendarEntry>>): List<WeekEventBlock> {
    val weekDates = filterNotNull()
    return weekDates
        .flatMap { entriesByDate[it].orEmpty() }
        .groupBy { "${it.type}_${it.id}" }
        .values
        .mapNotNull { dailyEntries ->
            val inWeek = dailyEntries.filter { it.date in weekDates }
            val first = inWeek.minByOrNull { it.date } ?: return@mapNotNull null
            WeekEventBlock(
                entry = first,
                startDate = inWeek.minOf { it.date },
                endDate = inWeek.maxOf { it.date }
            )
        }
        .sortedWith(compareBy<WeekEventBlock> { it.startDate }.thenBy { it.entry.title })
}

private fun YearMonth.calendarWeeks(): List<List<LocalDate?>> = calendarCells().chunked(7)

private fun YearMonth.calendarCells(): List<LocalDate?> {
    val firstDay = atDay(1)
    val leadingEmptyCells = firstDay.dayOfWeek.value - 1
    val monthDays = (1..lengthOfMonth()).map { atDay(it) }
    val cells = List(leadingEmptyCells) { null } + monthDays
    val trailingEmptyCells = (7 - cells.size % 7).takeIf { it < 7 } ?: 0
    return cells + List(trailingEmptyCells) { null }
}

private fun String.toLocalDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(take(10)) }.getOrNull()

private fun datesBetween(start: LocalDate?, end: LocalDate?): List<LocalDate> {
    if (start == null) return emptyList()
    val safeEnd = end ?: start
    return generateSequence(start) { it.plusDays(1) }
        .takeWhile { !it.isAfter(safeEnd) }
        .toList()
}
