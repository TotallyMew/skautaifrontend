package lt.skautai.android.ui.events

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
fun EventMovementScreen(
    eventId: String,
    onBack: () -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    LaunchedEffect((uiState as? EventDetailUiState.Success)?.error) {
        (uiState as? EventDetailUiState.Success)?.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventoriaus judejimas") },
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
            when (val state = uiState) {
                is EventDetailUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is EventDetailUiState.Error -> SkautaiErrorState(
                    message = state.message,
                    onRetry = { viewModel.loadEvent(eventId) },
                    modifier = Modifier.align(Alignment.Center)
                )
                is EventDetailUiState.Success -> {
                    val myRoles = state.event.eventRoles.map { it.role }.toSet()
                    val canInventory = "events.inventory.distribute" in permissions ||
                        myRoles.any { it in setOf("VIRSININKAS", "KOMENDANTAS", "UKVEDYS") }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                    ) {
                        item {
                            EventMovementCard(
                                inventoryPlan = state.inventoryPlan,
                                pastovykles = state.pastovykles,
                                members = state.members,
                                custody = state.custody,
                                movements = state.movements,
                                canManage = canInventory,
                                isWorking = state.isWorking,
                                onCreatePastovykle = { name, responsibleUserId, notes ->
                                    viewModel.createPastovykle(eventId, name, responsibleUserId, notes)
                                },
                                onCreateMovement = { movementType, itemId, quantity, pastovykleId, toUserId, fromCustodyId, notes ->
                                    viewModel.createMovement(
                                        eventId,
                                        movementType,
                                        itemId,
                                        quantity,
                                        pastovykleId,
                                        toUserId,
                                        fromCustodyId,
                                        notes
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
