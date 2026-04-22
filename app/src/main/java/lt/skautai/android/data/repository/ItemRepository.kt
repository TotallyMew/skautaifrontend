package lt.skautai.android.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import lt.skautai.android.data.local.dao.ItemDao
import lt.skautai.android.data.local.mapper.toDto
import lt.skautai.android.data.local.mapper.toEntity
import lt.skautai.android.data.local.mapper.toItemDtos
import lt.skautai.android.data.local.mapper.toItemEntities
import lt.skautai.android.data.remote.CreateItemRequestDto
import lt.skautai.android.data.remote.ItemApiService
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.UpdateItemRequestDto
import lt.skautai.android.util.TokenManager

@Singleton
class ItemRepository @Inject constructor(
    private val itemApiService: ItemApiService,
    private val tokenManager: TokenManager,
    private val itemDao: ItemDao
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeItems(
        custodianId: String? = null,
        status: String? = "ACTIVE",
        type: String? = null,
        category: String? = null
    ): Flow<List<ItemDto>> {
        return tokenManager.activeTuntasId.flatMapLatest { tuntasId ->
            if (tuntasId == null) {
                flowOf(emptyList())
            } else {
                itemDao.observeItems(tuntasId, custodianId, status, type, category)
                    .map { it.toItemDtos() }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeItem(itemId: String): Flow<ItemDto?> {
        return tokenManager.activeTuntasId.flatMapLatest { tuntasId ->
            if (tuntasId == null) {
                flowOf(null)
            } else {
                itemDao.observeItem(itemId, tuntasId).map { it?.toDto() }
            }
        }
    }

    suspend fun refreshItems(
        custodianId: String? = null,
        status: String? = "ACTIVE",
        type: String? = null,
        category: String? = null
    ): Result<Unit> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = itemApiService.getItems(
                token = "Bearer $token",
                tuntasId = tuntasId,
                custodianId = custodianId,
                type = type,
                category = category,
                status = status
            )
            if (response.isSuccessful) {
                val items = response.body()?.items.orEmpty()
                itemDao.deleteForQuery(tuntasId, custodianId, status, type, category)
                itemDao.upsertAll(items.toItemEntities())
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant inventoriu"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshItem(itemId: String): Result<Unit> {
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
                itemDao.upsert(response.body()!!.toEntity())
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida gaunant daikta"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getItems(
        custodianId: String? = null,
        type: String? = null,
        category: String? = null,
        status: String? = "ACTIVE"
    ): Result<List<ItemDto>> {
        val refreshResult = refreshItems(custodianId, status, type, category)
        val tuntasId = tokenManager.activeTuntasId.first()
        val cachedItems = tuntasId
            ?.let { itemDao.getItems(it, custodianId, status, type, category).toItemDtos() }
            .orEmpty()
        return if (refreshResult.isSuccess || cachedItems.isNotEmpty()) {
            Result.success(cachedItems)
        } else {
            Result.failure(refreshResult.exceptionOrNull() ?: Exception("Klaida gaunant inventoriu"))
        }
    }

    suspend fun getItem(itemId: String): Result<ItemDto> {
        val refreshResult = refreshItem(itemId)
        val tuntasId = tokenManager.activeTuntasId.first()
        val cachedItem = tuntasId?.let { itemDao.getItem(itemId, it)?.toDto() }
        return if (cachedItem != null) {
            Result.success(cachedItem)
        } else {
            Result.failure(refreshResult.exceptionOrNull() ?: Exception("Klaida gaunant daikta"))
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
                itemDao.deleteItem(itemId, tuntasId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida trinant daikta"))
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
                val item = response.body()!!
                itemDao.upsert(item.toEntity())
                Result.success(item)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida kuriant daikta"))
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
                val item = response.body()!!
                itemDao.upsert(item.toEntity())
                Result.success(item)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Klaida atnaujinant daikta"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
