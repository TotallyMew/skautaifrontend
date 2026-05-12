package lt.skautai.android.ui.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.CreateRequisitionDto
import lt.skautai.android.data.remote.CreateRequisitionItemDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.data.repository.MemberRepository
import lt.skautai.android.data.repository.OrganizationalUnitRepository
import lt.skautai.android.data.repository.RequisitionRepository
import lt.skautai.android.util.TokenManager
import javax.inject.Inject

data class RequisitionCreateUiState(
    val isLoadingUnits: Boolean = true,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val canRequestForTuntas: Boolean = false,
    val orgUnits: List<OrganizationalUnitDto> = emptyList(),
    val selectedOrgUnitId: String? = null,
    val selectedOrgUnitName: String? = null,
    val requestType: String = "NEW_ITEM",
    val existingItemId: String? = null,
    val existingItems: List<ItemDto> = emptyList(),
    val itemName: String = "",
    val itemDescription: String = "",
    val quantity: String = "1",
    val neededByDate: String = "",
    val notes: String = ""
)

@HiltViewModel
class RequisitionCreateViewModel @Inject constructor(
    private val repository: RequisitionRepository,
    private val orgUnitRepository: OrganizationalUnitRepository,
    private val memberRepository: MemberRepository,
    private val itemRepository: ItemRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RequisitionCreateUiState())
    val uiState: StateFlow<RequisitionCreateUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val unitsResult = orgUnitRepository.getUnits()
            val currentUserId = tokenManager.userId.first()
            val currentMemberResult = currentUserId?.let { memberRepository.getMember(it) }

            unitsResult
                .onSuccess { units ->
                    val ownUnits = currentUserId?.let { userId ->
                        findOwnUnits(userId, currentMemberResult?.getOrNull(), units)
                    }.orEmpty()
                    val selected = ownUnits.firstOrNull()
                    val currentMember = currentMemberResult?.getOrNull()
                    _uiState.value = _uiState.value.copy(
                        isLoadingUnits = false,
                        canRequestForTuntas = canRequestForTuntas(currentMember),
                        orgUnits = ownUnits,
                        selectedOrgUnitId = selected?.id,
                        selectedOrgUnitName = selected?.name
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingUnits = false)
                }

            itemRepository.getItems(status = "ACTIVE")
                .onSuccess { items ->
                    _uiState.value = _uiState.value.copy(existingItems = items)
                }
        }
    }

    fun onItemNameChange(value: String) {
        _uiState.value = _uiState.value.copy(itemName = value)
    }

    fun onItemDescriptionChange(value: String) {
        _uiState.value = _uiState.value.copy(itemDescription = value)
    }

    fun onQuantityChange(value: String) {
        _uiState.value = _uiState.value.copy(quantity = value)
    }

    fun onRequestTypeChange(value: String) {
        val resetFields = if (value == "RESTOCK_EXISTING") {
            _uiState.value.copy(itemDescription = "")
        } else {
            _uiState.value.copy(existingItemId = null)
        }
        _uiState.value = resetFields.copy(requestType = value)
    }

    fun onExistingItemSelected(itemId: String?) {
        val selected = _uiState.value.existingItems.firstOrNull { it.id == itemId }
        _uiState.value = _uiState.value.copy(
            existingItemId = itemId,
            itemName = selected?.name ?: _uiState.value.itemName
        )
    }

    fun onNeededByDateChange(value: String) {
        _uiState.value = _uiState.value.copy(neededByDate = value)
    }

    fun onNotesChange(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }

    fun onOrgUnitSelected(orgUnitId: String?) {
        val selected = _uiState.value.orgUnits.find { it.id == orgUnitId }
        _uiState.value = _uiState.value.copy(
            selectedOrgUnitId = orgUnitId,
            selectedOrgUnitName = selected?.name
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun createRequest() {
        val state = _uiState.value
        if (state.requestType == "NEW_ITEM" && state.itemName.isBlank()) {
            _uiState.value = state.copy(error = "Įveskite norimo daikto pavadinimą")
            return
        }
        val quantity = state.quantity.toIntOrNull()
        if (quantity == null || quantity < 1) {
            _uiState.value = state.copy(error = "Kiekis turi būti teigiamas skaičius")
            return
        }
        if (state.requestType == "RESTOCK_EXISTING" && state.existingItemId.isNullOrBlank()) {
            _uiState.value = state.copy(error = "Pasirinkite esamą daiktą papildymui")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            repository.createRequest(
                CreateRequisitionDto(
                    requestingUnitId = state.selectedOrgUnitId,
                    neededByDate = state.neededByDate.ifBlank { null },
                    notes = state.notes.ifBlank { null },
                    items = listOf(
                        CreateRequisitionItemDto(
                            itemName = if (state.requestType == "RESTOCK_EXISTING") {
                                state.existingItems.firstOrNull { it.id == state.existingItemId }?.name ?: state.itemName
                            } else {
                                state.itemName
                            },
                            itemDescription = if (state.requestType == "RESTOCK_EXISTING") {
                                null
                            } else {
                                state.itemDescription.ifBlank { null }
                            },
                            quantity = quantity,
                            requestType = state.requestType,
                            existingItemId = state.existingItemId
                        )
                    )
                )
            ).onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, isSuccess = true)
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = it.message ?: "Klaida kuriant prašymą"
                )
            }
        }
    }

    private suspend fun findOwnUnits(
        currentUserId: String,
        currentMember: MemberDto?,
        units: List<OrganizationalUnitDto>
    ): List<OrganizationalUnitDto> {
        val leadershipUnitIds = currentMember?.leadershipRoles
            ?.filter { it.termStatus == "ACTIVE" && !it.organizationalUnitId.isNullOrBlank() }
            ?.mapNotNull { it.organizationalUnitId }
            .orEmpty()
            .toSet()

        val ownUnitIds = leadershipUnitIds.toMutableSet()
        units.forEach { unit ->
            val isAssigned = orgUnitRepository.getUnitMembers(unit.id)
                .getOrDefault(emptyList())
                .any { it.userId == currentUserId && it.leftAt == null }
            if (isAssigned) ownUnitIds += unit.id
        }

        return units.filter { it.id in ownUnitIds }
    }

    private fun canRequestForTuntas(currentMember: MemberDto?): Boolean {
        if (currentMember == null) return false

        val allowedTopLevelRoles = setOf("Tuntininkas", "Tuntininko pavaduotojas", "Inventorininkas")
        val isTopLevel = currentMember.leadershipRoles.any {
            it.termStatus == "ACTIVE" &&
                it.organizationalUnitId.isNullOrBlank() &&
                it.roleName in allowedTopLevelRoles
        }
        val isDraugininkas = currentMember.leadershipRoles.any {
            it.termStatus == "ACTIVE" &&
                !it.organizationalUnitId.isNullOrBlank() &&
                it.roleName.contains("drauginink", ignoreCase = true) &&
                !it.roleName.contains("pavaduotoj", ignoreCase = true)
        }
        return isTopLevel || isDraugininkas
    }
}
