package lt.skautai.android.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.util.NavRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryDetailScreen(
    itemId: String,
    navController: NavController,
    viewModel: InventoryDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    val deleteError by viewModel.deleteError.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadItem(itemId)
    }

    LaunchedEffect(deleted) {
        if (deleted) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(deleteError) {
        deleteError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onDeleteErrorShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daikto informacija") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atgal")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(NavRoutes.InventoryAddEdit.createRoute(itemId))
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Redaguoti")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Ištrinti",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is InventoryDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is InventoryDetailUiState.Error -> {
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
                        Button(onClick = { viewModel.loadItem(itemId) }) {
                            Text("Bandyti dar kartą")
                        }
                    }
                }

                is InventoryDetailUiState.Success -> {
                    ItemDetailContent(item = state.item)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Ištrinti daiktą?") },
            text = { Text("Daiktas bus pažymėtas kaip neaktyvus. Šio veiksmo negalima atšaukti.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteItem(itemId)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Ištrinti")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Atšaukti")
                }
            }
        )
    }
}

@Composable
private fun ItemDetailContent(item: ItemDto) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(item.status)
            ConditionChip(item.condition)
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        DetailRow(label = "Kategorija", value = when (item.category) {
            "COLLECTIVE" -> "Bendras"
            "ASSIGNED" -> "Priskirtas"
            "INDIVIDUAL" -> "Asmeninis"
            else -> item.category
        })

        DetailRow(label = "Kiekis", value = "${item.quantity} vnt.")

        item.notes?.let {
            if (it.isNotBlank()) DetailRow(label = "Pastabos", value = it)
        }

        item.purchaseDate?.let {
            DetailRow(label = "Pirkimo data", value = it)
        }

        item.purchasePrice?.let {
            DetailRow(label = "Pirkimo kaina", value = "%.2f €".format(it))
        }

        item.locationId?.let {
            DetailRow(label = "Vieta", value = it)
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Sukurta: ${item.createdAt.take(10)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Atnaujinta: ${item.updatedAt.take(10)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun StatusChip(status: String) {
    val (label, color) = when (status) {
        "ACTIVE" -> "Aktyvus" to MaterialTheme.colorScheme.primary
        "PENDING_APPROVAL" -> "Laukia patvirtinimo" to MaterialTheme.colorScheme.tertiary
        "INACTIVE" -> "Neaktyvus" to MaterialTheme.colorScheme.onSurfaceVariant
        else -> status to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun ConditionChip(condition: String) {
    val (label, color) = when (condition) {
        "GOOD" -> "Gera" to MaterialTheme.colorScheme.primary
        "DAMAGED" -> "Pažeista" to MaterialTheme.colorScheme.error
        "WRITTEN_OFF" -> "Nurašyta" to MaterialTheme.colorScheme.onSurfaceVariant
        else -> condition to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    )
}