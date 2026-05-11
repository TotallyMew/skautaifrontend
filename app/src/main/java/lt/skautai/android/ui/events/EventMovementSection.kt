package lt.skautai.android.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lt.skautai.android.data.remote.EventInventoryCustodyDto
import lt.skautai.android.data.remote.EventInventoryMovementDto
import lt.skautai.android.data.remote.EventInventoryPlanDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiEmptyState
import lt.skautai.android.ui.common.SkautaiTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventMovementCard(
    inventoryPlan: EventInventoryPlanDto?,
    custody: List<EventInventoryCustodyDto>,
    movements: List<EventInventoryMovementDto>,
    isWorking: Boolean,
    onOpenItemQr: () -> Unit,
    onOpenCustodyQr: () -> Unit,
    onCreateMovement: (String, String, String, String?, String?, String?, String) -> Unit
) {
    val plannedItems = inventoryPlan?.items.orEmpty()
    val movementItemOptions = remember(plannedItems, movements, custody) {
        val planned = plannedItems.map { MovementItemOption(it.id, it.name) }
        val fromMovements = movements.map { MovementItemOption(it.eventInventoryItemId, it.itemName) }
        val fromCustody = custody.map { MovementItemOption(it.eventInventoryItemId, it.itemName) }
        (planned + fromMovements + fromCustody)
            .distinctBy { it.id }
            .sortedBy { it.name.lowercase() }
    }
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    val selectedItemName = movementItemOptions.firstOrNull { it.id == selectedItemId }?.name
    val scopedCustody = remember(custody, selectedItemId) {
        selectedItemId?.let { itemId -> custody.filter { it.eventInventoryItemId == itemId } } ?: custody
    }
    val scopedMovements = remember(movements, selectedItemId) {
        selectedItemId?.let { itemId -> movements.filter { it.eventInventoryItemId == itemId } } ?: movements
    }
    val hasAvailableInventory = plannedItems.any { it.availableQuantity > 0 }
    val activeMovements = scopedCustody.count { it.status == "OPEN" }
    val peopleCustody = scopedCustody.filter { it.status == "OPEN" && it.holderUserId != null }
    val campCustody = scopedCustody.filter { it.status == "OPEN" && it.holderUserId == null && it.pastovykleId != null }

    SkautaiCard(modifier = Modifier.fillMaxWidth(), tonal = MaterialTheme.colorScheme.surfaceBright) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Gyvas inventorius",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "QR srautai gyvam renginio inventoriaus judėjimui.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MovementStatusCard(
                    label = "Aktyvūs judėjimai",
                    value = activeMovements.toString(),
                    modifier = Modifier.weight(1f)
                )
                MovementStatusCard(
                    label = "Istorijoje",
                    value = scopedMovements.size.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            MovementItemFilter(
                items = movementItemOptions,
                selectedItemId = selectedItemId,
                onSelectedItemChange = { selectedItemId = it }
            )

            ActionGrid(
                isWorking = isWorking,
                hasAvailableInventory = hasAvailableInventory,
                onOpenItemQr = onOpenItemQr,
                onOpenCustodyQr = onOpenCustodyQr
            )

            if (!hasAvailableInventory) {
                Text(
                    text = "Daikto skenavimas aktyviai veiks, kai renginyje bus bent vienas prieinamas inventoriaus įrašas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            CustodySection(
                title = "Pas žmones",
                subtitle = "${peopleCustody.size} įrašai",
                icon = Icons.Default.PersonOutline,
                custody = peopleCustody,
                emptyTitle = "Niekas dar nepasiėmė daiktų",
                emptyMessage = "Kai inventorius bus išduotas žmonėms, jis atsiras čia.",
                isWorking = isWorking,
                onReturn = { row ->
                    onCreateMovement(
                        if (row.pastovykleId != null) "RETURN_TO_PASTOVYKLE" else "RETURN_TO_EVENT_STORAGE",
                        row.eventInventoryItemId,
                        row.remainingQuantity.toString(),
                        row.pastovykleId,
                        null,
                        row.id,
                        ""
                    )
                }
            )
            CustodySection(
                title = "Pastovyklės",
                subtitle = "${campCustody.size} įrašai",
                icon = Icons.Default.Forest,
                custody = campCustody,
                emptyTitle = "Pastovyklėms dar niekas nepriskirta",
                emptyMessage = "Priskirtas inventorius čia bus matomas iš karto.",
                isWorking = isWorking,
                onReturn = { row ->
                    onCreateMovement(
                        "RETURN_TO_EVENT_STORAGE",
                        row.eventInventoryItemId,
                        row.remainingQuantity.toString(),
                        null,
                        null,
                        row.id,
                        ""
                    )
                }
            )
            HistorySection(
                movements = scopedMovements,
                selectedItemName = selectedItemName,
                totalMovementCount = movements.size
            )
        }
    }
}

