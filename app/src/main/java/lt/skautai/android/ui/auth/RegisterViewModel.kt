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

data class RegisterUiState(
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val password: String = "",
    val phone: String = "",
    val tuntasName: String = "",
    val tuntasKrastas: String = "",
    val isLoading: Boolean = false,
    val nameError: String? = null,
    val surnameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val tuntasNameError: String? = null,
    val tuntasKrastasError: String? = null,
    val formError: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value, nameError = null, formError = null)
    }

    fun onSurnameChange(value: String) {
        _uiState.value = _uiState.value.copy(surname = value, surnameError = null, formError = null)
    }

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value, emailError = null, formError = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, passwordError = null, formError = null)
    }

    fun onPhoneChange(value: String) {
        _uiState.value = _uiState.value.copy(phone = value)
    }

    fun onTuntasNameChange(value: String) {
        _uiState.value = _uiState.value.copy(
            tuntasName = value,
            tuntasNameError = null,
            formError = null
        )
    }

    fun onTuntasKrastasChange(value: String) {
        _uiState.value = _uiState.value.copy(
            tuntasKrastas = value,
            tuntasKrastasError = null,
            formError = null
        )
    }

    fun register() {
        val state = _uiState.value
        val nameError = if (state.name.isBlank()) "Įveskite vardą." else null
        val surnameError = if (state.surname.isBlank()) "Įveskite pavardę." else null
        val tuntasNameError = if (state.tuntasName.isBlank()) "Įveskite tunto pavadinimą." else null
        val emailError = RegistrationValidation.emailError(state.email)?.normalizeLithuanianAscii()
        val passwordError = RegistrationValidation.passwordError(state.password)?.normalizeLithuanianAscii()
        val krastasError = RegistrationValidation.krastasError(state.tuntasKrastas)?.normalizeLithuanianAscii()

        if (
            nameError != null ||
            surnameError != null ||
            tuntasNameError != null ||
            emailError != null ||
            passwordError != null ||
            krastasError != null
        ) {
            _uiState.value = state.copy(
                nameError = nameError,
                surnameError = surnameError,
                emailError = emailError,
                passwordError = passwordError,
                tuntasNameError = tuntasNameError,
                tuntasKrastasError = krastasError,
                formError = "Patikslinkite pažymėtus laukus."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(
                isLoading = true,
                nameError = null,
                surnameError = null,
                emailError = null,
                passwordError = null,
                tuntasNameError = null,
                tuntasKrastasError = null,
                formError = null
            )
            authRepository.registerTuntininkas(
                name = state.name.trim(),
                surname = state.surname.trim(),
                email = RegistrationValidation.normalizeEmail(state.email),
                password = state.password,
                phone = state.phone.ifBlank { null },
                tuntasName = state.tuntasName.trim(),
                tuntasKrastas = state.tuntasKrastas
            ).onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    formError = error.message ?: "Registracija nepavyko."
                )
            }
        }
    }
}

private fun String.normalizeLithuanianAscii(): String = this
    .replace('į', 'i')
    .replace('Į', 'I')
    .replace('š', 's')
    .replace('Š', 'S')
    .replace('ž', 'z')
    .replace('Ž', 'Z')
    .replace('ė', 'e')
    .replace('Ė', 'E')
    .replace('ų', 'u')
    .replace('Ų', 'U')
    .replace('ū', 'u')
    .replace('Ū', 'U')
    .replace('ą', 'a')
    .replace('Ą', 'A')
    .replace('ę', 'e')
    .replace('Ę', 'E')
    .replace('č', 'c')
    .replace('Č', 'C')
