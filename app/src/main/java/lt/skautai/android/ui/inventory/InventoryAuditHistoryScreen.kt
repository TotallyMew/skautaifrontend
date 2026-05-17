package lt.skautai.android.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.ItemCheckDto
import lt.skautai.android.data.remote.StorageAuditSessionDto
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.ui.common.ItemCheckResultPill
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiSectionHeader
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.SkautaiStatusTone

enum class AuditHistoryFilter(val apiValue: String?) {
    All(null),
    Open("OPEN"),
    Completed("COMPLETED")
}

sealed interface InventoryAuditHistoryUiState {
    data object Loading : InventoryAuditHistoryUiState
    data class Success(val sessions: List<StorageAuditSessionDto>) : InventoryAuditHistoryUiState
    data class Error(val message: String) : InventoryAuditHistoryUiState
    data object Empty : InventoryAuditHistoryUiState
}

@HiltViewModel
class InventoryAuditHistoryViewModel @Inject constructor(
    private val itemRepository: ItemRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<InventoryAuditHistoryUiState>(InventoryAuditHistoryUiState.Loading)
    val uiState: StateFlow<InventoryAuditHistoryUiState> = _uiState.asStateFlow()

    private val _filter = MutableStateFlow(AuditHistoryFilter.All)
    val filter: StateFlow<AuditHistoryFilter> = _filter.asStateFlow()

    init {
        load()
    }

    fun setFilter(filter: AuditHistoryFilter) {
        if (_filter.value == filter) return
        _filter.value = filter
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = InventoryAuditHistoryUiState.Loading
            itemRepository.listStorageAuditSessions(_filter.value.apiValue)
                .onSuccess { sessions ->
                    _uiState.value = if (sessions.isEmpty()) {
                        InventoryAuditHistoryUiState.Empty
                    } else {
                        InventoryAuditHistoryUiState.Success(sessions)
                    }
                }
                .onFailure {
                    _uiState.value = InventoryAuditHistoryUiState.Error(
                        it.message ?: "Nepavyko gauti inventorizaciju istorijos"
                    )
                }
        }
    }
}

@Composable
fun InventoryAuditHistoryScreen(
    onOpenSession: (String) -> Unit,
    viewModel: InventoryAuditHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val filter by viewModel.filter.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SkautaiSectionHeader(
                title = "Inventorizaciju istorija",
                subtitle = "Perziurek atviras ir uzbaigtas sesijas"
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AuditFilterButton("Visos", filter == AuditHistoryFilter.All) {
                    viewModel.setFilter(AuditHistoryFilter.All)
                }
                AuditFilterButton("Atviros", filter == AuditHistoryFilter.Open) {
                    viewModel.setFilter(AuditHistoryFilter.Open)
                }
                AuditFilterButton("Uzbaigtos", filter == AuditHistoryFilter.Completed) {
                    viewModel.setFilter(AuditHistoryFilter.Completed)
                }
            }
        }

        when (val state = uiState) {
            InventoryAuditHistoryUiState.Loading -> item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is InventoryAuditHistoryUiState.Error -> item {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            InventoryAuditHistoryUiState.Empty -> item {
                SkautaiCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Inventorizaciju sesiju dar nera.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            is InventoryAuditHistoryUiState.Success -> items(state.sessions, key = { it.id }) { session ->
                AuditSessionCard(
                    session = session,
                    onOpen = { onOpenSession(session.id) }
                )
            }
        }
    }
}

@Composable
private fun AuditFilterButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        Button(onClick = onClick) { Text(label) }
    } else {
        FilledTonalButton(onClick = onClick) { Text(label) }
    }
}

@Composable
private fun AuditSessionCard(
    session: StorageAuditSessionDto,
    onOpen: () -> Unit
) {
    SkautaiCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = auditSessionScopeLabel(session),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Pradeta ${session.createdAt.take(16).replace('T', ' ')}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SkautaiStatusPill(
                    label = if (session.status == "OPEN") "Atvira" else "Uzbaigta",
                    tone = if (session.status == "OPEN") SkautaiStatusTone.Warning else SkautaiStatusTone.Success
                )
            }
            Text(
                text = "Patikrinta ${session.summary.checked}/${session.summary.total} • Nerasta ${session.summary.missing} • Sugadinta ${session.summary.damaged}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

sealed interface InventoryAuditSessionUiState {
    data object Loading : InventoryAuditSessionUiState
    data class Success(val session: StorageAuditSessionDto) : InventoryAuditSessionUiState
    data class Error(val message: String) : InventoryAuditSessionUiState
}

@HiltViewModel
class InventoryAuditSessionViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow<InventoryAuditSessionUiState>(InventoryAuditSessionUiState.Loading)
    val uiState: StateFlow<InventoryAuditSessionUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = InventoryAuditSessionUiState.Loading
            itemRepository.getStorageAuditSession(sessionId)
                .onSuccess { _uiState.value = InventoryAuditSessionUiState.Success(it) }
                .onFailure {
                    _uiState.value = InventoryAuditSessionUiState.Error(
                        it.message ?: "Nepavyko gauti inventorizacijos sesijos"
                    )
                }
        }
    }
}

@Composable
fun InventoryAuditSessionScreen(
    onContinueSession: (StorageAuditSessionDto) -> Unit,
    viewModel: InventoryAuditSessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        InventoryAuditSessionUiState.Loading -> {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator()
            }
        }
        is InventoryAuditSessionUiState.Error -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                FilledTonalButton(onClick = viewModel::load) {
                    Text("Bandyti dar karta")
                }
            }
        }
        is InventoryAuditSessionUiState.Success -> {
            val session = state.session
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SkautaiSectionHeader(
                                title = auditSessionScopeLabel(session),
                                subtitle = "Sesija ${session.id.take(8)}"
                            )
                            Text(
                                text = "Patikrinta ${session.summary.checked}/${session.summary.total}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Rasta ${session.summary.found} • Nerasta ${session.summary.missing} • Ne vietoje ${session.summary.misplaced} • Sugadinta ${session.summary.damaged}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (session.status == "OPEN") {
                                Button(onClick = { onContinueSession(session) }) {
                                    Text("Testi inventorizacija")
                                }
                            }
                        }
                    }
                }
                items(session.checks, key = { it.id }) { check ->
                    AuditCheckCard(check)
                }
            }
        }
    }
}

@Composable
private fun AuditCheckCard(check: ItemCheckDto) {
    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = check.itemName ?: "Nezinomas daiktas",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                ItemCheckResultPill(check.result.toUiResult())
            }
            check.actualLocationPath?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = "Vieta: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            check.notes?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun auditSessionScopeLabel(session: StorageAuditSessionDto): String {
    val parts = listOfNotNull(
        session.scopeCustodianName,
        session.scopeType,
        session.scopeCategory,
        if (session.scopeSharedOnly) "Bendras inventorius" else null
    )
    return parts.joinToString(" / ").ifBlank { "Inventorizacija" }
}

private fun String.toUiResult() = runCatching {
    lt.skautai.android.ui.common.ItemCheckResult.valueOf(this)
}.getOrDefault(lt.skautai.android.ui.common.ItemCheckResult.FOUND)
