package lt.skautai.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import lt.skautai.android.data.live.LiveEventService
import lt.skautai.android.data.notifications.FcmTokenRegistrar
import lt.skautai.android.data.repository.UserRepository
import lt.skautai.android.data.repository.AuthRepository
import lt.skautai.android.data.sync.PendingSyncScheduler
import lt.skautai.android.util.TokenManager
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val pendingSyncScheduler: PendingSyncScheduler,
    private val liveEventService: LiveEventService,
    private val fcmTokenRegistrar: FcmTokenRegistrar
) : ViewModel() {

    init {
        pendingSyncScheduler.schedule()
        pendingSyncScheduler.startWatchingNetwork()
        liveEventService.start()
        viewModelScope.launch {
            tokenManager.token
                .distinctUntilChanged()
                .collect { token ->
                    if (!token.isNullOrBlank()) {
                        fcmTokenRegistrar.registerCurrentToken()
                    }
                }
        }
        viewModelScope.launch {
            val tuntasId = tokenManager.activeTuntasId.first() ?: return@launch
            userRepository.getMyPermissions(tuntasId)
                .onSuccess {
                    tokenManager.savePermissions(it.permissions)
                    tokenManager.saveLeadershipUnitIds(it.leadershipUnitIds)
                    tokenManager.cacheTuntasContext(tuntasId, it.permissions, it.leadershipUnitIds)
                }
        }
    }

    override fun onCleared() {
        pendingSyncScheduler.stopWatchingNetwork()
        liveEventService.stop()
        super.onCleared()
    }

    fun logout(onComplete: () -> Unit) {
        fcmTokenRegistrar.unregisterCurrentToken {
            viewModelScope.launch {
                authRepository.logout()
                onComplete()
            }
        }
    }
}
