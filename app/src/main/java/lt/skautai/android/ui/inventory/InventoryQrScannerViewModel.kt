package lt.skautai.android.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.repository.ItemRepository

@HiltViewModel
class InventoryQrScannerViewModel @Inject constructor(
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _isResolving = MutableStateFlow(false)
    val isResolving: StateFlow<Boolean> = _isResolving.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun resolveToken(token: String, onResolved: (String) -> Unit) {
        if (_isResolving.value) return

        viewModelScope.launch {
            _isResolving.value = true
            itemRepository.resolveQrToken(token)
                .onSuccess {
                    _message.value = null
                    onResolved(it)
                }
                .onFailure {
                    _message.value = it.message ?: "Nepavyko atpazinti QR kodo"
                }
            _isResolving.value = false
        }
    }

    fun showMessage(message: String) {
        _message.value = message
    }

    fun clearMessage() {
        _message.value = null
    }
}
