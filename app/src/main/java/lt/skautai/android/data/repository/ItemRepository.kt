package lt.skautai.android.data.repository

import lt.skautai.android.util.userFacingException

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
import lt.skautai.android.data.remote.CreateStorageAuditSessionRequestDto
import lt.skautai.android.data.remote.ConsumeItemRequestDto
import lt.skautai.android.data.remote.ItemApiService
import lt.skautai.android.data.remote.ItemAssignmentDto
import lt.skautai.android.data.remote.StorageAuditSessionDto
import lt.skautai.android.data.remote.ItemConditionLogDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.ItemHistoryDto
import lt.skautai.android.data.remote.ItemTransferDto
import lt.skautai.android.data.remote.ReturnItemToSharedRequestDto
import lt.skautai.android.data.remote.RestockItemRequestDto
import lt.skautai.android.data.remote.ReviewItemAdditionRequestDto
import lt.skautai.android.data.remote.TransferItemToUnitRequestDto
import lt.skautai.android.data.remote.UpsertStorageAuditCheckRequestDto
import lt.skautai.android.data.remote.UpsertStorageAuditChecksRequestDto
import lt.skautai.android.data.remote.UpdateItemRequestDto
import lt.skautai.android.data.remote.WriteOffItemRequestDto
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
    private val pendingOperationRepository: PendingOperationRepository,
    private val refreshCoordinator: RefreshCoordinator
) {
    data class ItemPage(
        val items: List<ItemDto>,
        val total: Int,
        val limit: Int,
        val offset: Int,
        val hasMore: Boolean
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeItems(
        custodianId: String? = null,
        status: String? = "ACTIVE",
        type: String? = null,
        category: String? = null,
        sharedOnly: Boolean = false,
        createdByUserId: String? = null
    ): Flow<List<ItemDto>> {
        return tokenManager.activeTuntasId.flatMapLatest { tuntasId ->
            if (tuntasId == null) {
                flowOf(emptyList())
            } else {
                itemDao.observeItems(tuntasId, custodianId, sharedOnly, createdByUserId, status, type, category)
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
        category: String? = null,
        sharedOnly: Boolean = false,
        createdByUserId: String? = null
    ): Result<Unit> {
        val queryKey = itemQueryKey(custodianId, status, type, category, sharedOnly, createdByUserId)
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val hasCachedRows = itemDao.getItems(tuntasId, custodianId, sharedOnly, createdByUserId, status, type, category).isNotEmpty()
            val updatedAfter = if (hasCachedRows) {
                refreshCoordinator.lastSuccessfulRefreshInstant(ITEMS_RESOURCE, queryKey)
            } else {
                null
            }
            val items = if (updatedAfter == null) {
                val allItems = mutableListOf<ItemDto>()
                var offset = 0
                var total = Int.MAX_VALUE
                while (offset < total) {
                    val response = itemApiService.getItems(
                        token = "Bearer $token",
                        tuntasId = tuntasId,
                        custodianId = custodianId,
                        type = type,
                        category = category,
                        status = status,
                        sharedOnly = sharedOnly,
                        createdByUserId = createdByUserId,
                        updatedAfter = null,
                        searchQuery = null,
                        limit = ITEM_REFRESH_PAGE_SIZE,
                        offset = offset
                    )
                    if (!response.isSuccessful) {
                        val error = response.errorMessage("Klaida gaunant inventoriu")
                        refreshCoordinator.recordAttempt(ITEMS_RESOURCE, queryKey, success = false, error = error)
                        return Result.failure(Exception(error))
                    }
                    val page = response.body()
                    val pageItems = page?.items.orEmpty().map { it.withSafeCollections() }
                    allItems += pageItems
                    total = page?.total ?: allItems.size
                    offset += pageItems.size
                    if (pageItems.isEmpty() || page?.hasMore == false) break
                }
                allItems
            } else {
                val response = itemApiService.getItems(
                    token = "Bearer $token",
                    tuntasId = tuntasId,
                    custodianId = custodianId,
                    type = type,
                    category = category,
                    status = status,
                    sharedOnly = sharedOnly,
                    createdByUserId = createdByUserId,
                    updatedAfter = updatedAfter,
                    searchQuery = null
                )
                if (!response.isSuccessful) {
                    val error = response.errorMessage("Klaida gaunant inventoriu")
                    refreshCoordinator.recordAttempt(ITEMS_RESOURCE, queryKey, success = false, error = error)
                    return Result.failure(Exception(error))
                }
                response.body()?.items.orEmpty().map { it.withSafeCollections() }
            }
            if (updatedAfter == null) {
                itemDao.deleteForQuery(tuntasId, custodianId, sharedOnly, createdByUserId, status, type, category)
            }
            itemDao.upsertAll(items.toItemEntities())
            refreshCoordinator.recordAttempt(ITEMS_RESOURCE, queryKey, success = true)
            Result.success(Unit)
        } catch (e: Exception) {
            refreshCoordinator.recordAttempt(ITEMS_RESOURCE, queryKey, success = false, error = e.message)
            Result.failure(e.userFacingException())
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
                itemDao.upsert(response.body()!!.withSafeCollections().toEntity())
                refreshCoordinator.recordAttempt(ITEM_RESOURCE, itemId, success = true)
                Result.success(Unit)
            } else {
                if (response.code() == 404) {
                    itemDao.deleteItem(itemId, tuntasId)
                }
                val error = response.errorMessage("Klaida gaunant daikta")
                refreshCoordinator.recordAttempt(ITEM_RESOURCE, itemId, success = false, error = error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            refreshCoordinator.recordAttempt(ITEM_RESOURCE, itemId, success = false, error = e.message)
            Result.failure(e.userFacingException())
        }
    }

    suspend fun getItems(
        custodianId: String? = null,
        type: String? = null,
        category: String? = null,
        status: String? = "ACTIVE",
        sharedOnly: Boolean = false,
        createdByUserId: String? = null,
        forceRefresh: Boolean = false
    ): Result<List<ItemDto>> {
        val queryKey = itemQueryKey(custodianId, status, type, category, sharedOnly, createdByUserId)
        val shouldRefresh = refreshCoordinator.shouldRefresh(
            resource = ITEMS_RESOURCE,
            queryKey = queryKey,
            ttl = CacheTtl.LIST,
            force = forceRefresh
        )
        val refreshResult = if (shouldRefresh) {
            refreshItems(custodianId, status, type, category, sharedOnly, createdByUserId)
        } else {
            Result.success(Unit)
        }
        val tuntasId = tokenManager.activeTuntasId.first()
        val cachedItems = tuntasId
            ?.let { itemDao.getItems(it, custodianId, sharedOnly, createdByUserId, status, type, category).toItemDtos() }
            .orEmpty()
        return if (refreshResult.isSuccess || cachedItems.isNotEmpty()) {
            Result.success(cachedItems)
        } else {
            Result.failure(refreshResult.exceptionOrNull() ?: Exception("Klaida gaunant inventoriu"))
        }
    }

    suspend fun getCachedItems(
        custodianId: String? = null,
        type: String? = null,
        category: String? = null,
        status: String? = "ACTIVE",
        sharedOnly: Boolean = false,
        createdByUserId: String? = null
    ): List<ItemDto> {
        val tuntasId = tokenManager.activeTuntasId.first() ?: return emptyList()
        return itemDao.getItems(tuntasId, custodianId, sharedOnly, createdByUserId, status, type, category).toItemDtos()
    }

    suspend fun getFreshItems(
        custodianId: String? = null,
        type: String? = null,
        category: String? = null,
        status: String? = "ACTIVE",
        sharedOnly: Boolean = false,
        createdByUserId: String? = null
    ): Result<List<ItemDto>> = getItems(custodianId, type, category, status, sharedOnly, createdByUserId, forceRefresh = true)

    suspend fun getItem(itemId: String): Result<ItemDto> {
        val shouldRefresh = refreshCoordinator.shouldRefresh(
            resource = ITEM_RESOURCE,
            queryKey = itemId,
            ttl = CacheTtl.DETAIL
        )
        val refreshResult = if (shouldRefresh) refreshItem(itemId) else Result.success(Unit)
        val tuntasId = tokenManager.activeTuntasId.first()
        val cachedItem = tuntasId?.let { itemDao.getItem(itemId, it)?.toDto() }
        return if (cachedItem != null) {
            Result.success(cachedItem)
        } else {
            Result.failure(refreshResult.exceptionOrNull() ?: Exception("Klaida gaunant daikta"))
        }
    }

    suspend fun resolveQrToken(qrToken: String): Result<String> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = itemApiService.resolveQrToken(
                token = "Bearer $token",
                tuntasId = tuntasId,
                tokenValue = qrToken
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!.itemId)
            } else {
                Result.failure(Exception(response.errorMessage("Nepavyko atpazinti QR kodo")))
            }
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    suspend fun createStorageAuditSession(
        custodianId: String? = null,
        type: String? = null,
        category: String? = null,
        sharedOnly: Boolean = false,
        personalOwnerUserId: String? = null
    ): Result<StorageAuditSessionDto> = try {
        val token = tokenManager.token.first()
            ?: return Result.failure(Exception("Nav prisijungta"))
        val tuntasId = tokenManager.activeTuntasId.first()
            ?: return Result.failure(Exception("Tuntas nepasirinktas"))
        val response = itemApiService.createStorageAuditSession(
            token = "Bearer $token",
            tuntasId = tuntasId,
            request = CreateStorageAuditSessionRequestDto(
                custodianId = custodianId,
                type = type,
                category = category,
                sharedOnly = sharedOnly,
                personalOwnerUserId = personalOwnerUserId
            )
        )
        if (response.isSuccessful) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception(response.errorMessage("Nepavyko sukurti inventorizacijos sesijos")))
        }
    } catch (e: Exception) {
        Result.failure(e.userFacingException())
    }

    suspend fun listStorageAuditSessions(
        status: String? = null
    ): Result<List<StorageAuditSessionDto>> = try {
        val token = tokenManager.token.first()
            ?: return Result.failure(Exception("Nav prisijungta"))
        val tuntasId = tokenManager.activeTuntasId.first()
            ?: return Result.failure(Exception("Tuntas nepasirinktas"))
        val response = itemApiService.listStorageAuditSessions(
            token = "Bearer $token",
            tuntasId = tuntasId,
            status = status
        )
        if (response.isSuccessful) {
            Result.success(response.body()?.sessions.orEmpty())
        } else {
            Result.failure(Exception(response.errorMessage("Nepavyko gauti inventorizaciju istorijos")))
        }
    } catch (e: Exception) {
        Result.failure(e.userFacingException())
    }

    suspend fun getStorageAuditSession(
        sessionId: String
    ): Result<StorageAuditSessionDto> = try {
        val token = tokenManager.token.first()
            ?: return Result.failure(Exception("Nav prisijungta"))
        val tuntasId = tokenManager.activeTuntasId.first()
            ?: return Result.failure(Exception("Tuntas nepasirinktas"))
        val response = itemApiService.getStorageAuditSession(
            token = "Bearer $token",
            tuntasId = tuntasId,
            sessionId = sessionId
        )
        if (response.isSuccessful) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception(response.errorMessage("Nepavyko gauti inventorizacijos sesijos")))
        }
    } catch (e: Exception) {
        Result.failure(e.userFacingException())
    }

    suspend fun upsertStorageAuditChecks(
        sessionId: String,
        checks: List<UpsertStorageAuditCheckRequestDto>
    ): Result<StorageAuditSessionDto> = try {
        val token = tokenManager.token.first()
            ?: return Result.failure(Exception("Nav prisijungta"))
        val tuntasId = tokenManager.activeTuntasId.first()
            ?: return Result.failure(Exception("Tuntas nepasirinktas"))
        val response = itemApiService.upsertStorageAuditChecks(
            token = "Bearer $token",
            tuntasId = tuntasId,
            sessionId = sessionId,
            request = UpsertStorageAuditChecksRequestDto(checks)
        )
        if (response.isSuccessful) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception(response.errorMessage("Nepavyko išsaugoti inventorizacijos rezultatų")))
        }
    } catch (e: Exception) {
        Result.failure(e.userFacingException())
    }

    suspend fun completeStorageAuditSession(sessionId: String): Result<StorageAuditSessionDto> = try {
        val token = tokenManager.token.first()
            ?: return Result.failure(Exception("Nav prisijungta"))
        val tuntasId = tokenManager.activeTuntasId.first()
            ?: return Result.failure(Exception("Tuntas nepasirinktas"))
        val response = itemApiService.completeStorageAuditSession(
            token = "Bearer $token",
            tuntasId = tuntasId,
            sessionId = sessionId
        )
        if (response.isSuccessful) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception(response.errorMessage("Nepavyko uzbaigti inventorizacijos")))
        }
    } catch (e: Exception) {
        Result.failure(e.userFacingException())
    }

    suspend fun getItemAssignments(itemId: String): Result<List<ItemAssignmentDto>> = try {
        val token = tokenManager.token.first()
            ?: return Result.failure(Exception("Nav prisijungta"))
        val tuntasId = tokenManager.activeTuntasId.first()
            ?: return Result.failure(Exception("Tuntas nepasirinktas"))
        val response = itemApiService.getItemAssignments("Bearer $token", tuntasId, itemId)
        if (response.isSuccessful) {
            Result.success(response.body()?.assignments.orEmpty())
        } else {
            Result.failure(Exception(response.errorMessage("Klaida gaunant priskyrimo istorija")))
        }
    } catch (e: Exception) {
        Result.failure(e.userFacingException())
    }

    suspend fun getItemConditionLog(itemId: String): Result<List<ItemConditionLogDto>> = try {
        val token = tokenManager.token.first()
            ?: return Result.failure(Exception("Nav prisijungta"))
        val tuntasId = tokenManager.activeTuntasId.first()
            ?: return Result.failure(Exception("Tuntas nepasirinktas"))
        val response = itemApiService.getItemConditionLog("Bearer $token", tuntasId, itemId)
        if (response.isSuccessful) {
            Result.success(response.body()?.entries.orEmpty())
        } else {
            Result.failure(Exception(response.errorMessage("Klaida gaunant bukles istorija")))
        }
    } catch (e: Exception) {
        Result.failure(e.userFacingException())
    }

    suspend fun getItemTransfers(itemId: String): Result<List<ItemTransferDto>> = try {
        val token = tokenManager.token.first()
            ?: return Result.failure(Exception("Nav prisijungta"))
        val tuntasId = tokenManager.activeTuntasId.first()
            ?: return Result.failure(Exception("Tuntas nepasirinktas"))
        val response = itemApiService.getItemTransfers("Bearer $token", tuntasId, itemId)
        if (response.isSuccessful) {
            Result.success(response.body()?.transfers.orEmpty())
        } else {
            Result.failure(Exception(response.errorMessage("Klaida gaunant judejimo istorija")))
        }
    } catch (e: Exception) {
        Result.failure(e.userFacingException())
    }

    suspend fun getItemHistory(itemId: String): Result<List<ItemHistoryDto>> = try {
        val token = tokenManager.token.first()
            ?: return Result.failure(Exception("Nav prisijungta"))
        val tuntasId = tokenManager.activeTuntasId.first()
            ?: return Result.failure(Exception("Tuntas nepasirinktas"))
        val response = itemApiService.getItemHistory("Bearer $token", tuntasId, itemId)
        if (response.isSuccessful) {
            Result.success(response.body()?.entries.orEmpty())
        } else {
            Result.failure(Exception(response.errorMessage("Klaida gaunant daikto istorija")))
        }
    } catch (e: Exception) {
        Result.failure(e.userFacingException())
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
            Result.failure(e.userFacingException())
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
                val item = response.body()!!.withSafeCollections()
                itemDao.upsert(item.toEntity())
                Result.success(item)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida kuriant daikta")))
            }
        } catch (e: IOException) {
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val now = Instant.now().toString()
            val currentUserId = tokenManager.userId.first()
            val currentUserName = tokenManager.userName.first()
            val localItem = ItemDto(
                id = "local-${UUID.randomUUID()}",
                qrToken = UUID.randomUUID().toString(),
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
                isConsumable = request.isConsumable,
                unitOfMeasure = request.unitOfMeasure,
                minimumQuantity = request.minimumQuantity,
                isLowStock = request.isConsumable && request.minimumQuantity?.let { request.quantity <= it } == true,
                locationId = request.locationId,
                locationName = null,
                locationPath = null,
                temporaryStorageLabel = request.temporaryStorageLabel,
                sourceSharedItemId = request.sourceSharedItemId,
                totalQuantityAcrossCustodians = request.quantity,
                responsibleUserId = request.responsibleUserId,
                responsibleUserName = null,
                createdByUserId = currentUserId,
                createdByUserName = currentUserName,
                photoUrl = request.photoUrl,
                purchaseDate = request.purchaseDate,
                purchasePrice = request.purchasePrice,
                notes = request.notes,
                customFields = request.customFields,
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
            Result.failure(e.userFacingException())
        }
    }

    suspend fun findDuplicateCandidate(
        name: String,
        type: String,
        category: String,
        custodianId: String?
    ): Result<ItemDto?> {
        return getItems(
            custodianId = custodianId,
            type = type,
            category = category,
            status = "ACTIVE"
        ).map { items ->
            items
                .filter { it.name.trim().equals(name.trim(), ignoreCase = true) }
                .maxByOrNull { it.updatedAt }
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
                val item = response.body()!!.withSafeCollections()
                itemDao.upsert(item.toEntity())
                Result.success(item)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida atnaujinant daikta")))
            }
        } catch (e: IOException) {
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val cached = itemDao.getItem(itemId, tuntasId)?.toDto()
                ?: return Result.failure(Exception("Daiktas nerastas vietinėje saugykloje"))
            val updated = cached.copy(
                name = request.name ?: cached.name,
                description = request.description ?: cached.description,
                type = request.type ?: cached.type,
                category = request.category ?: cached.category,
                condition = request.condition ?: cached.condition,
                quantity = request.quantity ?: cached.quantity,
                isConsumable = request.isConsumable ?: cached.isConsumable,
                unitOfMeasure = request.unitOfMeasure ?: cached.unitOfMeasure,
                minimumQuantity = if (request.clearMinimumQuantity) null else request.minimumQuantity ?: cached.minimumQuantity,
                isLowStock = (request.isConsumable ?: cached.isConsumable) &&
                    (if (request.clearMinimumQuantity) null else request.minimumQuantity ?: cached.minimumQuantity)
                        ?.let { (request.quantity ?: cached.quantity) <= it } == true,
                custodianId = request.custodianId ?: cached.custodianId,
                locationId = request.locationId ?: cached.locationId,
                temporaryStorageLabel = request.temporaryStorageLabel ?: cached.temporaryStorageLabel,
                sourceSharedItemId = request.sourceSharedItemId ?: cached.sourceSharedItemId,
                responsibleUserId = request.responsibleUserId ?: cached.responsibleUserId,
                photoUrl = request.photoUrl ?: cached.photoUrl,
                purchaseDate = request.purchaseDate ?: cached.purchaseDate,
                purchasePrice = request.purchasePrice ?: cached.purchasePrice,
                notes = request.notes ?: cached.notes,
                customFields = request.customFields ?: cached.customFields.orEmpty(),
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
            Result.failure(e.userFacingException())
        }
    }

    suspend fun getCachedItem(itemId: String): ItemDto? {
        val tuntasId = tokenManager.activeTuntasId.first() ?: return null
        return itemDao.getItem(itemId, tuntasId)?.toDto()
    }

    suspend fun getFreshItem(itemId: String): Result<ItemDto> {
        refreshItem(itemId)
        return getItem(itemId)
    }

    suspend fun writeOffItem(itemId: String, reason: String): Result<ItemDto> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val response = itemApiService.writeOffItem(
                token = "Bearer $token",
                tuntasId = tuntasId,
                itemId = itemId,
                request = WriteOffItemRequestDto(reason = reason)
            )
            if (response.isSuccessful) {
                val item = response.body()!!.withSafeCollections()
                itemDao.upsert(item.toEntity())
                Result.success(item)
            } else {
                Result.failure(Exception(response.errorMessage("Klaida nurašant daiktą")))
            }
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    suspend fun transferItemToUnit(
        itemId: String,
        request: TransferItemToUnitRequestDto
    ): Result<ItemDto> = try {
        val token = tokenManager.token.first()
            ?: return Result.failure(Exception("Nav prisijungta"))
        val tuntasId = tokenManager.activeTuntasId.first()
            ?: return Result.failure(Exception("Tuntas nepasirinktas"))
        val response = itemApiService.transferItemToUnit(
            token = "Bearer $token",
            tuntasId = tuntasId,
            itemId = itemId,
            request = request
        )
        if (response.isSuccessful) {
            val item = response.body()!!.withSafeCollections()
            itemDao.upsert(item.toEntity())
            refreshItem(itemId)
            Result.success(item)
        } else {
            Result.failure(Exception(response.errorMessage("Klaida perduodant daikta vienetui")))
        }
    } catch (e: Exception) {
        Result.failure(e.userFacingException())
    }

    suspend fun returnItemToShared(
        itemId: String,
        request: ReturnItemToSharedRequestDto
    ): Result<ItemDto> = try {
        val token = tokenManager.token.first()
            ?: return Result.failure(Exception("Nav prisijungta"))
        val tuntasId = tokenManager.activeTuntasId.first()
            ?: return Result.failure(Exception("Tuntas nepasirinktas"))
        val response = itemApiService.returnItemToShared(
            token = "Bearer $token",
            tuntasId = tuntasId,
            itemId = itemId,
            request = request
        )
        if (response.isSuccessful) {
            val item = response.body()!!.withSafeCollections()
            itemDao.upsert(item.toEntity())
            refreshItem(itemId)
            Result.success(item)
        } else {
            Result.failure(Exception(response.errorMessage("Klaida grazinant daikta i bendra inventoriu")))
        }
    } catch (e: Exception) {
        Result.failure(e.userFacingException())
    }

    suspend fun restockItem(
        itemId: String,
        request: RestockItemRequestDto
    ): Result<ItemDto> = try {
        val token = tokenManager.token.first()
            ?: return Result.failure(Exception("Nav prisijungta"))
        val tuntasId = tokenManager.activeTuntasId.first()
            ?: return Result.failure(Exception("Tuntas nepasirinktas"))
        val response = itemApiService.restockItem(
            token = "Bearer $token",
            tuntasId = tuntasId,
            itemId = itemId,
            request = request
        )
        if (response.isSuccessful) {
            val item = response.body()!!.withSafeCollections()
            itemDao.upsert(item.toEntity())
            refreshItem(itemId)
            Result.success(item)
        } else {
            Result.failure(Exception(response.errorMessage("Klaida papildant daikta")))
        }
    } catch (e: Exception) {
        Result.failure(e.userFacingException())
    }

    suspend fun refreshItemsPage(
        custodianId: String? = null,
        status: String? = "ACTIVE",
        type: String? = null,
        category: String? = null,
        sharedOnly: Boolean = false,
        createdByUserId: String? = null,
        searchQuery: String? = null,
        limit: Int,
        offset: Int,
        replaceCache: Boolean = false
    ): Result<ItemPage> = try {
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
            status = status,
            sharedOnly = sharedOnly,
            createdByUserId = createdByUserId,
            updatedAfter = null,
            searchQuery = searchQuery,
            limit = limit,
            offset = offset
        )
        if (response.isSuccessful) {
            val body = response.body()
            val items = body?.items.orEmpty().map { it.withSafeCollections() }
            if (replaceCache) {
                itemDao.deleteForQuery(tuntasId, custodianId, sharedOnly, createdByUserId, status, type, category)
            }
            itemDao.upsertAll(items.toItemEntities())
            Result.success(
                ItemPage(
                    items = items,
                    total = body?.total ?: items.size,
                    limit = body?.limit ?: limit,
                    offset = body?.offset ?: offset,
                    hasMore = body?.hasMore ?: false
                )
            )
        } else {
            Result.failure(Exception(response.errorMessage("Klaida gaunant inventoriu")))
        }
    } catch (e: Exception) {
        Result.failure(e.userFacingException())
    }

    suspend fun consumeItem(
        itemId: String,
        quantity: Int,
        notes: String?
    ): Result<ItemDto> {
        return try {
        val token = tokenManager.token.first()
            ?: return Result.failure(Exception("Nav prisijungta"))
        val tuntasId = tokenManager.activeTuntasId.first()
            ?: return Result.failure(Exception("Tuntas nepasirinktas"))
        val response = itemApiService.consumeItem(
            token = "Bearer $token",
            tuntasId = tuntasId,
            itemId = itemId,
            request = ConsumeItemRequestDto(quantity = quantity, notes = notes?.ifBlank { null })
        )
        if (response.isSuccessful) {
            val item = response.body()!!.withSafeCollections()
            itemDao.upsert(item.toEntity())
            refreshItem(itemId)
            Result.success(item)
        } else {
            Result.failure(Exception(response.errorMessage("Klaida sunaudojant kiekį")))
        }
    } catch (e: IOException) {
        val tuntasId = tokenManager.activeTuntasId.first()
            ?: return Result.failure(Exception("Tuntas nepasirinktas"))
        if (quantity < 1) return Result.failure(Exception("Kiekis turi buti teigiamas skaicius"))
        val cached = itemDao.getItem(itemId, tuntasId)?.toDto()
            ?: return Result.failure(Exception("Daiktas nerastas vietineje saugykloje"))
        if (!cached.isConsumable) return Result.failure(Exception("Sunaudoti galima tik sunaudojamas prekes"))
        if (cached.quantity < quantity) return Result.failure(Exception("Negalima sunaudoti daugiau nei yra likutyje"))
        val updatedQuantity = cached.quantity - quantity
        val updated = cached.copy(
            quantity = updatedQuantity,
            isLowStock = cached.isConsumable && cached.minimumQuantity?.let { updatedQuantity <= it } == true,
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
                operationType = PendingOperationType.ITEM_CONSUME,
                payload = ConsumeItemRequestDto(quantity = quantity, notes = notes?.ifBlank { null })
            )
        }
        Result.success(updated)
    } catch (e: Exception) {
        Result.failure(e.userFacingException())
    }
    }

    suspend fun reviewItemAddition(
        itemId: String,
        decision: String,
        rejectionReason: String? = null
    ): Result<ItemDto> = try {
        val token = tokenManager.token.first()
            ?: return Result.failure(Exception("Nav prisijungta"))
        val tuntasId = tokenManager.activeTuntasId.first()
            ?: return Result.failure(Exception("Tuntas nepasirinktas"))
        val response = itemApiService.reviewItemAddition(
            token = "Bearer $token",
            tuntasId = tuntasId,
            itemId = itemId,
            request = ReviewItemAdditionRequestDto(decision = decision, rejectionReason = rejectionReason)
        )
        if (response.isSuccessful) {
            val item = response.body()!!.withSafeCollections()
            itemDao.upsert(item.toEntity())
            Result.success(item)
        } else {
            Result.failure(Exception(response.errorMessage("Klaida peržiūrint prašymą")))
        }
    } catch (e: Exception) {
        Result.failure(e.userFacingException())
    }

    private fun ItemDto.toCreateRequest(): CreateItemRequestDto = CreateItemRequestDto(
        name = name,
        description = description,
        type = type,
        category = category,
        custodianId = custodianId,
        origin = origin,
        quantity = quantity,
        isConsumable = isConsumable,
        unitOfMeasure = unitOfMeasure ?: DEFAULT_UNIT_OF_MEASURE,
        minimumQuantity = minimumQuantity,
        condition = condition,
        locationId = locationId,
        temporaryStorageLabel = temporaryStorageLabel,
        sourceSharedItemId = sourceSharedItemId,
        responsibleUserId = responsibleUserId,
        photoUrl = photoUrl,
        purchaseDate = purchaseDate,
        purchasePrice = purchasePrice,
        notes = notes,
        customFields = customFields.orEmpty()
    )

    private fun ItemDto.withSafeCollections(): ItemDto = copy(
        unitOfMeasure = unitOfMeasure ?: DEFAULT_UNIT_OF_MEASURE,
        quantityBreakdown = quantityBreakdown.orEmpty(),
        customFields = customFields.orEmpty()
    )

    private fun itemQueryKey(
        custodianId: String?,
        status: String?,
        type: String?,
        category: String?,
        sharedOnly: Boolean,
        createdByUserId: String?
    ): String = listOf(
        "custodian=${custodianId.orEmpty()}",
        "status=${status.orEmpty()}",
        "type=${type.orEmpty()}",
        "category=${category.orEmpty()}",
        "shared=$sharedOnly",
        "createdBy=${createdByUserId.orEmpty()}"
    ).joinToString("|")

    companion object {
        private const val ITEMS_RESOURCE = "items"
        private const val ITEM_RESOURCE = "item"
        private const val ITEM_REFRESH_PAGE_SIZE = 200
        private const val DEFAULT_UNIT_OF_MEASURE = "vnt."
    }
}
