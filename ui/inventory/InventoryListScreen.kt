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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import lt.skautai.android.ui.common.RemoteImage
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiChip
import lt.skautai.android.ui.common.SkautaiEmptyState
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiSearchBar
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.inventoryTypeLabel
import lt.skautai.android.ui.common.itemConditionLabel
import lt.skautai.android.ui.common.inventoryCategoryLabel
import lt.skautai.android.util.NavRoutes

private val KnownInventoryCategories = listOf(
    "CAMPING",
    "TOOLS",
    "COOKING",
    "FIRST_AID",
    "UNIFORMS",
    "BOOKS",
    "PERSONAL_LOANS"
)

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
    val canApprove = "items.transfer" in permissions
    val openedCustodianId = viewModel.openedCustodianId
    val pullRefreshState = rememberPullRefreshState(isRefreshing, viewModel::loadItems)

    val pendingItems = (uiState as? InventoryListUiState.Success)?.pendingItems.orEmpty()

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
                selectedType = selectedType,
                selectedCategory = selectedCategory,
                openedCustodianId = openedCustodianId,
                canCreate = "items.create" in permissions,
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
        tonal = MaterialTheme.colorScheme.secondaryContainer
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
                Text(
                    text = "Laukia ${items.size} patvirtinimai",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onApproveAll,
                    enabled = items.isNotEmpty()
                ) {
                    Text("Patvirtinti visus", fontWeight = FontWeight.SemiBold)
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Sutraukti" else "Isskleisti",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
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
    selectedType: String?,
    selectedCategory: String?,
    openedCustodianId: String?,
    canCreate: Boolean,
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
                title = "Inventorius tuscias",
                subtitle = inventoryContextEmptySubtitle(openedCustodianId, selectedType, selectedCategory),
                icon = Icons.Default.Inventory2,
                actionLabel = if (canCreate) "Prideti daikta" else "Grizti i pradzia",
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
            val filtered = viewModel.filteredItems(state.activeItems)
            val typeFilters = remember(state.activeItems) {
                listOf(
                    "COLLECTIVE" to ("Bendras" to state.activeItems.count { it.type == "COLLECTIVE" }),
                    "ASSIGNED" to ("Priskirtas" to state.activeItems.count { it.type == "ASSIGNED" }),
                    "INDIVIDUAL" to ("Asmeninis" to state.activeItems.count { it.type == "INDIVIDUAL" })
                )
            }
            val categoryFilters = remember(state.activeItems) {
                val counts = state.activeItems.groupingBy { it.category }.eachCount()
                KnownInventoryCategories
                    .map { category -> category to (inventoryCategoryLabel(category) to (counts[category] ?: 0)) }
            }

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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            }
        }

        if (filteredItems.isEmpty()) {
            item {
                SkautaiEmptyState(
                    title = "Nieko nerasta",
                    subtitle = "Pabandyk kita paieskos fraze arba pakeisk inventoriaus tipo filtra.",
                    icon = Icons.Default.Inventory2
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
    Text(
        text = "$title / $count",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 10.dp, bottom = 6.dp)
    )
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
        InventoryRowVisual(item = item)
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
        InventoryRowVisual(item = item)
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
private fun InventoryRowVisual(item: ItemDto) {
    if (item.photoUrl.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(conditionColor(item.condition), CircleShape)
        )
    } else {
        RemoteImage(
            imageUrl = item.photoUrl,
            contentDescription = item.name,
            modifier = Modifier.size(44.dp)
        )
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
