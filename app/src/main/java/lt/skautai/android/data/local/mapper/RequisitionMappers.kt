package lt.skautai.android.data.local.mapper

import com.google.gson.reflect.TypeToken
import lt.skautai.android.data.local.entity.RequisitionEntity
import lt.skautai.android.data.remote.RequisitionDto
import lt.skautai.android.data.remote.RequisitionItemDto

private val requisitionItemsType = object : TypeToken<List<RequisitionItemDto>>() {}.type

fun RequisitionDto.toEntity(): RequisitionEntity {
    val safeItems = items.orEmpty()
    return RequisitionEntity(
        id = id,
        tuntasId = tuntasId,
        createdByUserId = createdByUserId,
        requestingUnitId = requestingUnitId,
        requestingUnitName = requestingUnitName,
        status = status ?: "PENDING",
        unitReviewStatus = unitReviewStatus ?: "PENDING",
        unitReviewedByUserId = unitReviewedByUserId,
        unitReviewedAt = unitReviewedAt,
        topLevelReviewStatus = topLevelReviewStatus ?: "PENDING",
        topLevelReviewedByUserId = topLevelReviewedByUserId,
        topLevelReviewedAt = topLevelReviewedAt,
        reviewLevel = reviewLevel ?: "UNIT",
        lastAction = lastAction ?: "CREATED",
        neededByDate = neededByDate,
        notes = notes,
        itemsJson = localGson.toJson(safeItems),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun RequisitionEntity.toDto(): RequisitionDto = RequisitionDto(
    id = id,
    tuntasId = tuntasId,
    createdByUserId = createdByUserId,
    requestingUnitId = requestingUnitId,
    requestingUnitName = requestingUnitName,
    status = status,
    unitReviewStatus = unitReviewStatus,
    unitReviewedByUserId = unitReviewedByUserId,
    unitReviewedAt = unitReviewedAt,
    topLevelReviewStatus = topLevelReviewStatus,
    topLevelReviewedByUserId = topLevelReviewedByUserId,
    topLevelReviewedAt = topLevelReviewedAt,
    reviewLevel = reviewLevel,
    lastAction = lastAction,
    neededByDate = neededByDate,
    notes = notes,
    items = fromJsonListOrEmpty(itemsJson, requisitionItemsType),
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun List<RequisitionEntity>.toRequisitionDtos(): List<RequisitionDto> = map { it.toDto() }

fun List<RequisitionDto>.toRequisitionEntities(): List<RequisitionEntity> = map { it.toEntity() }
