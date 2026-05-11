package lt.skautai.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import lt.skautai.android.ui.theme.ScoutGradients
import lt.skautai.android.ui.theme.ScoutStatusColors

enum class SkautaiStatusTone {
    Success,
    Warning,
    Danger,
    Neutral,
    Info
}

enum class SkautaiSurfaceRole {
    Default,
    Muted,
    Identity,
    DenseList
}

enum class SummaryVariant {
    Plain,
    Hero
}

@Composable
fun skautaiSurfaceTone(role: SkautaiSurfaceRole): Color {
    val scheme = MaterialTheme.colorScheme
    return when (role) {
        SkautaiSurfaceRole.Default -> scheme.surfaceBright
        SkautaiSurfaceRole.Muted -> scheme.surfaceContainerLow
        SkautaiSurfaceRole.Identity -> scheme.primaryContainer
        SkautaiSurfaceRole.DenseList -> scheme.surfaceContainer
    }
}

data class SkautaiStatusStyle(
    val containerColor: Color,
    val contentColor: Color,
    val icon: ImageVector? = null
)

@Composable
fun skautaiStatusStyle(tone: SkautaiStatusTone): SkautaiStatusStyle {
    val scheme = MaterialTheme.colorScheme
    return when (tone) {
        SkautaiStatusTone.Success -> SkautaiStatusStyle(
            containerColor = ScoutStatusColors.OkContainer,
            contentColor = ScoutStatusColors.OnOkContainer,
            icon = Icons.Default.CheckCircle
        )
        SkautaiStatusTone.Warning -> SkautaiStatusStyle(
            containerColor = ScoutStatusColors.PendingContainer,
            contentColor = ScoutStatusColors.OnPendingContainer,
            icon = Icons.Default.Schedule
        )
        SkautaiStatusTone.Danger -> SkautaiStatusStyle(
            containerColor = scheme.errorContainer,
            contentColor = scheme.onErrorContainer,
            icon = Icons.Default.WarningAmber
        )
        SkautaiStatusTone.Info -> SkautaiStatusStyle(
            containerColor = ScoutStatusColors.InfoContainer,
            contentColor = ScoutStatusColors.OnInfoContainer,
            icon = Icons.Default.Info
        )
        SkautaiStatusTone.Neutral -> SkautaiStatusStyle(
            containerColor = ScoutStatusColors.NeutralContainer,
            contentColor = ScoutStatusColors.OnNeutralContainer,
            icon = null
        )
    }
}

@Composable
fun SkautaiCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    tonal: Color = MaterialTheme.colorScheme.surfaceBright,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable () -> Unit
) {
    Card(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        colors = CardDefaults.cardColors(containerColor = tonal),
        shape = shape,
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
fun SkautaiTopBarTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SkautaiSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(leadingIcon, contentDescription = null) },
        trailingIcon = trailingIcon?.let { icon ->
            {
                if (onTrailingIconClick != null) {
                    IconButton(onClick = onTrailingIconClick) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    Icon(icon, contentDescription = null)
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun SkautaiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    readOnly: Boolean = false,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { text -> { Text(text) } },
        isError = isError,
        supportingText = supportingText?.let { text -> { Text(text) } },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
        trailingIcon = trailingIcon?.let { icon ->
            {
                if (onTrailingIconClick != null) {
                    IconButton(onClick = onTrailingIconClick) {
                        Icon(icon, contentDescription = null)
                    }
                } else {
                    Icon(icon, contentDescription = null)
                }
            }
        },
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = MaterialTheme.shapes.large,
        readOnly = readOnly,
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors()
    )
}

@Composable
fun SkautaiPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = MaterialTheme.shapes.large
    ) {
        leadingIcon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        }
        Text(text)
    }
}

@Composable
fun SkautaiSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = MaterialTheme.shapes.large
    ) {
        leadingIcon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        }
        Text(text)
    }
}

@Composable
fun SkautaiTertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        leadingIcon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        }
        Text(text)
    }
}

@Composable
fun SkautaiDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        ),
        border = ButtonDefaults.outlinedButtonBorder(enabled).copy(
            brush = Brush.linearGradient(
                listOf(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.error)
            )
        )
    ) {
        leadingIcon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        }
        Text(text)
    }
}

