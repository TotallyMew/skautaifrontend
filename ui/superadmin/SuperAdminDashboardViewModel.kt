package lt.skautai.android.ui.superadmin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.AuthApiService
import lt.skautai.android.data.remote.TuntasDto
import lt.skautai.android.util.TokenManager
import javax.inject.Inject

data class SuperAdminDashboardUiState(
    val tuntai: List<TuntasDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionSuccess: String? = null
)

@HiltViewModel
class SuperAdminDashboardViewModel @Inject constructor(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuperAdminDashboardUiState())
    val uiState: StateFlow<SuperAdminDashboardUiState> = _uiState.asStateFlow()

    init {
        loadTuntai()
    }

    fun loadTuntai() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val token = tokenManager.token.first() ?: return@launch
                val response = authApiService.getTuntai("Bearer $token")
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        tuntai = response.body() ?: emptyList()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Nepavyko gauti tuntų sąrašo"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Klaida"
                )
            }
        }
    }

    fun approveTuntas(id: String) {
        viewModelScope.launch {
            try {
                val token = tokenManager.token.first() ?: return@launch
                val response = authApiService.approveTuntas("Bearer $token", id)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(actionSuccess = "Tuntas patvirtintas")
                    loadTuntai()
                } else {
                    _uiState.value = _uiState.value.copy(error = "Nepavyko patvirtinti tunto")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Klaida")
            }
        }
    }

    fun rejectTuntas(id: String) {
        viewModelScope.launch {
            try {
                val token = tokenManager.token.first() ?: return@launch
                val response = authApiService.rejectTuntas("Bearer $token", id)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(actionSuccess = "Tuntas atmestas")
                    loadTuntai()
                } else {
                    _uiState.value = _uiState.value.copy(error = "Nepavyko atmesti tunto")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Klaida")
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, actionSuccess = null)
    }
}