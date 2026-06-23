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

data class PasswordResetUiState(
    val email: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class PasswordResetViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PasswordResetUiState())
    val uiState: StateFlow<PasswordResetUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value, error = null)
    }

    fun onNewPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(newPassword = value, error = null)
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = value, error = null)
    }

    fun requestReset() {
        val email = _uiState.value.email.trim()
        if (email.isBlank() || !email.contains("@")) {
            _uiState.value = _uiState.value.copy(error = "Įveskite teisingą el. pašto adresą.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.requestPasswordReset(email)
                .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, message = it) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun resetPassword(token: String) {
        val state = _uiState.value
        val error = when {
            token.isBlank() -> "Atkūrimo nuoroda neteisinga."
            state.newPassword.length < 8 -> "Slaptažodis turi būti bent 8 simbolių."
            state.newPassword.none(Char::isLetter) -> "Slaptažodyje turi būti bent viena raidė."
            state.newPassword.none(Char::isDigit) -> "Slaptažodyje turi būti bent vienas skaičius."
            state.newPassword != state.confirmPassword -> "Slaptažodžiai nesutampa."
            else -> null
        }
        if (error != null) {
            _uiState.value = state.copy(error = error)
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            authRepository.resetPassword(token, state.newPassword)
                .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, message = it) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        }
    }
}
