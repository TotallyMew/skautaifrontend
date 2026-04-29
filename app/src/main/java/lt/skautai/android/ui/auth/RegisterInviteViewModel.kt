package lt.skautai.android.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.repository.AuthRepository
import lt.skautai.android.util.RegistrationValidation

data class RegisterInviteUiState(
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val password: String = "",
    val phone: String = "",
    val inviteCode: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val tuntaiCount: Int = 0
)

@HiltViewModel
class RegisterInviteViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterInviteUiState())
    val uiState: StateFlow<RegisterInviteUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) { _uiState.value = _uiState.value.copy(name = value, error = null) }
    fun onSurnameChange(value: String) { _uiState.value = _uiState.value.copy(surname = value, error = null) }
    fun onEmailChange(value: String) { _uiState.value = _uiState.value.copy(email = value, error = null) }
    fun onPasswordChange(value: String) { _uiState.value = _uiState.value.copy(password = value, error = null) }
    fun onPhoneChange(value: String) { _uiState.value = _uiState.value.copy(phone = value) }
    fun onInviteCodeChange(value: String) { _uiState.value = _uiState.value.copy(inviteCode = value, error = null) }

    fun register() {
        val state = _uiState.value

        if (state.name.isBlank() || state.surname.isBlank() || state.inviteCode.isBlank()) {
            _uiState.value = state.copy(error = "Užpildykite privalomus laukus")
            return
        }

        RegistrationValidation.emailError(state.email)?.let { error ->
            _uiState.value = state.copy(error = error)
            return
        }

        RegistrationValidation.passwordError(state.password)?.let { error ->
            _uiState.value = state.copy(error = error)
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            authRepository.registerWithInvite(
                name = state.name.trim(),
                surname = state.surname.trim(),
                email = RegistrationValidation.normalizeEmail(state.email),
                password = state.password,
                phone = state.phone.ifBlank { null },
                inviteCode = state.inviteCode.trim()
            ).onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true,
                    tuntaiCount = response.tuntai.orEmpty().size
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Registracija nepavyko."
                )
            }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
