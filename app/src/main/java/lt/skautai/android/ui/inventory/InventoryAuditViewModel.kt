package lt.skautai.android.ui.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.UpsertStorageAuditCheckRequestDto
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.ui.common.ItemCheckResult
import lt.skautai.android.ui.common.ItemCheckSummary
import lt.skautai.android.ui.common.buildItemCheckSummary
import lt.skautai.android.util.TokenManager

sealed interface InventoryAuditUiState {
    object Loading : InventoryAuditUiState
    data class Success(val items: List<ItemDto>) : InventoryAuditUiState
    data class Error(val message: String) : InventoryAuditUiState
    object Empty : InventoryAuditUiState
}

internal fun buildInventoryAuditSummary(
    items: List<ItemDto>,
    results: Map<String, ItemCheckResult>
): ItemCheckSummary = buildItemCheckSummary(
    total = items.size,
    results = items.map { results[it.id] }
)

internal fun applyMissingToUnchecked(
    items: List<ItemDto>,
    results: Map<String, ItemCheckResult>
): Map<String, ItemCheckResult> {
    if (items.isEmpty()) return results
    val next = results.toMutableMap()
    items.forEach { item ->
        next.putIfAbsent(item.id, ItemCheckResult.MISSING)
    }
    return next
}

@HiltViewModel
class InventoryAuditViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val tokenManager: TokenManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private var sessionId: String? = null

    private val initialCustodianId = savedStateHandle.get<String?>("custodianId")
    private val initialType = savedStateHandle.get<String?>("type")
    private val initialCategory = savedStateHandle.get<String?>("category")
    private val initialSharedOnly = savedStateHandle.get<Boolean>("sharedOnly") ?: false
    private val personalOwnerOnly = savedStateHandle.get<String?>("personalOwner") == "me"

    private val _uiState = MutableStateFlow<InventoryAuditUiState>(InventoryAuditUiState.Loading)
    val uiState: StateFlow<InventoryAuditUiState> = _uiState.asStateFlow()

    private val _auditResults = MutableStateFlow<Map<String, ItemCheckResult>>(emptyMap())
    val auditResults: StateFlow<Map<String, ItemCheckResult>> = _auditResults.asStateFlow()

    private val _showUncheckedOnly = MutableStateFlow(false)
    val showUncheckedOnly: StateFlow<Boolean> = _showUncheckedOnly.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isResolving = MutableStateFlow(false)
    val isResolving: StateFlow<Boolean> = _isResolving.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        observeItems()
        loadItems()
    }

    private fun observeItems() {
        viewModelScope.launch {
            val currentUserId = if (personalOwnerOnly) {
                tokenManager.userId.first()
            } else {
                null
            }
            itemRepository.observeItems(
                custodianId = initialCustodianId,
                type = initialType,
                category = initialCategory,
                sharedOnly = initialSharedOnly,
                createdByUserId = currentUserId
            ).collect { items ->
                _uiState.value = when {
                    items.isEmpty() -> InventoryAuditUiState.Empty
                    else -> InventoryAuditUiState.Success(items.sortedBy { it.name.lowercase() })
                }
                val validIds = items.mapTo(mutableSetOf()) { it.id }
                _auditResults.value = _auditResults.value.filterKeys { it in validIds }
                if (items.isNotEmpty() && sessionId == null) {
                    bootstrapSession()
                }
            }
        }
    }

    fun loadItems() {
        viewModelScope.launch {
            _isRefreshing.value = true
            if (_uiState.value !is InventoryAuditUiState.Success) {
                _uiState.value = InventoryAuditUiState.Loading
            }
            val currentUserId = if (personalOwnerOnly) {
                tokenManager.userId.first()
            } else {
                null
            }
            itemRepository.refreshItems(
                custodianId = initialCustodianId,
                type = initialType,
                category = initialCategory,
                sharedOnly = initialSharedOnly,
                createdByUserId = currentUserId
            ).onFailure {
                if (_uiState.value !is InventoryAuditUiState.Success) {
                    _uiState.value = InventoryAuditUiState.Error(
                        it.message ?: "Nepavyko gauti inventoriaus inventorizacijai"
                    )
                }
            }
            _isRefreshing.value = false
        }
    }

    fun summary(items: List<ItemDto>): ItemCheckSummary =
        buildInventoryAuditSummary(items, _auditResults.value)

    fun toggleUncheckedOnly() {
        _showUncheckedOnly.value = !_showUncheckedOnly.value
    }

    fun markItem(itemId: String, result: ItemCheckResult?) {
        _auditResults.value = _auditResults.value.toMutableMap().also { map ->
            if (result == null) {
                map.remove(itemId)
            } else {
                map[itemId] = result
            }
        }
        if (result != null) {
            syncChecks(listOf(itemId to result))
        }
    }

    fun markUncheckedAsMissing() {
        val items = (uiState.value as? InventoryAuditUiState.Success)?.items.orEmpty()
        if (items.isEmpty()) return
        _auditResults.value = applyMissingToUnchecked(items, _auditResults.value)
        syncChecks(
            items.mapNotNull { item ->
                _auditResults.value[item.id]?.let { result -> item.id to result }
            }
        )
        _message.value = "Neinventorizuoti daiktai pažymėti kaip nerasti."
    }

    fun resolveToken(token: String) {
        if (_isResolving.value) return
        viewModelScope.launch {
            _isResolving.value = true
            itemRepository.resolveQrToken(token)
                .onSuccess { itemId ->
                    val items = (uiState.value as? InventoryAuditUiState.Success)?.items.orEmpty()
                    val item = items.firstOrNull { it.id == itemId }
                    if (item == null) {
                        _message.value = "Nuskenuotas daiktas nepatenka į šią inventorizaciją."
                    } else {
                        markItem(item.id, ItemCheckResult.FOUND)
                        _message.value = "Pažymėta kaip rasta: ${item.name}"
                    }
                }
                .onFailure {
                    _message.value = it.message ?: "Nepavyko atpažinti QR kodo"
                }
            _isResolving.value = false
        }
    }

    fun onMessageShown() {
        _message.value = null
    }

    fun showMessage(message: String) {
        _message.value = message
    }

    private fun bootstrapSession() {
        viewModelScope.launch {
            val ownerUserId = if (personalOwnerOnly) tokenManager.userId.first() else null
            itemRepository.createStorageAuditSession(
                custodianId = initialCustodianId,
                type = initialType,
                category = initialCategory,
                sharedOnly = initialSharedOnly,
                personalOwnerUserId = ownerUserId
            ).onSuccess { session ->
                sessionId = session.id
                if (session.checks.isNotEmpty()) {
                    _auditResults.update { existing ->
                        existing + session.checks.mapNotNull { check ->
                            val itemId = check.itemId ?: return@mapNotNull null
                            val result = check.result.toItemCheckResult() ?: return@mapNotNull null
                            itemId to result
                        }
                    }
                }
            }
        }
    }

    private fun syncChecks(entries: List<Pair<String, ItemCheckResult>>) {
        val currentSessionId = sessionId ?: return
        viewModelScope.launch {
            itemRepository.upsertStorageAuditChecks(
                sessionId = currentSessionId,
                checks = entries.map { (itemId, result) ->
                    UpsertStorageAuditCheckRequestDto(
                        itemId = itemId,
                        result = result.name
                    )
                }
            )
        }
    }
}

private fun String.toItemCheckResult(): ItemCheckResult? = runCatching {
    ItemCheckResult.valueOf(this)
}.getOrNull()
