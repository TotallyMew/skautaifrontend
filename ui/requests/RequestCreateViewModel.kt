package lt.skautai.android.ui.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.CreateBendrasRequestDto
import lt.skautai.android.data.remote.OrganizationalUnitDto
import lt.skautai.android.data.remote.MemberDto
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
    val selectedOrgUnitId: String? = null,
    val selectedOrgUnitName: String? = null,
    val itemDescription: String = "",
    val quantity: String = "1",
    val neededByDate: String = "",
    val notes: String = ""
)

@HiltViewModel
class RequestCreateViewModel @Inject constructor(
    private val requestRepository: RequestRepository,
    private val orgUnitRepository: OrganizationalUnitRepository,
    private val memberRepository: MemberRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RequestCreateUiState())
    val uiState: StateFlow<RequestCreateUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val unitsResult = orgUnitRepository.getUnits()
            val currentUserId = tokenManager.userId.first()
            val currentMemberResult = currentUserId?.let { memberRepository.getMember(it) }

            unitsResult
                .onSuccess { units ->
                    val ownUnit = currentUserId?.let { userId ->
                        findOwnUnit(userId, currentMemberResult?.getOrNull(), units)
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoadingItems = false,
                        orgUnits = ownUnit?.let(::listOf) ?: emptyList(),
                        selectedOrgUnitId = ownUnit?.id,
                        selectedOrgUnitName = ownUnit?.name
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingItems = false)
                }
        }
    }

    fun onItemDescriptionChange(value: String) {
        _uiState.value = _uiState.value.copy(itemDescription = value)
    }

    fun onOrgUnitSelected(orgUnitId: String?) {
        val selectedUnit = _uiState.value.orgUnits.find { it.id == orgUnitId }
        _uiState.value = _uiState.value.copy(
            selectedOrgUnitId = orgUnitId,
            selectedOrgUnitName = selectedUnit?.name
        )
    }

    fun onQuantityChange(value: String) {
        _uiState.value = _uiState.value.copy(quantity = value)
    }

    fun onNeededByDateChange(value: String) {
        _uiState.value = _uiState.value.copy(neededByDate = value)
    }

    fun onNotesChange(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun createRequest() {
        val state = _uiState.value

        if (state.itemDescription.isBlank()) {
            _uiState.value = state.copy(error = "Įveskite daikto aprašymą")
            return
        }
        val qty = state.quantity.toIntOrNull()
        if (qty == null || qty < 1) {
            _uiState.value = state.copy(error = "Kiekis turi būti teigiamas skaičius")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            requestRepository.createRequest(
                CreateBendrasRequestDto(
                    itemDescription = state.itemDescription,
                    quantity = qty,
                    neededByDate = state.neededByDate.ifBlank { null },
                    requestingUnitId = state.selectedOrgUnitId,
                    notes = state.notes.ifBlank { null }
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
