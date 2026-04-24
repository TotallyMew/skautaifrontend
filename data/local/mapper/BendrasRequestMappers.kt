package lt.skautai.android.data.local.mapper

import com.google.gson.reflect.TypeToken
import lt.skautai.android.data.local.entity.BendrasRequestEntity
import lt.skautai.android.data.remote.BendrasRequestDto
import lt.skautai.android.data.remote.BendrasRequestItemDto

private val bendrasRequestItemsType = object : TypeToken<List<BendrasRequestItemDto>>() {}.type

fun BendrasRequestDto.toEntity(): BendrasRequestEntity {
    val safeItems = items.orEmpty()
    return BendrasRequestEntity(
        id = id,
        tuntasId = tuntasId,
        requestedByUserId = requestedByUserId,
        itemId = itemId,
        itemName = itemName ?: itemDescription ?: safeItems.firstOrNull()?.itemName ?: "Prasymas",
        itemDescription = itemDescription,
        quantity = quantity,
        neededByDate = neededByDate,
        requestingUnitId = requestingUnitId,
        requestingUnitName = requestingUnitName,
        needsDraugininkasApproval = needsDraugininkasApproval,
        draugininkasStatus = draugininkasStatus,
        draugininkasReviewedByUserId = draugininkasReviewedByUserId,
        draugininkasRejectionReason = draugininkasRejectionReason,
        topLevelStatus = topLevelStatus ?: "PENDING",
        topLevelReviewedByUserId = topLevelReviewedByUserId,
        topLevelRejectionReason = topLevelRejectionReason,
        notes = notes,
        itemsJson = localGson.toJson(safeItems),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun BendrasRequestEntity.toDto(): BendrasRequestDto = BendrasRequestDto(
    id = id,
    tuntasId = tuntasId,
    requestedByUserId = requestedByUserId,
    itemId = itemId,
    itemName = itemName,
    itemDescription = itemDescription,
    quantity = quantity,
    neededByDate = neededByDate,
    requestingUnitId = requestingUnitId,
    requestingUnitName = requestingUnitName,
    needsDraugininkasApproval = needsDraugininkasApproval,
    draugininkasStatus = draugininkasStatus,
    draugininkasReviewedByUserId = draugininkasReviewedByUserId,
    draugininkasRejectionReason = draugininkasRejectionReason,
    topLevelStatus = topLevelStatus,
    topLevelReviewedByUserId = topLevelReviewedByUserId,
    topLevelRejectionReason = topLevelRejectionReason,
    notes = notes,
    items = fromJsonListOrEmpty(itemsJson, bendrasRequestItemsType),
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun List<BendrasRequestEntity>.toBendrasRequestDtos(): List<BendrasRequestDto> = map { it.toDto() }

fun List<BendrasRequestDto>.toBendrasRequestEntities(): List<BendrasRequestEntity> = map { it.toEntity() }
