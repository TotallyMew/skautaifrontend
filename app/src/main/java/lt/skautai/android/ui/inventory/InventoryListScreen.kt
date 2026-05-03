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
        selectedTypes,
        selectedCategories,
        selectedLocationIds,
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
                placeholder = "Ieškoti pagal pavadinimą, vietą ar pastabas",
                leadingIcon = Icons.Default.Search
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
                selectedTypes = selectedTypes,
                selectedCategories = selectedCategories,
                selectedLocationIds = selectedLocationIds,
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
                IconButton(onClick = onApproveAll, enabled = items.isNotEmpty()) {
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
    selectedTypes: Set<String>,
    selectedCategories: Set<String>,
    selectedLocationIds: Set<String>,
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
                title = "Inventorius tuščias",
                subtitle = inventoryContextEmptySubtitle(openedCustodianId, selectedTypes, selectedCategories),
                icon = Icons.Default.Inventory2,
                    actionLabel = if (canCreate) addSharedActionLabel(canCreateSharedDirectly) else "Grįžti į pradžią",
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
                selectedTypes,
                selectedCategories,
                selectedLocationIds,
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
                selectedTypes = selectedTypes,
                selectedCategories = selectedCategories,
                selectedLocationIds = selectedLocationIds,
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
    selectedTypes: Set<String>,
    selectedCategories: Set<String>,
    selectedLocationIds: Set<String>,
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
    onTypeSelected: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onLocationSelected: (String) -> Unit,
    onStatusSelected: (String) -> Unit,
    onOpenItem: (String) -> Unit,
    onToggleItemSelection: (String, Boolean) -> Unit
) {
    val groups = remember(filteredItems) { filteredItems.toInventoryGroups() }
    var filtersExpanded by remember { mutableStateOf(false) }
    val activeFilterCount = selectedTypes.size + selectedCategories.size + selectedLocationIds.size +
        if (selectedStatus != "ACTIVE") 1 else 0

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
                                    item {
                                        SkautaiChip(
                                            label = "Visi (${allItems.size})",
                                            selected = activeFilterCount == 0,
                                            onClick = onClearFilters
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
                    Text("Atšaukti")
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
            label = "Pažymėta",
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
    openedCustodianId != null -> items.firstOrNull()?.custodianName ?: "Vieneto inventorius"
    selectedType == "INDIVIDUAL" -> "Mano siūlomi skolinti daiktai"
    selectedCategory != null -> inventoryCategoryLabel(selectedCategory)
    else -> "Bendras tunto inventorius"
}

private fun inventoryContextSubtitle(
    openedCustodianId: String?,
    selectedType: String?,
    selectedCategory: String?
): String = when {
    openedCustodianId != null -> "Vieneto daiktai, jų būklė ir priskyrimas."
    selectedType == "INDIVIDUAL" -> "Asmeniniai daiktai, kuriuos galima skolinti kitiems."
    selectedCategory != null -> "Filtruotas sąrašas pagal pasirinktą kategoriją."
    else -> "Bendro inventoriaus katalogas su būkle, kilme ir vieta."
}

private fun inventoryContextEmptySubtitle(
    openedCustodianId: String?,
    selectedTypes: Set<String>,
    selectedCategories: Set<String>
): String = when {
    openedCustodianId != null -> "Šis vienetas dar neturi jam priskirtų ar savo sukurtų inventoriaus įrašų."
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
    sortedWith(compareBy<ItemDto>({ it.type }, { inventoryCategoryLabel(it.category) }, { it.name.lowercase() }))
        .groupBy { item -> "${inventoryTypeLabel(item.type)} - ${inventoryCategoryLabel(item.category)}" }
        .map { (title, items) -> InventoryGroup(title, items) }
