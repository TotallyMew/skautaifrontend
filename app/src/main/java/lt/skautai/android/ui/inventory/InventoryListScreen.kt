package lt.skautai.android.ui.inventory

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Support
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.LocationDto
import lt.skautai.android.ui.common.RemoteImage
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiChip
import lt.skautai.android.ui.common.SkautaiConfirmDialog
import lt.skautai.android.ui.common.SkautaiEmptyState
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiSearchBar
import lt.skautai.android.ui.common.SkautaiSectionHeader
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.SkautaiStatusTone
import lt.skautai.android.ui.common.SkautaiSurfaceRole
import lt.skautai.android.ui.common.inventoryCategoryLabel
import lt.skautai.android.ui.common.inventoryTypeLabel
import lt.skautai.android.ui.common.itemConditionLabel
import lt.skautai.android.ui.common.skautaiSurfaceTone
import lt.skautai.android.util.NavRoutes
import lt.skautai.android.util.QrPdfShareLauncher
import lt.skautai.android.util.InventoryCsv
import lt.skautai.android.util.InventoryImportDraft
import lt.skautai.android.util.InventoryImportDuplicateMode
import lt.skautai.android.util.InventoryImportField
import lt.skautai.android.util.InventoryImportPreview
import lt.skautai.android.util.canCreateItems
import lt.skautai.android.util.canExportInventory
import lt.skautai.android.util.canGenerateInventoryQrPdf
import lt.skautai.android.util.canImportInventory
import lt.skautai.android.util.canManageAllItems
import lt.skautai.android.util.canManageSharedInventory
import lt.skautai.android.util.canReviewItemAdditions
import lt.skautai.android.util.canSubmitItemAddition
import lt.skautai.android.util.hasPermissionAll
import lt.skautai.android.util.toPrintableQrItemOrNull
import java.time.LocalDate

private val KnownInventoryCategories = listOf(
    "CAMPING",
    "TOOLS",
    "COOKING",
    "FIRST_AID",
    "UNIFORMS",
    "BOOKS",
    "PERSONAL_LOANS"
)

private fun addSharedActionLabel(canCreateSharedDirectly: Boolean): String =
    if (canCreateSharedDirectly) "Pridėti daiktą" else "Pateikti patvirtinimui"

