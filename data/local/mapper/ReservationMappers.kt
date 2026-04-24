package lt.skautai.android.data.local.mapper

import com.google.gson.reflect.TypeToken
import lt.skautai.android.data.local.entity.ReservationEntity
import lt.skautai.android.data.remote.ReservationDto
import lt.skautai.android.data.remote.ReservationItemDto

private val reservationItemsType = object : TypeToken<List<ReservationItemDto>>() {}.type

fun ReservationDto.toEntity(): ReservationEntity {
    val safeItems = items.orEmpty()
    return ReservationEntity(
        id = id,
        title = title,
        tuntasId = tuntasId,
        reservedByUserId = reservedByUserId,
        reservedByName = reservedByName,
        approvedByUserId = approvedByUserId,
        requestingUnitId = requestingUnitId,
        requestingUnitName = requestingUnitName,
        eventId = eventId,
        totalItems = totalItems,
        totalQuantity = totalQuantity,
        startDate = startDate,
        endDate = endDate,
        status = status,
        unitReviewStatus = unitReviewStatus,
        unitReviewedByUserId = unitReviewedByUserId,
        unitReviewedAt = unitReviewedAt,
        topLevelReviewStatus = topLevelReviewStatus,
        topLevelReviewedByUserId = topLevelReviewedByUserId,
        topLevelReviewedAt = topLevelReviewedAt,
        pickupAt = pickupAt,
        pickupProposalStatus = pickupProposalStatus ?: "NONE",
        pickupProposedAt = pickupProposedAt,
        pickupProposedByUserId = pickupProposedByUserId,
        pickupRespondedAt = pickupRespondedAt,
        pickupRespondedByUserId = pickupRespondedByUserId,
        returnAt = returnAt,
        returnProposalStatus = returnProposalStatus ?: "NONE",
        returnProposedAt = returnProposedAt,
        returnProposedByUserId = returnProposedByUserId,
        returnRespondedAt = returnRespondedAt,
        returnRespondedByUserId = returnRespondedByUserId,
        notes = notes,
        itemsJson = localGson.toJson(safeItems),
        itemIdsIndex = safeItems.joinToString(separator = "", prefix = "|", postfix = "|") { it.itemId },
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun ReservationEntity.toDto(): ReservationDto = ReservationDto(
    id = id,
    title = title,
    tuntasId = tuntasId,
    reservedByUserId = reservedByUserId,
    reservedByName = reservedByName,
    approvedByUserId = approvedByUserId,
    requestingUnitId = requestingUnitId,
    requestingUnitName = requestingUnitName,
    eventId = eventId,
    totalItems = totalItems,
    totalQuantity = totalQuantity,
    startDate = startDate,
    endDate = endDate,
    status = status,
    unitReviewStatus = unitReviewStatus,
    unitReviewedByUserId = unitReviewedByUserId,
    unitReviewedAt = unitReviewedAt,
    topLevelReviewStatus = topLevelReviewStatus,
    topLevelReviewedByUserId = topLevelReviewedByUserId,
    topLevelReviewedAt = topLevelReviewedAt,
    pickupAt = pickupAt,
    pickupProposalStatus = pickupProposalStatus,
    pickupProposedAt = pickupProposedAt,
    pickupProposedByUserId = pickupProposedByUserId,
    pickupRespondedAt = pickupRespondedAt,
    pickupRespondedByUserId = pickupRespondedByUserId,
    returnAt = returnAt,
    returnProposalStatus = returnProposalStatus,
    returnProposedAt = returnProposedAt,
    returnProposedByUserId = returnProposedByUserId,
    returnRespondedAt = returnRespondedAt,
    returnRespondedByUserId = returnRespondedByUserId,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    items = fromJsonListOrEmpty(itemsJson, reservationItemsType)
)

fun List<ReservationEntity>.toReservationDtos(): List<ReservationDto> = map { it.toDto() }

fun List<ReservationDto>.toReservationEntities(): List<ReservationEntity> = map { it.toEntity() }
