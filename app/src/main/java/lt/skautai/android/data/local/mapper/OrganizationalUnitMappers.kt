package lt.skautai.android.data.local.mapper

import lt.skautai.android.data.local.entity.OrganizationalUnitEntity
import lt.skautai.android.data.remote.OrganizationalUnitDto

fun OrganizationalUnitDto.toEntity(): OrganizationalUnitEntity = OrganizationalUnitEntity(
    id = id,
    tuntasId = tuntasId,
    name = name,
    type = type,
    subtype = subtype,
    acceptedRankId = acceptedRankId,
    acceptedRankName = acceptedRankName,
    memberCount = memberCount,
    itemCount = itemCount,
    createdAt = createdAt
)

fun OrganizationalUnitEntity.toDto(): OrganizationalUnitDto = OrganizationalUnitDto(
    id = id,
    tuntasId = tuntasId,
    name = name,
    type = type,
    subtype = subtype,
    acceptedRankId = acceptedRankId,
    acceptedRankName = acceptedRankName,
    memberCount = memberCount,
    itemCount = itemCount,
    createdAt = createdAt
)

fun List<OrganizationalUnitEntity>.toOrganizationalUnitDtos(): List<OrganizationalUnitDto> =
    map { it.toDto() }

fun List<OrganizationalUnitDto>.toOrganizationalUnitEntities(): List<OrganizationalUnitEntity> =
    map { it.toEntity() }
