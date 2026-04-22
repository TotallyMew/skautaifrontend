package lt.skautai.android.data.local.mapper

import com.google.gson.reflect.TypeToken
import lt.skautai.android.data.local.entity.EventEntity
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.EventRoleDto
import lt.skautai.android.data.remote.StovyklaDetailsDto

private val eventRolesType = object : TypeToken<List<EventRoleDto>>() {}.type
private val stovyklaDetailsType = object : TypeToken<StovyklaDetailsDto>() {}.type

fun EventDto.toEntity(): EventEntity = EventEntity(
    id = id,
    tuntasId = tuntasId,
    name = name,
    type = type,
    startDate = startDate,
    endDate = endDate,
    locationId = locationId,
    organizationalUnitId = organizationalUnitId,
    createdByUserId = createdByUserId,
    status = status,
    notes = notes,
    createdAt = createdAt,
    eventRolesJson = localGson.toJson(eventRoles),
    stovyklaDetailsJson = toJsonOrNull(stovyklaDetails)
)

fun EventEntity.toDto(): EventDto = EventDto(
    id = id,
    tuntasId = tuntasId,
    name = name,
    type = type,
    startDate = startDate,
    endDate = endDate,
    locationId = locationId,
    organizationalUnitId = organizationalUnitId,
    createdByUserId = createdByUserId,
    status = status,
    notes = notes,
    createdAt = createdAt,
    eventRoles = fromJsonListOrEmpty(eventRolesJson, eventRolesType),
    stovyklaDetails = fromJsonOrNull(stovyklaDetailsJson, stovyklaDetailsType)
)

fun List<EventEntity>.toEventDtos(): List<EventDto> = map { it.toDto() }

fun List<EventDto>.toEventEntities(): List<EventEntity> = map { it.toEntity() }
