package lt.skautai.android.ui.requests

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.RequisitionDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiEmptyState
import lt.skautai.android.ui.common.SkautaiStatusPill

@Composable
fun RequisitionListScreen(
    onRequestClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    viewModel: RequisitionListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isMyActiveMode = viewModel.mode == "my_active"
    val isAssignedMode = viewModel.mode == "assigned"

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadRequests()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is RequisitionListUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is RequisitionListUiState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = viewModel::loadRequests) {
                        Text("Bandyti dar karta")
                    }
                }
            }

            is RequisitionListUiState.Success -> {
                if (state.requests.isEmpty()) {
                    SkautaiEmptyState(
                        title = when {
                            isAssignedMode -> "Tvirtinimu nera"
                            isMyActiveMode -> "Patvirtintu prasymu nera"
                            else -> "Pirkimo prasymu dar nera"
                        },
                        subtitle = when {
                            isAssignedMode -> "Siuo metu nera prasymu, kurie lauktu tavo sprendimo."
                            isMyActiveMode -> "Cia matysi tik savo patvirtintus pirkimo ir papildymo prasymus."
                            else -> "Cia bus inventoriaus pirkimo ir papildymo prasymai."
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 0.dp)
                    ) {
                        item {
                            SkautaiCard(
                                modifier = Modifier.fillMaxWidth(),
                                tonal = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                                    Column {
                                        Text(
                                            text = when {
                                                isAssignedMode -> "Man skirti tvirtinti"
                                                isMyActiveMode -> "Mano patvirtinti prasymai"
                                                else -> "Visi pirkimo ir papildymo prasymai"
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = when {
                                                isAssignedMode -> "Prasymai, kurie laukia tavo sprendimo."
                                                isMyActiveMode -> "Tik tavo galutinai patvirtinti prasymai."
                                                else -> "Visa prasymu istorija: laukiantys, patvirtinti ir atmesti."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        items(state.requests, key = { it.id }) { request ->
                            RequisitionCard(
                                request = request,
                                onClick = { onRequestClick(request.id) }
                            )
                        }
                    }
                }

                if (!isAssignedMode) {
                    FloatingActionButton(
                        onClick = onCreateClick,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Naujas pirkimo prasymas")
                    }
                }
            }
        }
    }
}

@Composable
private fun RequisitionCard(
    request: RequisitionDto,
    onClick: () -> Unit
) {
    val firstItem = request.items.firstOrNull()
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = firstItem?.itemName ?: "Pirkimo prasymas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${request.items.sumOf { it.quantityRequested }} vnt. prasoma papildyti",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RequisitionStatusPill(request)
            }
            firstItem?.itemDescription?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Text(
                text = buildList {
                    add(request.requestingUnitName ?: "Tuntui")
                    request.neededByDate?.let { add("reikia iki ${it.take(10)}") }
                }.joinToString(" / "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RequisitionStatusPill(request: RequisitionDto) {
    val (label, container, content) = when {
        request.status == "APPROVED" && request.topLevelReviewStatus == "APPROVED" ->
            Triple("Patvirtinta", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        request.status == "APPROVED" ->
            Triple("Patvirtinta vienete", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        request.unitReviewStatus == "FORWARDED" ->
            Triple("Perduota", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        request.unitReviewStatus == "PENDING" ->
            Triple("Laukia vieneto", MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant)
        request.topLevelReviewStatus == "PENDING" ->
            Triple("Laukia tunto", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        request.status == "REJECTED" ->
            Triple("Atmesta", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        else ->
            Triple("Pateikta", MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    SkautaiStatusPill(label = label, containerColor = container, contentColor = content)
}

internal fun requisitionStatusLabel(request: RequisitionDto): String = when {
    request.status == "APPROVED" && request.topLevelReviewStatus == "APPROVED" -> "Patvirtinta inventorininko / tuntininko"
    request.status == "APPROVED" -> "Patvirtinta vienete"
    request.unitReviewStatus == "FORWARDED" -> "Perduota inventorininkui"
    request.unitReviewStatus == "PENDING" -> "Laukia vieneto sprendimo"
    request.topLevelReviewStatus == "PENDING" -> "Laukia inventorininko / tuntininko sprendimo"
    request.status == "REJECTED" -> "Atmesta"
    else -> "Pateikta"
}
