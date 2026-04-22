package lt.skautai.android.data.local.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import lt.skautai.android.data.local.entity.ItemEntity
import lt.skautai.android.data.remote.ItemDistributionDto
import lt.skautai.android.data.remote.ItemDto

private val gson = Gson()
private val quantityBreakdownType = object : TypeToken<List<ItemDistributionDto>>() {}.type

fun ItemDto.toEntity(): ItemEntity = ItemEntity(
    id = id,
    tuntasId = tuntasId,
    custodianId = custodianId,
    custodianName = custodianName,
    origin = origin,
    name = name,
    description = description,
    type = type,
    category = category,
    condition = condition,
    quantity = quantity,
    locationId = locationId,
    temporaryStorageLabel = temporaryStorageLabel,
    sourceSharedItemId = sourceSharedItemId,
    quantityBreakdownJson = gson.toJson(quantityBreakdown ?: emptyList<ItemDistributionDto>()),
    totalQuantityAcrossCustodians = totalQuantityAcrossCustodians,
    responsibleUserId = responsibleUserId,
    photoUrl = photoUrl,
    purchaseDate = purchaseDate,
    purchasePrice = purchasePrice,
    notes = notes,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ItemEntity.toDto(): ItemDto = ItemDto(
    id = id,
    tuntasId = tuntasId,
    custodianId = custodianId,
    custodianName = custodianName,
    origin = origin,
    name = name,
    description = description,
    type = type,
    category = category,
    condition = condition,
    quantity = quantity,
    locationId = locationId,
    temporaryStorageLabel = temporaryStorageLabel,
    sourceSharedItemId = sourceSharedItemId,
    quantityBreakdown = runCatching {
        gson.fromJson<List<ItemDistributionDto>>(quantityBreakdownJson, quantityBreakdownType)
    }.getOrNull().orEmpty(),
    totalQuantityAcrossCustodians = totalQuantityAcrossCustodians,
    responsibleUserId = responsibleUserId,
    photoUrl = photoUrl,
    purchaseDate = purchaseDate,
    purchasePrice = purchasePrice,
    notes = notes,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun List<ItemEntity>.toItemDtos(): List<ItemDto> = map { it.toDto() }

fun List<ItemDto>.toItemEntities(): List<ItemEntity> = map { it.toEntity() }
