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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    viewModel: InventoryAuditViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val auditResults by viewModel.auditResults.collectAsStateWithLifecycle()
    val showUncheckedOnly by viewModel.showUncheckedOnly.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isResolving by viewModel.isResolving.collectAsStateWithLifecycle()
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
                        "Sis QR kodas neatpazintas. Tikimasi formato ${QrPayload.forScanToken("token")}."
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
            setPrompt("Skenuok inventoriaus QR koda inventorizacijai")
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
                title = "Inventorizuoti kol kas nera ko",
                subtitle = "Pasirinktoje inventoriaus srityje neradome aktyviu daiktu.",
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
                            onMarkUncheckedMissing = viewModel::markUncheckedAsMissing
                        )
                    }

                    if (visibleItems.isEmpty()) {
                        item {
                            SkautaiEmptyState(
                                title = "Viskas jau pazymeta",
                                subtitle = "Sioje inventorizacijoje neliko neperziuretu daiktu.",
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
                                result = auditResults[item.id],
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
            currentResult = auditResults[item.id],
            onDismiss = { selectedItem = null },
            onSelect = { result ->
                viewModel.markItem(item.id, result)
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
    showUncheckedOnly: Boolean,
    onRefresh: () -> Unit,
    onScan: () -> Unit,
    onToggleUncheckedOnly: () -> Unit,
    onMarkUncheckedMissing: () -> Unit
) {
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
                SummaryChip("Ne vietoje", summary.misplaced, SkautaiStatusTone.Warning)
                SummaryChip("Sugadinta", summary.damaged, SkautaiStatusTone.Info)
            }

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
                    Text(if (isRefreshing) "Atnaujinama..." else "Atnaujinti sarasa")
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
                if (summary.unchecked > 0) {
                    OutlinedButton(onClick = onMarkUncheckedMissing) {
                        Text("Likusius zymeti kaip nerastus")
                    }
                }
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
    result: ItemCheckResult?,
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
                ItemCheckResultPill(result = result)
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkautaiChip(
                    label = "Bukle: ${itemConditionLabel(item.condition)}",
                    selected = false,
                    onClick = onClick
                )
                Text(
                    text = "Spausk zymeti",
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
    currentResult: ItemCheckResult?,
    onDismiss: () -> Unit,
    onSelect: (ItemCheckResult?) -> Unit
) {
    val options = listOf(
        AuditOption(ItemCheckResult.FOUND, "Rasta", Icons.Default.CheckCircle),
        AuditOption(ItemCheckResult.MISSING, "Nerasta", Icons.Default.HelpOutline),
        AuditOption(ItemCheckResult.MISPLACED, "Ne vietoje", Icons.Default.Place),
        AuditOption(ItemCheckResult.DAMAGED, "Sugadinta", Icons.Default.ReportProblem)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pasirink inventorizacijos rezultata.",
                    style = MaterialTheme.typography.bodyMedium
                )
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option.result) }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = if (currentResult == option.result) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(option.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(null) }) {
                Text("Isvalyti zyma")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Uzdaryti")
            }
        }
    )
}

private data class AuditOption(
    val result: ItemCheckResult,
    val label: String,
    val icon: ImageVector
)
