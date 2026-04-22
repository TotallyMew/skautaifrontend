package lt.skautai.android.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.remote.ReservationDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiSectionHeader
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.theme.ScoutGradients
import lt.skautai.android.ui.theme.ScoutPalette
import lt.skautai.android.util.NavRoutes

private val HomeForest = ScoutPalette.Forest
private val HomeForestSoft = ScoutPalette.ForestMist
private val HomeMoss = ScoutPalette.MossSoft
private val HomeSage = ScoutPalette.MossMist
private val HomeLichen = ScoutPalette.Lichen
private val HomeSand = ScoutPalette.GoldSoft
private val HomeClay = ScoutPalette.Earth

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (uiState.isLoading && uiState.availableUnits.isEmpty() && uiState.activeReservations.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeroCard(
                userName = userName?.substringBefore(" ") ?: "skautai"
            )
        }

        if (uiState.availableUnits.size > 1) {
            item {
                SkautaiSectionHeader(title = "Aktyvus vienetas")
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.availableUnits, key = { it.id }) { unit ->
                        UnitChip(
                            unit = unit,
                            selected = uiState.activeUnitId == unit.id,
                            onClick = { viewModel.selectActiveUnit(unit.id) }
                        )
                    }
                }
            }
        }

        if (uiState.activeUnitId != null) {
            item {
                InventoryContextCard(
                    title = uiState.activeUnitName ?: "Tavo vieneto inventorius",
                    count = uiState.activeUnitItemCount,
                    subtitle = if (uiState.activeUnitFromSharedCount > 0) {
                        "${uiState.activeUnitItemCount} irasai / ${uiState.activeUnitFromSharedCount} is tunto"
                    } else {
                        "${uiState.activeUnitItemCount} irasai"
                    },
                    icon = Icons.Default.Group,
                    emptyTitle = "Vieneto inventorius tuscias",
                    accent = MaterialTheme.colorScheme.onPrimaryContainer,
                    background = MaterialTheme.colorScheme.primaryContainer,
                    prominent = true,
                    onOpen = {
                        navController.navigate(NavRoutes.InventoryList.createRoute(custodianId = uiState.activeUnitId))
                    },
                    addLabel = "Prideti nauja",
                    onAdd = {
                        navController.navigate(NavRoutes.InventoryAddEdit.createRoute(mode = "UNIT_OWN"))
                    },
                    tertiaryLabel = "Paimti is tunto",
                    onTertiary = { navController.navigate(NavRoutes.InventoryList.createRoute()) }
                )
            }
        }

        item {
            InventoryContextCard(
                title = "Bendras tunto inventorius",
                count = uiState.sharedInventoryCount,
                subtitle = if (uiState.sharedPendingApprovalCount > 0) {
                    "${uiState.sharedInventoryCount} irasai / ${uiState.sharedPendingApprovalCount} nepatvirtinti"
                } else {
                    "${uiState.sharedInventoryCount} irasai"
                },
                icon = Icons.Default.Flag,
                emptyTitle = "Tunto sandelis tuscias",
                accent = MaterialTheme.colorScheme.onTertiaryContainer,
                background = MaterialTheme.colorScheme.tertiaryContainer,
                prominent = uiState.activeUnitId == null,
                onOpen = { navController.navigate(NavRoutes.InventoryList.createRoute()) },
                addLabel = "Prideti",
                preferAddAsPrimary = true,
                onAdd = {
                    navController.navigate(NavRoutes.InventoryAddEdit.createRoute(mode = "SHARED"))
                }
            )
        }

        item {
            InventoryContextCard(
                title = "Mano siulomas skolinti",
                count = uiState.personalLendingCount,
                subtitle = "${uiState.personalLendingCount} irasai",
                icon = Icons.Default.Person,
                emptyTitle = "Dar nesi pridejes skolinimui",
                accent = MaterialTheme.colorScheme.onSurfaceVariant,
                background = MaterialTheme.colorScheme.surfaceVariant,
                onOpen = { navController.navigate(NavRoutes.InventoryList.createRoute(type = "INDIVIDUAL")) },
                addLabel = "Prideti savo",
                onAdd = {
                    navController.navigate(NavRoutes.InventoryAddEdit.createRoute(mode = "PERSONAL"))
                }
            )
        }

        item {
            SkautaiSectionHeader(title = "Greita paieska")
        }

        item {
            CategoryBrowseGrid(
                onCategoryClick = { category ->
                    navController.navigate(NavRoutes.InventoryList.createRoute(category = category))
                }
            )
        }

        item {
            SkautaiSectionHeader(
                title = "Rezervacijos",
                actionLabel = "Visos",
                onAction = { navController.navigate(NavRoutes.ReservationList.createRoute()) }
            )
        }

        item {
            ReservationOverviewGrid(
                myCount = uiState.myReservationCount,
                assignedCount = uiState.assignedReservationCount,
                trackedCount = uiState.trackedReservationCount,
                onOpenMyReservations = {
                    navController.navigate(NavRoutes.ReservationList.createRoute(mode = "my_active"))
                },
                onOpenAssignedReservations = {
                    navController.navigate(NavRoutes.ReservationList.createRoute(mode = "assigned"))
                },
                onOpenTrackedReservations = {
                    navController.navigate(NavRoutes.ReservationList.createRoute(mode = "tracked"))
                }
            )
        }

        item {
            SkautaiSectionHeader(
                title = "Pirkimo ir papildymo prasymai",
                actionLabel = "Visi",
                onAction = { navController.navigate(NavRoutes.RequestList.createRoute()) }
            )
        }

        item {
            RequisitionOverviewGrid(
                myCount = uiState.myRequisitionCount,
                assignedCount = uiState.assignedRequisitionCount,
                onOpenMyRequests = {
                    navController.navigate(NavRoutes.RequestList.createRoute(mode = "my_active"))
                },
                onOpenAssignedRequests = {
                    navController.navigate(NavRoutes.RequestList.createRoute(mode = "assigned"))
                }
            )
        }

    }
}

