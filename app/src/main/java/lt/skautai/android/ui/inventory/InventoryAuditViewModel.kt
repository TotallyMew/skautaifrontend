package lt.skautai.android.ui.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.ItemCheckDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.UpsertStorageAuditCheckRequestDto
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.ui.common.ItemCheckResult
import lt.skautai.android.ui.common.ItemCheckSummary
import lt.skautai.android.util.TokenManager

sealed interface InventoryAuditUiState {
    object Loading : InventoryAuditUiState
    data class Success(val items: List<ItemDto>) : InventoryAuditUiState
    data class Error(val message: String) : InventoryAuditUiState
    object Empty : InventoryAuditUiState
}

data class AuditEntryDraft(
    val result: ItemCheckResult,
    val actualQuantity: Int,
    val conditionAtCheck: String? = null,
    val actualLocationNote: String = "",
    val notes: String = ""
)

internal fun buildInventoryAuditSummary(
    items: List<ItemDto>,
    results: Map<String, AuditEntryDraft>
): ItemCheckSummary {
    var found = 0
    var missing = 0
    var misplaced = 0
    var damaged = 0
    var consumed = 0
    var matched = 0
    var decreased = 0
    var increased = 0
    var expectedQuantityTotal = 0
    var actualQuantityTotal = 0
    var shortageQuantityTotal = 0
    var overageQuantityTotal = 0

    items.forEach { item ->
        val draft = results[item.id]
        if (draft != null) {
            when (draft.result) {
                ItemCheckResult.FOUND -> found += 1
                ItemCheckResult.MISSING -> missing += 1
                ItemCheckResult.MISPLACED -> misplaced += 1
                ItemCheckResult.DAMAGED -> damaged += 1
                ItemCheckResult.CONSUMED -> consumed += 1
            }
            expectedQuantityTotal += item.quantity
            actualQuantityTotal += draft.actualQuantity
            when {
                draft.actualQuantity < item.quantity -> {
                    decreased += 1
                    shortageQuantityTotal += item.quantity - draft.actualQuantity
                }

                draft.actualQuantity > item.quantity -> {
                    increased += 1
                    overageQuantityTotal += draft.actualQuantity - item.quantity
                }

                else -> matched += 1
            }
        }
    }

    return ItemCheckSummary(
        total = items.size,
        found = found,
        missing = missing,
        misplaced = misplaced,
        damaged = damaged,
        consumed = consumed,
        unchecked = (items.size - results.size).coerceAtLeast(0),
        matched = matched,
        decreased = decreased,
        increased = increased,
        expectedQuantityTotal = expectedQuantityTotal,
        actualQuantityTotal = actualQuantityTotal,
        shortageQuantityTotal = shortageQuantityTotal,
        overageQuantityTotal = overageQuantityTotal
    )
}

