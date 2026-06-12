package lt.skautai.android.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import lt.skautai.android.data.remote.NotificationApiService
import lt.skautai.android.data.remote.NotificationListDto
import lt.skautai.android.util.SESSION_EXPIRED_MESSAGE
import lt.skautai.android.util.TokenManager
import lt.skautai.android.util.errorMessage
import lt.skautai.android.util.userFacingException

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationApiService: NotificationApiService,
    private val tokenManager: TokenManager
) {
    private suspend fun token() = tokenManager.token.first()
        ?: throw Exception(SESSION_EXPIRED_MESSAGE)

    suspend fun getNotifications(unreadOnly: Boolean = false): Result<NotificationListDto> = runCatching {
        val response = notificationApiService.getNotifications(
            token = "Bearer ${token()}",
            unreadOnly = unreadOnly
        )
        if (!response.isSuccessful) {
            throw Exception(response.errorMessage("Nepavyko gauti pranešimų"))
        }
        response.body() ?: NotificationListDto(emptyList(), total = 0, unreadCount = 0)
    }.recoverCatching { throw it.userFacingException() }

    suspend fun markRead(id: String): Result<String> = runCatching {
        val response = notificationApiService.markRead("Bearer ${token()}", id)
        if (!response.isSuccessful) {
            throw Exception(response.errorMessage("Nepavyko pažymėti pranešimo"))
        }
        response.body()?.message ?: "Pranešimas pažymėtas kaip skaitytas"
    }.recoverCatching { throw it.userFacingException() }

    suspend fun markAllRead(): Result<String> = runCatching {
        val response = notificationApiService.markAllRead("Bearer ${token()}")
        if (!response.isSuccessful) {
            throw Exception(response.errorMessage("Nepavyko pažymėti pranešimų"))
        }
        response.body()?.message ?: "Pranešimai pažymėti kaip skaityti"
    }.recoverCatching { throw it.userFacingException() }
}
