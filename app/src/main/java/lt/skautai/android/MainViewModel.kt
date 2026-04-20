package lt.skautai.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import lt.skautai.android.data.repository.UserRepository
import lt.skautai.android.util.TokenManager
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val userRepository: UserRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            val tuntasId = tokenManager.activeTuntasId.first() ?: return@launch
            userRepository.getMyPermissions(tuntasId)
                .onSuccess { tokenManager.savePermissions(it) }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            tokenManager.clearAll()
            onComplete()
        }
    }
}