package lt.skautai.android.ui.tuntas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.InvitationResponseDto
import lt.skautai.android.data.remote.UserTuntasDto
import lt.skautai.android.data.repository.InvitationRepository
import lt.skautai.android.data.repository.UserRepository
import lt.skautai.android.util.TokenManager

sealed interface TuntasSelectUiState {
    object Loading : TuntasSelectUiState
    data class Success(
        val tuntai: List<UserTuntasDto>,
        val activeTuntasId: String?,
        val inviteCode: String = "",
        val isAcceptingInvite: Boolean = false,
        val isLeavingTuntas: Boolean = false,
        val acceptedInvitation: InvitationResponseDto? = null,
        val message: String? = null
    ) : TuntasSelectUiState
    data class Error(val message: String) : TuntasSelectUiState
    object Empty : TuntasSelectUiState
}

@HiltViewModel
class TuntasSelectViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val invitationRepository: InvitationRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<TuntasSelectUiState>(TuntasSelectUiState.Loading)
    val uiState: StateFlow<TuntasSelectUiState> = _uiState.asStateFlow()

    private val _navigateHome = MutableStateFlow(false)
    val navigateHome: StateFlow<Boolean> = _navigateHome.asStateFlow()

    private val _navigateLogin = MutableStateFlow(false)
    val navigateLogin: StateFlow<Boolean> = _navigateLogin.asStateFlow()

    init {
        loadTuntai()
    }

    fun loadTuntai() {
        viewModelScope.launch {
            _uiState.value = TuntasSelectUiState.Loading
            refreshTuntai(message = null)
        }
    }

    fun selectTuntas(tuntasId: String) {
        viewModelScope.launch {
            val state = _uiState.value as? TuntasSelectUiState.Success ?: return@launch
            activateTuntas(state.tuntai, tuntasId)
                .onSuccess { _navigateHome.value = true }
                .onFailure { error ->
                    _uiState.value = state.copy(message = error.message ?: "Nepavyko pakeisti tunto")
                }
        }
    }

    fun onInviteCodeChange(value: String) {
        val state = (_uiState.value as? TuntasSelectUiState.Success)
            ?: TuntasSelectUiState.Success(emptyList(), null)
        _uiState.value = state.copy(
            inviteCode = value.uppercase(),
            acceptedInvitation = null,
            message = null
        )
    }

    fun acceptInvitation() {
        val state = (_uiState.value as? TuntasSelectUiState.Success)
            ?: TuntasSelectUiState.Success(emptyList(), null)
        if (state.inviteCode.isBlank()) {
            _uiState.value = state.copy(message = "Iveskite pakvietimo koda")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isAcceptingInvite = true, message = null)
            invitationRepository.acceptInvitation(state.inviteCode)
                .onSuccess { invitation ->
                    val refreshedTuntai = userRepository.getMyTuntai().getOrDefault(state.tuntai)
                    invitation.tuntasId?.let { tuntasId ->
                        val activationResult = activateTuntas(refreshedTuntai, tuntasId)
                        if (activationResult.isFailure) {
                            _uiState.value = state.copy(
                                isAcceptingInvite = false,
                                message = activationResult.exceptionOrNull()?.message ?: "Nepavyko aktyvuoti tunto"
                            )
                            return@launch
                        }
                    }
                    _uiState.value = TuntasSelectUiState.Success(
                        tuntai = refreshedTuntai,
                        activeTuntasId = tokenManager.activeTuntasId.first(),
                        inviteCode = "",
                        acceptedInvitation = invitation,
                        message = "Pakvietimas priimtas"
                    )
                    _navigateHome.value = true
                }
                .onFailure { error ->
                    _uiState.value = state.copy(
                        isAcceptingInvite = false,
                        message = error.message ?: "Nepavyko priimti pakvietimo"
                    )
                }
        }
    }

    fun leaveTuntas(tuntasId: String) {
        viewModelScope.launch {
            val state = _uiState.value as? TuntasSelectUiState.Success ?: return@launch
            _uiState.value = state.copy(isLeavingTuntas = true, message = null)
            userRepository.leaveTuntas(tuntasId)
                .onSuccess {
                    val refreshedTuntai = userRepository.getMyTuntai().getOrDefault(emptyList())
                    val previousActive = state.activeTuntasId
                    val nextActive = when {
                        previousActive != null && previousActive != tuntasId && refreshedTuntai.any { it.id == previousActive } -> previousActive
                        else -> refreshedTuntai.firstOrNull()?.id
                    }
                    if (nextActive != null) {
                        val activationResult = activateTuntas(refreshedTuntai, nextActive)
                        if (activationResult.isFailure) {
                            _uiState.value = state.copy(
                                isLeavingTuntas = false,
                                message = activationResult.exceptionOrNull()?.message ?: "Nepavyko aktyvuoti kito tunto"
                            )
                            return@launch
                        }
                    } else {
                        tokenManager.clearActiveTuntas()
                    }
                    _uiState.value = TuntasSelectUiState.Success(
                        tuntai = refreshedTuntai,
                        activeTuntasId = tokenManager.activeTuntasId.first(),
                        message = "Tuntas paliktas"
                    )
                    _navigateHome.value = refreshedTuntai.isNotEmpty()
                }
                .onFailure { error ->
                    _uiState.value = state.copy(
                        isLeavingTuntas = false,
                        message = error.message ?: "Nepavyko palikti tunto"
                    )
                }
        }
    }

    fun clearMessage() {
        val state = _uiState.value as? TuntasSelectUiState.Success ?: return
        _uiState.value = state.copy(message = null)
    }

    fun logout() {
        viewModelScope.launch {
            tokenManager.clearAll()
            _navigateLogin.value = true
        }
    }

    fun onNavigatedHome() {
        _navigateHome.value = false
    }

    fun onNavigatedLogin() {
        _navigateLogin.value = false
    }

    private suspend fun refreshTuntai(message: String?) {
        val activeTuntasId = tokenManager.activeTuntasId.first()
        userRepository.getMyTuntai()
            .onSuccess { tuntai ->
                _uiState.value = TuntasSelectUiState.Success(
                    tuntai = tuntai,
                    activeTuntasId = activeTuntasId,
                    message = message
                )
            }
            .onFailure { error ->
                _uiState.value = TuntasSelectUiState.Error(
                    error.message ?: "Nepavyko gauti tuntu saraso"
                )
            }
    }

    private suspend fun activateTuntas(tuntai: List<UserTuntasDto>, tuntasId: String): Result<Unit> {
        val tuntas = tuntai.firstOrNull { it.id == tuntasId }
            ?: return Result.failure(Exception("Tuntas nerastas"))
        if (tuntas.status != "ACTIVE") {
            return Result.failure(Exception("Tuntas dar nepatvirtintas"))
        }
        val cachedPerms = tokenManager.permissionsForTuntas(tuntasId)
        return userRepository.getMyPermissions(tuntasId)
            .mapCatching { permissions ->
                tokenManager.setActiveTuntas(tuntasId, tuntas.name)
                tokenManager.setActiveOrgUnit(null)
                tokenManager.savePermissions(permissions)
                tokenManager.cachePermissionsForTuntas(tuntasId, permissions)
            }
            .recoverCatching { error ->
                if (cachedPerms != null) {
                    tokenManager.setActiveTuntas(tuntasId, tuntas.name)
                    tokenManager.setActiveOrgUnit(null)
                    tokenManager.savePermissions(cachedPerms.toList())
                } else {
                    throw error
                }
            }
    }
}
