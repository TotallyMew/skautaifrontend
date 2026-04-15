package lt.skautai.android.util

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val tokenManager: TokenManager
) {
    val token: Flow<String?> = tokenManager.token
    val userId: Flow<String?> = tokenManager.userId
    val userName: Flow<String?> = tokenManager.userName
    val userEmail: Flow<String?> = tokenManager.userEmail
    val userType: Flow<String?> = tokenManager.userType
    val activeTuntasId: Flow<String?> = tokenManager.activeTuntasId

    suspend fun saveSession(
        token: String,
        userId: String,
        name: String,
        email: String,
        type: String
    ) {
        tokenManager.saveToken(token, userId, name, email, type)
    }

    suspend fun setActiveTuntas(tuntasId: String) {
        tokenManager.setActiveTuntas(tuntasId)
    }

    suspend fun clearSession() {
        tokenManager.clearAll()
    }
}