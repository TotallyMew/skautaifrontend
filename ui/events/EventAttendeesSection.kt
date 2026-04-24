package lt.skautai.android.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import lt.skautai.android.data.remote.EventRoleDto
import lt.skautai.android.data.remote.MemberDto

@Composable
fun StabasCard(
    roles: List<EventRoleDto>,
    canManage: Boolean,
    onRemoveRole: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Stabas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            if (roles.isEmpty()) {
                EmptyStateText("Stabo nariu dar nera.")
            }
            roles.forEach { role ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = role.userName ?: role.userId,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = eventRoleLabel(role.role),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (canManage && role.role != "VIRSININKAS") {
                        TextButton(onClick = { onRemoveRole(role.id) }) {
                            Text("Salinti")
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun StaffPickerSheet(
    members: List<MemberDto>,
    isWorking: Boolean,
    onAssignRole: (String, String) -> Unit
) {
    var selectedUserId by remember { mutableStateOf<String?>(null) }
    var selectedRole by remember { mutableStateOf("VADOVAS") }
    val roleOptions = listOf("VIRSININKAS", "KOMENDANTAS", "UKVEDYS", "PROGRAMERIS", "MAISTININKAS", "VADOVAS", "SAVANORIS")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Prideti i staba", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        DropdownField(
            label = "Zmogus",
            value = members.firstOrNull { it.userId == selectedUserId }?.fullName() ?: "Pasirinkti",
            options = members.map { it.userId to it.fullName() },
            onSelect = { selectedUserId = it }
        )
        DropdownField(
            label = "Role",
            value = eventRoleLabel(selectedRole),
            options = roleOptions.map { it to eventRoleLabel(it) },
            onSelect = { selectedRole = it }
        )
        Button(
            onClick = { selectedUserId?.let { onAssignRole(it, selectedRole) } },
            enabled = !isWorking && selectedUserId != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Prideti")
        }
    }
}

private fun eventRoleLabel(role: String): String = when (role) {
    "VIRSININKAS" -> "Virsininkas"
    "KOMENDANTAS" -> "Komendantas"
    "UKVEDYS" -> "Ukvedys"
    "PROGRAMERIS" -> "Programeris"
    "MAISTININKAS" -> "Maistininkas"
    "VADOVAS" -> "Vadovas"
    "SAVANORIS" -> "Savanoris"
    else -> role
}
