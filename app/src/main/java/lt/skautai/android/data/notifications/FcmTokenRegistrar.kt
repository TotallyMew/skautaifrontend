package lt.skautai.android.data.notifications

import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import lt.skautai.android.data.repository.DeviceRepository

@Singleton
class FcmTokenRegistrar @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun registerCurrentToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener
            val token = task.result?.takeIf(String::isNotBlank) ?: return@addOnCompleteListener
            registerToken(token)
        }
    }

    fun registerToken(token: String) {
        scope.launch {
            deviceRepository.registerDevice(token)
        }
    }

    fun unregisterCurrentToken(onComplete: () -> Unit = {}) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                onComplete()
                return@addOnCompleteListener
            }
            val token = task.result?.takeIf(String::isNotBlank)
            if (token == null) {
                onComplete()
                return@addOnCompleteListener
            }
            scope.launch {
                deviceRepository.unregisterDevice(token)
                onComplete()
            }
        }
    }
}
