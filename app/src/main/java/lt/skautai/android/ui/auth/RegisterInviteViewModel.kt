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
    val nameError: String? = null,
    val surnameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val phoneError: String? = null,
    val inviteCodeError: String? = null,
    val isSuccess: Boolean = false,
    val hasActiveTuntas: Boolean = false
)

@HiltViewModel
class RegisterInviteViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterInviteUiState())
    val uiState: StateFlow<RegisterInviteUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) { _uiState.value = _uiState.value.copy(name = value, nameError = null, error = null) }
    fun onSurnameChange(value: String) { _uiState.value = _uiState.value.copy(surname = value, surnameError = null, error = null) }
    fun onEmailChange(value: String) { _uiState.value = _uiState.value.copy(email = value, emailError = null, error = null) }
    fun onPasswordChange(value: String) { _uiState.value = _uiState.value.copy(password = value, passwordError = null, error = null) }
    fun onPhoneChange(value: String) { _uiState.value = _uiState.value.copy(phone = value, phoneError = null, error = null) }
    fun onInviteCodeChange(value: String) { _uiState.value = _uiState.value.copy(inviteCode = value, inviteCodeError = null, error = null) }

    fun register() {
        val state = _uiState.value
        val nameError = RegistrationValidation.nameError(state.name)
        val surnameError = RegistrationValidation.surnameError(state.surname)
        val emailError = RegistrationValidation.emailError(state.email)
        val passwordError = RegistrationValidation.passwordError(state.password)
        val phoneError = RegistrationValidation.phoneError(state.phone)
        val inviteCodeError = RegistrationValidation.inviteCodeError(state.inviteCode)

        if (
            nameError != null ||
            surnameError != null ||
            emailError != null ||
            passwordError != null ||
            phoneError != null ||
            inviteCodeError != null
        ) {
            _uiState.value = state.copy(
                nameError = nameError,
                surnameError = surnameError,
                emailError = emailError,
                passwordError = passwordError,
                phoneError = phoneError,
                inviteCodeError = inviteCodeError,
                error = "Patikslinkite pažymėtus laukus."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(
                isLoading = true,
                error = null,
                nameError = null,
                surnameError = null,
                emailError = null,
                passwordError = null,
                phoneError = null,
                inviteCodeError = null
            )
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
                    hasActiveTuntas = response.tuntai.orEmpty().count { it.status == "ACTIVE" } == 1
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
