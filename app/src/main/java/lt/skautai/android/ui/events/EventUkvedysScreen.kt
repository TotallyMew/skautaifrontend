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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiErrorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventUkvedysScreen(
    eventId: String,
    onBack: () -> Unit,
    viewModel: EventUkvedysViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(eventId) { viewModel.load(eventId) }

    LaunchedEffect((uiState as? EventUkvedysUiState.Success)?.error) {
        (uiState as? EventUkvedysUiState.Success)?.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val state = uiState
    val canInventory = "events.inventory.distribute" in permissions ||
        (state as? EventUkvedysUiState.Success)?.event?.eventRoles
            ?.any { it.role in setOf("VIRSININKAS", "KOMENDANTAS", "UKVEDYS") } == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ukvedzio suvestine") },
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
                is EventUkvedysUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is EventUkvedysUiState.Error -> SkautaiErrorState(
                    message = state.message,
                    onRetry = { viewModel.load(eventId) },
                    modifier = Modifier.align(Alignment.Center)
                )
                is EventUkvedysUiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            UkvedysCard(
                                eventStatus = state.event.status,
                                inventoryPlan = state.inventoryPlan,
                                pastovykles = state.pastovykles,
                                pastovykleRequestsById = state.pastovykleRequestsById,
                                canManage = canInventory,
                                isWorking = state.isWorking,
                                onCreatePurchase = { selected ->
                                    viewModel.createPurchaseFromSelected(eventId, selected)
                                },
                                onCreateBucket = { name, type, pastovykleId, notes ->
                                    viewModel.createInventoryBucket(eventId, name, type, pastovykleId, notes)
                                },
                                onUpdateBucket = { bucketId, name, type, pastovykleId, notes ->
                                    viewModel.updateInventoryBucket(eventId, bucketId, name, type, pastovykleId, notes)
                                },
                                onDeleteBucket = { bucketId ->
                                    viewModel.deleteInventoryBucket(eventId, bucketId)
                                },
                                onCreateAllocation = { inventoryItemId, bucketId, quantity, notes ->
                                    viewModel.createAllocation(eventId, inventoryItemId, bucketId, quantity, notes)
                                },
                                onUpdateAllocation = { allocationId, quantity, notes ->
                                    viewModel.updateAllocation(eventId, allocationId, quantity, notes)
                                },
                                onDeleteAllocation = { allocationId ->
                                    viewModel.deleteAllocation(eventId, allocationId)
                                },
                                onApproveRequest = { pastovykleId, requestId ->
                                    viewModel.approvePastovykleRequest(eventId, pastovykleId, requestId)
                                },
                                onRejectRequest = { pastovykleId, requestId ->
                                    viewModel.rejectPastovykleRequest(eventId, pastovykleId, requestId)
                                },
                                onFulfillRequest = { pastovykleId, requestId ->
                                    viewModel.fulfillPastovykleRequest(eventId, pastovykleId, requestId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
