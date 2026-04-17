package lt.skautai.android.data.repository

import lt.skautai.android.data.remote.ItemApiService
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.util.TokenManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepository @Inject constructor(
    private val itemApiService: ItemApiService,
    private val tokenManager: TokenManager
) {

    suspend fun getItems(
        ownerType: String? = null,
        category: String? = null,
        status: String? = null
    ): Result<List<ItemDto>> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = itemApiService.getItems(
                token = "Bearer $token",
                tuntasId = tuntasId,
                ownerType = ownerType,
                category = category,
                status = status
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!.items)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant inventorių"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}