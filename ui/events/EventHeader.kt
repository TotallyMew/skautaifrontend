package lt.skautai.android.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.StovyklaDetailsDto
import androidx.compose.ui.unit.dp

@Composable
fun EventHeader(
    event: EventDto,
    isCancelling: Boolean,
    canManage: Boolean,
    canStart: Boolean,
    onActivate: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                HeaderActions(
                    event = event,
                    isCancelling = isCancelling,
                    canManage = canManage,
                    onCancel = onCancel
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EventStatusPill(status = event.status)
                Text(
                    text = eventTypeLabel(event.type),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = "${event.startDate.take(10)} - ${event.endDate.take(10)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            event.notes?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (event.type == "STOVYKLA") {
                event.stovyklaDetails?.let { StovyklaDetailsCompact(it) }
            }

            EventPrimaryStatusAction(
                event = event,
                canManage = canManage,
                canStart = canStart,
                onActivate = onActivate,
                onComplete = onComplete
            )
        }
    }
}

@Composable
private fun EventPrimaryStatusAction(
    event: EventDto,
    canManage: Boolean,
    canStart: Boolean,
    onActivate: () -> Unit,
    onComplete: () -> Unit
) {
    when {
        event.status == "PLANNING" && canStart -> {
            Button(onClick = onActivate, modifier = Modifier.fillMaxWidth()) {
                Text("Pradeti rengini")
            }
        }

        event.status == "ACTIVE" && canManage -> {
            Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
                Text("Baigti rengini")
            }
        }
    }
}

@Composable
private fun HeaderActions(
    event: EventDto,
    isCancelling: Boolean,
    canManage: Boolean,
    onCancel: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (canManage && event.status in listOf("PLANNING", "ACTIVE")) {
            Box {
                IconButton(onClick = { expanded = true }, enabled = !isCancelling) {
                    if (isCancelling) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Default.MoreVert, contentDescription = "Daugiau veiksmu")
                    }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Atsaukti rengini", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            expanded = false
                            onCancel()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StovyklaDetailsCompact(details: StovyklaDetailsDto) {
    details.actualParticipants?.let {
        HorizontalDivider()
        EventInfoRow("Stabo zmones", it.toString())
    }
}