internal fun applyMissingToUnchecked(
    items: List<ItemDto>,
    results: Map<String, AuditEntryDraft>
): Map<String, AuditEntryDraft> {
    if (items.isEmpty()) return results
    val next = results.toMutableMap()
    items.forEach { item ->
        next.putIfAbsent(
            item.id,
            AuditEntryDraft(
                result = ItemCheckResult.MISSING,
                actualQuantity = 0
            )
        )
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

    private val _auditResults = MutableStateFlow<Map<String, AuditEntryDraft>>(emptyMap())
    val auditResults: StateFlow<Map<String, AuditEntryDraft>> = _auditResults.asStateFlow()

    private val _showUncheckedOnly = MutableStateFlow(false)
    val showUncheckedOnly: StateFlow<Boolean> = _showUncheckedOnly.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isResolving = MutableStateFlow(false)
    val isResolving: StateFlow<Boolean> = _isResolving.asStateFlow()

    private val _isCompleting = MutableStateFlow(false)
    val isCompleting: StateFlow<Boolean> = _isCompleting.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        observeItems()
        loadItems()
    }

    private fun observeItems() {
        viewModelScope.launch {
            val currentUserId = if (personalOwnerOnly) tokenManager.userId.first() else null
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
            val currentUserId = if (personalOwnerOnly) tokenManager.userId.first() else null
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

    fun saveItemEntry(item: ItemDto, draft: AuditEntryDraft?) {
        _auditResults.value = _auditResults.value.toMutableMap().also { map ->
            if (draft == null) {
                map.remove(item.id)
            } else {
                map[item.id] = draft
            }
        }
        if (draft != null) {
            syncChecks(listOf(item.id to draft))
        }
    }

    fun markUncheckedAsMissing() {
        val items = (uiState.value as? InventoryAuditUiState.Success)?.items.orEmpty()
        if (items.isEmpty()) return
        _auditResults.value = applyMissingToUnchecked(items, _auditResults.value)
        syncChecks(
            items.mapNotNull { item ->
                _auditResults.value[item.id]?.let { draft -> item.id to draft }
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
                        _message.value = "Nuskenuotas daiktas nepatenka i sia inventorizacija."
                    } else {
                        saveItemEntry(item, defaultDraft(item, ItemCheckResult.FOUND))
                        _message.value = "Pazymeta kaip rasta: ${item.name}"
                    }
                }
                .onFailure {
                    _message.value = it.message ?: "Nepavyko atpazinti kodo"
                }
            _isResolving.value = false
        }
    }

    fun completeAudit(onCompleted: (String) -> Unit) {
        val currentSessionId = sessionId ?: return
        if (_isCompleting.value) return
        val items = (uiState.value as? InventoryAuditUiState.Success)?.items.orEmpty()
        val unchecked = (items.size - _auditResults.value.size).coerceAtLeast(0)
        if (unchecked > 0) {
            _message.value = "Prieš užbaigiant reikia patikrinti visus daiktus. Liko: $unchecked."
            return
        }
        viewModelScope.launch {
            _isCompleting.value = true
            itemRepository.completeStorageAuditSession(currentSessionId)
                .onSuccess {
                    _message.value = "Inventorizacija užbaigta."
                    onCompleted(currentSessionId)
                }
                .onFailure {
                    _message.value = it.message ?: "Nepavyko užbaigti inventorizacijos"
                }
            _isCompleting.value = false
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
                            check.toDraft()?.let { draft -> itemId to draft }
                        }
                    }
                }
            }
        }
    }

    private fun syncChecks(entries: List<Pair<String, AuditEntryDraft>>) {
        val currentSessionId = sessionId ?: return
        viewModelScope.launch {
            itemRepository.upsertStorageAuditChecks(
                sessionId = currentSessionId,
                checks = entries.map { (itemId, draft) ->
                    UpsertStorageAuditCheckRequestDto(
                        itemId = itemId,
                        result = draft.result.name,
                        actualQuantity = draft.actualQuantity,
                        actualLocationNote = draft.actualLocationNote.ifBlank { null },
                        conditionAtCheck = draft.conditionAtCheck,
                        notes = draft.notes.ifBlank { null }
                    )
                }
            )
        }
    }
}

internal fun defaultDraft(item: ItemDto, result: ItemCheckResult): AuditEntryDraft = AuditEntryDraft(
    result = result,
    actualQuantity = when (result) {
        ItemCheckResult.MISSING -> 0
        else -> item.quantity
    },
    conditionAtCheck = when (result) {
        ItemCheckResult.MISSING -> null
        ItemCheckResult.DAMAGED -> "DAMAGED"
        else -> item.condition
    }
)

private fun ItemCheckDto.toDraft(): AuditEntryDraft? {
    val parsedResult = result.toItemCheckResult() ?: return null
    return AuditEntryDraft(
        result = parsedResult,
        actualQuantity = actualQuantity,
        conditionAtCheck = conditionAtCheck,
        actualLocationNote = actualLocationNote.orEmpty(),
        notes = notes.orEmpty()
    )
}

private fun String.toItemCheckResult(): ItemCheckResult? = runCatching {
    ItemCheckResult.valueOf(this)
}.getOrNull()
