package lt.skautai.android.ui.requests

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.data.remote.BendrasRequestDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiEmptyState
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.SkautaiStatusTone
import lt.skautai.android.ui.common.SkautaiSummaryCard

@Composable
fun RequestListScreen(
    onRequestClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    viewModel: RequestListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is RequestListUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is RequestListUiState.Error -> {
                SkautaiErrorState(
                    message = state.message,
                    onRetry = viewModel::loadRequests,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is RequestListUiState.Success -> {
                if (state.requests.isEmpty()) {
                    SkautaiEmptyState(
                        title = "Paemimo prasymu nera",
                        subtitle = "Cia bus tavo vieneto prasymai paimti jau esamus daiktus is bendro tunto inventoriaus.",
                        icon = Icons.Default.Inbox,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        item {
                            SkautaiSummaryCard(
                                title = "Reikia paimti turima daikta is tunto?",
                                subtitle = "Cia vienetas praso gauti jau esama bendro tunto inventoriaus daikta, o ne pirkti nauja.",
                                metrics = listOf(
                                    "Prasymai" to state.requests.size.toString(),
                                    "Laukia" to state.requests.count { it.topLevelStatus == "PENDING" }.toString(),
                                    "Perduota" to state.requests.count { it.topLevelStatus == "FORWARDED" }.toString()
                                ),
                                foresty = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        items(state.requests, key = { it.id }) { request ->
                            SharedTransferRequestCard(
                                request = request,
                                onClick = { onRequestClick(request.id) }
                            )
                        }
                    }
                }

                FloatingActionButton(
                    onClick = onCreateClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = "Paimti is tunto")
                }
            }
        }
    }
}

@Composable
private fun SharedTransferRequestCard(
    request: BendrasRequestDto,
    onClick: () -> Unit
) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                        text = request.itemName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Prasoma is bendro inventoriaus: ${request.quantity} vnt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RequestStatusChip(status = request.topLevelStatus)
            }
            request.requestingUnitName?.let {
                Text(
                    text = "Vienetas: $it",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            request.itemDescription?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun RequestStatusChip(status: String) {
    val (label, container, content) = when (status) {
        "PENDING" -> Triple("Laukia", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        "APPROVED" -> Triple("Patvirtinta", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        "REJECTED" -> Triple("Atmesta", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        "FORWARDED" -> Triple("Perduota", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        else -> Triple(status, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
    }
    SkautaiStatusPill(label = label, containerColor = container, contentColor = content)
}
