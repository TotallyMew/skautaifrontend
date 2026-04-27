package lt.skautai.android.ui.locations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import lt.skautai.android.data.remote.LocationDto
import lt.skautai.android.data.remote.LocationVisibility

/**
 * Cascading location picker. Each level shows children of the selection above it.
 * Selecting at any level immediately reports that location as the result; deeper
 * levels then appear optionally so the user can drill further down.
 */
@Composable
fun LocationPickerField(
    label: String,
    locations: List<LocationDto>,
    selectedId: String?,
    onSelected: (LocationDto?) -> Unit,
    filter: (LocationDto) -> Boolean = { true },
    onQuickCreate: ((name: String, visibility: String) -> Unit)? = null,
    errorText: String? = null
) {
    val byId = remember(locations) { locations.associateBy { it.id } }

    // Build chain [root … selectedItem] from the current selectedId
    val chain = remember(locations, selectedId) {
        buildAncestorChain(selectedId, byId)
    }

    // Roots: pass the filter AND have no parent within the filtered set
    val filteredIds = remember(locations, filter) {
        locations.filter(filter).map { it.id }.toSet()
    }
    val roots = remember(locations, filteredIds) {
        locations.filter { it.id in filteredIds }
            .filter { it.parentLocationId == null || it.parentLocationId !in filteredIds }
            .sortedBy { it.name.lowercase() }
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var quickCreateName by remember { mutableStateOf("") }
    var quickCreateVisibility by remember { mutableStateOf(LocationVisibility.PUBLIC.apiValue) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Level 0 — root picker
        SingleLevelDropdown(
            label = label,
            candidates = roots,
            selected = chain.firstOrNull(),
            onSelected = { chosen -> onSelected(chosen) },
            errorText = errorText,
            extraItem = if (onQuickCreate != null) {
                {
                    DropdownMenuItem(
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Text("Pridėti naują lokaciją")
                            }
                        },
                        onClick = {
                            quickCreateName = ""
                            quickCreateVisibility = LocationVisibility.PUBLIC.apiValue
                            showCreateDialog = true
                        }
                    )
                }
            } else null
        )

        // Level 1+ — child pickers, one per ancestor in the chain
        chain.forEachIndexed { index, ancestor ->
            val children = locations
                .filter { it.parentLocationId == ancestor.id }
                .sortedBy { it.name.lowercase() }
            if (children.isEmpty()) return@forEachIndexed

            val childSelection = chain.getOrNull(index + 1)
            SingleLevelDropdown(
                label = "Sublokacija",
                candidates = children,
                selected = childSelection,
                // Selecting null here means "keep the ancestor as selection"
                onSelected = { chosen -> onSelected(chosen ?: ancestor) },
                errorText = null,
                extraItem = null
            )
        }
    }

    if (showCreateDialog && onQuickCreate != null) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Nauja lokacija") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = quickCreateName,
                        onValueChange = { quickCreateName = it },
                        label = { Text("Pavadinimas") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    VisibilityField(
                        selected = quickCreateVisibility,
                        onSelected = { quickCreateVisibility = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (quickCreateName.isNotBlank()) {
                            onQuickCreate(quickCreateName.trim(), quickCreateVisibility)
                            showCreateDialog = false
                        }
                    },
                    enabled = quickCreateName.isNotBlank()
                ) { Text("Sukurti") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Atšaukti") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleLevelDropdown(
    label: String,
    candidates: List<LocationDto>,
    selected: LocationDto?,
    onSelected: (LocationDto?) -> Unit,
    errorText: String?,
    extraItem: (@Composable () -> Unit)?
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            isError = errorText != null,
            supportingText = errorText?.let { message -> { Text(message) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Nepasirinkta") },
                onClick = {
                    onSelected(null)
                    expanded = false
                }
            )
            extraItem?.invoke()
            candidates.forEach { location ->
                DropdownMenuItem(
                    text = { Text(location.name) },
                    onClick = {
                        onSelected(location)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun buildAncestorChain(
    selectedId: String?,
    byId: Map<String, LocationDto>
): List<LocationDto> {
    if (selectedId == null) return emptyList()
    val chain = mutableListOf<LocationDto>()
    var current = byId[selectedId]
    while (current != null) {
        chain.add(0, current)
        current = current.parentLocationId?.let { byId[it] }
    }
    return chain
}
