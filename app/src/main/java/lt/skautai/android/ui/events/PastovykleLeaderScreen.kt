package lt.skautai.android.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastovykleLeaderScreen(
    eventId: String,
    onBack: () -> Unit,
    viewModel: PastovykleLeaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(eventId) {
        viewModel.load(eventId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mano pastovykle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atgal")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            PastovykleLeaderUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is PastovykleLeaderUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.load(eventId) }) {
                        Text("Bandyti dar karta")
                    }
                }
            }

            is PastovykleLeaderUiState.Success -> {
                val myPastovykles = state.pastovykles.filter { it.responsibleUserId == state.currentUserId }
                var selectedPastovykleId by remember(myPastovykles) {
                    mutableStateOf(myPastovykles.firstOrNull()?.id)
                }
                val selectedPastovykle = myPastovykles.firstOrNull { it.id == selectedPastovykleId }
                val inventory = state.pastovykleInventoryById[selectedPastovykleId].orEmpty()
                val requests = state.pastovykleRequestsById[selectedPastovykleId].orEmpty()
                val unitItems = state.items.filter { it.custodianId == state.activeOrgUnitId }
                val pastovykleBucketIds = state.inventoryPlan?.buckets
                    .orEmpty()
                    .filter { it.pastovykleId == selectedPastovykleId }
                    .map { it.id }
                    .toSet()
                val allocations = state.inventoryPlan?.allocations
                    .orEmpty()
                    .filter { it.bucketId in pastovykleBucketIds }

                if (myPastovykles.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Jums dar nepriskirta nei viena pastovyklė.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            DropdownField(
                                label = "Pastovykle",
                                value = selectedPastovykle?.name ?: "Pasirinkti",
                                options = myPastovykles.map { it.id to it.name },
                                onSelect = { selectedPastovykleId = it }
                            )
                        }

                        if (selectedPastovykle != null) {
                            item {
                                RequestNeedCard(
                                    inventoryItems = state.inventoryPlan?.items.orEmpty(),
                                    isWorking = state.isWorking,
                                    onCreateRequest = { inventoryItemId, quantity, notes ->
                                        viewModel.createPastovykleRequest(eventId, selectedPastovykle.id, inventoryItemId, quantity, notes)
                                    }
                                )
                            }
                            item {
                                BringFromUnitCard(
                                    unitItems = unitItems,
                                    isWorking = state.isWorking,
                                    onAssign = { itemId, quantity, notes ->
                                        viewModel.assignFromUnitInventory(eventId, selectedPastovykle.id, itemId, quantity, notes)
                                    }
                                )
                            }
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Jau paskirta man", style = MaterialTheme.typography.titleMedium)
                                        if (allocations.isEmpty()) {
                                            Text("Ukvedys dar nesuplanuavo paskirstymo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        } else {
                                            allocations.forEach { allocation ->
                                                Text("${allocation.bucketName}: ${allocation.quantity} vnt.")
                                            }
                                        }
                                    }
                                }
                            }
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Mano pastovyklės inventorius", style = MaterialTheme.typography.titleMedium)
                                        if (inventory.isEmpty()) {
                                            Text("Inventorius dar nepriskirtas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        } else {
                                            inventory.forEach { row ->
                                                Text("${row.itemName}: ${row.quantityAssigned - row.quantityReturned} / ${row.quantityAssigned}")
                                            }
                                        }
                                    }
                                }
                            }
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Poreikiai", style = MaterialTheme.typography.titleMedium)
                                        if (requests.isEmpty()) {
                                            Text("Poreikių dar nėra.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        } else {
                                            requests.forEach { request ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(Modifier.weight(1f)) {
                                                        Text("${request.itemName}: ${request.quantity} vnt.")
                                                        Text(request.status, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                    if (request.status in listOf("PENDING", "APPROVED")) {
                                                        Button(onClick = {
                                                            viewModel.selfProvidePastovykleRequest(eventId, selectedPastovykle.id, request.id, "Pasirūpinta savo jėgomis")
                                                        }) {
                                                            Text("Pasirupinau pats")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestNeedCard(
    inventoryItems: List<lt.skautai.android.data.remote.EventInventoryItemDto>,
    isWorking: Boolean,
    onCreateRequest: (String, String, String) -> Unit
) {
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    var quantity by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ko man reikia", style = MaterialTheme.typography.titleMedium)
            DropdownField(
                label = "Daiktas",
                value = inventoryItems.firstOrNull { it.id == selectedItemId }?.name ?: "Pasirinkti",
                options = inventoryItems.map { it.id to it.name },
                onSelect = { selectedItemId = it }
            )
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it.filter(Char::isDigit) },
                label = { Text("Kiekis") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Pastabos") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    selectedItemId?.let { onCreateRequest(it, quantity, notes) }
                    quantity = ""
                    notes = ""
                },
                enabled = !isWorking && selectedItemId != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Prasyti is ukvedzio")
            }
        }
    }
}

@Composable
private fun BringFromUnitCard(
    unitItems: List<lt.skautai.android.data.remote.ItemDto>,
    isWorking: Boolean,
    onAssign: (String, String, String) -> Unit
) {
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    var quantity by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Atsivesiu is savo vieneto", style = MaterialTheme.typography.titleMedium)
            DropdownField(
                label = "Vieneto daiktas",
                value = unitItems.firstOrNull { it.id == selectedItemId }?.name ?: "Pasirinkti",
                options = unitItems.map { it.id to "${it.name} (${it.quantity})" },
                onSelect = { selectedItemId = it }
            )
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it.filter(Char::isDigit) },
                label = { Text("Kiekis") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Pastabos") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    selectedItemId?.let { onAssign(it, quantity, notes) }
                    quantity = ""
                    notes = ""
                },
                enabled = !isWorking && selectedItemId != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Pazymeti, kad atsivesiu")
            }
        }
    }
}
