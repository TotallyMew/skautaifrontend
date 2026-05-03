package lt.skautai.android.ui.reservations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.ReservationItemDto
import lt.skautai.android.ui.locations.LocationPickerField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationMovementScreen(
    onBack: () -> Unit,
    viewModel: ReservationMovementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val title = when (viewModel.mode) {
        "return" -> "Gavimas"
        "mark_returned" -> "Grąžinimas"
        else -> "Išdavimas"
    }
    val description = when (viewModel.mode) {
        "return" -> "Pasirink, kiek daiktų inventorininkas gavo."
        "mark_returned" -> "Pasirink, kiek daiktų grąžinai."
        else -> "Pasirink, kiek daiktų išduodama."
    }
    val actionLabel = when (viewModel.mode) {
        "return" -> "Pažymėti gautą"
        "mark_returned" -> "Pažymėti grąžintą"
        else -> "Pažymėti išduotą"
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onBack()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atgal")
                    }
                }
            )
        },
        snackbarHost = { lt.skautai.android.ui.common.SkautaiErrorSnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Button(
                onClick = viewModel::submit,
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .height(52.dp)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text(actionLabel)
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = uiState.reservation?.title.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(uiState.reservation?.items.orEmpty(), key = { it.itemId }) { item ->
                    MovementItemRow(
                        item = item,
                        selectedQuantity = uiState.selectedQuantities[item.itemId] ?: 0,
                        maxQuantity = viewModel.maxQuantity(item),
                        onIncrease = { viewModel.increase(item.itemId) },
                        onDecrease = { viewModel.decrease(item.itemId) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                item {
                    LocationPickerField(
                        label = "Lokacija",
                        locations = uiState.locations,
                        selectedId = uiState.selectedLocationId,
                        onSelected = { viewModel.onLocationChange(it?.id) },
                        filter = { location ->
                            when (location.visibility) {
                                "UNIT" -> uiState.reservation?.requestingUnitId == location.ownerUnitId
                                "PRIVATE" -> false
                                else -> true
                            }
                        }
                    )
                }
                item {
                    OutlinedTextField(
                        value = uiState.notes,
                        onValueChange = viewModel::onNotesChange,
                        label = { Text("Pastabos") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.height(88.dp))
                }
            }
        }
    }
}

@Composable
private fun MovementItemRow(
    item: ReservationItemDto,
    selectedQuantity: Int,
    maxQuantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.itemName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Galima: $maxQuantity",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(onClick = onDecrease, enabled = selectedQuantity > 0) {
                Icon(Icons.Default.Remove, contentDescription = "Mažinti")
            }
            Text(
                text = selectedQuantity.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            FilledIconButton(onClick = onIncrease, enabled = selectedQuantity < maxQuantity) {
                Icon(Icons.Default.Add, contentDescription = "Didinti")
            }
        }
    }
}
