package lt.skautai.android.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiErrorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventNeedsScreen(
    eventId: String,
    onBack: () -> Unit,
    viewModel: EventNeedsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showInventoryPicker by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) { viewModel.load(eventId) }

    LaunchedEffect((uiState as? EventNeedsUiState.Success)?.error) {
        (uiState as? EventNeedsUiState.Success)?.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val state = uiState
    val canInventory = "events.inventory.distribute" in permissions ||
        (state as? EventNeedsUiState.Success)?.event?.eventRoles
            ?.any { it.role in setOf("VIRSININKAS", "KOMENDANTAS", "UKVEDYS") } == true

    if (showInventoryPicker && state is EventNeedsUiState.Success) {
        LaunchedEffect(Unit) { viewModel.loadItemCatalog(eventId) }
        ModalBottomSheet(onDismissRequest = { showInventoryPicker = false }) {
            InventoryPickerSheet(
                items = state.items,
                buckets = state.inventoryPlan?.buckets.orEmpty(),
                members = state.members,
                isWorking = state.isWorking,
                onCreateNeedsBulk = { selected, bucketId, responsibleId, notes ->
                    viewModel.createNeedsBulk(eventId, selected, bucketId, responsibleId, notes)
                    showInventoryPicker = false
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Poreikiai") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atgal")
                    }
                }
            )
        },
        snackbarHost = { SkautaiErrorSnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state) {
                is EventNeedsUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is EventNeedsUiState.Error -> SkautaiErrorState(
                    message = state.message,
                    onRetry = { viewModel.load(eventId) },
                    modifier = Modifier.align(Alignment.Center)
                )
                is EventNeedsUiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            NeedsCard(
                                inventoryPlan = state.inventoryPlan,
                                members = state.members,
                                canEdit = canInventory,
                                isWorking = state.isWorking,
                                onOpenInventoryPicker = { showInventoryPicker = true },
                                onCreateNeed = { itemId, name, quantity, bucketId, responsibleUserId, notes ->
                                    viewModel.createNeed(eventId, itemId, name, quantity, bucketId, responsibleUserId, notes)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
