package lt.skautai.android.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.EventInventoryBucketDto
import lt.skautai.android.data.remote.EventInventoryItemDto
import lt.skautai.android.data.remote.EventInventorySummaryDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.ui.common.QuickActionTile
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiTextField
import lt.skautai.android.ui.common.SkautaiChip
import lt.skautai.android.ui.common.SkautaiEmptyState
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiSearchBar
import lt.skautai.android.ui.common.SkautaiSectionHeader
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.SkautaiStatusTone
import lt.skautai.android.ui.common.SkautaiSummaryCard
import lt.skautai.android.ui.common.SkautaiTopBarTitle
import lt.skautai.android.ui.common.eventStatusTone

fun isEventReadOnlyStatus(status: String): Boolean = status in setOf("COMPLETED", "CANCELLED")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventScreenScaffold(
    title: String,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    SkautaiTopBarTitle(
                        title = title,
                        subtitle = "Renginio darbo erdvė"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atgal")
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        snackbarHost = { SkautaiErrorSnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = floatingActionButton,
        content = content
    )
}

@Composable
fun eventFormFieldColors(): TextFieldColors {
    val scheme = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = scheme.surfaceBright,
        unfocusedContainerColor = scheme.surfaceBright,
        disabledContainerColor = scheme.surfaceContainerLow,
        errorContainerColor = scheme.errorContainer,
        focusedBorderColor = scheme.primary,
        unfocusedBorderColor = scheme.outlineVariant,
        disabledBorderColor = scheme.outlineVariant,
        focusedLabelColor = scheme.primary,
        unfocusedLabelColor = scheme.onSurfaceVariant,
        cursorColor = scheme.primary
    )
}

@Composable
fun EventFormSection(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    EventDetailSection(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        content = content
    )
}

@Composable
fun EventFormEyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun EventFormSupportText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun EventTonalDateButton(
    label: String,
    value: String?,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (enabled) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value?.takeIf { it.isNotBlank() } ?: "Pasirinkti datą",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )
        }
    }
}

@Composable
fun EventPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EventStatusPill(status: String) {
    val label = when (status) {
        "PLANNING" -> "Planuojamas"
        "ACTIVE" -> "Vyksta"
        "WRAP_UP" -> "Suvedimas"
        "COMPLETED" -> "Užbaigtas"
        "CANCELLED" -> "Atšauktas"
        else -> status
    }
    SkautaiStatusPill(label = label, tone = eventStatusTone(status))
}

@Composable
fun EventModeChip(
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SkautaiChip(
        label = text,
        selected = selected,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun EmptyStateText(text: String) {
    EventDetailEmptyCard(text = text)
}

@Composable
fun EventDetailHero(
    event: EventDto,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    subtitle: String = "${eventTypeLabel(event.type)} · ${event.startDate.take(10)} - ${event.endDate.take(10)}",
    metrics: List<Pair<String, String>> = event.inventorySummary?.toHeroMetrics().orEmpty(),
    content: (@Composable () -> Unit)? = null
) {
    if (!expanded) return

    SkautaiSummaryCard(
        eyebrow = "Renginys",
        title = event.name,
        subtitle = subtitle,
        metrics = metrics,
        foresty = true,
        modifier = modifier
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EventStatusPill(status = event.status)
            SkautaiStatusPill(label = eventTypeLabel(event.type), tone = SkautaiStatusTone.Info)
        }
        content?.invoke()
    }
}

@Composable
fun EventDetailSection(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceBright,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SkautaiSectionHeader(
                title = title,
                subtitle = subtitle,
                actionLabel = actionLabel,
                onAction = onAction
            )
            content()
        }
    }
}

