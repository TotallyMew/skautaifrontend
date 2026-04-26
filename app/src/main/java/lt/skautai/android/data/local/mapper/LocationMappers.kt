package lt.skautai.android.data.local.mapper

import lt.skautai.android.data.local.entity.LocationEntity
import lt.skautai.android.data.remote.LocationDto

fun LocationDto.toEntity(): LocationEntity = LocationEntity(
    id = id,
    tuntasId = tuntasId,
    name = name,
    visibility = visibility,
    parentLocationId = parentLocationId,
    ownerUserId = ownerUserId,
    ownerUnitId = ownerUnitId,
    ownerUnitName = ownerUnitName,
    fullPath = fullPath,
    hasChildren = hasChildren,
    isLeafSelectable = isLeafSelectable,
    isEditable = isEditable,
    address = address,
    description = description,
    latitude = latitude,
    longitude = longitude,
    createdAt = createdAt
)

fun LocationEntity.toDto(): LocationDto = LocationDto(
    id = id,
    tuntasId = tuntasId,
    name = name,
    visibility = visibility,
    parentLocationId = parentLocationId,
    ownerUserId = ownerUserId,
    ownerUnitId = ownerUnitId,
    ownerUnitName = ownerUnitName,
    fullPath = fullPath,
    hasChildren = hasChildren,
    isLeafSelectable = isLeafSelectable,
    isEditable = isEditable,
    address = address,
    description = description,
    latitude = latitude,
    longitude = longitude,
    createdAt = createdAt
)

fun List<LocationEntity>.toLocationDtos(): List<LocationDto> = map { it.toDto() }

fun List<LocationDto>.toLocationEntities(): List<LocationEntity> = map { it.toEntity() }
