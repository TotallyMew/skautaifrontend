package lt.skautai.android.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import lt.skautai.android.data.remote.MyTaskApiService
import lt.skautai.android.data.remote.MyTaskListDto
import lt.skautai.android.util.SESSION_EXPIRED_MESSAGE
import lt.skautai.android.util.TUNTAS_SELECTION_REQUIRED_MESSAGE
import lt.skautai.android.util.TokenManager
import lt.skautai.android.util.errorMessage
import lt.skautai.android.util.userFacingException

@Singleton
class MyTaskRepository @Inject constructor(
    private val myTaskApiService: MyTaskApiService,
    private val tokenManager: TokenManager
) {
    private suspend fun token() = tokenManager.token.first()
        ?: throw Exception(SESSION_EXPIRED_MESSAGE)

    private suspend fun tuntasId() = tokenManager.activeTuntasId.first()
        ?: throw Exception(TUNTAS_SELECTION_REQUIRED_MESSAGE)

    suspend fun getMyTasks(): Result<MyTaskListDto> {
        return try {
            val response = myTaskApiService.getMyTasks(
                token = "Bearer ${token()}",
                tuntasId = tuntasId()
            )
            if (response.isSuccessful) {
                Result.success(response.body() ?: MyTaskListDto(tasks = emptyList(), total = 0))
            } else {
                Result.failure(Exception(response.errorMessage("Nepavyko gauti mano užduočių.")))
            }
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }
}
