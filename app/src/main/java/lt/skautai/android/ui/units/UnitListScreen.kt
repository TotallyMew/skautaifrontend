package lt.skautai.android.ui.units

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.OrganizationalUnitDto

@Composable
fun UnitListScreen(
    onCreateClick: () -> Unit,
    onUnitClick: (String) -> Unit = {},
    viewModel: UnitListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
            uiState.error != null -> Text(
                text = uiState.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center).padding(16.dp)
            )
            uiState.units.isEmpty() -> Text(
                text = "Nėra vienetų",
                modifier = Modifier.align(Alignment.Center)
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.units) { unit ->
                    UnitCard(unit = unit, onClick = { onUnitClick(unit.id) })
                }
            }
        }
    }
}

@Composable
private fun UnitCard(unit: OrganizationalUnitDto, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = unit.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = unitTypeLabel(unit.type) + (unit.subtype?.let { " · ${subtypeLabel(it)}" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun unitTypeLabel(type: String): String = when (type) {
    "VILKU_DRAUGOVE" -> "Vilkų draugovė"
    "SKAUTU_DRAUGOVE" -> "Skautų draugovė"
    "PATYRUSIU_SKAUTU_DRAUGOVE" -> "Patyrusių skautų draugovė"
    "GILDIJA" -> "Gildija"
    "VYR_SKAUTU_VIENETAS" -> "Vyr. skautų vienetas"
    "VYR_SKAUCIU_VIENETAS" -> "Vyr. skauičių vienetas"
    else -> type
}

fun subtypeLabel(subtype: String): String = when (subtype) {
    "DRAUGOVE" -> "Draugovė"
    "BURELIS" -> "Burelis"
    else -> subtype
}