private data class MovementItemOption(
    val id: String,
    val name: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MovementItemFilter(
    items: List<MovementItemOption>,
    selectedItemId: String?,
    onSelectedItemChange: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = items.firstOrNull { it.id == selectedItemId }?.name ?: "Visi daiktai"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Rodyti judėjimą") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Visi daiktai") },
                onClick = {
                    onSelectedItemChange(null)
                    expanded = false
                }
            )
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.name) },
                    onClick = {
                        onSelectedItemChange(item.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ActionGrid(
    isWorking: Boolean,
    hasAvailableInventory: Boolean,
    onOpenItemQr: () -> Unit,
    onOpenCustodyQr: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ActionGridButton(
            title = "Pasiimti / Išduoti",
            icon = Icons.Default.QrCodeScanner,
            onClick = onOpenItemQr,
            enabled = !isWorking && hasAvailableInventory,
            modifier = Modifier.weight(1f)
        )
        ActionGridButton(
            title = "Perduoti / Grąžinti",
            icon = Icons.Default.SwapHoriz,
            onClick = onOpenCustodyQr,
            enabled = !isWorking,
            modifier = Modifier.weight(1f)
        )
    }

}

@Composable
private fun ActionGridButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MovementStatusCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CustodySection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    custody: List<EventInventoryCustodyDto>,
    emptyTitle: String,
    emptyMessage: String,
    isWorking: Boolean,
    onReturn: (EventInventoryCustodyDto) -> Unit
) {
    EventListSection(title = title, subtitle = subtitle) {
        if (custody.isEmpty()) {
            CompactMovementEmptyState(
                title = emptyTitle,
                message = emptyMessage,
                icon = icon
            )
        } else {
            custody.forEachIndexed { index, row ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(row.itemName, fontWeight = FontWeight.SemiBold)
                    Text(
                        buildString {
                            row.pastovykleName?.let { append("$it / ") }
                            row.holderUserName?.let { append(it) }
                            if (isBlank()) append("Renginio sandėlis")
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EventMetricPill("${row.remainingQuantity}/${row.quantity}")
                        TextButton(
                            onClick = { onReturn(row) },
                            enabled = !isWorking && row.remainingQuantity > 0,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Grąžinti")
                        }
                    }
                }
                if (index != custody.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun HistorySection(
    movements: List<EventInventoryMovementDto>,
    selectedItemName: String?,
    totalMovementCount: Int
) {
    var query by remember { mutableStateOf("") }
    val normalizedQuery = query.trim()
    val filteredMovements = remember(movements, normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            movements
        } else {
            movements.filter { it.matchesMovementQuery(normalizedQuery) }
        }
    }
    val visibleMovements = if (normalizedQuery.isBlank()) filteredMovements.take(12) else filteredMovements
    val scopeLabel = selectedItemName?.let { " · $it" }.orEmpty()
    val subtitle = when {
        normalizedQuery.isNotBlank() -> "${filteredMovements.size} rasta iš ${movements.size}$scopeLabel"
        selectedItemName != null -> "${movements.size} iš ${totalMovementCount}$scopeLabel"
        else -> "${movements.size} įrašai"
    }

    EventListSection(title = "Istorija", subtitle = subtitle) {
        if (movements.isEmpty()) {
            CompactMovementEmptyState(
                title = if (selectedItemName == null) "Judėjimo istorija tuščia" else "Šis daiktas dar nejudėjo",
                message = if (selectedItemName == null) {
                    "Pirmi veiksmai čia atsiras automatiškai."
                } else {
                    "Pasirink kitą daiktą arba grįžk į visų daiktų judėjimą."
                },
                icon = Icons.Default.History
            )
        } else {
            SkautaiTextField(
                value = query,
                onValueChange = { query = it },
                label = "Ieškoti istorijoje",
                placeholder = "Daiktas, žmogus, pastovyklė, tipas ar pastabos",
                singleLine = true,
                leadingIcon = Icons.Default.Search,
                modifier = Modifier.fillMaxWidth()
            )
            if (visibleMovements.isEmpty()) {
                CompactMovementEmptyState(
                    title = "Šiam daiktui judėjimo įrašų nerasta",
                    message = "Pabandyk kitą pavadinimą, žmogų arba pastovyklę.",
                    icon = Icons.Default.History
                )
                return@EventListSection
            }
            visibleMovements.forEachIndexed { index, movement ->
                MovementHistoryRow(movement)
                if (index != visibleMovements.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun CompactMovementEmptyState(
    title: String,
    message: String,
    icon: ImageVector
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        SkautaiEmptyState(
            title = title,
            subtitle = message,
            icon = icon,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MovementHistoryRow(movement: EventInventoryMovementDto) {
    val route = listOfNotNull(
        movement.fromPastovykleName ?: movement.fromUserName,
        movement.toPastovykleName ?: movement.toUserName
    ).joinToString(" -> ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${movementLabel(movement.movementType)}: ${movement.itemName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = listOfNotNull(
                        route.ifBlank { null },
                        "Kiekis ${movement.quantity}",
                        movement.performedByUserName?.let { "Atliko $it" }
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun movementLabel(type: String): String = when (type) {
    "PASTOVYKLE_REQUEST" -> "Prašymas"
    "ASSIGN_TO_PASTOVYKLE" -> "Išduota pastovyklei"
    "CHECKOUT_TO_PERSON" -> "Pasiimta"
    "RETURN_TO_PASTOVYKLE" -> "Grąžinta pastovyklei"
    "RETURN_TO_EVENT_STORAGE" -> "Grąžinta į sandėlį"
    "TRANSFER" -> "Perduota"
    else -> type
}

private fun EventInventoryMovementDto.matchesMovementQuery(query: String): Boolean {
    val needle = query.lowercase()
    return listOfNotNull(
        itemName,
        movementLabel(movementType),
        movementType,
        fromPastovykleName,
        toPastovykleName,
        fromUserName,
        toUserName,
        performedByUserName,
        notes
    ).any { value -> value.lowercase().contains(needle) }
}
