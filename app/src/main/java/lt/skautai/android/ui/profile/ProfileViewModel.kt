package lt.skautai.android.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.MyProfileDto
import lt.skautai.android.data.repository.UserRepository
import lt.skautai.android.util.RegistrationValidation

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isSavingProfile: Boolean = false,
    val isSavingPassword: Boolean = false,
    val profile: MyProfileDto? = null,
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val phone: String = "",
    val currentPassword: String = "",
    val newPassword: String = "",
    val repeatPassword: String = "",
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            userRepository.getMyProfile()
                .onSuccess { profile ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        profile = profile,
                        name = profile.name,
                        surname = profile.surname,
                        email = profile.email,
                        phone = profile.phone.orEmpty(),
                        error = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Nepavyko gauti profilio"
                    )
                }
        }
    }

    fun onNameChange(value: String) { update { copy(name = value, error = null) } }
    fun onSurnameChange(value: String) { update { copy(surname = value, error = null) } }
    fun onEmailChange(value: String) { update { copy(email = value, error = null) } }
    fun onPhoneChange(value: String) { update { copy(phone = value, error = null) } }
    fun onCurrentPasswordChange(value: String) { update { copy(currentPassword = value, error = null) } }
    fun onNewPasswordChange(value: String) { update { copy(newPassword = value, error = null) } }
    fun onRepeatPasswordChange(value: String) { update { copy(repeatPassword = value, error = null) } }

    fun saveProfile() {
        val state = _uiState.value
        val normalizedEmail = RegistrationValidation.normalizeEmail(state.email)
        when {
            state.name.isBlank() -> return setError("Įveskite vardą")
            state.surname.isBlank() -> return setError("Įveskite pavardę")
            RegistrationValidation.emailError(normalizedEmail) != null ->
                return setError(RegistrationValidation.emailError(normalizedEmail)!!)
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSavingProfile = true, error = null, message = null)
            userRepository.updateMyProfile(
                name = state.name.trim(),
                surname = state.surname.trim(),
                email = normalizedEmail,
                phone = state.phone.trim().ifBlank { null }
            ).onSuccess { profile ->
                _uiState.value = _uiState.value.copy(
                    isSavingProfile = false,
                    profile = profile,
                    name = profile.name,
                    surname = profile.surname,
                    email = profile.email,
                    phone = profile.phone.orEmpty(),
                    message = "Profilis atnaujintas"
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSavingProfile = false,
                    error = error.message ?: "Nepavyko atnaujinti profilio"
                )
            }
        }
    }

    fun changePassword() {
        val state = _uiState.value
        when {
            state.currentPassword.isBlank() -> return setError("Įveskite dabartinį slaptažodį")
            RegistrationValidation.passwordError(state.newPassword) != null ->
                return setError(RegistrationValidation.passwordError(state.newPassword)!!)
            state.newPassword != state.repeatPassword ->
                return setError("Nauji slaptažodžiai nesutampa")
            state.currentPassword == state.newPassword ->
                return setError("Naujas slaptažodis turi skirtis nuo dabartinio")
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSavingPassword = true, error = null, message = null)
            userRepository.changeMyPassword(
                currentPassword = state.currentPassword,
                newPassword = state.newPassword
            ).onSuccess { message ->
                _uiState.value = _uiState.value.copy(
                    isSavingPassword = false,
                    currentPassword = "",
                    newPassword = "",
                    repeatPassword = "",
                    message = message
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSavingPassword = false,
                    error = error.message ?: "Nepavyko pakeisti slaptažodžio"
                )
            }
        }
    }

    fun clearMessage() {
        update { copy(message = null) }
    }

    fun clearError() {
        update { copy(error = null) }
    }

    private fun setError(message: String) {
        update { copy(error = message) }
    }

    private fun update(block: ProfileUiState.() -> ProfileUiState) {
        _uiState.value = _uiState.value.block()
    }
}
