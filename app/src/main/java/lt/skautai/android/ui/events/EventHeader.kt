package lt.skautai.android.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import lt.skautai.android.data.remote.EventDto

@Composable
fun EventHeader(
    event: EventDto,
    isCancelling: Boolean,
    canManage: Boolean,
    canStart: Boolean,
    onEdit: () -> Unit,
    onActivate: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    EventDetailHero(
        event = event,
        expanded = true,
        subtitle = "${eventTypeLabel(event.type)} - ${event.startDate.take(10)} iki ${event.endDate.take(10)}"
    ) {
        event.notes?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.84f),
                maxLines = 3
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EventPrimaryStatusAction(
                event = event,
                canManage = canManage,
                canStart = canStart,
                onActivate = onActivate,
                onComplete = onComplete,
                modifier = Modifier.fillMaxWidth()
            )
            HeaderActions(
                event = event,
                isCancelling = isCancelling,
                canManage = canManage,
                onEdit = onEdit,
                onCancel = onCancel
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
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        event.status == "PLANNING" && canStart -> {
            EventPrimaryButton(
                text = "Pradeti rengini",
                onClick = onActivate,
                enabled = true,
                modifier = modifier
            )
        }

        event.status == "ACTIVE" && canManage -> {
            EventPrimaryButton(
                text = "Pereiti i suvedima",
                onClick = onComplete,
                enabled = true,
                modifier = modifier
            )
        }

        else -> {
            Box(modifier = modifier)
        }
    }
}

@Composable
private fun HeaderActions(
    event: EventDto,
    isCancelling: Boolean,
    canManage: Boolean,
    onEdit: () -> Unit,
    onCancel: () -> Unit
) {
    if (canManage && event.status in listOf("PLANNING", "ACTIVE")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onEdit,
                enabled = !isCancelling,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("Redaguoti")
            }
            OutlinedButton(
                onClick = onCancel,
                enabled = !isCancelling,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.weight(1f)
            ) {
                if (isCancelling) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                }
                Text("Atsaukti")
            }
        }
    }
}
