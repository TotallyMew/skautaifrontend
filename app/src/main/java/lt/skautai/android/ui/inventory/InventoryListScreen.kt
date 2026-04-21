package lt.skautai.android.ui.inventory

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiChip
import lt.skautai.android.ui.common.SkautaiEmptyState
import lt.skautai.android.ui.common.SkautaiSearchBar
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.inventoryTypeLabel
import lt.skautai.android.ui.common.itemConditionLabel
import lt.skautai.android.ui.common.inventoryCategoryLabel
import lt.skautai.android.util.NavRoutes

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun InventoryListScreen(
    navController: NavController,
    viewModel: InventoryListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val canApprove = "items.transfer" in permissions
    val openedCustodianId = viewModel.openedCustodianId
    val pullRefreshState = rememberPullRefreshState(isRefreshing, viewModel::loadItems)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadItems()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = inventoryContextTitle(
                    openedCustodianId = openedCustodianId,
                    selectedType = selectedType,
                    selectedCategory = selectedCategory,
                    items = (uiState as? InventoryListUiState.Success)?.activeItems.orEmpty()
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )

            SkautaiSearchBar(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                placeholder = "Ieskoti...",
                leadingIcon = Icons.Default.Search
            )

            if (canApprove) {
                InventorySegmentedTabs(
                    selectedTab = selectedTab,
                    onTabSelected = viewModel::onTabSelected
                )
            }

            InventoryBody(
                state = uiState,
                canApprove = canApprove,
                selectedTab = selectedTab,
                selectedType = selectedType,
                selectedCategory = selectedCategory,
                openedCustodianId = openedCustodianId,
                viewModel = viewModel,
                navController = navController
            )
        }
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun InventorySegmentedTabs(
    selectedTab: InventoryListTab,
    onTabSelected: (InventoryListTab) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            InventorySegment(
                label = "Inventorius",
                icon = Icons.Default.Inventory2,
                selected = selectedTab == InventoryListTab.INVENTORY,
                onClick = { onTabSelected(InventoryListTab.INVENTORY) },
                modifier = Modifier.weight(1f)
            )
            InventorySegment(
                label = "Tvirtinimai",
                icon = Icons.Default.PendingActions,
                selected = selectedTab == InventoryListTab.APPROVALS,
                onClick = { onTabSelected(InventoryListTab.APPROVALS) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun InventorySegment(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun InventoryBody(
    state: InventoryListUiState,
    canApprove: Boolean,
    selectedTab: InventoryListTab,
    selectedType: String?,
    selectedCategory: String?,
    openedCustodianId: String?,
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
            val showApprovals = canApprove && selectedTab == InventoryListTab.APPROVALS
            SkautaiEmptyState(
                title = if (showApprovals) "Tvirtinimu nera" else "Inventorius tuscias",
                subtitle = if (showApprovals) {
                    "Siuo metu nera inventoriaus irasu, laukianciu patvirtinimo."
                } else {
                    inventoryContextEmptySubtitle(openedCustodianId, selectedType, selectedCategory)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        is InventoryListUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = viewModel::loadItems) {
                        Text("Bandyti dar karta")
                    }
                }
            }
        }

        is InventoryListUiState.Success -> {
            val filtered = viewModel.filteredItems(state.activeItems)
            val typeFilters = remember(state.activeItems) {
                listOf(
                    "COLLECTIVE" to ("Bendras" to state.activeItems.count { it.type == "COLLECTIVE" }),
                    "ASSIGNED" to ("Priskirtas" to state.activeItems.count { it.type == "ASSIGNED" }),
                    "INDIVIDUAL" to ("Asmeninis" to state.activeItems.count { it.type == "INDIVIDUAL" })
                )
            }
            val categoryFilters = remember(state.activeItems) {
                state.activeItems
                    .groupingBy { it.category }
                    .eachCount()
                    .toList()
                    .sortedBy { inventoryCategoryLabel(it.first) }
                    .map { it.first to (inventoryCategoryLabel(it.first) to it.second) }
            }

            if (canApprove && selectedTab == InventoryListTab.APPROVALS) {
                ApprovalContent(
                    items = state.pendingItems,
                    navController = navController,
                    onApprove = viewModel::approveItem,
                    onReject = viewModel::rejectItem,
                    onApproveAll = viewModel::approveAllPending
                )
            } else {
                InventoryCatalogContent(
                    allItems = state.activeItems,
                    filteredItems = filtered,
                    selectedType = selectedType,
                    selectedCategory = selectedCategory,
                    openedCustodianId = openedCustodianId,
                    typeFilters = typeFilters,
                    categoryFilters = categoryFilters,
                    onClearFilters = viewModel::clearFilters,
                    onTypeSelected = viewModel::onTypeSelected,
                    onCategorySelected = viewModel::onCategorySelected,
                    onOpenItem = { itemId -> navController.navigate(NavRoutes.InventoryDetail.createRoute(itemId)) }
                )
            }
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
    openedCustodianId: String?,
    typeFilters: List<Pair<String, Pair<String, Int>>>,
    categoryFilters: List<Pair<String, Pair<String, Int>>>,
    onClearFilters: () -> Unit,
    onTypeSelected: (String?) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onOpenItem: (String) -> Unit
) {
    val groups = remember(filteredItems) { filteredItems.toInventoryGroups() }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(0.dp),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        item {
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
                items(categoryFilters, key = { "category_${it.first}" }) { (category, chip) ->
                    SkautaiChip(
                        label = "${chip.first} (${chip.second})",
                        selected = selectedCategory == category,
                        onClick = { onCategorySelected(if (selectedCategory == category) null else category) }
                    )
                }
            }
        }

        item(key = "list_title") {
            Text(
                text = inventoryContextListTitle(openedCustodianId, selectedType, selectedCategory),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        }

        if (filteredItems.isEmpty()) {
            item {
                SkautaiEmptyState(
                    title = "Nieko nerasta",
                    subtitle = "Pabandyk kita paieskos fraze arba pakeisk inventoriaus tipo filtra."
                )
            }
        } else {
            groups.forEach { group ->
                stickyHeader(key = "header_${group.title}") {
                    InventoryGroupHeader(title = group.title, count = group.items.size)
                }
                items(group.items, key = { it.id }) { item ->
                    InventoryDenseRow(item = item, onOpen = { onOpenItem(item.id) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun InventoryGroupHeader(title: String, count: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = "$title / $count",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
        )
    }
}

@Composable
private fun ApprovalContent(
    items: List<ItemDto>,
    navController: NavController,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onApproveAll: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        item {
            SkautaiCard(
                modifier = Modifier.fillMaxWidth(),
                tonal = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Laukia ${items.size} patvirtinimai",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    TextButton(
                        onClick = onApproveAll,
                        enabled = items.isNotEmpty()
                    ) {
                        Text("Patvirtinti visus", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (items.isEmpty()) {
            item {
                SkautaiEmptyState(
                    title = "Tvirtinimu nera",
                    subtitle = "Siuo metu nera inventoriaus irasu, laukianciu patvirtinimo."
                )
            }
        } else {
            items(items, key = { "pending_${it.id}" }) { item ->
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

private fun inventoryContextTitle(
    openedCustodianId: String?,
    selectedType: String?,
    selectedCategory: String?,
    items: List<ItemDto>
): String = when {
    openedCustodianId != null -> items.firstOrNull()?.custodianName ?: "Vieneto inventorius"
    selectedType == "INDIVIDUAL" -> "Mano siulomas skolinti"
    selectedCategory != null -> "${inventoryCategoryLabel(selectedCategory)} inventorius"
    else -> "Bendras tunto inventorius"
}

private fun inventoryContextListTitle(
    openedCustodianId: String?,
    selectedType: String?,
    selectedCategory: String?
): String = when {
    openedCustodianId != null -> "Vieneto daiktai"
    selectedType == "INDIVIDUAL" -> "Asmeniniai skolinami daiktai"
    selectedCategory != null -> "Kategorijos daiktai"
    else -> "Katalogas"
}

private fun inventoryContextEmptySubtitle(
    openedCustodianId: String?,
    selectedType: String?,
    selectedCategory: String?
): String = when {
    openedCustodianId != null -> "Sis vienetas dar neturi jam priskirtu ar savo sukurtu inventoriaus irasu."
    selectedType == "INDIVIDUAL" -> "Dar nera asmeniniu daiktu, siulomu skolinti."
    selectedCategory != null -> "Sioje kategorijoje dar nera inventoriaus irasu."
    else -> "Kai atsiras pirmi daiktai, cia matysi bendro tunto inventoriaus kataloga."
}

@Composable
private fun InventoryDenseRow(item: ItemDto, onOpen: () -> Unit) {
    val subtitle = listOfNotNull(
        item.custodianName?.takeIf { it.isNotBlank() }?.toInitialsLabel(),
        item.locationId?.takeIf { it.isNotBlank() }
    ).joinToString(" / ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(conditionColor(item.condition), CircleShape)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.name} (${item.quantity} vnt.)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                InventoryPill(
                    label = itemConditionLabel(item.condition),
                    container = conditionContainerColor(item.condition),
                    content = conditionContentColor(item.condition)
                )
            }
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
        item.locationId?.takeIf { it.isNotBlank() }
    ).joinToString(" / ").ifBlank { "Laukia patvirtinimo" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.name} (${item.quantity} vnt.)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                InventoryPill(
                    label = itemConditionLabel(item.condition),
                    container = conditionContainerColor(item.condition),
                    content = conditionContentColor(item.condition)
                )
            }
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
private fun InventoryPill(label: String, container: Color, content: Color) {
    SkautaiStatusPill(
        label = label,
        containerColor = container,
        contentColor = content
    )
}

@Composable
private fun conditionColor(condition: String): Color = when (condition) {
    "GOOD" -> MaterialTheme.colorScheme.primary
    "DAMAGED" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun conditionContainerColor(condition: String): Color = when (condition) {
    "GOOD" -> MaterialTheme.colorScheme.primaryContainer
    "DAMAGED" -> MaterialTheme.colorScheme.errorContainer
    "WRITTEN_OFF" -> MaterialTheme.colorScheme.surfaceVariant
    else -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun conditionContentColor(condition: String): Color = when (condition) {
    "GOOD" -> MaterialTheme.colorScheme.onPrimaryContainer
    "DAMAGED" -> MaterialTheme.colorScheme.onErrorContainer
    "WRITTEN_OFF" -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private data class InventoryGroup(
    val title: String,
    val items: List<ItemDto>
)

private fun List<ItemDto>.toInventoryGroups(): List<InventoryGroup> =
    sortedWith(compareBy<ItemDto>({ it.type }, { inventoryCategoryLabel(it.category) }, { it.name.lowercase() }))
        .groupBy { item -> "${inventoryTypeLabel(item.type)} - ${inventoryCategoryLabel(item.category)}" }
        .map { (title, items) -> InventoryGroup(title, items) }

private fun String.toInitialsLabel(): String {
    val parts = trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    if (parts.size < 2) return trim()
    return parts
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar()?.let { ch -> "$ch." } }
        .joinToString("")
}
