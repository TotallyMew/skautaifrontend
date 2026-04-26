package lt.skautai.android.ui.units

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiEmptyState
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiSearchBar
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.theme.ScoutUnitColors
import lt.skautai.android.ui.theme.ScoutUnitPalette

@Composable
fun UnitListScreen(
    onCreateClick: () -> Unit,
    onUnitClick: (String) -> Unit = {},
    viewModel: UnitListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadUnits()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            uiState.error != null -> SkautaiErrorState(
                message = uiState.error!!,
                onRetry = viewModel::loadUnits,
                modifier = Modifier.align(Alignment.Center)
            )
            uiState.units.isEmpty() -> SkautaiEmptyState(
                title = "Vienetu dar nera",
                subtitle = "Cia bus draugoves, gildijos ir kiti tunto vienetai.",
                icon = Icons.Default.AccountTree,
                modifier = Modifier.align(Alignment.Center)
            )
            else -> {
                val filteredUnits = remember(uiState.units, query) {
                    uiState.units.filter { unit ->
                        val searchable = listOf(
                            unit.name,
                            unitTypeLabel(unit.type),
                            unit.subtype?.let { subtypeLabel(it) }.orEmpty(),
                            unit.acceptedRankName.orEmpty()
                        ).joinToString(" ").lowercase()

                        searchable.contains(query.trim().lowercase())
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    item {
                        SkautaiCard(
                            modifier = Modifier.fillMaxWidth(),
                            tonal = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AccountTree, contentDescription = null)
                                    Column {
                                        Text(
                                            text = "${uiState.units.size} vienetai",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Tunto struktura ir vienetu kontekstas.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                SkautaiSearchBar(
                                    value = query,
                                    onValueChange = { query = it },
                                    placeholder = "Ieskoti vieneto",
                                    leadingIcon = Icons.Default.Search
                                )
                            }
                        }
                    }
                    if (filteredUnits.isEmpty()) {
                        item {
                            SkautaiEmptyState(
                                title = "Nieko nerasta",
                                subtitle = "Pabandyk ieskoti pagal pavadinima, tipa ar priimama laipsni.",
                                icon = Icons.Default.AccountTree
                            )
                        }
                    }
                    items(filteredUnits, key = { it.id }) { unit ->
                        UnitCard(unit = unit, onClick = { onUnitClick(unit.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitCard(unit: OrganizationalUnitDto, onClick: () -> Unit) {
    val palette = unit.palette()

    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = palette.cardTone),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                color = palette.iconTone
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AccountTree,
                        contentDescription = null,
                        tint = palette.accent
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = unit.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${unit.memberCount} ${unit.memberCount.nounForm("narys", "nariai", "nariu")} • ${unit.itemCount} ${unit.itemCount.nounForm("daiktas", "daiktai", "daiktu")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = unitTypeLabel(unit.type) + (unit.subtype?.let { " / ${subtypeLabel(it)}" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SkautaiStatusPill(
                        label = unit.subtype?.let { subtypeLabel(it) } ?: "Vienetas",
                        containerColor = palette.accent,
                        contentColor = Color.White
                    )
                    unit.acceptedRankName?.let {
                        SkautaiStatusPill(
                            label = "Priima: $it",
                            containerColor = palette.iconTone,
                            contentColor = palette.accent
                        )
                    }
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = palette.accent)
        }
    }
}

private fun OrganizationalUnitDto.palette(): ScoutUnitPalette = when (type) {
    "PATYRUSIU_SKAUTU_DRAUGOVE" -> ScoutUnitColors.PatyreSkautai
    "SKAUTU_DRAUGOVE" -> ScoutUnitColors.Skautai
    "VILKU_DRAUGOVE" -> ScoutUnitColors.Vilkai
    "GILDIJA" -> ScoutUnitColors.Gildija
    "VYR_SKAUTU_VIENETAS" -> ScoutUnitColors.VyrSkautai
    "VYR_SKAUCIU_VIENETAS" -> ScoutUnitColors.VyrSkautes
    else -> ScoutUnitColors.Default
}

fun unitTypeLabel(type: String): String = when (type) {
    "VILKU_DRAUGOVE" -> "Vilku draugove"
    "SKAUTU_DRAUGOVE" -> "Skautu draugove"
    "PATYRUSIU_SKAUTU_DRAUGOVE" -> "Patyrusiu skautu draugove"
    "GILDIJA" -> "Gildija"
    "VYR_SKAUTU_VIENETAS" -> "Vyr. skautu vienetas"
    "VYR_SKAUCIU_VIENETAS" -> "Vyr. skauciu vienetas"
    else -> type
}

fun subtypeLabel(subtype: String): String = when (subtype) {
    "DRAUGOVE" -> "Draugove"
    "BURELIS" -> "Burelis"
    else -> subtype
}

private fun Int.nounForm(one: String, few: String, many: String): String {
    val lastTwo = this % 100
    val last = this % 10
    return when {
        lastTwo in 11..19 -> many
        last == 1 -> one
        last in 2..9 -> few
        else -> many
    }
}
