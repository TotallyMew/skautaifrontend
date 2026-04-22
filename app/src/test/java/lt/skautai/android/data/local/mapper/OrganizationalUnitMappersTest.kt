package lt.skautai.android.data.local.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import lt.skautai.android.data.remote.OrganizationalUnitDto

class OrganizationalUnitMappersTest {
    @Test
    fun organizationalUnitDtoRoundTripPreservesCachedFields() {
        val dto = OrganizationalUnitDto(
            id = "unit-1",
            tuntasId = "tuntas-1",
            name = "Skautu draugove",
            type = "DRAUGOVE",
            subtype = "SKAUTAI",
            acceptedRankId = "rank-1",
            acceptedRankName = "Skautas",
            memberCount = 12,
            itemCount = 4,
            createdAt = "2026-01-01T00:00:00Z"
        )

        val roundTrip = dto.toEntity().toDto()

        assertEquals(dto, roundTrip)
    }

    @Test
    fun organizationalUnitDtoRoundTripPreservesNullableFields() {
        val dto = OrganizationalUnitDto(
            id = "unit-2",
            tuntasId = "tuntas-1",
            name = "Tuntas",
            type = "TUNTAS",
            subtype = null,
            acceptedRankId = null,
            acceptedRankName = null,
            createdAt = "2026-01-01T00:00:00Z"
        )

        val roundTrip = dto.toEntity().toDto()

        assertNull(roundTrip.subtype)
        assertNull(roundTrip.acceptedRankId)
        assertEquals(dto, roundTrip)
    }
}
