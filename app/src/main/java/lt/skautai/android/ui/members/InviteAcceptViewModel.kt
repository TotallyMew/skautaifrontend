package lt.skautai.android.ui.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.InvitationResponseDto
import lt.skautai.android.data.repository.InvitationRepository
import lt.skautai.android.data.repository.UserRepository
import lt.skautai.android.util.TokenManager
import javax.inject.Inject

data class InviteAcceptUiState(
    val code: String = "",
    val isSaving: Boolean = false,
    val acceptedInvitation: InvitationResponseDto? = null,
    val error: String? = null
)

@HiltViewModel
class InviteAcceptViewModel @Inject constructor(
    private val invitationRepository: InvitationRepository,
    private val userRepository: UserRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(InviteAcceptUiState())
    val uiState: StateFlow<InviteAcceptUiState> = _uiState.asStateFlow()

    fun onCodeChange(value: String) {
        _uiState.value = _uiState.value.copy(
            code = value.uppercase(),
            error = null,
            acceptedInvitation = null
        )
    }

    fun acceptInvitation() {
        val state = _uiState.value
        if (state.code.isBlank()) {
            _uiState.value = state.copy(error = "Iveskite pakvietimo koda")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            invitationRepository.acceptInvitation(state.code)
                .onSuccess { response ->
                    tokenManager.activeTuntasId.first()?.let { tuntasId ->
                        userRepository.getMyPermissions(tuntasId)
                            .onSuccess { tokenManager.savePermissions(it) }
                    }
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        code = "",
                        acceptedInvitation = response
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = error.message ?: "Nepavyko priimti pakvietimo"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