private fun Int.inventoryApprovalNoun(): String {
    val lastTwo = this % 100
    val last = this % 10
    return when {
        lastTwo in 11..19 -> "patvirtinimų"
        last == 1 -> "patvirtinimas"
        last in 2..9 -> "patvirtinimai"
        else -> "patvirtinimų"
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun InventoryListScreen(
    navController: NavController,
    viewModel: InventoryListViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val selectionMode by viewModel.selectionMode.collectAsStateWithLifecycle()
    val selectedItemIds by viewModel.selectedItemIds.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val selectedTypes by viewModel.selectedTypes.collectAsStateWithLifecycle()
    val selectedCategories by viewModel.selectedCategories.collectAsStateWithLifecycle()
    val selectedLocationIds by viewModel.selectedLocationIds.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatus.collectAsStateWithLifecycle()
    val assignedOnly by viewModel.assignedOnly.collectAsStateWithLifecycle()
    val importDraft by viewModel.importDraft.collectAsStateWithLifecycle()
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    val canReviewAdditions = permissions.canReviewItemAdditions()
    val canApprove = canReviewAdditions || permissions.canSubmitItemAddition()
    val canExportCsv = permissions.canExportInventory()
    val canImportCsv = permissions.canImportInventory()
    val canGenerateQrPdf = permissions.canGenerateInventoryQrPdf()
    val canCreateSharedDirectly = permissions.hasPermissionAll("items.create")
    val openedCustodianId = viewModel.openedCustodianId
    val openedPersonalOwnerOnly = viewModel.openedPersonalOwnerOnly
    val pullRefreshState = rememberPullRefreshState(isRefreshing, viewModel::loadItems)
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingExportCsv by remember { mutableStateOf("") }
    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            runCatching { context.writeTextToUri(uri, pendingExportCsv) }
                .onSuccess { viewModel.onPdfShareFailed("CSV eksportas išsaugotas.") }
                .onFailure { viewModel.onPdfShareFailed(it.message ?: "Nepavyko išsaugoti CSV failo.") }
        }
        pendingExportCsv = ""
    }
    val importCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching { context.readInventoryImportTable(uri) }
                .onSuccess { (fileName, table) -> viewModel.prepareInventoryImport(fileName, table) }
                .onFailure { viewModel.onPdfShareFailed(it.message ?: "Nepavyko perskaityti importo failo.") }
        }
    }

    val pendingItems = (uiState as? InventoryListUiState.Success)?.pendingItems.orEmpty()
    val filteredVisibleItems = remember(
        uiState,
        searchQuery,
        selectedTypes,
        selectedCategories,
        selectedLocationIds,
        assignedOnly,
        locations
    ) {
        val successState = uiState as? InventoryListUiState.Success ?: return@remember emptyList()
        viewModel.filteredItems(successState.items)
    }
    val selectedPrintableItems = remember(filteredVisibleItems, selectedItemIds) {
        filteredVisibleItems
            .filter { it.id in selectedItemIds }
            .mapNotNull { it.toPrintableQrItemOrNull() }
    }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onActionMessageShown()
        }
    }

    importDraft?.let { draft ->
        InventoryImportMappingDialog(
            draft = draft,
            previewProvider = viewModel::previewInventoryImport,
            onHeaderRowSelected = viewModel::updateInventoryImportHeaderRow,
            onImport = viewModel::executeInventoryImport,
            onDismiss = viewModel::cancelInventoryImport
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SkautaiSearchBar(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                placeholder = "Ieškoti pagal pavadinimą, vietą ar pastabas",
                leadingIcon = Icons.Default.Search,
                trailingIcon = Icons.Default.QrCodeScanner,
                onTrailingIconClick = { navController.navigate(NavRoutes.InventoryQrScanner.route) }
            )

            if (selectionMode) {
                QrSelectionBar(
                    selectedCount = selectedPrintableItems.size,
                    onGeneratePdf = {
                        if (selectedPrintableItems.isEmpty()) {
                            viewModel.onPdfShareFailed("Pasirink bent vieną daiktą QR PDF generavimui.")
                        } else {
                            runCatching {
                                QrPdfShareLauncher.share(context, selectedPrintableItems)
                            }.onSuccess {
                                viewModel.onPdfShared()
                            }.onFailure {
                                viewModel.onPdfShareFailed(
                                    it.message ?: "Nepavyko sugeneruoti QR PDF"
                                )
                            }
                        }
                    },
                    onCancel = viewModel::exitSelectionMode
                )
            }

            if (canReviewAdditions && pendingItems.isNotEmpty()) {
                ApprovalBanner(
                    items = pendingItems,
                    navController = navController,
                    onApprove = viewModel::approveItem,
                    onReject = viewModel::rejectItem,
                    onApproveAll = viewModel::approveAllPending
                )
            }

            InventoryBody(
                state = uiState,
                searchQuery = searchQuery,
                selectedTypes = selectedTypes,
            selectedCategories = selectedCategories,
            selectedLocationIds = selectedLocationIds,
            selectedStatus = selectedStatus,
            assignedOnly = assignedOnly,
                locations = locations,
                openedCustodianId = openedCustodianId,
                openedPersonalOwnerOnly = openedPersonalOwnerOnly,
                canCreate = permissions.canCreateItems(),
                canCreateSharedDirectly = canCreateSharedDirectly,
                canViewInactive = permissions.canManageAllItems(),
                canApprovePending = canApprove,
                canExportCsv = canExportCsv,
                canImportCsv = canImportCsv,
                canGenerateQrPdf = canGenerateQrPdf,
                selectionMode = selectionMode,
                selectedItemIds = selectedItemIds,
                onExportCsv = {
                    pendingExportCsv = viewModel.inventoryExportCsv(filteredVisibleItems)
                    exportCsvLauncher.launch("inventorius-${LocalDate.now()}.csv")
                },
                onImportCsv = {
                    importCsvLauncher.launch(
                        arrayOf(
                            "text/*",
                            "text/csv",
                            "application/csv",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                    )
                },
                onStartQrSelection = viewModel::enterSelectionMode,
                viewModel = viewModel,
                navController = navController
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            SkautaiErrorSnackbarHost(hostState = snackbarHostState)
        }
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun InventoryImportMappingDialog(
    draft: InventoryImportDraft,
    previewProvider: (Map<InventoryImportField, Int?>, InventoryImportDuplicateMode) -> InventoryImportPreview?,
    onHeaderRowSelected: (Int) -> Unit,
    onImport: (Map<InventoryImportField, Int?>, InventoryImportDuplicateMode) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val savedTemplate = remember(draft.headers) { context.loadInventoryImportMapping(draft.headers) }
    var mapping by remember(draft) { mutableStateOf(savedTemplate ?: draft.suggestedMapping) }
    var duplicateMode by remember(draft) { mutableStateOf(InventoryImportDuplicateMode.Merge) }
    val preview = remember(draft, mapping, duplicateMode) {
        previewProvider(mapping, duplicateMode)
    }
    val requiredMissing = InventoryImportField.entries.any { it.required && mapping[it] == null }
    val canImport = preview != null && !preview.result.hasFatalErrors && preview.result.rows.isNotEmpty() && !requiredMissing

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Importo peržiūra") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = draft.fileName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${draft.rowCount} eilučių, ${draft.headers.size} stulpelių. Antraštė aptikta ${draft.headerRowIndex + 1} eilutėje.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (draft.unknownColumns.isNotEmpty()) {
                            Text(
                                text = "Neatpažinti stulpeliai bus ignoruoti: ${draft.unknownColumns.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    var headerExpanded by remember(draft.fileName, draft.headerRowIndex, draft.sourceRows.size) { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Antraštės eilutė", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Box {
                            OutlinedButton(onClick = { headerExpanded = true }) {
                                Text("${draft.headerRowIndex + 1} eilutė")
                            }
                            DropdownMenu(expanded = headerExpanded, onDismissRequest = { headerExpanded = false }) {
                                draft.sourceRows.take(12).forEachIndexed { index, row ->
                                    val preview = row.joinToString(" | ").take(60).ifBlank { "(tuščia)" }
                                    DropdownMenuItem(
                                        text = { Text("${index + 1}: $preview") },
                                        onClick = {
                                            onHeaderRowSelected(index)
                                            headerExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    SkautaiSectionHeader(
                        title = "Stulpelių mapinimas",
                        subtitle = if (savedTemplate != null) {
                            "Pritaikytas paskutinis šio formato mapping šablonas."
                        } else {
                            "Privalomas tik pavadinimas. Kiekis, kategorija, būklė ir tipas turi numatytas reikšmes."
                        }
                    )
                }

                items(InventoryImportField.entries, key = { it.key }) { field ->
                    ImportFieldMappingRow(
                        field = field,
                        headers = draft.headers,
                        selectedIndex = mapping[field],
                        onSelected = { index ->
                            mapping = mapping + (field to index)
                        }
                    )
                }

                item {
                    SkautaiSectionHeader(
                        title = "Sutapimai su esamais daiktais",
                        subtitle = "${preview?.duplicateExistingCount ?: 0} importo eilučių sutampa su esamu inventoriumi."
                    )
                }

                items(InventoryImportDuplicateMode.entries, key = { it.name }) { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { duplicateMode = mode }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = duplicateMode == mode,
                            onClick = { duplicateMode = mode }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = mode.label, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    ImportPreviewSummary(preview = preview)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    context.saveInventoryImportMapping(draft.headers, mapping)
                    onImport(mapping, duplicateMode)
                },
                enabled = canImport
            ) {
                Text("Importuoti")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Atšaukti")
            }
        }
    )
}

private fun android.content.Context.loadInventoryImportMapping(headers: List<String>): Map<InventoryImportField, Int?>? {
    val raw = getSharedPreferences("inventory_import", 0)
        .getString(headers.importTemplateKey(), null)
        ?: return null
    val values = raw.split("|").associate {
        val parts = it.split("=", limit = 2)
        parts.first() to parts.getOrNull(1)
    }
    val mapping = InventoryImportField.entries.associateWith { field ->
        values[field.key]?.toIntOrNull()?.takeIf { it in headers.indices }
    }
    return mapping.takeIf { it[InventoryImportField.Name] != null }
}

private fun android.content.Context.saveInventoryImportMapping(
    headers: List<String>,
    mapping: Map<InventoryImportField, Int?>
) {
    val encoded = InventoryImportField.entries.joinToString("|") { field ->
        "${field.key}=${mapping[field] ?: ""}"
    }
    getSharedPreferences("inventory_import", 0)
        .edit()
        .putString(headers.importTemplateKey(), encoded)
        .apply()
}

private fun List<String>.importTemplateKey(): String =
    "mapping_" + joinToString("|") { it.trim().lowercase() }.hashCode()

private fun ItemDto.unitLabel(): String =
    customFields.firstOrNull { it.fieldName.equals("Mato vienetas", ignoreCase = true) }
        ?.fieldValue
        ?.takeIf { it.isNotBlank() }
        ?: "vnt."

@Composable
private fun ImportFieldMappingRow(
    field: InventoryImportField,
    headers: List<String>,
    selectedIndex: Int?,
    onSelected: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = selectedIndex?.let { headers.getOrNull(it) }
        ?: field.defaultValue?.let { "Nenaudoti ($it)" }
        ?: "Nenaudoti"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(0.9f)) {
            Text(
                text = if (field.required) "${field.label} *" else field.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (field.required) FontWeight.SemiBold else FontWeight.Normal
            )
            field.defaultValue?.let {
                Text(
                    text = "Jei nemapinta: $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Box(modifier = Modifier.weight(1.1f)) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = selectedLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (!field.required) {
                    DropdownMenuItem(
                        text = { Text(field.defaultValue?.let { "Nenaudoti ($it)" } ?: "Nenaudoti") },
                        onClick = {
                            expanded = false
                            onSelected(null)
                        }
                    )
                }
                headers.forEachIndexed { index, header ->
                    DropdownMenuItem(
                        text = { Text(header.ifBlank { "Stulpelis ${index + 1}" }) },
                        onClick = {
                            expanded = false
                            onSelected(index)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportPreviewSummary(preview: InventoryImportPreview?) {
    val result = preview?.result
    val error = result?.errors?.firstOrNull()
    SkautaiCard(
        modifier = Modifier.fillMaxWidth(),
        tonal = skautaiSurfaceTone(
            if (error == null) SkautaiSurfaceRole.Muted else SkautaiSurfaceRole.Identity
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (error == null) "Kas bus importuota" else "Reikia pataisyti mappingą",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = "Nauji įrašai: ${preview?.rowsToCreateCount ?: 0}, atnaujinami: ${preview?.rowsToUpdateCount ?: 0}, praleistos eilutės: ${result?.skippedRows ?: 0}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val sample = result?.rows.orEmpty().take(3).joinToString("; ") {
                    "${it.name} (${it.quantity} vnt.)"
                }
                if (sample.isNotBlank()) {
                    Text(
                        text = "Pavyzdžiai: $sample",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if ((result?.mergedRows ?: 0) > 0) {
                    Text(
                        text = "Failo viduje sujungta pasikartojančių eilučių: ${result?.mergedRows}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                result?.warnings?.take(2)?.forEach { warning ->
                    Text(
                        text = warning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ApprovalBanner(
    items: List<ItemDto>,
    navController: NavController,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onApproveAll: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showApproveAllConfirm by remember { mutableStateOf(false) }
    var rejectedItem by remember { mutableStateOf<ItemDto?>(null) }

    if (showApproveAllConfirm) {
        SkautaiConfirmDialog(
            title = "Patvirtinti visus?",
            message = "Bus patvirtinti ${items.size} laukiantys inventoriaus įrašai.",
            confirmText = "Patvirtinti",
            onConfirm = {
                showApproveAllConfirm = false
                onApproveAll()
            },
            onDismiss = { showApproveAllConfirm = false },
            enabled = items.isNotEmpty()
        )
    }

    rejectedItem?.let { item ->
        SkautaiConfirmDialog(
            title = "Atmesti įrašą?",
            message = "Įrašas „${item.name}“ bus atmestas ir nebus įtrauktas į katalogą.",
            confirmText = "Atmesti",
            onConfirm = {
                rejectedItem = null
                onReject(item.id)
            },
            onDismiss = { rejectedItem = null },
            isDanger = true
        )
    }

    SkautaiCard(
        modifier = Modifier.fillMaxWidth(),
        tonal = skautaiSurfaceTone(SkautaiSurfaceRole.Identity)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Laukia ${items.size} ${items.size.inventoryApprovalNoun()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Patikrink naujus įrašus prieš įtraukiant į katalogus.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SkautaiStatusPill(label = items.size.toString(), tone = SkautaiStatusTone.Warning)
                IconButton(onClick = { showApproveAllConfirm = true }, enabled = items.isNotEmpty()) {
                    Icon(Icons.Default.Check, contentDescription = "Patvirtinti visus")
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Sutraukti" else "Išskleisti",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    items.forEach { item ->
                        PendingInventoryRow(
                            item = item,
                            onOpen = { navController.navigate(NavRoutes.InventoryDetail.createRoute(item.id)) },
                            onApprove = { onApprove(item.id) },
                            onReject = { rejectedItem = item }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryBody(
    state: InventoryListUiState,
    searchQuery: String,
    selectedTypes: Set<String>,
    selectedCategories: Set<String>,
    selectedLocationIds: Set<String>,
    selectedStatus: String,
    assignedOnly: Boolean,
    locations: List<LocationDto>,
    openedCustodianId: String?,
    openedPersonalOwnerOnly: Boolean,
    canCreate: Boolean,
    canCreateSharedDirectly: Boolean,
    canViewInactive: Boolean,
    canApprovePending: Boolean,
    canExportCsv: Boolean,
    canImportCsv: Boolean,
    canGenerateQrPdf: Boolean,
    selectionMode: Boolean,
    selectedItemIds: Set<String>,
    onExportCsv: () -> Unit,
    onImportCsv: () -> Unit,
    onStartQrSelection: () -> Unit,
    viewModel: InventoryListViewModel,
    navController: NavController
) {
    when (state) {
        is InventoryListUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is InventoryListUiState.Empty -> {
            SkautaiEmptyState(
                title = "Inventorius tuščias",
                subtitle = inventoryContextEmptySubtitle(openedCustodianId, openedPersonalOwnerOnly, selectedTypes, selectedCategories),
                icon = Icons.Default.Inventory2,
                    actionLabel = if (canCreate) addSharedActionLabel(canCreateSharedDirectly) else "Grįžti į pradžią",
                onAction = {
                    if (canCreate) {
                        val mode = when {
                            openedPersonalOwnerOnly || "INDIVIDUAL" in selectedTypes -> "PERSONAL"
                            openedCustodianId != null -> "UNIT_OWN"
                            else -> "SHARED"
                        }
                        navController.navigate(
                            NavRoutes.InventoryAddEdit.createRoute(
                                mode = mode,
                                custodianId = if (mode == "UNIT_OWN") openedCustodianId else null
                            )
                        )
                    } else {
                        navController.navigate(NavRoutes.Home.route)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        is InventoryListUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize()) {
                SkautaiErrorState(
                    message = state.message,
                    onRetry = viewModel::loadItems,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        is InventoryListUiState.Success -> {
            val filtered = remember(
                state.items,
                searchQuery,
                selectedTypes,
                selectedCategories,
                selectedLocationIds,
                locations
            ) {
                viewModel.filteredItems(state.items)
            }
            val typeFilters = remember(state.items) {
                listOf(
                    "COLLECTIVE" to ("Bendras" to state.items.count { it.effectiveInventoryType() == "COLLECTIVE" }),
                    "ASSIGNED" to ("Priskirtas" to state.items.count { it.effectiveInventoryType() == "ASSIGNED" }),
                    "INDIVIDUAL" to ("Asmeninis" to state.items.count { it.effectiveInventoryType() == "INDIVIDUAL" })
                )
            }
        val categoryFilters = remember(state.items) {
            val counts = state.items.groupingBy { it.category }.eachCount()
            (KnownInventoryCategories + state.items.map { it.category })
                .distinct()
                .map { category ->
                category to (inventoryCategoryLabel(category) to (counts[category] ?: 0))
            }
        }

            InventoryCatalogContent(
                allItems = state.items,
                filteredItems = filtered,
                selectedTypes = selectedTypes,
                selectedCategories = selectedCategories,
                selectedLocationIds = selectedLocationIds,
                selectedStatus = selectedStatus,
                assignedOnly = assignedOnly,
                locations = locations,
                openedCustodianId = openedCustodianId,
                canViewInactive = canViewInactive,
                canApprovePending = canApprovePending,
                canExportCsv = canExportCsv,
                canImportCsv = canImportCsv,
                canGenerateQrPdf = canGenerateQrPdf,
                selectionMode = selectionMode,
                selectedItemIds = selectedItemIds,
                onExportCsv = onExportCsv,
                onImportCsv = onImportCsv,
                onStartQrSelection = onStartQrSelection,
                typeFilters = typeFilters,
                categoryFilters = categoryFilters,
                onClearFilters = viewModel::clearFilters,
                onClearTypeFilters = viewModel::clearTypeFilters,
                onTypeSelected = viewModel::onTypeSelected,
                onCategorySelected = viewModel::onCategorySelected,
                onAssignedOnlyChange = viewModel::onAssignedOnlyChange,
                onLocationSelected = viewModel::onLocationSelected,
                onStatusSelected = viewModel::onStatusSelected,
                onOpenItem = { itemId -> navController.navigate(NavRoutes.InventoryDetail.createRoute(itemId)) },
                onToggleItemSelection = viewModel::toggleSelectedItem
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InventoryCatalogContent(
    allItems: List<ItemDto>,
    filteredItems: List<ItemDto>,
    selectedTypes: Set<String>,
    selectedCategories: Set<String>,
    selectedLocationIds: Set<String>,
    selectedStatus: String,
    assignedOnly: Boolean,
    locations: List<LocationDto>,
    openedCustodianId: String?,
    canViewInactive: Boolean,
    canApprovePending: Boolean,
    canExportCsv: Boolean,
    canImportCsv: Boolean,
    canGenerateQrPdf: Boolean,
    selectionMode: Boolean,
    selectedItemIds: Set<String>,
    onExportCsv: () -> Unit,
    onImportCsv: () -> Unit,
    onStartQrSelection: () -> Unit,
    typeFilters: List<Pair<String, Pair<String, Int>>>,
    categoryFilters: List<Pair<String, Pair<String, Int>>>,
    onClearFilters: () -> Unit,
    onClearTypeFilters: () -> Unit,
    onTypeSelected: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onAssignedOnlyChange: (Boolean) -> Unit,
    onLocationSelected: (String) -> Unit,
    onStatusSelected: (String) -> Unit,
    onOpenItem: (String) -> Unit,
    onToggleItemSelection: (String, Boolean) -> Unit
) {
    val groups = remember(filteredItems) { filteredItems.toInventoryGroups() }
    var filtersExpanded by remember { mutableStateOf(false) }
    var toolsExpanded by remember { mutableStateOf(false) }
    val activeFilterCount = selectedTypes.size + selectedCategories.size + selectedLocationIds.size +
        (if (assignedOnly) 1 else 0) +
        (if (selectedStatus != "ACTIVE") 1 else 0)
    val assignedCount = allItems.count { it.isAssignedToPerson() }
    val canShowTools = canExportCsv || canImportCsv || canGenerateQrPdf
    val hasPrintableItems = filteredItems.any { it.toPrintableQrItemOrNull() != null }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(0.dp),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        if (!selectionMode && canShowTools) {
            item {
                InventoryToolsCard(
                    expanded = toolsExpanded,
                    canExportCsv = canExportCsv,
                    canImportCsv = canImportCsv,
                    canGenerateQrPdf = canGenerateQrPdf,
                    canGenerateQrPdfNow = hasPrintableItems,
                    onExpandedChange = { toolsExpanded = it },
                    onExportCsv = onExportCsv,
                    onImportCsv = onImportCsv,
                    onStartQrSelection = onStartQrSelection
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkautaiCard(
                    modifier = Modifier.fillMaxWidth(),
                    tonal = skautaiSurfaceTone(SkautaiSurfaceRole.Muted)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { filtersExpanded = !filtersExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                SkautaiSectionHeader(
                                    title = "Filtrai",
                                    subtitle = if (activeFilterCount == 0) {
                                        "Rodymas pagal visą inventorių."
                                    } else {
                                        "Aktyvu: $activeFilterCount"
                                    }
                                )
                            }
                            IconButton(onClick = { filtersExpanded = !filtersExpanded }) {
                                Icon(
                                    imageVector = if (filtersExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (filtersExpanded) "Sutraukti filtrus" else "Isskleisti filtrus"
                                )
                            }
                        }
                        AnimatedVisibility(visible = filtersExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(vertical = 2.dp)
                                ) {
                                    item {
                                        SkautaiChip(
                                            label = "Aktyvus",
                                            selected = selectedStatus == "ACTIVE",
                                            onClick = { onStatusSelected("ACTIVE") }
                                        )
                                    }
                                    if (canViewInactive) {
                                        item {
                                            SkautaiChip(
                                                label = "Neaktyvus",
                                                selected = selectedStatus == "INACTIVE",
                                                onClick = { onStatusSelected("INACTIVE") }
                                            )
                                        }
                                    }
                                    if (canApprovePending) {
                                        item {
                                            SkautaiChip(
                                                label = "Laukia",
                                                selected = selectedStatus == "PENDING_APPROVAL",
                                                onClick = { onStatusSelected("PENDING_APPROVAL") }
                                            )
                                        }
                                    }
                                }
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(vertical = 2.dp)
                                ) {
                                    if (selectedTypes.isNotEmpty()) {
                                        item {
                                            SkautaiChip(
                                                label = "Nuvalyti tipus",
                                                selected = false,
                                                onClick = onClearTypeFilters
                                            )
                                        }
                                    }
                                    item {
                                        SkautaiChip(
                                            label = "Visi (${allItems.size})",
                                            selected = activeFilterCount == 0,
                                            onClick = onClearFilters
                                        )
                                    }
                                    item {
                                        SkautaiChip(
                                            label = "Priskirta ($assignedCount)",
                                            selected = assignedOnly,
                                            onClick = { onAssignedOnlyChange(!assignedOnly) }
                                        )
                                    }
                                    items(typeFilters, key = { it.first }) { (type, chip) ->
                                        SkautaiChip(
                                            label = "${chip.first} (${chip.second})",
                                            selected = type in selectedTypes,
                                            onClick = { onTypeSelected(type) }
                                        )
                                    }
                                }
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 2.dp)
                                ) {
                                    items(categoryFilters, key = { "category_${it.first}" }) { (category, chip) ->
                                        SkautaiChip(
                                            label = "${chip.first} (${chip.second})",
                                            selected = category in selectedCategories,
                                            onClick = { onCategorySelected(category) }
                                        )
                                    }
                                }
                                if (locations.isNotEmpty()) {
                                    LocationFilterRows(
                                        locations = locations,
                                        selectedLocationIds = selectedLocationIds,
                                        onLocationSelected = onLocationSelected
                                    )
                                }
                            }
                        }
                        if (!filtersExpanded && activeFilterCount > 0) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                item {
                                    SkautaiChip(
                                        label = "Valyti filtrus",
                                        selected = false,
                                        onClick = onClearFilters
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (filteredItems.isEmpty()) {
            item {
                SkautaiEmptyState(
                    title = "Nieko nerasta",
                    subtitle = noResultsSubtitle(openedCustodianId, selectedTypes, selectedCategories),
                    icon = Icons.Default.Inventory2
                )
            }
        } else {
            groups.forEach { group ->
                stickyHeader(key = "header_${group.title}") {
                    InventoryGroupHeader(title = group.title, count = group.items.size)
                }
                items(group.items, key = { it.id }) { item ->
                    val isSelectable = item.toPrintableQrItemOrNull() != null
                    InventoryDenseRow(
                        item = item,
                        selectionMode = selectionMode,
                        isSelected = item.id in selectedItemIds,
                        isSelectable = isSelectable,
                        onOpen = { onOpenItem(item.id) },
                        onToggleSelection = { onToggleItemSelection(item.id, isSelectable) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun InventoryToolsCard(
    expanded: Boolean,
    canExportCsv: Boolean,
    canImportCsv: Boolean,
    canGenerateQrPdf: Boolean,
    canGenerateQrPdfNow: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onExportCsv: () -> Unit,
    onImportCsv: () -> Unit,
    onStartQrSelection: () -> Unit
) {
    val availableCount = listOf(canExportCsv, canImportCsv, canGenerateQrPdf).count { it }
    SkautaiCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        tonal = skautaiSurfaceTone(SkautaiSurfaceRole.Muted)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!expanded) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SkautaiSectionHeader(
                        title = "Įrankiai",
                        subtitle = "$availableCount veiksmai: eksportas, importas ir QR"
                    )
                }
                IconButton(onClick = { onExpandedChange(!expanded) }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Sutraukti įrankius" else "Išskleisti įrankius"
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (canExportCsv) {
                            OutlinedButton(
                                onClick = onExportCsv,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text("Eksportuoti CSV", maxLines = 1)
                            }
                        }
                        if (canImportCsv) {
                            Button(
                                onClick = onImportCsv,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text("Importuoti CSV", maxLines = 1)
                            }
                        }
                    }
                    if (canGenerateQrPdf) {
                        OutlinedButton(
                            onClick = onStartQrSelection,
                            enabled = canGenerateQrPdfNow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Generuoti QR PDF", maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationFilterRows(
    locations: List<LocationDto>,
    selectedLocationIds: Set<String>,
    onLocationSelected: (String) -> Unit
) {
    val nodesByParent = remember(locations) { locations.groupBy { it.parentLocationId } }
    val locationById = remember(locations) { locations.associateBy { it.id } }
    val expandedParentIds = remember(selectedLocationIds, locations) {
        buildSet {
            selectedLocationIds.forEach { selectedId ->
                var current: String? = selectedId
                while (current != null) {
                    add(current)
                    current = locationById[current]?.parentLocationId
                }
            }
        }
    }
    val levels = buildList {
        var currentLevel = nodesByParent[null].orEmpty().sortedBy { it.name.lowercase() }
        while (currentLevel.isNotEmpty()) {
            add(currentLevel)
            val children = currentLevel
                .filter { it.id in expandedParentIds }
                .flatMap { nodesByParent[it.id].orEmpty() }
                .sortedBy { it.name.lowercase() }
            currentLevel = children
        }
    }.filter { it.isNotEmpty() }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        levels.forEachIndexed { levelIndex, levelLocations ->
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = if (levelIndex == levels.lastIndex) 2.dp else 0.dp)
            ) {
                items(levelLocations, key = { it.id }) { location ->
                    SkautaiChip(
                        label = location.name,
                        selected = location.id in selectedLocationIds,
                        onClick = { onLocationSelected(location.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InventoryGroupHeader(title: String, count: Int) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 6.dp)
            ) {
                Text(
                    text = "$title / $count",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun InventoryDenseRow(
    item: ItemDto,
    selectionMode: Boolean,
    isSelected: Boolean,
    isSelectable: Boolean,
    onOpen: () -> Unit,
    onToggleSelection: () -> Unit
) {
    val subtitle = listOfNotNull(
        item.custodianName?.takeIf { it.isNotBlank() },
        item.locationPath?.takeIf { it.isNotBlank() },
        item.temporaryStorageLabel?.takeIf { it.isNotBlank() }
    ).joinToString(" / ")

    Surface(
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f)
        } else {
            MaterialTheme.colorScheme.background
        },
        shape = if (selectionMode) RoundedCornerShape(8.dp) else RoundedCornerShape(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = if (selectionMode) onToggleSelection else onOpen)
            .padding(vertical = if (selectionMode) 4.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = if (selectionMode) 8.dp else 0.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (selectionMode) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .heightIn(min = 52.dp)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(999.dp)
                        )
                )
            }
            InventoryRowVisual(item = item)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.quantity} ${item.unitLabel()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (selectionMode) {
                SelectionCheckbox(
                    isSelected = isSelected,
                    isSelectable = isSelectable
                )
            } else {
                SkautaiStatusPill(
                    label = itemConditionLabel(item.condition),
                    tone = conditionTone(item.condition)
                )
            }
        }
    }
}

@Composable
private fun QrSelectionBar(
    selectedCount: Int,
    onGeneratePdf: () -> Unit,
    onCancel: () -> Unit
) {
    SkautaiCard(
        modifier = Modifier.fillMaxWidth(),
        tonal = skautaiSurfaceTone(SkautaiSurfaceRole.Identity)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Pasirinkta QR PDF: $selectedCount",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onGeneratePdf,
                    enabled = selectedCount > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Generuoti PDF")
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Atšaukti")
                }
            }
        }
    }
}

@Composable
private fun SelectionCheckbox(
    isSelected: Boolean,
    isSelectable: Boolean
) {
    Checkbox(
        checked = isSelected,
        onCheckedChange = null,
        enabled = isSelectable
    )
}

@Composable
private fun PendingInventoryRow(
    item: ItemDto,
    onOpen: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val subtitle = listOfNotNull(
        item.custodianName?.takeIf { it.isNotBlank() },
        item.locationPath?.takeIf { it.isNotBlank() }
    ).joinToString(" / ").ifBlank { "Laukia patvirtinimo" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InventoryRowVisual(item = item)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "${item.name} (${item.quantity} vnt.)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onReject, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Atmesti",
                tint = MaterialTheme.colorScheme.error
            )
        }
        IconButton(onClick = onApprove, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Patvirtinti",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun InventoryRowVisual(item: ItemDto) {
    if (!item.photoUrl.isNullOrBlank()) {
        RemoteImage(
            imageUrl = item.photoUrl,
            contentDescription = item.name,
            modifier = Modifier.size(52.dp)
        )
        return
    }

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
                imageVector = categoryIcon(item.category),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(10.dp)
                    .background(conditionDotColor(item.condition), CircleShape)
            )
        }
    }
}

private fun categoryIcon(category: String): ImageVector = when (category) {
    "CAMPING" -> Icons.Default.Warehouse
    "TOOLS" -> Icons.Default.Build
    "COOKING" -> Icons.Default.Folder
    "FIRST_AID" -> Icons.Default.HealthAndSafety
    "UNIFORMS" -> Icons.Default.Shield
    "BOOKS" -> Icons.Default.MenuBook
    "PERSONAL_LOANS" -> Icons.Default.Support
    else -> Icons.Default.Inventory2
}

@Composable
private fun conditionDotColor(condition: String): Color = when (condition) {
    "GOOD" -> MaterialTheme.colorScheme.primary
    "DAMAGED" -> MaterialTheme.colorScheme.tertiary
    "WRITTEN_OFF" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun conditionTone(condition: String): SkautaiStatusTone = when (condition) {
    "GOOD" -> SkautaiStatusTone.Success
    "DAMAGED" -> SkautaiStatusTone.Warning
    "WRITTEN_OFF" -> SkautaiStatusTone.Danger
    else -> SkautaiStatusTone.Neutral
}

private fun inventoryContextTitle(
    openedCustodianId: String?,
    openedPersonalOwnerOnly: Boolean,
    selectedType: String?,
    selectedCategory: String?,
    items: List<ItemDto>
): String = when {
    openedCustodianId != null -> items.firstOrNull()?.custodianName ?: "Vieneto inventorius"
    selectedType == "INDIVIDUAL" && openedPersonalOwnerOnly -> "Mano siūlomi skolinti daiktai"
    selectedType == "INDIVIDUAL" -> "Asmeniniai skolinami daiktai"
    selectedCategory != null -> inventoryCategoryLabel(selectedCategory)
    else -> "Bendras tunto inventorius"
}

private fun inventoryContextSubtitle(
    openedCustodianId: String?,
    openedPersonalOwnerOnly: Boolean,
    selectedType: String?,
    selectedCategory: String?
): String = when {
    openedCustodianId != null -> "Vieneto daiktai, jų būklė ir priskyrimas."
    selectedType == "INDIVIDUAL" && openedPersonalOwnerOnly -> "Tavo asmeniniai daiktai, kuriuos siūlai skolinti kitiems."
    selectedType == "INDIVIDUAL" -> "Visų narių asmeniniai daiktai, kuriuos galima skolinti kitiems."
    selectedCategory != null -> "Filtruotas sąrašas pagal pasirinktą kategoriją."
    else -> "Bendro inventoriaus katalogas su būkle, kilme ir vieta."
}

private fun inventoryContextEmptySubtitle(
    openedCustodianId: String?,
    openedPersonalOwnerOnly: Boolean,
    selectedTypes: Set<String>,
    selectedCategories: Set<String>
): String = when {
    openedCustodianId != null -> "Šis vienetas dar neturi savo inventoriaus įrašų."
    openedPersonalOwnerOnly -> "Dar neturi asmeninių daiktų, kuriuos siūlai skolinti kitiems."
    "INDIVIDUAL" in selectedTypes -> "Dar nėra asmeninių daiktų, siūlomų skolinti."
    selectedCategories.isNotEmpty() -> "Pasirinktose kategorijose dar nėra inventoriaus įrašų."
    else -> "Kai atsiras pirmi daiktai, čia matysi bendro tunto inventoriaus katalogą."
}

private fun noResultsSubtitle(
    openedCustodianId: String?,
    selectedTypes: Set<String>,
    selectedCategories: Set<String>
): String = when {
    openedCustodianId != null -> "Pabandyk kitą paieškos frazę arba nuimk vieneto filtrus."
    selectedTypes.isNotEmpty() || selectedCategories.isNotEmpty() -> "Pabandyk pakeisti aktyvius filtrus arba paieškos frazę."
    else -> "Pabandyk kitą paieškos frazę arba susiaurink sąrašą pagal tipą."
}

private data class InventoryGroup(
    val title: String,
    val items: List<ItemDto>
)

private fun List<ItemDto>.toInventoryGroups(): List<InventoryGroup> =
    sortedWith(compareBy<ItemDto>({ it.effectiveInventoryType() }, { inventoryCategoryLabel(it.category) }, { it.name.lowercase() }))
        .groupBy { item -> "${inventoryTypeLabel(item.effectiveInventoryType())} - ${inventoryCategoryLabel(item.category)}" }
        .map { (title, items) -> InventoryGroup(title, items) }

private fun ItemDto.effectiveInventoryType(): String =
    if (origin == "TRANSFERRED_FROM_TUNTAS" && custodianId != null) "COLLECTIVE" else type

private fun ItemDto.isAssignedToPerson(): Boolean =
    effectiveInventoryType() == "ASSIGNED" || responsibleUserId != null

private fun android.content.Context.writeTextToUri(uri: Uri, text: String) {
    contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
        writer.write(text)
    } ?: error("Nepavyko atidaryti failo rašymui.")
}

private fun android.content.Context.readInventoryImportTable(uri: Uri): Pair<String, List<List<String>>> {
    val fileName = displayName(uri) ?: uri.lastPathSegment ?: "inventorius"
    val mime = contentResolver.getType(uri).orEmpty()
    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: error("Nepavyko atidaryti failo skaitymui.")
    val table = if (
        fileName.endsWith(".xlsx", ignoreCase = true) ||
        mime == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    ) {
        InventoryCsv.parseXlsxTable(bytes)
    } else {
        InventoryCsv.parseTextTable(bytes.toString(Charsets.UTF_8))
    }
    return fileName to table
}

private fun android.content.Context.displayName(uri: Uri): String? =
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) cursor.getString(index) else null
        } else {
            null
        }
    }