@Composable
fun SkautaiConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "Atšaukti",
    isDanger: Boolean = false,
    enabled: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = enabled,
                colors = if (isDanger) {
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.textButtonColors()
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText) }
        }
    )
}

@Composable
fun SkautaiChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            labelColor = MaterialTheme.colorScheme.onSurface
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun SkautaiStatusPill(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(999.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun SkautaiStatusBadge(
    label: String,
    tone: SkautaiStatusTone,
    modifier: Modifier = Modifier,
    showIcon: Boolean = false
) {
    val style = skautaiStatusStyle(tone)
    Surface(
        color = style.containerColor,
        contentColor = style.contentColor,
        shape = RoundedCornerShape(999.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showIcon) {
                style.icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(14.dp)) }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SkautaiStatusPill(
    label: String,
    tone: SkautaiStatusTone,
    modifier: Modifier = Modifier
) {
    val style = skautaiStatusStyle(tone)
    SkautaiStatusPill(
        label = label,
        containerColor = style.containerColor,
        contentColor = style.contentColor,
        modifier = modifier
    )
}

@Composable
fun SkautaiErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(36.dp)
            )
        }
        Text(
            text = "Įvyko klaida",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(4.dp))
            FilledTonalButton(onClick = onRetry) {
                Text("Bandyti dar kartą")
            }
        }
    }
}

@Composable
fun SkautaiErrorSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(hostState = hostState, modifier = modifier.imePadding()) { data ->
        val style = snackbarStyleForMessage(data.visuals.message)
        Snackbar(
            snackbarData = data,
            containerColor = style.containerColor,
            contentColor = style.contentColor
        )
    }
}

@Composable
private fun snackbarStyleForMessage(message: String): SkautaiStatusStyle {
    val normalized = message.lowercase()
    val looksSuccessful = listOf(
        "sukur",
        "issaug",
        "išsaug",
        "patvirt",
        "pakeist",
        "palikt",
        "pridet",
        "pridėt",
        "issiust",
        "išsiųst"
    ).any { it in normalized }

    return skautaiStatusStyle(
        if (looksSuccessful) SkautaiStatusTone.Success else SkautaiStatusTone.Danger
    )
}

@Composable
fun SkautaiInlineErrorBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    val style = skautaiStatusStyle(SkautaiStatusTone.Danger)
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = style.containerColor,
        contentColor = style.contentColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            style.icon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = style.contentColor
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SkautaiEmptyState(
    title: String,
    subtitle: String,
    icon: ImageVector,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(34.dp)
            )
        }
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            FilledTonalButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun SkautaiStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tone: Color = MaterialTheme.colorScheme.surfaceContainerLow
) {
    SkautaiCard(modifier = modifier, tonal = tone) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
fun SkautaiSummaryCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    metrics: List<Pair<String, String>> = emptyList(),
    foresty: Boolean = false,
    variant: SummaryVariant = if (foresty) SummaryVariant.Hero else SummaryVariant.Plain,
    content: (@Composable () -> Unit)? = null
) {
    val isHero = variant == SummaryVariant.Hero
    val titleColor = if (isHero) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (isHero) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f) else MaterialTheme.colorScheme.onSurfaceVariant
    val eyebrowColor = if (isHero) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f) else MaterialTheme.colorScheme.primary
    SkautaiCard(
        modifier = modifier,
        tonal = if (isHero) Color.Transparent else MaterialTheme.colorScheme.surfaceBright,
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = if (isHero) {
                        Brush.linearGradient(ScoutGradients.HomeHero)
                    } else {
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.surfaceBright, MaterialTheme.colorScheme.surfaceBright)
                        )
                    }
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            eyebrow?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = eyebrowColor
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = titleColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor
            )
            if (metrics.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    metrics.forEach { (label, value) ->
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = subtitleColor
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.titleMedium,
                                color = titleColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            content?.invoke()
        }
    }
}

@Composable
fun SkautaiSectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 12.dp, top = 2.dp)
                    .clickable(onClick = onAction)
            )
        }
    }
}

