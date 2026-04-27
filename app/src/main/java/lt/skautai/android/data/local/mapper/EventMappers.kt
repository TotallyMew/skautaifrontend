package lt.skautai.android.data.local.mapper

import com.google.gson.reflect.TypeToken
import lt.skautai.android.data.local.entity.EventEntity
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.EventInventoryCustodyDto
import lt.skautai.android.data.remote.EventInventoryMovementDto
import lt.skautai.android.data.remote.EventInventoryPlanDto
import lt.skautai.android.data.remote.EventInventoryRequestDto
import lt.skautai.android.data.remote.EventPurchaseDto
import lt.skautai.android.data.remote.EventRoleDto
import lt.skautai.android.data.remote.PastovykleDto
import lt.skautai.android.data.remote.PastovykleInventoryDto

private val eventRolesType = object : TypeToken<List<EventRoleDto>>() {}.type
private val pastovyklesType = object : TypeToken<List<PastovykleDto>>() {}.type
private val offlineDetailsType = object : TypeToken<EventOfflineDetails>() {}.type

data class EventOfflineDetails(
    val pastovykles: List<PastovykleDto> = emptyList(),
    val inventoryPlan: EventInventoryPlanDto? = null,
    val purchases: List<EventPurchaseDto> = emptyList(),
    val pastovykleInventoryById: Map<String, List<PastovykleInventoryDto>> = emptyMap(),
    val pastovykleRequestsById: Map<String, List<EventInventoryRequestDto>> = emptyMap(),
    val inventoryCustody: List<EventInventoryCustodyDto> = emptyList(),
    val inventoryMovements: List<EventInventoryMovementDto> = emptyList()
)

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
    stovyklaDetailsJson = null
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
    inventorySummary = null
)

fun EventEntity.cachedPastovykles(): List<PastovykleDto> =
    offlineDetailsOrLegacy().pastovykles

fun EventEntity.withCachedPastovykles(pastovykles: List<PastovykleDto>): EventEntity =
    withOfflineDetails(offlineDetailsOrLegacy().copy(pastovykles = pastovykles))

fun EventEntity.cachedPurchases(): List<EventPurchaseDto> =
    offlineDetailsOrLegacy().purchases

fun EventEntity.withCachedPurchases(purchases: List<EventPurchaseDto>): EventEntity =
    withOfflineDetails(offlineDetailsOrLegacy().copy(purchases = purchases))

fun EventEntity.cachedInventoryPlan(): EventInventoryPlanDto? =
    offlineDetailsOrLegacy().inventoryPlan

fun EventEntity.withCachedInventoryPlan(plan: EventInventoryPlanDto?): EventEntity =
    withOfflineDetails(offlineDetailsOrLegacy().copy(inventoryPlan = plan))

fun EventEntity.cachedPastovykleInventoryById(): Map<String, List<PastovykleInventoryDto>> =
    offlineDetailsOrLegacy().pastovykleInventoryById

fun EventEntity.withCachedPastovykleInventoryById(
    inventoryById: Map<String, List<PastovykleInventoryDto>>
): EventEntity = withOfflineDetails(offlineDetailsOrLegacy().copy(pastovykleInventoryById = inventoryById))

fun EventEntity.cachedPastovykleRequestsById(): Map<String, List<EventInventoryRequestDto>> =
    offlineDetailsOrLegacy().pastovykleRequestsById

fun EventEntity.withCachedPastovykleRequestsById(
    requestsById: Map<String, List<EventInventoryRequestDto>>
): EventEntity = withOfflineDetails(offlineDetailsOrLegacy().copy(pastovykleRequestsById = requestsById))

fun EventEntity.cachedInventoryCustody(): List<EventInventoryCustodyDto> =
    offlineDetailsOrLegacy().inventoryCustody

fun EventEntity.withCachedInventoryCustody(custody: List<EventInventoryCustodyDto>): EventEntity =
    withOfflineDetails(offlineDetailsOrLegacy().copy(inventoryCustody = custody))

fun EventEntity.cachedInventoryMovements(): List<EventInventoryMovementDto> =
    offlineDetailsOrLegacy().inventoryMovements

fun EventEntity.withCachedInventoryMovements(movements: List<EventInventoryMovementDto>): EventEntity =
    withOfflineDetails(offlineDetailsOrLegacy().copy(inventoryMovements = movements))

private fun EventEntity.withOfflineDetails(details: EventOfflineDetails): EventEntity =
    copy(stovyklaDetailsJson = localGson.toJson(details))

private fun EventEntity.offlineDetailsOrLegacy(): EventOfflineDetails {
    fromJsonOrNull<EventOfflineDetails>(stovyklaDetailsJson, offlineDetailsType)?.let { return it }
    val legacyPastovykles = fromJsonOrNull<List<PastovykleDto>>(stovyklaDetailsJson, pastovyklesType).orEmpty()
    return EventOfflineDetails(pastovykles = legacyPastovykles)
}

fun List<EventEntity>.toEventDtos(): List<EventDto> = map { it.toDto() }

fun List<EventDto>.toEventEntities(): List<EventEntity> = map { it.toEntity() }
