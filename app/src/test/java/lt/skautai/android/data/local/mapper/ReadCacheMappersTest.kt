package lt.skautai.android.data.local.mapper

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import lt.skautai.android.data.local.entity.BendrasRequestEntity
import lt.skautai.android.data.local.entity.EventEntity
import lt.skautai.android.data.local.entity.MemberEntity
import lt.skautai.android.data.local.entity.ReservationEntity
import lt.skautai.android.data.local.entity.RequisitionEntity
import lt.skautai.android.data.remote.BendrasRequestDto
import lt.skautai.android.data.remote.BendrasRequestItemDto
import lt.skautai.android.data.remote.EventDto
import lt.skautai.android.data.remote.EventRoleDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.MemberLeadershipRoleDto
import lt.skautai.android.data.remote.MemberRankDto
import lt.skautai.android.data.remote.MemberUnitAssignmentDto
import lt.skautai.android.data.remote.ReservationDto
import lt.skautai.android.data.remote.ReservationItemDto
import lt.skautai.android.data.remote.RequisitionDto
import lt.skautai.android.data.remote.RequisitionItemDto
import lt.skautai.android.data.remote.StovyklaDetailsDto

class ReadCacheMappersTest {
    private val gson = Gson()

    @Test
    fun memberDtoRoundTripPreservesNestedLists() {
        val dto = MemberDto(
            userId = "user-1",
            name = "Jonas",
            surname = "Jonaitis",
            email = "jonas@example.com",
            phone = null,
            joinedAt = "2026-01-01T00:00:00Z",
            unitAssignments = listOf(MemberUnitAssignmentDto("ua-1", "unit-1", "Draugove", "PRIMARY", "2026-01-01")),
            leadershipRoles = listOf(MemberLeadershipRoleDto("lr-1", "role-1", "draugininkas", "unit-1", "Draugove", null, "2026-01-01", null, null, null, 1, "ACTIVE")),
            leadershipRoleHistory = emptyList(),
            ranks = listOf(MemberRankDto("rank-1", "role-2", "skautas", null, "2026-01-01"))
        )

        assertEquals(dto, dto.toEntity("tuntas-1").toDto())
    }

    @Test
    fun reservationDtoRoundTripPreservesItems() {
        val dto = ReservationDto(
            id = "reservation-1",
            title = "Stovykla",
            tuntasId = "tuntas-1",
            reservedByUserId = "user-1",
            reservedByName = "Jonas",
            approvedByUserId = null,
            requestingUnitId = "unit-1",
            requestingUnitName = "Draugove",
            eventId = null,
            totalItems = 1,
            totalQuantity = 2,
            startDate = "2026-02-01",
            endDate = "2026-02-03",
            status = "PENDING",
            notes = null,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-02T00:00:00Z",
            items = listOf(ReservationItemDto("item-1", "Palapine", 2, null, null, 3))
        )

        val entity = dto.toEntity()

        assertEquals("|item-1|", entity.itemIdsIndex)
        assertEquals(dto, entity.toDto())
    }

    @Test
    fun reservationDtoMissingProposalStatusesMapsToNone() {
        val dto = gson.fromJson(
            """
            {
              "id": "reservation-2",
              "title": "Mano rezervacija",
              "tuntasId": "tuntas-1",
              "reservedByUserId": "user-1",
              "reservedByName": "Jonas",
              "totalItems": 0,
              "totalQuantity": 0,
              "startDate": "2026-02-01",
              "endDate": "2026-02-03",
              "status": "PENDING",
              "createdAt": "2026-01-01T00:00:00Z",
              "updatedAt": "2026-01-02T00:00:00Z",
              "items": []
            }
            """.trimIndent(),
            ReservationDto::class.java
        )

        val entity = dto.toEntity()

        assertEquals("NONE", entity.pickupProposalStatus)
        assertEquals("NONE", entity.returnProposalStatus)
    }

    @Test
    fun reservationDtoWithNullItemsMapsToEmptyList() {
        val dto = gson.fromJson(
            """
            {
              "id": "reservation-3",
              "title": "Tuscia rezervacija",
              "tuntasId": "tuntas-1",
              "reservedByUserId": "user-1",
              "totalItems": 0,
              "totalQuantity": 0,
              "startDate": "2026-02-01",
              "endDate": "2026-02-03",
              "status": "PENDING",
              "pickupProposalStatus": "NONE",
              "returnProposalStatus": "NONE",
              "createdAt": "2026-01-01T00:00:00Z",
              "updatedAt": "2026-01-02T00:00:00Z"
            }
            """.trimIndent(),
            ReservationDto::class.java
        )

        val entity = dto.toEntity()

        assertEquals("[]", entity.itemsJson)
        assertEquals("||", entity.itemIdsIndex)
    }

