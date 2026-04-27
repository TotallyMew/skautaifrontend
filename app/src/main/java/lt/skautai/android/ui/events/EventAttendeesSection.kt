package lt.skautai.android.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
    var pendingRoleRemoval by remember { mutableStateOf<EventRoleDto?>(null) }

    pendingRoleRemoval?.let { role ->
        AlertDialog(
            onDismissRequest = { pendingRoleRemoval = null },
            title = { Text("Salinti is stabo?") },
            text = {
                Text("${role.userName ?: role.userId} bus pasalintas is renginio stabo pareigu ${eventRoleLabel(role.role)}.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRoleRemoval = null
                        onRemoveRole(role.id)
                    }
                ) {
                    Text("Salinti", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRoleRemoval = null }) { Text("Atsaukti") }
            }
        )
    }

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
                        TextButton(onClick = { pendingRoleRemoval = role }) {
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
    var searchQuery by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("VADOVAS") }
    val roleOptions = listOf("VIRSININKAS", "KOMENDANTAS", "UKVEDYS", "PROGRAMERIS", "MAISTININKAS", "VADOVAS", "SAVANORIS")
    val filteredMembers = remember(members, searchQuery) {
        val query = searchQuery.trim()
        members
            .sortedBy { it.fullName().lowercase() }
            .filter { member ->
                query.isBlank() ||
                    member.fullName().contains(query, ignoreCase = true) ||
                    member.email.contains(query, ignoreCase = true)
            }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Prideti i staba", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Ieskoti zmogaus") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier.heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(filteredMembers, key = { it.userId }) { member ->
                    val isSelected = member.userId == selectedUserId
                    TextButton(
                        onClick = { selectedUserId = member.userId },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = member.fullName(),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Text(
                                text = member.email,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
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
