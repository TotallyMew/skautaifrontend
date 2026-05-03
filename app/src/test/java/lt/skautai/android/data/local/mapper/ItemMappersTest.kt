package lt.skautai.android.data.local.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import lt.skautai.android.data.local.entity.ItemEntity
import lt.skautai.android.data.remote.ItemDistributionDto
import lt.skautai.android.data.remote.ItemDto

class ItemMappersTest {
    @Test
    fun itemDtoRoundTripPreservesCachedFields() {
        val dto = ItemDto(
            id = "item-1",
            qrToken = "qr-1",
            tuntasId = "tuntas-1",
            custodianId = "unit-1",
            custodianName = "Vienetas",
            origin = "UNIT_ACQUIRED",
            name = "Palapine",
            description = "Keturiu vietu",
            type = "SHARED",
            category = "TENTS",
            condition = "GOOD",
            quantity = 2,
            locationId = "location-1",
            locationName = "Sandelys",
            locationPath = "Tuntas / Sandelys",
            temporaryStorageLabel = "Spinta",
            sourceSharedItemId = null,
            quantityBreakdown = listOf(ItemDistributionDto("Vienetas", 2)),
            totalQuantityAcrossCustodians = 3,
            responsibleUserId = "user-1",
            createdByUserId = "creator-1",
            createdByUserName = "Jonas Jonaitis",
            photoUrl = "https://example.com/photo.jpg",
            purchaseDate = "2026-01-01",
            purchasePrice = 42.5,
            notes = "Pastaba",
            status = "ACTIVE",
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-02T00:00:00Z"
        )

        val roundTrip = dto.toEntity().toDto()

        assertEquals(dto, roundTrip)
    }

    @Test
    fun itemDtoRoundTripPreservesNullableFields() {
        val dto = ItemDto(
            id = "item-2",
            qrToken = "qr-2",
            tuntasId = "tuntas-1",
            custodianId = null,
            custodianName = null,
            origin = "TUNTAS",
            name = "Puodas",
            description = null,
            type = "SHARED",
            category = "COOKING",
            condition = "GOOD",
            quantity = 1,
            locationId = null,
            locationName = null,
            locationPath = null,
            temporaryStorageLabel = null,
            sourceSharedItemId = null,
            responsibleUserId = null,
            createdByUserId = null,
            createdByUserName = null,
            photoUrl = null,
            purchaseDate = null,
            purchasePrice = null,
            notes = null,
            status = "ACTIVE",
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-02T00:00:00Z"
        )

        val roundTrip = dto.toEntity().toDto()

        assertNull(roundTrip.custodianId)
        assertNull(roundTrip.locationId)
        assertEquals(dto, roundTrip)
    }

    @Test
    fun itemEntityWithNullQuantityBreakdownJsonMapsToEmptyList() {
        val entity = ItemEntity(
            id = "item-3",
            qrToken = "qr-3",
            tuntasId = "tuntas-1",
            custodianId = null,
            custodianName = null,
            origin = "UNIT_ACQUIRED",
            name = "Testas",
            description = null,
            type = "COLLECTIVE",
            category = "CAMPING",
            condition = "GOOD",
            quantity = 1,
            locationId = null,
            locationName = null,
            locationPath = null,
            temporaryStorageLabel = null,
            sourceSharedItemId = null,
            quantityBreakdownJson = "null",
            totalQuantityAcrossCustodians = 1,
            responsibleUserId = null,
            createdByUserId = null,
            createdByUserName = null,
            photoUrl = null,
            purchaseDate = null,
            purchasePrice = null,
            notes = null,
            status = "ACTIVE",
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-02T00:00:00Z"
        )

        assertEquals(emptyList<ItemDistributionDto>(), entity.toDto().quantityBreakdown)
    }
}
