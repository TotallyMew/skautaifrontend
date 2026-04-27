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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    syncStatus.isOffline -> Icons.Default.CloudOff
                                    syncStatus.failedCount > 0 -> Icons.Default.SyncProblem
                                    syncStatus.pendingCount > 0 -> Icons.Default.CloudDone
                                    else -> Icons.Default.CloudDone
                                },
                                contentDescription = null
                            )
                            Text(
                                text = when {
                                    syncStatus.isOffline -> "Nera interneto rysio"
                                    syncStatus.failedCount > 0 -> "Kai kurie pakeitimai neissaugoti"
                                    syncStatus.pendingCount > 0 -> "Sinchronizuojama..."
                                    else -> "Visi pakeitimai issaugoti"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = when {
                                syncStatus.isOffline ->
                                    "Pakeitimai issaugoti sirenginyje. Kai tinklas atsiras, jie bus automatiskai issiunti i serveri."
                                syncStatus.failedCount > 0 ->
                                    "Dalis pakeitimu nepavyko issaugoti. Patikrink interneto rysi ir bandyk dar karta."
                                syncStatus.pendingCount > 0 ->
                                    "Laukia, kol bus nusiusti ${syncStatus.pendingCount} pakeitim(-ai) i serveri."
                                else ->
                                    "Visi vietiniai pakeitimai jau sinchronizuoti su serveriu."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (syncStatus.failedCount > 0) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = viewModel::retryFailed,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Bandyti dar karta")
                                }
                                OutlinedButton(
                                    onClick = viewModel::dismissFailed,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Panaikinti")
                                }
                            }
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
                        Row(
                            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (operation.error != null) Icons.Default.SyncProblem else Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = if (operation.error != null) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = operation.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${operation.subtitle} • ${operation.createdAt}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                operation.error?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            if (operation.error != null) {
                                IconButton(onClick = { viewModel.dismissOperation(operation.id) }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Panaikinti",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