@Composable
private fun HeroCard(userName: String) {
    SkautaiCard(
        modifier = Modifier.fillMaxWidth(),
        tonal = HomeSage
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = ScoutGradients.HomeHero,
                        startY = 0f,
                        endY = 900f
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "PRADZIOS APZVALGA",
                style = MaterialTheme.typography.labelMedium,
                color = HomeForest.copy(alpha = 0.72f),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Labas, $userName",
                style = MaterialTheme.typography.headlineSmall,
                color = HomeForest,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Greita tavo vieneto, tunto ir asmeninio inventoriaus apzvalga.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InventoryContextCard(
    title: String,
    count: Int,
    subtitle: String,
    icon: ImageVector,
    emptyTitle: String,
    accent: Color,
    background: Color,
    onOpen: () -> Unit,
    addLabel: String,
    onAdd: () -> Unit,
    tertiaryLabel: String? = null,
    onTertiary: (() -> Unit)? = null,
    preferAddAsPrimary: Boolean = false,
    prominent: Boolean = false
) {
    val isEmpty = count == 0
    val primaryLabel = when {
        isEmpty -> "Prideti"
        preferAddAsPrimary -> addLabel
        else -> "Atidaryti"
    }
    val primaryAction = when {
        isEmpty -> onAdd
        preferAddAsPrimary -> onAdd
        else -> onOpen
    }
    val secondaryLabel = when {
        isEmpty -> "Atidaryti"
        preferAddAsPrimary -> "Atidaryti"
        else -> addLabel
    }
    val secondaryAction = when {
        isEmpty -> onOpen
        preferAddAsPrimary -> onOpen
        else -> onAdd
    }
    val cardPadding = if (prominent) 18.dp else 16.dp
    val iconBoxSize = if (prominent) 52.dp else 46.dp
    val iconSize = if (prominent) 31.dp else 27.dp

    SkautaiCard(
        modifier = Modifier.fillMaxWidth(),
        tonal = background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(if (prominent) 16.dp else 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = accent.copy(alpha = if (prominent) 0.18f else 0.14f),
                    contentColor = accent,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Box(
                        modifier = Modifier.size(iconBoxSize),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = if (prominent) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (isEmpty) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                        contentColor = accent,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(58.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                    Text(
                        text = emptyTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = primaryAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HomeForest,
                        contentColor = ScoutPalette.White
                    )
                ) {
                    if (isEmpty || preferAddAsPrimary) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Text(primaryLabel)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = secondaryAction,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.82f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
                            contentColor = accent
                        )
                    ) {
                        Text(secondaryLabel, fontWeight = FontWeight.SemiBold)
                    }
                    if (tertiaryLabel != null && onTertiary != null) {
                        OutlinedButton(
                            onClick = onTertiary,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, accent.copy(alpha = 0.82f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
                                contentColor = accent
                            )
                        ) {
                            Text(tertiaryLabel, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBrowseGrid(
    onCategoryClick: (String) -> Unit
) {
    val entries = listOf(
        BrowseEntry("Camping", "Palapines ir tentu sistema", Icons.Default.Warehouse, HomeMoss, "CAMPING"),
        BrowseEntry("Tools", "Irankiai ir darbai", Icons.Default.Handyman, HomeLichen, "TOOLS"),
        BrowseEntry("Cooking", "Virtuves iranga", Icons.Default.Restaurant, HomeSand, "COOKING"),
        BrowseEntry("First aid", "Vaistinele ir sauga", Icons.Default.LocalHospital, HomeMoss, "FIRST_AID"),
        BrowseEntry("Uniforms", "Apranga ir atributika", Icons.Default.WorkspacePremium, HomeLichen, "UNIFORMS"),
        BrowseEntry("Books", "Vadovai ir leidiniai", Icons.Default.MenuBook, HomeSand, "BOOKS")
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        entries.chunked(3).forEach { rowEntries ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowEntries.forEach { entry ->
                    SkautaiCard(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.86f),
                        onClick = { onCategoryClick(entry.category) },
                        tonal = entry.tone
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.48f),
                                contentColor = HomeForest,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = entry.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = entry.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = entry.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequisitionOverviewGrid(
    myCount: Int,
    assignedCount: Int,
    onOpenMyRequests: () -> Unit,
    onOpenAssignedRequests: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RequisitionOverviewTile(
            title = "Mano prasymai",
            count = myCount,
            subtitle = "Kuriuos pats pateikei",
            icon = Icons.Default.Assignment,
            tone = HomeForestSoft,
            onClick = onOpenMyRequests,
            modifier = Modifier.weight(1f)
        )
        RequisitionOverviewTile(
            title = "Man skirti",
            count = assignedCount,
            subtitle = "Laukia tavo sprendimo",
            icon = Icons.Default.Inbox,
            tone = HomeLichen,
            onClick = onOpenAssignedRequests,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ReservationOverviewGrid(
    myCount: Int,
    assignedCount: Int,
    trackedCount: Int,
    onOpenMyReservations: () -> Unit,
    onOpenAssignedReservations: () -> Unit,
    onOpenTrackedReservations: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ReservationOverviewTile(
            title = "Mano rezervacijos",
            count = myCount,
            subtitle = "Tavo aktyvios rezervacijos",
            icon = Icons.Default.EventAvailable,
            tone = HomeForestSoft,
            onClick = onOpenMyReservations
        )
        ReservationOverviewTile(
            title = "Prašymai",
            count = assignedCount,
            subtitle = "Laukia tavo sokprendimo",
            icon = Icons.Default.Inbox,
            tone = HomeLichen,
            onClick = onOpenAssignedReservations
        )
        ReservationOverviewTile(
            title = "Sekamos",
            count = trackedCount,
            subtitle = "Isduoti arba pazymeti gauta",
            icon = Icons.Default.Visibility,
            tone = HomeClay,
            onClick = onOpenTrackedReservations
        )
    }
}

@Composable
private fun ReservationOverviewTile(
    title: String,
    count: Int,
    subtitle: String,
    icon: ImageVector,
    tone: Color,
    onClick: () -> Unit
) {
    SkautaiCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        tonal = tone
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.56f),
                contentColor = HomeForest,
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(
                    modifier = Modifier.size(46.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "$count",
                style = MaterialTheme.typography.headlineSmall,
                color = HomeForest,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RequisitionOverviewTile(
    title: String,
    count: Int,
    subtitle: String,
    icon: ImageVector,
    tone: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SkautaiCard(
        modifier = modifier.aspectRatio(1.08f),
        onClick = onClick,
        tonal = tone
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HomeForest
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.headlineMedium,
                    color = HomeForest,
                    fontWeight = FontWeight.Bold
                )
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
private fun UnitChip(
    unit: OrganizationalUnitDto,
    selected: Boolean,
    onClick: () -> Unit
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    SkautaiCard(onClick = onClick, tonal = container) {
        Text(
            text = unit.name,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            color = content,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun ReservationCard(
    reservation: ReservationDto,
    onClick: () -> Unit
) {
    SkautaiCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        tonal = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SkautaiStatusPill(
                label = when (reservation.status) {
                    "APPROVED" -> "Patvirtinta"
                    "ACTIVE" -> "Aktyvi"
                    else -> reservation.status
                },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = reservation.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Iki ${reservation.endDate.take(10)} · ${reservation.totalQuantity} vnt.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class BrowseEntry(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tone: Color,
    val category: String
)
