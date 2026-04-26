package lt.skautai.android.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import java.io.IOException
import java.time.Instant
import java.util.UUID
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
import lt.skautai.android.data.sync.PendingEntityType
import lt.skautai.android.data.sync.PendingOperationRepository
import lt.skautai.android.data.sync.PendingOperationType
import lt.skautai.android.util.TokenManager
import lt.skautai.android.util.errorMessage

@Singleton
class ItemRepository @Inject constructor(
    private val itemApiService: ItemApiService,
    private val tokenManager: TokenManager,
    private val itemDao: ItemDao,
    private val pendingOperationRepository: PendingOperationRepository
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
                Result.failure(Exception(response.errorMessage("Klaida gaunant inventoriu")))
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
                if (response.code() == 404) {
                    itemDao.deleteItem(itemId, tuntasId)
                }
                Result.failure(Exception(response.errorMessage("Klaida gaunant daikta")))
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
                Result.failure(Exception(response.errorMessage("Klaida trinant daikta")))
            }
        } catch (e: IOException) {
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            if (itemId.startsWith("local-") && pendingOperationRepository.hasCreateOperationInFlight(
                    entityType = PendingEntityType.ITEM,
                    entityId = itemId,
                    createOperationType = PendingOperationType.ITEM_CREATE
                )
            ) {
                return Result.failure(Exception("Daiktas dabar sinchronizuojamas. Pabandykite dar kartą vėliau."))
            }
            if (itemId.startsWith("local-") && pendingOperationRepository.deletePendingCreateIfExists(
                    entityType = PendingEntityType.ITEM,
                    entityId = itemId,
                    createOperationType = PendingOperationType.ITEM_CREATE
                )
            ) {
                itemDao.deleteItem(itemId, tuntasId)
                return Result.success(Unit)
            }
            itemDao.deleteItem(itemId, tuntasId)
            pendingOperationRepository.enqueue(
                tuntasId = tuntasId,
                entityType = PendingEntityType.ITEM,
                entityId = itemId,
                operationType = PendingOperationType.ITEM_DELETE,
                payload = mapOf("id" to itemId)
            )
            Result.success(Unit)
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
                Result.failure(Exception(response.errorMessage("Klaida kuriant daikta")))
            }
        } catch (e: IOException) {
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val now = Instant.now().toString()
            val localItem = ItemDto(
                id = "local-${UUID.randomUUID()}",
                tuntasId = tuntasId,
                custodianId = request.custodianId,
                custodianName = null,
                origin = request.origin,
                name = request.name,
                description = request.description,
                type = request.type,
                category = request.category,
                condition = request.condition,
                quantity = request.quantity,
                locationId = request.locationId,
                locationName = null,
                locationPath = null,
                temporaryStorageLabel = request.temporaryStorageLabel,
                sourceSharedItemId = request.sourceSharedItemId,
                totalQuantityAcrossCustodians = request.quantity,
                responsibleUserId = request.responsibleUserId,
                photoUrl = request.photoUrl,
                purchaseDate = request.purchaseDate,
                purchasePrice = request.purchasePrice,
                notes = request.notes,
                status = "ACTIVE",
                createdAt = now,
                updatedAt = now
            )
            itemDao.upsert(localItem.toEntity())
            pendingOperationRepository.enqueue(
                tuntasId = tuntasId,
                entityType = PendingEntityType.ITEM,
                entityId = localItem.id,
                operationType = PendingOperationType.ITEM_CREATE,
                payload = request
            )
            Result.success(localItem)
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
                Result.failure(Exception(response.errorMessage("Klaida atnaujinant daikta")))
            }
        } catch (e: IOException) {
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val cached = itemDao.getItem(itemId, tuntasId)?.toDto()
                ?: return Result.failure(Exception("Daiktas nerastas offline cache"))
            val updated = cached.copy(
                name = request.name ?: cached.name,
                description = request.description ?: cached.description,
                type = request.type ?: cached.type,
                category = request.category ?: cached.category,
                condition = request.condition ?: cached.condition,
                quantity = request.quantity ?: cached.quantity,
                custodianId = request.custodianId ?: cached.custodianId,
                locationId = request.locationId ?: cached.locationId,
                temporaryStorageLabel = request.temporaryStorageLabel ?: cached.temporaryStorageLabel,
                sourceSharedItemId = request.sourceSharedItemId ?: cached.sourceSharedItemId,
                responsibleUserId = request.responsibleUserId ?: cached.responsibleUserId,
                photoUrl = request.photoUrl ?: cached.photoUrl,
                purchaseDate = request.purchaseDate ?: cached.purchaseDate,
                purchasePrice = request.purchasePrice ?: cached.purchasePrice,
                notes = request.notes ?: cached.notes,
                status = request.status ?: cached.status,
                updatedAt = Instant.now().toString()
            )
            itemDao.upsert(updated.toEntity())
            val mergedIntoCreate = if (itemId.startsWith("local-")) {
                pendingOperationRepository.replaceCreatePayloadIfPending(
                    entityType = PendingEntityType.ITEM,
                    entityId = itemId,
                    createOperationType = PendingOperationType.ITEM_CREATE,
                    payload = updated.toCreateRequest()
                )
            } else {
                false
            }
            if (!mergedIntoCreate) {
                pendingOperationRepository.enqueue(
                    tuntasId = tuntasId,
                    entityType = PendingEntityType.ITEM,
                    entityId = itemId,
                    operationType = PendingOperationType.ITEM_UPDATE,
                    payload = request
                )
            }
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun ItemDto.toCreateRequest(): CreateItemRequestDto = CreateItemRequestDto(
        name = name,
        description = description,
        type = type,
        category = category,
        custodianId = custodianId,
        origin = origin,
        quantity = quantity,
        condition = condition,
        locationId = locationId,
        temporaryStorageLabel = temporaryStorageLabel,
        sourceSharedItemId = sourceSharedItemId,
        responsibleUserId = responsibleUserId,
        photoUrl = photoUrl,
        purchaseDate = purchaseDate,
        purchasePrice = purchasePrice,
        notes = notes
    )
}