    @Test
    fun requestEntitiesWithNullJsonMapToEmptyLists() {
        val bendras = BendrasRequestEntity(
            id = "request-1",
            tuntasId = "tuntas-1",
            requestedByUserId = "user-1",
            itemId = null,
            itemName = "Virve",
            itemDescription = null,
            quantity = 1,
            neededByDate = null,
            requestingUnitId = null,
            requestingUnitName = null,
            needsDraugininkasApproval = false,
            draugininkasStatus = null,
            draugininkasReviewedByUserId = null,
            draugininkasRejectionReason = null,
            topLevelStatus = "PENDING",
            topLevelReviewedByUserId = null,
            topLevelRejectionReason = null,
            notes = null,
            itemsJson = "null",
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-02T00:00:00Z"
        )
        val requisition = RequisitionEntity(
            id = "req-1",
            tuntasId = "tuntas-1",
            createdByUserId = "user-1",
            requestingUnitId = null,
            requestingUnitName = null,
            status = "PENDING",
            unitReviewStatus = "PENDING",
            unitReviewedByUserId = null,
            unitReviewedAt = null,
            topLevelReviewStatus = "PENDING",
            topLevelReviewedByUserId = null,
            topLevelReviewedAt = null,
            reviewLevel = "UNIT",
            lastAction = "CREATED",
            neededByDate = null,
            notes = null,
            itemsJson = "null",
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-02T00:00:00Z"
        )

        assertEquals(emptyList<BendrasRequestItemDto>(), bendras.toDto().items)
        assertEquals(emptyList<RequisitionItemDto>(), requisition.toDto().items)
    }

    @Test
    fun bendrasRequestDtoMissingReadFieldsMapsWithDefaults() {
        val dto = gson.fromJson(
            """
            {
              "id": "request-2",
              "tuntasId": "tuntas-1",
              "requestedByUserId": "user-1",
              "itemDescription": "Reikia virves",
              "quantity": 1,
              "needsDraugininkasApproval": false,
              "createdAt": "2026-01-01T00:00:00Z",
              "updatedAt": "2026-01-02T00:00:00Z"
            }
            """.trimIndent(),
            BendrasRequestDto::class.java
        )

        val entity = dto.toEntity()

        assertEquals("Reikia virves", entity.itemName)
        assertEquals("PENDING", entity.topLevelStatus)
        assertEquals("[]", entity.itemsJson)
    }

    @Test
    fun requisitionDtoMissingReviewFieldsMapsWithDefaults() {
        val dto = gson.fromJson(
            """
            {
              "id": "req-2",
              "tuntasId": "tuntas-1",
              "createdByUserId": "user-1",
              "createdAt": "2026-01-01T00:00:00Z",
              "updatedAt": "2026-01-02T00:00:00Z"
            }
            """.trimIndent(),
            RequisitionDto::class.java
        )

        val entity = dto.toEntity()

        assertEquals("PENDING", entity.status)
        assertEquals("PENDING", entity.unitReviewStatus)
        assertEquals("PENDING", entity.topLevelReviewStatus)
        assertEquals("UNIT", entity.reviewLevel)
        assertEquals("CREATED", entity.lastAction)
        assertEquals("[]", entity.itemsJson)
    }

    @Test
    fun eventDtoRoundTripPreservesOptionalDetails() {
        val dto = EventDto(
            id = "event-1",
            tuntasId = "tuntas-1",
            name = "Zygis",
            type = "HIKE",
            startDate = "2026-03-01",
            endDate = "2026-03-02",
            locationId = null,
            organizationalUnitId = "unit-1",
            createdByUserId = "user-1",
            status = "ACTIVE",
            notes = null,
            createdAt = "2026-01-01T00:00:00Z",
            eventRoles = listOf(EventRoleDto("role-1", "user-1", "LEADER", null, null, "2026-01-01")),
            stovyklaDetails = StovyklaDetailsDto("details-1", "2026-02-01", 20, null)
        )

        val roundTrip = dto.toEntity().toDto()

        assertEquals(dto, roundTrip)
        assertNull(EventEntity("event-2", "tuntas-1", "Test", "HIKE", "2026-01-01", "2026-01-02", null, null, null, "ACTIVE", null, "2026-01-01", "[]", "null").toDto().stovyklaDetails)
    }
}
