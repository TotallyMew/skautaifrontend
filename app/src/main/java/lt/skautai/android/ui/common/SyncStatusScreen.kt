package lt.skautai.android.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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

@Composable
fun SyncStatusScreen(
    viewModel: PendingSyncViewModel = hiltViewModel()
) {
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SkautaiCard(
                    modifier = Modifier.fillMaxWidth(),
                    tonal = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Sinchronizavimo busena",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        StatusMetricRow("Laukia", syncStatus.pendingCount.toString())
                        StatusMetricRow("Nepavyko", syncStatus.failedCount.toString())
                        StatusMetricRow(
                            "Tinklas",
                            if (syncStatus.isOffline) "Offline" else "Prisijungta"
                        )
                        Button(
                            onClick = viewModel::retryFailed,
                            enabled = syncStatus.failedCount > 0,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Bandyti dar karta")
                        }
                    }
                }
            }

            if (syncStatus.operations.isEmpty()) {
                item {
                    SkautaiEmptyState(
                        title = "Nera laukianciu sinchronizavimu",
                        subtitle = if (syncStatus.isOffline) {
                            "Kai vel bus interneto rysys, nauji pakeitimai cia atsiras automatiskai."
                        } else {
                            "Visi vietiniai pakeitimai jau issiusti i serveri."
                        },
                        icon = if (syncStatus.isOffline) Icons.Default.CloudOff else Icons.Default.CloudDone,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                items(syncStatus.operations, key = { it.id }) { operation ->
                    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (operation.error != null) Icons.Default.SyncProblem else Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = if (operation.error != null) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = operation.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = operation.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                SkautaiStatusPill(
                                    label = operation.statusLabel,
                                    containerColor = if (operation.error != null) {
                                        MaterialTheme.colorScheme.errorContainer
                                    } else {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    },
                                    contentColor = if (operation.error != null) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    }
                                )
                            }
                            Text(
                                text = "Sukurta: ${operation.createdAt}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            operation.error?.let {
                                HorizontalDivider()
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
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
private fun StatusMetricRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontWeight = FontWeight.SemiBold
        )
    }
}