@Composable
fun EventDetailNavTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    QuickActionTile(
        icon = icon,
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun EventContextBanner(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    SkautaiCard(
        modifier = modifier.fillMaxWidth(),
        tonal = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.EventAvailable, contentDescription = null)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EventDetailMetricRow(
    metrics: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        metrics.forEach { (label, value) ->
            EventDetailMetricTile(
                label = label,
                value = value,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun EventDetailMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    SkautaiCard(
        modifier = modifier,
        tonal = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun EventDetailSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    SkautaiSearchBar(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        leadingIcon = Icons.Default.Search,
        modifier = modifier
    )
}

@Composable
fun EventDetailEmptyCard(
    text: String,
    modifier: Modifier = Modifier
) {
    SkautaiCard(
        modifier = modifier.fillMaxWidth(),
        tonal = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun EventDetailCenteredEmpty(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        SkautaiEmptyState(
            title = title,
            subtitle = subtitle,
            icon = icon,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

fun EventInventorySummaryDto.toHeroMetrics(): List<Pair<String, String>> = listOf(
    "Planas" to totalPlannedQuantity.toString(),
    "Turima" to totalAvailableQuantity.toString(),
    "Trūksta" to totalShortageQuantity.toString()
)

@Composable
fun EventListSection(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    subtitle?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun EventListGroupHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            "$count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EventInventoryListRow(
    item: EventInventoryItemDto,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    bottom: (@Composable RowScope.() -> Unit)? = null
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            leading?.invoke()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    planItemSubtitle(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!item.notes.isNullOrBlank()) {
                    Text(
                        text = item.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                bottom?.let {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), content = it)
                }
            }
            if (trailing != null) trailing() else EventQuantitySummary(item)
        }
        HorizontalDivider()
    }
}

@Composable
private fun EventQuantitySummary(item: EventInventoryItemDto) {
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Aprūpinta ${item.availableQuantity}/${item.plannedQuantity}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        if (item.shortageQuantity > 0) {
            EventMetricPill("Trūksta ${item.shortageQuantity}", EventMetricTone.Warning)
        } else if (item.reservationGroupId != null) {
            EventMetricPill("Rezervuota", EventMetricTone.Good)
        }
    }
}

@Composable
fun EventMetricPill(text: String, tone: EventMetricTone = EventMetricTone.Neutral) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (tone) {
        EventMetricTone.Good -> scheme.primaryContainer to scheme.onPrimaryContainer
        EventMetricTone.Warning -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        EventMetricTone.Neutral -> scheme.primaryContainer to scheme.primary
    }
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1
        )
    }
}

enum class EventMetricTone { Good, Warning, Neutral }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    if (options.size > 6) {
        SearchablePickerField(
            label = label,
            value = value,
            options = options,
            onSelect = onSelect,
            modifier = modifier
        )
        return
    }

    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = eventFormFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SearchablePickerField(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    var showDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredOptions = remember(options, searchQuery) {
        val query = searchQuery.trim()
        options
            .sortedBy { it.second.lowercase() }
            .filter { (_, text) ->
                query.isBlank() || text.contains(query, ignoreCase = true)
            }
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            colors = eventFormFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true }
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                searchQuery = ""
            },
            title = { Text(label) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SkautaiTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = "Ieškoti",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    SkautaiCard(
                        modifier = Modifier.fillMaxWidth(),
                        tonal = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        if (filteredOptions.isEmpty()) {
                            Text(
                                text = "Nieko nerasta",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                                items(filteredOptions, key = { it.first }) { (id, text) ->
                                    TextButton(
                                        onClick = {
                                            onSelect(id)
                                            showDialog = false
                                            searchQuery = ""
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = text,
                                                modifier = Modifier.weight(1f),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (text == value) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        searchQuery = ""
                    }
                ) {
                    Text("Uždaryti")
                }
            }
        )
    }
}

@Composable
fun EventInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}

fun MemberDto.fullName(): String = "$name $surname".trim()

fun eventTypeLabel(type: String): String = when (type) {
    "STOVYKLA" -> "Stovykla"
    "SUEIGA" -> "Sueiga"
    "RENGINYS" -> "Renginys"
    else -> type.replace(Regex("^CUSTOM_", RegexOption.IGNORE_CASE), "").replace('_', ' ')
}

fun planItemSubtitle(item: EventInventoryItemDto): String {
    val parts = mutableListOf<String>()
    item.bucketName?.takeIf { it.isNotBlank() }?.let { parts += "Paskirtis: $it" }
    if (item.reservationGroupId != null) parts += "Rezervuota"
    item.responsibleUserName?.takeIf { it.isNotBlank() }?.let { parts += "Atsakingas: $it" }
    item.sourcePickupSummary?.takeIf { it.isNotBlank() }?.let { parts += "Pasiimti iš: $it" }
    parts += "Santykis rodo aprūpinimą, ne sandėlio likutį"
    return parts.joinToString(" / ").ifBlank { "Paskirtis neparinkta" }
}

@Composable
fun MemberPickerSheet(
    members: List<MemberDto>,
    title: String = "Pasirinkti narį",
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(members, searchQuery) {
        val q = searchQuery.trim()
        members.sortedBy { it.fullName().lowercase() }
            .filter { q.isBlank() || it.fullName().contains(q, ignoreCase = true) || it.email.contains(q, ignoreCase = true) }
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        SkautaiTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = "Ieškoti",
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        SkautaiCard(modifier = Modifier.fillMaxWidth(), tonal = MaterialTheme.colorScheme.surfaceContainerLow) {
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(filtered, key = { it.userId }) { member ->
                    TextButton(
                        onClick = { onSelect(member.userId); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(member.fullName(), color = MaterialTheme.colorScheme.onSurface)
                            Text(member.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Atšaukti") }
    }
}

@Composable
fun BucketFilterChips(
    buckets: List<EventInventoryBucketDto>,
    selectedBucketId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (buckets.isEmpty()) return
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SkautaiChip(label = "Visi", selected = selectedBucketId == null, onClick = { onSelect(null) })
        }
        items(buckets, key = { it.id }) { bucket ->
            SkautaiChip(label = bucket.name, selected = selectedBucketId == bucket.id, onClick = { onSelect(bucket.id) })
        }
    }
}