@Composable
fun QuickActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SkautaiCard(
        modifier = modifier,
        onClick = onClick,
        tonal = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(42.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PreviewMetric(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.76f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}

fun itemStatusTone(status: String): SkautaiStatusTone = when (status) {
    "ACTIVE" -> SkautaiStatusTone.Success
    "PENDING_APPROVAL" -> SkautaiStatusTone.Warning
    "INACTIVE" -> SkautaiStatusTone.Neutral
    else -> SkautaiStatusTone.Neutral
}

fun itemConditionTone(condition: String): SkautaiStatusTone = when (condition) {
    "GOOD" -> SkautaiStatusTone.Success
    "DAMAGED" -> SkautaiStatusTone.Warning
    "WRITTEN_OFF" -> SkautaiStatusTone.Danger
    else -> SkautaiStatusTone.Neutral
}

fun eventStatusTone(status: String): SkautaiStatusTone = when (status) {
    "PLANNING" -> SkautaiStatusTone.Info
    "ACTIVE" -> SkautaiStatusTone.Success
    "WRAP_UP" -> SkautaiStatusTone.Warning
    "COMPLETED" -> SkautaiStatusTone.Neutral
    "CANCELLED" -> SkautaiStatusTone.Danger
    else -> SkautaiStatusTone.Neutral
}

fun requestStatusTone(status: String): SkautaiStatusTone = when (status) {
    "PENDING", "SUBMITTED", "FORWARDED", "PENDING_TOP_LEVEL_APPROVAL" -> SkautaiStatusTone.Warning
    "APPROVED", "FULFILLED", "COMPLETED" -> SkautaiStatusTone.Success
    "REJECTED", "CANCELLED" -> SkautaiStatusTone.Danger
    else -> SkautaiStatusTone.Neutral
}

fun requisitionStatusTone(status: String): SkautaiStatusTone = requestStatusTone(status)

fun reservationStatusTone(status: String): SkautaiStatusTone = when (status) {
    "PENDING", "PENDING_UNIT_REVIEW", "PENDING_TOP_LEVEL_REVIEW" -> SkautaiStatusTone.Warning
    "APPROVED", "ISSUED", "RETURNED", "COMPLETED" -> SkautaiStatusTone.Success
    "REJECTED", "CANCELLED", "OVERDUE" -> SkautaiStatusTone.Danger
    else -> SkautaiStatusTone.Neutral
}

fun itemStatusLabel(status: String): String = when (status) {
    "ACTIVE" -> "Aktyvus"
    "PENDING_APPROVAL" -> "Laukia"
    "INACTIVE" -> "Neaktyvus"
    else -> status
}

fun itemConditionLabel(condition: String): String = when (condition) {
    "GOOD" -> "Gera"
    "DAMAGED" -> "Vidutinė"
    "WRITTEN_OFF" -> "Bloga"
    else -> customCodeLabel(condition)
}

fun inventoryTypeLabel(type: String): String = when (type) {
    "COLLECTIVE" -> "Bendras"
    "ASSIGNED" -> "Priskirtas"
    "INDIVIDUAL" -> "Asmeninis"
    else -> type
}

fun inventoryCategoryLabel(category: String): String = when (category) {
    "CAMPING" -> "Stovyklavimas"
    "TOOLS" -> "Įrankiai"
    "COOKING" -> "Maistas"
    "FIRST_AID" -> "Pirmoji pagalba"
    "UNIFORMS" -> "Uniformos"
    "BOOKS" -> "Knygos"
    "PERSONAL_LOANS" -> "Asmeniniai"
    else -> customCodeLabel(category)
}

private fun customCodeLabel(value: String): String =
    value.trim()
        .replace(Regex("^CUSTOM_", RegexOption.IGNORE_CASE), "")
        .replace('_', ' ')
        .lowercase()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

fun itemOriginLabel(origin: String): String = when (origin) {
    "UNIT_ACQUIRED" -> "Savo vieneto"
    "TRANSFERRED_FROM_TUNTAS" -> "Tunto"
    else -> origin
}
