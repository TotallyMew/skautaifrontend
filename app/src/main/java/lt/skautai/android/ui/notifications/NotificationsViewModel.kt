package lt.skautai.android.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lt.skautai.android.data.remote.NotificationDto
import lt.skautai.android.data.repository.NotificationRepository

data class NotificationsUiState(
    val notifications: List<NotificationDto> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationsUiState(isLoading = true))
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            notificationRepository.getNotifications()
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        notifications = response.notifications,
                        unreadCount = response.unreadCount
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Nepavyko gauti pranešimų"
                    )
                }
        }
    }

    fun markRead(id: String, afterMarked: () -> Unit) {
        viewModelScope.launch {
            val current = _uiState.value
            _uiState.value = current.copy(
                notifications = current.notifications.map {
                    if (it.id == id) it.copy(readAt = it.readAt ?: "") else it
                },
                unreadCount = (current.unreadCount - current.notifications.count { it.id == id && it.readAt == null }).coerceAtLeast(0)
            )
            notificationRepository.markRead(id)
            afterMarked()
        }
    }

    fun markAllRead() {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            notificationRepository.markAllRead()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        notifications = _uiState.value.notifications.map { it.copy(readAt = it.readAt ?: "") },
                        unreadCount = 0
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = error.message ?: "Nepavyko pažymėti pranešimų"
                    )
                }
        }
    }
}
