package lt.skautai.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import lt.skautai.android.util.TokenManager
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val tokenManager: TokenManager
) : ViewModel() {

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            tokenManager.clearAll()
            onComplete()
        }
    }
}