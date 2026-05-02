package lt.skautai.android.ui.inventory

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import lt.skautai.android.util.canCreateItems
import lt.skautai.android.util.canManageAllItems
import lt.skautai.android.util.canManageSharedInventory
import lt.skautai.android.util.hasPermissionAll
import lt.skautai.android.util.toPrintableQrItemOrNull

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
    if (canCreateSharedDirectly) "Prideti daikta" else "Pateikti patvirtinimui"

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
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedLocationId by viewModel.selectedLocationId.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatus.collectAsStateWithLifecycle()
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    val canApprove = permissions.canManageSharedInventory()
    val canCreateSharedDirectly = permissions.hasPermissionAll("items.create")
    val openedCustodianId = viewModel.openedCustodianId
    val pullRefreshState = rememberPullRefreshState(isRefreshing, viewModel::loadItems)
    val snackbarHostState = remember { SnackbarHostState() }

    val pendingItems = (uiState as? InventoryListUiState.Success)?.pendingItems.orEmpty()
    val filteredVisibleItems = remember(
        uiState,
        searchQuery,
        selectedType,
        selectedCategory,
        selectedLocationId,
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
                placeholder = "IeÅ¡koti pagal pavadinimÄ…, vietÄ… ar pastabas",
                leadingIcon = Icons.Default.Search
            )

            if (selectionMode) {
                QrSelectionBar(
                    selectedCount = selectedPrintableItems.size,
                    onGeneratePdf = {
                        if (selectedPrintableItems.isEmpty()) {
                            viewModel.onPdfShareFailed("Pasirink bent viena daikta QR PDF generavimui.")
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
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = { navController.navigate(NavRoutes.InventoryQrScanner.route) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Skenuoti QR")
                    }
                    OutlinedButton(
                        onClick = viewModel::enterSelectionMode,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Spausdinti QR")
                    }
                }
            }

            if (canApprove && pendingItems.isNotEmpty()) {
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
                selectedType = selectedType,
                selectedCategory = selectedCategory,
                selectedLocationId = selectedLocationId,
                selectedStatus = selectedStatus,
                locations = locations,
                openedCustodianId = openedCustodianId,
                canCreate = permissions.canCreateItems(),
                canCreateSharedDirectly = canCreateSharedDirectly,
                canViewInactive = permissions.canManageAllItems(),
                canApprovePending = canApprove,
                selectionMode = selectionMode,
                selectedItemIds = selectedItemIds,
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
private fun ApprovalBanner(
    items: List<ItemDto>,
    navController: NavController,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onApproveAll: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

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
                        text = "Laukia ${items.size} patvirtinimai",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Patikrink naujus irasus pries itraukiant i katalogus.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SkautaiStatusPill(label = items.size.toString(), tone = SkautaiStatusTone.Warning)
                IconButton(onClick = onApproveAll, enabled = items.isNotEmpty()) {
                    Icon(Icons.Default.Check, contentDescription = "Patvirtinti visus")
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Sutraukti" else "Isskleisti",
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
                            onReject = { onReject(item.id) }
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
    selectedType: String?,
    selectedCategory: String?,
    selectedLocationId: String?,
    selectedStatus: String,
    locations: List<LocationDto>,
    openedCustodianId: String?,
    canCreate: Boolean,
    canCreateSharedDirectly: Boolean,
    canViewInactive: Boolean,
    canApprovePending: Boolean,
    selectionMode: Boolean,
    selectedItemIds: Set<String>,
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
                title = "Inventorius tuÅ¡Äias",
                subtitle = inventoryContextEmptySubtitle(openedCustodianId, selectedType, selectedCategory),
                icon = Icons.Default.Inventory2,
                actionLabel = if (canCreate) addSharedActionLabel(canCreateSharedDirectly) else "Grizti i pradzia",
                onAction = {
                    if (canCreate) {
                        navController.navigate(NavRoutes.InventoryAddEdit.createRoute(mode = "SHARED"))
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
                selectedType,
                selectedCategory,
                selectedLocationId,
                locations
            ) {
                viewModel.filteredItems(state.items)
            }
            val typeFilters = remember(state.items) {
                listOf(
                    "COLLECTIVE" to ("Bendras" to state.items.count { it.type == "COLLECTIVE" }),
                    "ASSIGNED" to ("Priskirtas" to state.items.count { it.type == "ASSIGNED" }),
                    "INDIVIDUAL" to ("Asmeninis" to state.items.count { it.type == "INDIVIDUAL" })
                )
            }
            val categoryFilters = remember(state.items) {
                val counts = state.items.groupingBy { it.category }.eachCount()
                KnownInventoryCategories.map { category ->
                    category to (inventoryCategoryLabel(category) to (counts[category] ?: 0))
                }
            }

            InventoryCatalogContent(
                allItems = state.items,
                filteredItems = filtered,
                selectedType = selectedType,
                selectedCategory = selectedCategory,
                selectedLocationId = selectedLocationId,
                selectedStatus = selectedStatus,
                locations = locations,
                openedCustodianId = openedCustodianId,
                canViewInactive = canViewInactive,
                canApprovePending = canApprovePending,
                selectionMode = selectionMode,
                selectedItemIds = selectedItemIds,
                typeFilters = typeFilters,
                categoryFilters = categoryFilters,
                onClearFilters = viewModel::clearFilters,
                onTypeSelected = viewModel::onTypeSelected,
                onCategorySelected = viewModel::onCategorySelected,
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
    selectedType: String?,
    selectedCategory: String?,
    selectedLocationId: String?,
    selectedStatus: String,
    locations: List<LocationDto>,
    openedCustodianId: String?,
    canViewInactive: Boolean,
    canApprovePending: Boolean,
    selectionMode: Boolean,
    selectedItemIds: Set<String>,
    typeFilters: List<Pair<String, Pair<String, Int>>>,
    categoryFilters: List<Pair<String, Pair<String, Int>>>,
    onClearFilters: () -> Unit,
    onTypeSelected: (String?) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onLocationSelected: (String?) -> Unit,
    onStatusSelected: (String) -> Unit,
    onOpenItem: (String) -> Unit,
    onToggleItemSelection: (String, Boolean) -> Unit
) {
    val groups = remember(filteredItems) { filteredItems.toInventoryGroups() }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(0.dp),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
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
                        SkautaiSectionHeader(
                            title = "Filtrai",
                            subtitle = if (selectedType == null && selectedCategory == null) {
                                "Rodymas pagal visÄ… inventoriÅ³."
                            } else {
                                "Aktyvus filtrai padeda greitai susiaurinti sarasa."
                            }
                        )
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
                            item {
                                SkautaiChip(
                                    label = "Visi (${allItems.size})",
                                    selected = selectedType == null && selectedCategory == null,
                                    onClick = onClearFilters
                                )
                            }
                            items(typeFilters, key = { it.first }) { (type, chip) ->
                                SkautaiChip(
                                    label = "${chip.first} (${chip.second})",
                                    selected = selectedType == type,
                                    onClick = { onTypeSelected(if (selectedType == type) null else type) }
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
                                    selected = selectedCategory == category,
                                    onClick = { onCategorySelected(if (selectedCategory == category) null else category) }
                                )
                            }
                        }
                        if (locations.isNotEmpty()) {
                            LocationFilterRows(
                                locations = locations,
                                selectedLocationId = selectedLocationId,
                                onLocationSelected = onLocationSelected
                            )
                        }
                    }
                }
            }
        }

        if (filteredItems.isEmpty()) {
            item {
                SkautaiEmptyState(
                    title = "Nieko n?rasta",
                    subtitle = noResultsSubtitle(openedCustodianId, selectedType, selectedCategory),
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
private fun LocationFilterRows(
    locations: List<LocationDto>,
    selectedLocationId: String?,
    onLocationSelected: (String?) -> Unit
) {
    val nodesByParent = remember(locations) { locations.groupBy { it.parentLocationId } }
    val locationById = remember(locations) { locations.associateBy { it.id } }
    val selectedPath = remember(selectedLocationId, locations) {
        buildList {
            var current = selectedLocationId
            while (current != null) {
                add(0, current)
                current = locationById[current]?.parentLocationId
            }
        }
    }
    val levels = buildList {
        add(nodesByParent[null].orEmpty().sortedBy { it.name.lowercase() })
        selectedPath.forEach { id ->
            val children = nodesByParent[id].orEmpty().sortedBy { it.name.lowercase() }
            if (children.isNotEmpty()) add(children)
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
                        selected = selectedLocationId == location.id,
                        onClick = {
                            onLocationSelected(if (selectedLocationId == location.id) null else location.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InventoryGroupHeader(title: String, count: Int) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "$title / $count",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = if (selectionMode) onToggleSelection else onOpen)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    text = "${item.quantity} vnt. / ${inventoryCategoryLabel(item.category)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (selectionMode) {
                SelectionStatePill(
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
                    Text("Atsaukti")
                }
            }
        }
    }
}

@Composable
private fun SelectionStatePill(
    isSelected: Boolean,
    isSelectable: Boolean
) {
    when {
        !isSelectable -> SkautaiStatusPill(
            label = "Nespausd.",
            tone = SkautaiStatusTone.Neutral
        )
        isSelected -> SkautaiStatusPill(
            label = "Pazymeta",
            tone = SkautaiStatusTone.Success
        )
        else -> SkautaiStatusPill(
            label = "Pasirinkti",
            tone = SkautaiStatusTone.Info
        )
    }
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
        IconButton(onClick = onReject, modifier = Modifier.size(44.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Atmesti",
                tint = MaterialTheme.colorScheme.error
            )
        }
        IconButton(onClick = onApprove, modifier = Modifier.size(44.dp)) {
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
    selectedType: String?,
    selectedCategory: String?,
    items: List<ItemDto>
): String = when {
    openedCustodianId != null -> items.firstOrNull()?.custodianName ?: "Vieneto inventori?s"
    selectedType == "INDIVIDUAL" -> "Mano siulomi skolinti daiktai"
    selectedCategory != null -> inventoryCategoryLabel(selectedCategory)
    else -> "Bendras tunto inventori?s"
}

private fun inventoryContextSubtitle(
    openedCustodianId: String?,
    selectedType: String?,
    selectedCategory: String?
): String = when {
    openedCustodianId != null -> "Vieneto daiktai, ju b?kle ir priskyrimas."
    selectedType == "INDIVIDUAL" -> "Asmeniniai daiktai, kuriuos galima skolinti kitiems."
    selectedCategory != null -> "Filtruotas sarasas pagal pasirinkta kategorija."
    else -> "Bendro inventoriaus katalogas su b?kle, kilme ir viet?."
}

private fun inventoryContextEmptySubtitle(
    openedCustodianId: String?,
    selectedType: String?,
    selectedCategory: String?
): String = when {
    openedCustodianId != null -> "Å is vienetas dar neturi jam priskirtÅ³ ar savo sukurtÅ³ inventoriaus Ä¯raÅ¡Å³."
    selectedType == "INDIVIDUAL" -> "Dar nÄ—ra asmeniniÅ³ daiktÅ³, siÅ«lomÅ³ skolinti."
    selectedCategory != null -> "Å ioje kategorijoje dar nÄ—ra inventoriaus Ä¯raÅ¡Å³."
    else -> "Kai atsiras pirmi daiktai, Äia matysi bendro tunto inventoriaus katalogÄ…."
}

private fun noResultsSubtitle(
    openedCustodianId: String?,
    selectedType: String?,
    selectedCategory: String?
): String = when {
    openedCustodianId != null -> "Pabandyk kita paieskos fraze arba nuimk vieneto filtrus."
    selectedType != null || selectedCategory != null -> "Pabandyk pakeisti aktyvius filtrus arba paieskos fraze."
    else -> "Pabandyk kita paieskos fraze arba susiaurink sarasa pagal tipa."
}

private data class InventoryGroup(
    val title: String,
    val items: List<ItemDto>
)

private fun List<ItemDto>.toInventoryGroups(): List<InventoryGroup> =
    sortedWith(compareBy<ItemDto>({ it.type }, { inventoryCategoryLabel(it.category) }, { it.name.lowercase() }))
        .groupBy { item -> "${inventoryTypeLabel(item.type)} - ${inventoryCategoryLabel(item.category)}" }
        .map { (title, items) -> InventoryGroup(title, items) }
