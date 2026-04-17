package lt.skautai.android.data.repository

import lt.skautai.android.data.remote.ItemApiService
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.util.TokenManager
import kotlinx.coroutines.flow.first
import lt.skautai.android.data.remote.CreateItemRequestDto
import lt.skautai.android.data.remote.UpdateItemRequestDto
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
        status: String? = "ACTIVE"
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
    suspend fun getItem(itemId: String): Result<ItemDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = itemApiService.getItem(
                token = "Bearer $token",
                tuntasId = tuntasId,
                itemId = itemId
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant daiktą"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteItem(itemId: String): Result<Unit> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = itemApiService.deleteItem(
                token = "Bearer $token",
                tuntasId = tuntasId,
                itemId = itemId
            )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida trinant daiktą"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun createItem(request: CreateItemRequestDto): Result<ItemDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = itemApiService.createItem(
                token = "Bearer $token",
                tuntasId = tuntasId,
                request = request
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida kuriant daiktą"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateItem(itemId: String, request: UpdateItemRequestDto): Result<ItemDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = itemApiService.updateItem(
                token = "Bearer $token",
                tuntasId = tuntasId,
                itemId = itemId,
                request = request
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida atnaujinant daiktą"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}