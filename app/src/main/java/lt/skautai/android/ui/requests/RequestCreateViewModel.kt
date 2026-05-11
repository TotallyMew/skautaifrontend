package lt.skautai.android.ui.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.CreateBendrasRequestDto
import lt.skautai.android.data.remote.CreateBendrasRequestItemDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.data.repository.MemberRepository
import lt.skautai.android.data.repository.OrganizationalUnitRepository
import lt.skautai.android.data.repository.RequestRepository
import lt.skautai.android.util.TokenManager
import javax.inject.Inject

data class RequestCreateUiState(
    val isLoadingItems: Boolean = true,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val orgUnits: List<OrganizationalUnitDto> = emptyList(),
    val sharedItems: List<ItemDto> = emptyList(),
    val selectedItems: Map<String, String> = emptyMap(),
    val searchQuery: String = "",
    val selectedOrgUnitId: String? = null,
    val selectedOrgUnitName: String? = null,
    val neededByDate: String = "",
    val notes: String = ""
)

@HiltViewModel
class RequestCreateViewModel @Inject constructor(
    private val requestRepository: RequestRepository,
    private val itemRepository: ItemRepository,
    private val orgUnitRepository: OrganizationalUnitRepository,
    private val memberRepository: MemberRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RequestCreateUiState())
    val uiState: StateFlow<RequestCreateUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val unitsResult = orgUnitRepository.getUnits()
            val sharedItemsResult = itemRepository.getItems(sharedOnly = true, status = "ACTIVE")
            val currentUserId = tokenManager.userId.first()
            val currentMemberResult = currentUserId?.let { memberRepository.getMember(it) }
            val units = unitsResult.getOrDefault(emptyList())
            val ownUnit = currentUserId?.let { userId ->
                findOwnUnit(userId, currentMemberResult?.getOrNull(), units)
            }

            _uiState.value = _uiState.value.copy(
                isLoadingItems = false,
                orgUnits = ownUnit?.let(::listOf) ?: emptyList(),
                sharedItems = sharedItemsResult.getOrDefault(emptyList())
                    .filter { it.custodianId == null && it.status == "ACTIVE" && it.quantity > 0 }
                    .sortedBy { it.name.lowercase() },
                selectedOrgUnitId = ownUnit?.id,
                selectedOrgUnitName = ownUnit?.name,
                error = when {
                    unitsResult.isFailure -> unitsResult.exceptionOrNull()?.message
                    sharedItemsResult.isFailure -> sharedItemsResult.exceptionOrNull()?.message
                    else -> null
                }
            )
        }
    }

    fun onOrgUnitSelected(orgUnitId: String?) {
        val selectedUnit = _uiState.value.orgUnits.find { it.id == orgUnitId }
        _uiState.value = _uiState.value.copy(
            selectedOrgUnitId = orgUnitId,
            selectedOrgUnitName = selectedUnit?.name
        )
    }

    fun onItemSelectionChange(itemId: String, selected: Boolean) {
        val selectedItems = _uiState.value.selectedItems.toMutableMap()
        if (selected) {
            selectedItems[itemId] = selectedItems[itemId] ?: "1"
        } else {
            selectedItems.remove(itemId)
        }
        _uiState.value = _uiState.value.copy(selectedItems = selectedItems)
    }

    fun increaseItem(itemId: String) {
        val state = _uiState.value
        val item = state.sharedItems.find { it.id == itemId } ?: return
        val current = state.selectedItems[itemId]?.toIntOrNull() ?: 0
        if (current >= item.quantity) return
        _uiState.value = state.copy(
            selectedItems = state.selectedItems + (itemId to (current + 1).toString())
        )
    }

    fun decreaseItem(itemId: String) {
        val state = _uiState.value
        val current = state.selectedItems[itemId]?.toIntOrNull() ?: 0
        val nextItems = if (current <= 1) {
            state.selectedItems - itemId
        } else {
            state.selectedItems + (itemId to (current - 1).toString())
        }
        _uiState.value = state.copy(selectedItems = nextItems)
    }

    fun onItemQuantityChange(itemId: String, value: String) {
        if (!_uiState.value.selectedItems.containsKey(itemId)) return
        _uiState.value = _uiState.value.copy(
            selectedItems = _uiState.value.selectedItems + (itemId to value.filter(Char::isDigit))
        )
    }

    fun onNeededByDateChange(value: String) {
        _uiState.value = _uiState.value.copy(neededByDate = value)
    }

    fun onSearchQueryChange(value: String) {
        _uiState.value = _uiState.value.copy(searchQuery = value)
    }

    fun onNotesChange(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun createRequest() {
        val state = _uiState.value

        if (state.selectedOrgUnitId.isNullOrBlank()) {
            _uiState.value = state.copy(error = "Pasirink vienetą, kuriam kuriamas prašymas")
            return
        }

        if (state.selectedItems.isEmpty()) {
            _uiState.value = state.copy(error = "Pasirink bent vieną daiktą")
            return
        }

        val selectedLines = mutableListOf<CreateBendrasRequestItemDto>()
        state.selectedItems.forEach { (itemId, quantityText) ->
            val item = state.sharedItems.find { it.id == itemId } ?: return@forEach
            val quantity = quantityText.toIntOrNull()
            when {
                quantity == null || quantity < 1 -> {
                    _uiState.value = state.copy(error = "Kiekis turi būti teigiamas skaičius")
                    return
                }
                quantity > item.quantity -> {
                    _uiState.value = state.copy(error = "Kiekis negali viršyti turimo daikto kiekio")
                    return
                }
                else -> selectedLines += CreateBendrasRequestItemDto(itemId = itemId, quantity = quantity)
            }
        }

        if (selectedLines.isEmpty()) {
            _uiState.value = state.copy(error = "Pasirink bent vieną daiktą")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            requestRepository.createRequest(
                CreateBendrasRequestDto(
                    itemDescription = if (selectedLines.size == 1) {
                        state.sharedItems.find { it.id == selectedLines.first().itemId }?.name
                    } else {
                        "Keli bendro inventoriaus daiktai"
                    },
                    neededByDate = state.neededByDate.ifBlank { null },
                    requestingUnitId = state.selectedOrgUnitId,
                    notes = state.notes.ifBlank { null },
                    items = selectedLines
                )
            ).onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, isSuccess = true)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = error.message ?: "Klaida kuriant prašymą"
                )
            }
        }
    }

    private suspend fun findOwnUnit(
        currentUserId: String,
        currentMember: MemberDto?,
        units: List<OrganizationalUnitDto>
    ): OrganizationalUnitDto? {
        val leadershipUnitId = currentMember?.leadershipRoles
            ?.firstOrNull { it.termStatus == "ACTIVE" && !it.organizationalUnitId.isNullOrBlank() }
            ?.organizationalUnitId

        if (leadershipUnitId != null) {
            return units.find { it.id == leadershipUnitId }
        }

        units.forEach { unit ->
            val isAssigned = orgUnitRepository.getUnitMembers(unit.id)
                .getOrDefault(emptyList())
                .any { it.userId == currentUserId && it.leftAt == null }
            if (isAssigned) return unit
        }

        return null
    }
}
