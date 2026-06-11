package lt.skautai.android.ui.inventory

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.ui.common.ItemCheckResult
import lt.skautai.android.ui.common.ItemCheckResultPill
import lt.skautai.android.ui.common.ItemCheckSummary
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiChip
import lt.skautai.android.ui.common.SkautaiEmptyState
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiSectionHeader
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.SkautaiStatusTone
import lt.skautai.android.ui.common.inventoryCategoryLabel
import lt.skautai.android.ui.common.itemConditionLabel
import lt.skautai.android.util.QrDestination
import lt.skautai.android.util.QrPayload

@Composable
fun InventoryAuditScreen(
    onAuditCompleted: (String) -> Unit,
    viewModel: InventoryAuditViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val auditResults by viewModel.auditResults.collectAsStateWithLifecycle()
    val showUncheckedOnly by viewModel.showUncheckedOnly.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isResolving by viewModel.isResolving.collectAsStateWithLifecycle()
    val isCompleting by viewModel.isCompleting.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedItem by remember { mutableStateOf<ItemDto?>(null) }
    var launchScan by remember { mutableStateOf(false) }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        when (val parsed = QrPayload.parse(result.contents)) {
            is QrDestination.ScanToken -> viewModel.resolveToken(parsed.token)
            QrDestination.Unknown -> {
                viewModel.showMessage(
                    if (result.contents.isNullOrBlank()) {
                        "Skenavimas nutrauktas. Gali bandyti dar karta."
                    } else {
                        "Šis QR kodas neatpažintas. Tikimasi formato ${QrPayload.forScanToken("token")}."
                    }
                )
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchScan = true
        } else {
            viewModel.showMessage("Be kameros leidimo QR kodo nuskenuoti nepavyks.")
        }
    }

    LaunchedEffect(launchScan) {
        if (!launchScan) return@LaunchedEffect
        launchScan = false
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Skenuok inventoriaus QR kodą inventorizacijai")
            setBeepEnabled(false)
            setOrientationLocked(true)
        }
        scanLauncher.launch(options)
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    when (val contentState = uiState) {
        InventoryAuditUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is InventoryAuditUiState.Error -> {
            SkautaiErrorState(
                message = contentState.message,
                onRetry = viewModel::loadItems
            )
        }

        InventoryAuditUiState.Empty -> {
            SkautaiEmptyState(
                title = "Inventorizuoti kol kas nėra ko",
                subtitle = "Pasirinktoje inventoriaus srityje neradome aktyvių daiktų.",
                icon = Icons.Default.Inventory2,
                actionLabel = "Atnaujinti",
                onAction = viewModel::loadItems
            )
        }

        is InventoryAuditUiState.Success -> {
            val summary = remember(contentState.items, auditResults) {
                buildInventoryAuditSummary(contentState.items, auditResults)
            }
            val visibleItems = remember(contentState.items, auditResults, showUncheckedOnly) {
                contentState.items
                    .filter { !showUncheckedOnly || auditResults[it.id] == null }
                    .sortedWith(
                        compareBy<ItemDto> { auditResults[it.id] != null }
                            .thenBy { it.name.lowercase() }
                    )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                SkautaiErrorSnackbarHost(hostState = snackbarHostState)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        AuditSummaryCard(
                            summary = summary,
                            isRefreshing = isRefreshing,
                            isResolving = isResolving,
                            isCompleting = isCompleting,
                            showUncheckedOnly = showUncheckedOnly,
                            onRefresh = viewModel::loadItems,
                            onScan = {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    launchScan = true
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            onToggleUncheckedOnly = viewModel::toggleUncheckedOnly,
                            onComplete = {
                                viewModel.completeAudit(onAuditCompleted)
                            }
                        )
                    }

                    if (visibleItems.isEmpty()) {
                        item {
                            SkautaiEmptyState(
                                title = "Viskas jau pažymėta",
                                subtitle = "Šioje inventorizacijoje neliko neperžiūrėtų daiktų.",
                                icon = Icons.Default.CheckCircle,
                                actionLabel = "Rodyti visus",
                                onAction = {
                                    if (showUncheckedOnly) {
                                        viewModel.toggleUncheckedOnly()
                                    }
                                }
                            )
                        }
                    } else {
                        items(visibleItems, key = { it.id }) { item ->
                            AuditItemCard(
                                item = item,
                                draft = auditResults[item.id],
                                onClick = { selectedItem = item }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedItem?.let { item ->
        AuditResultDialog(
            item = item,
            currentDraft = auditResults[item.id],
            onDismiss = { selectedItem = null },
            onSelect = { draft ->
                viewModel.saveItemEntry(item, draft)
                selectedItem = null
            }
        )
    }
}

@Composable
private fun AuditSummaryCard(
    summary: ItemCheckSummary,
    isRefreshing: Boolean,
    isResolving: Boolean,
    isCompleting: Boolean,
    showUncheckedOnly: Boolean,
    onRefresh: () -> Unit,
    onScan: () -> Unit,
    onToggleUncheckedOnly: () -> Unit,
    onComplete: () -> Unit
) {
    val onMarkUncheckedMissing = {}
    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SkautaiSectionHeader(
                title = "Inventorizacijos sesija",
                subtitle = "${summary.found}/${summary.total} rasta, ${summary.unchecked} dar nepatikrinta"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryChip("Rasta", summary.found, SkautaiStatusTone.Success)
                SummaryChip("Nerasta", summary.missing, SkautaiStatusTone.Danger)
                SummaryChip("Sumažėjo", summary.decreased, SkautaiStatusTone.Warning)
                SummaryChip("Padaugėjo", summary.increased, SkautaiStatusTone.Info)
            }

            Text(
                text = "Laukta ${summary.expectedQuantityTotal} vnt., rasta ${summary.actualQuantityTotal} vnt., trūksta ${summary.shortageQuantityTotal} vnt., perteklius ${summary.overageQuantityTotal} vnt.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onScan,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(if (isResolving) "Tikrinama..." else "Skenuoti QR")
                }
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text(if (isRefreshing) "Atnaujinama..." else "Atnaujinti sąrašą")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = showUncheckedOnly,
                    onClick = onToggleUncheckedOnly,
                    label = { Text("Tik nepatikrinti") }
                )
                if (false) {
                    OutlinedButton(onClick = onMarkUncheckedMissing) {
                        Text("Likusius žymėti kaip nerastus")
                    }
                }
            }

            if (summary.unchecked > 0) {
                Text(
                    text = "Užbaigti galėsi, kai patikrinsi visus daiktus. Liko: ${summary.unchecked}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onComplete,
                enabled = !isCompleting && summary.unchecked == 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isCompleting) "Užbaigiama..." else "Užbaigti inventorizaciją")
            }
        }
    }
}

@Composable
private fun SummaryChip(
    label: String,
    count: Int,
    tone: SkautaiStatusTone
) {
    if (count <= 0) return
    SkautaiStatusPill(label = "$label: $count", tone = tone)
}

@Composable
private fun AuditItemCard(
    item: ItemDto,
    draft: AuditEntryDraft?,
    onClick: () -> Unit
) {
    SkautaiCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = inventoryCategoryLabel(item.category),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ItemCheckResultPill(result = draft?.result)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.locationPath ?: item.temporaryStorageLabel ?: "Vieta nenurodyta",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!item.responsibleUserName.isNullOrBlank()) {
                Text(
                    text = "Atsakingas: ${item.responsibleUserName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            draft?.let {
                val difference = it.actualQuantity - item.quantity
                val quantityLabel = buildString {
                    append("Kiekis: ")
                    append(it.actualQuantity)
                    append(" / ")
                    append(item.quantity)
                    append(" vnt.")
                    if (difference != 0) {
                        append(" (")
                        if (difference > 0) append("+")
                        append(difference)
                        append(")")
                    }
                }
                SkautaiChip(
                    label = quantityLabel,
                    selected = false,
                    onClick = onClick
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkautaiChip(
                    label = "Būklė: ${itemConditionLabel(item.condition)}",
                    selected = false,
                    onClick = onClick
                )
                Text(
                    text = if (draft == null) "Spausk žymėti" else "Spausk koreguoti",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun AuditResultDialog(
    item: ItemDto,
    currentDraft: AuditEntryDraft?,
    onDismiss: () -> Unit,
    onSelect: (AuditEntryDraft?) -> Unit
) {
    val initialDraft = currentDraft ?: defaultDraft(item, ItemCheckResult.FOUND)
    var selectedResult by rememberSaveable(item.id, currentDraft?.result) { mutableStateOf(initialDraft.result) }
    var quantityText by rememberSaveable(item.id, currentDraft?.actualQuantity) { mutableStateOf(initialDraft.actualQuantity.toString()) }
    var conditionAtCheck by rememberSaveable(item.id, currentDraft?.conditionAtCheck) {
        mutableStateOf(initialDraft.conditionAtCheck ?: item.condition)
    }
    var actualLocationNote by rememberSaveable(item.id, currentDraft?.actualLocationNote) { mutableStateOf(initialDraft.actualLocationNote) }
    var notes by rememberSaveable(item.id, currentDraft?.notes) { mutableStateOf(initialDraft.notes) }
    val parsedQuantity = quantityText.toIntOrNull()
    val displayQuantity = parsedQuantity ?: 0
    val showLocation = selectedResult == ItemCheckResult.MISPLACED
    val showCondition = selectedResult != ItemCheckResult.MISSING

    val options = listOf(
        AuditOption(ItemCheckResult.FOUND, "Rasta", Icons.Default.CheckCircle),
        AuditOption(ItemCheckResult.MISSING, "Nerasta", Icons.Default.HelpOutline),
        AuditOption(ItemCheckResult.MISPLACED, "Ne vietoje", Icons.Default.Place),
        AuditOption(ItemCheckResult.DAMAGED, "Sugadinta", Icons.Default.ReportProblem)
    )
    val conditionOptions = listOf("GOOD", "NEEDS_INSPECTION", "UNDER_REPAIR", "DAMAGED", "MISSING", "WRITTEN_OFF")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = item.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge
                )
                if (currentDraft != null) {
                    IconButton(
                        onClick = { onSelect(null) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Išvalyti žymą")
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Lauktas kiekis: ${item.quantity} vnt.",
                    style = MaterialTheme.typography.bodyMedium
                )

                options.chunked(2).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowOptions.forEach { option ->
                            FilterChip(
                                selected = selectedResult == option.result,
                                onClick = {
                                    selectedResult = option.result
                                    when (option.result) {
                                        ItemCheckResult.MISSING -> quantityText = "0"
                                        ItemCheckResult.DAMAGED -> conditionAtCheck = "DAMAGED"
                                        else -> if (conditionAtCheck == "MISSING") conditionAtCheck = item.condition
                                    }
                                },
                                label = { Text(option.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (option.result) {
                                        ItemCheckResult.FOUND -> MaterialTheme.colorScheme.primaryContainer
                                        ItemCheckResult.MISSING -> MaterialTheme.colorScheme.errorContainer
                                        ItemCheckResult.MISPLACED -> MaterialTheme.colorScheme.tertiaryContainer
                                        ItemCheckResult.DAMAGED -> MaterialTheme.colorScheme.secondaryContainer
                                        ItemCheckResult.CONSUMED -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    selectedLabelColor = when (option.result) {
                                        ItemCheckResult.FOUND -> MaterialTheme.colorScheme.onPrimaryContainer
                                        ItemCheckResult.MISSING -> MaterialTheme.colorScheme.onErrorContainer
                                        ItemCheckResult.MISPLACED -> MaterialTheme.colorScheme.onTertiaryContainer
                                        ItemCheckResult.DAMAGED -> MaterialTheme.colorScheme.onSecondaryContainer
                                        ItemCheckResult.CONSUMED -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    selectedLeadingIconColor = when (option.result) {
                                        ItemCheckResult.FOUND -> MaterialTheme.colorScheme.onPrimaryContainer
                                        ItemCheckResult.MISSING -> MaterialTheme.colorScheme.onErrorContainer
                                        ItemCheckResult.MISPLACED -> MaterialTheme.colorScheme.onTertiaryContainer
                                        ItemCheckResult.DAMAGED -> MaterialTheme.colorScheme.onSecondaryContainer
                                        ItemCheckResult.CONSUMED -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                ),
                                leadingIcon = {
                                    Icon(
                                        imageVector = option.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Faktinis kiekis",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { quantityText = (displayQuantity - 1).coerceAtLeast(0).toString() },
                            enabled = displayQuantity > 0,
                            modifier = Modifier.heightIn(min = 56.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Mažinti")
                        }
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { quantityText = it.filter(Char::isDigit) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { quantityText = (displayQuantity + 1).toString() },
                            modifier = Modifier.heightIn(min = 56.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Didinti")
                        }
                    }
                }

                if (showCondition) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Būklė",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        conditionOptions.chunked(2).forEach { rowOptions ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowOptions.forEach { condition ->
                                    FilterChip(
                                        selected = conditionAtCheck == condition,
                                        onClick = {
                                            conditionAtCheck = condition
                                            if (condition == "DAMAGED") selectedResult = ItemCheckResult.DAMAGED
                                        },
                                        label = { Text(itemConditionLabel(condition)) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = when (condition) {
                                                "GOOD" -> MaterialTheme.colorScheme.primaryContainer
                                                "DAMAGED", "UNDER_REPAIR", "NEEDS_INSPECTION" -> MaterialTheme.colorScheme.tertiaryContainer
                                                "MISSING", "WRITTEN_OFF" -> MaterialTheme.colorScheme.errorContainer
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            selectedLabelColor = when (condition) {
                                                "GOOD" -> MaterialTheme.colorScheme.onPrimaryContainer
                                                "DAMAGED", "UNDER_REPAIR", "NEEDS_INSPECTION" -> MaterialTheme.colorScheme.onTertiaryContainer
                                                "MISSING", "WRITTEN_OFF" -> MaterialTheme.colorScheme.onErrorContainer
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (showLocation) {
                    OutlinedTextField(
                        value = actualLocationNote,
                        onValueChange = { actualLocationNote = it },
                        label = { Text("Kur radai") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Pastabos") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val actualQuantity = quantityText.toIntOrNull()
                    if (actualQuantity == null) {
                        onSelect(
                            AuditEntryDraft(
                                result = selectedResult,
                                actualQuantity = defaultDraft(item, selectedResult).actualQuantity,
                                conditionAtCheck = conditionAtCheck.takeIf { showCondition },
                                actualLocationNote = actualLocationNote.takeIf { showLocation }.orEmpty(),
                                notes = notes
                            )
                        )
                    } else {
                        onSelect(
                            AuditEntryDraft(
                                result = selectedResult,
                                actualQuantity = actualQuantity,
                                conditionAtCheck = conditionAtCheck.takeIf { showCondition },
                                actualLocationNote = actualLocationNote.takeIf { showLocation }.orEmpty(),
                                notes = notes
                            )
                        )
                    }
                }
            ) {
                Text("Išsaugoti")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text("Uždaryti")
            }
        }
    )
}

private data class AuditOption(
    val result: ItemCheckResult,
    val label: String,
    val icon: ImageVector
)
