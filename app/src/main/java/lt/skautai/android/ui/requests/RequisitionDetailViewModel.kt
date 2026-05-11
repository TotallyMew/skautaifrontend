package lt.skautai.android.ui.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.AddRequisitionItemToInventoryDto
import lt.skautai.android.data.remote.AddRequisitionToInventoryDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.RequisitionDto
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.data.repository.RequisitionRepository
import lt.skautai.android.util.TokenManager
import javax.inject.Inject

sealed interface RequisitionDetailUiState {
    data object Loading : RequisitionDetailUiState
    data class Success(
        val request: RequisitionDto,
        val inventoryItems: List<ItemDto> = emptyList(),
        val isActioning: Boolean = false,
        val error: String? = null
    ) : RequisitionDetailUiState
    data class Error(val message: String) : RequisitionDetailUiState
}

@HiltViewModel
class RequisitionDetailViewModel @Inject constructor(
    private val repository: RequisitionRepository,
    private val itemRepository: ItemRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<RequisitionDetailUiState>(RequisitionDetailUiState.Loading)
    val uiState: StateFlow<RequisitionDetailUiState> = _uiState.asStateFlow()

    val permissions: StateFlow<Set<String>> = tokenManager.permissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val currentUserId: StateFlow<String?> = tokenManager.userId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeOrgUnitId: StateFlow<String?> = tokenManager.activeOrgUnitId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun loadRequest(id: String) {
        viewModelScope.launch {
            _uiState.value = RequisitionDetailUiState.Loading
            repository.getRequest(id)
                .onSuccess { request ->
                    val items = itemRepository.getItems(status = "ACTIVE").getOrDefault(emptyList())
                    _uiState.value = RequisitionDetailUiState.Success(request, inventoryItems = items)
                }
                .onFailure {
                    _uiState.value = RequisitionDetailUiState.Error(
                        it.message ?: "Klaida gaunant prašymą"
                    )
                }
        }
    }

    fun approveInUnit(id: String) = runUnitReview(id, "APPROVED", null)

    fun forwardToTop(id: String) = runUnitReview(id, "FORWARDED", null)

    fun rejectInUnit(id: String, reason: String?) = runUnitReview(id, "REJECTED", reason)

    fun approveTopLevel(id: String) = runTopLevelReview(id, "APPROVED", null)

    fun rejectTopLevel(id: String, reason: String?) = runTopLevelReview(id, "REJECTED", reason)

    fun markPurchased(id: String) {
        val current = _uiState.value as? RequisitionDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isActioning = true, error = null)
            repository.markPurchased(id)
                .onSuccess { loadRequest(id) }
                .onFailure {
                    _uiState.value = current.copy(
                        isActioning = false,
                        error = it.message ?: "Klaida pažymint kaip nupirkta"
                    )
                }
        }
    }

    fun addPurchasedItemToInventory(
        id: String,
        requisitionItemId: String,
        action: String,
        existingItemId: String?,
        notes: String?
    ) {
        val current = _uiState.value as? RequisitionDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isActioning = true, error = null)
            repository.addToInventory(
                id,
                AddRequisitionToInventoryDto(
                    items = listOf(
                        AddRequisitionItemToInventoryDto(
                            requisitionItemId = requisitionItemId,
                            action = action,
                            existingItemId = existingItemId,
                            custodianId = current.request.requestingUnitId,
                            notes = notes?.ifBlank { null }
                        )
                    )
                )
            ).onSuccess { loadRequest(id) }
                .onFailure {
                    _uiState.value = current.copy(
                        isActioning = false,
                        error = it.message ?: "Klaida pridedant į inventorių"
                    )
                }
        }
    }

    fun cancelRequest(id: String) {
        val current = _uiState.value as? RequisitionDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isActioning = true, error = null)
            repository.cancelRequest(id)
                .onSuccess { loadRequest(id) }
                .onFailure {
                    _uiState.value = current.copy(
                        isActioning = false,
                        error = it.message ?: "Klaida atsaukiant prasyma"
                    )
                }
        }
    }

    private fun runUnitReview(id: String, action: String, reason: String?) {
        val current = _uiState.value as? RequisitionDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isActioning = true, error = null)
            repository.unitReview(id, action, reason)
                .onSuccess { loadRequest(id) }
                .onFailure {
                    _uiState.value = current.copy(
                        isActioning = false,
                        error = it.message ?: "Klaida atliekant vieneto sprendimą"
                    )
                }
        }
    }

    private fun runTopLevelReview(id: String, action: String, reason: String?) {
        val current = _uiState.value as? RequisitionDetailUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isActioning = true, error = null)
            repository.topLevelReview(id, action, reason)
                .onSuccess { loadRequest(id) }
                .onFailure {
                    _uiState.value = current.copy(
                        isActioning = false,
                        error = it.message ?: "Klaida atliekant tunto sprendimą"
                    )
                }
        }
    }

    fun clearError() {
        val current = _uiState.value as? RequisitionDetailUiState.Success ?: return
        _uiState.value = current.copy(error = null)
    }
}
