package lt.skautai.android.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.ui.common.SkautaiErrorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventMovementScreen(
    eventId: String,
    onBack: () -> Unit,
    onOpenItemQr: (String) -> Unit,
    onOpenCustodyQr: (String) -> Unit,
    viewModel: EventMovementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(eventId) {
        viewModel.load(eventId)
    }

    LaunchedEffect((uiState as? EventMovementUiState.Success)?.error) {
        (uiState as? EventMovementUiState.Success)?.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    EventScreenScaffold(
        title = "Inventoriaus judėjimas",
        onBack = onBack,
        snackbarHostState = snackbarHostState
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is EventMovementUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is EventMovementUiState.Error -> {
                    SkautaiErrorState(
                        message = state.message,
                        onRetry = { viewModel.load(eventId) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is EventMovementUiState.Success -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            EventMovementCard(
                                inventoryPlan = state.inventoryPlan,
                                custody = state.custody,
                                movements = state.movements,
                                isWorking = state.isWorking,
                                onOpenItemQr = { onOpenItemQr(eventId) },
                                onOpenCustodyQr = { onOpenCustodyQr(eventId) },
                                onCreateMovement = { movementType, itemId, quantity, pastovykleId, toUserId, fromCustodyId, notes ->
                                    viewModel.createMovement(
                                        eventId = eventId,
                                        movementType = movementType,
                                        eventInventoryItemId = itemId,
                                        quantityText = quantity,
                                        pastovykleId = pastovykleId,
                                        toUserId = toUserId,
                                        fromCustodyId = fromCustodyId,
                                        notes = notes
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
