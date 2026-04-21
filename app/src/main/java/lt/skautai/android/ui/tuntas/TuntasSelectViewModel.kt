package lt.skautai.android.ui.tuntas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.UserTuntasDto
import lt.skautai.android.data.repository.UserRepository
import lt.skautai.android.util.TokenManager
import javax.inject.Inject

sealed interface TuntasSelectUiState {
    object Loading : TuntasSelectUiState
    data class Success(val tuntai: List<UserTuntasDto>) : TuntasSelectUiState
    data class Error(val message: String) : TuntasSelectUiState
    object Empty : TuntasSelectUiState
}

@HiltViewModel
class TuntasSelectViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<TuntasSelectUiState>(TuntasSelectUiState.Loading)
    val uiState: StateFlow<TuntasSelectUiState> = _uiState.asStateFlow()

    private val _navigateToInventory = MutableStateFlow(false)
    val navigateToInventory: StateFlow<Boolean> = _navigateToInventory.asStateFlow()

    init {
        loadTuntai()
    }

    fun loadTuntai() {
        viewModelScope.launch {
            _uiState.value = TuntasSelectUiState.Loading
            userRepository.getMyTuntai()
                .onSuccess { tuntai ->
                    when {
                        tuntai.isEmpty() -> _uiState.value = TuntasSelectUiState.Empty
                        tuntai.size == 1 -> {
                            val tuntas = tuntai.first()
                            val id = tuntas.id
                            tokenManager.setActiveTuntas(id, tuntas.name)
                            userRepository.getMyPermissions(id)
                                .onSuccess { tokenManager.savePermissions(it) }
                            _navigateToInventory.value = true
                        }
                        else -> _uiState.value = TuntasSelectUiState.Success(tuntai)
                    }
                }
                .onFailure { error ->
                    _uiState.value = TuntasSelectUiState.Error(
                        error.message ?: "Nepavyko gauti tuntų sąrašo"
                    )
                }
        }
    }

    fun selectTuntas(tuntasId: String) {
        viewModelScope.launch {
            val tuntasName = (_uiState.value as? TuntasSelectUiState.Success)
                ?.tuntai
                ?.firstOrNull { it.id == tuntasId }
                ?.name
            tokenManager.setActiveTuntas(tuntasId, tuntasName)
            userRepository.getMyPermissions(tuntasId)
                .onSuccess { tokenManager.savePermissions(it) }
            _navigateToInventory.value = true
        }
    }

    fun onNavigatedToInventory() {
        _navigateToInventory.value = false
    }
}
