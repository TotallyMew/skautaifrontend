package lt.skautai.android.ui.common

import lt.skautai.android.data.remote.BendrasRequestDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.MemberLeadershipRoleDto
import lt.skautai.android.data.remote.UnitMembershipDto
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailActivityRulesTest {

    @Test
    fun `reservation statuses treat in-progress review states as active`() {
        assertTrue("PENDING".isActiveReservationStatus())
        assertTrue("APPROVED".isActiveReservationStatus())
        assertTrue("ISSUED".isActiveReservationStatus())
    }

    @Test
    fun `reservation statuses exclude finished and cancelled states`() {
        assertFalse("CANCELLED".isActiveReservationStatus())
        assertFalse("REJECTED".isActiveReservationStatus())
        assertFalse("RETURNED".isActiveReservationStatus())
        assertFalse("COMPLETED".isActiveReservationStatus())
    }

    @Test
    fun `request statuses exclude terminal states`() {
        assertTrue("PENDING".isActiveRequestStatus())
        assertTrue("FORWARDED".isActiveRequestStatus())
        assertFalse("REJECTED".isActiveRequestStatus())
        assertFalse("CANCELLED".isActiveRequestStatus())
        assertFalse("FULFILLED".isActiveRequestStatus())
        assertFalse("COMPLETED".isActiveRequestStatus())
    }

    @Test
    fun `shared request is inactive when either review path rejects or cancels it`() {
        assertTrue(sharedRequest(topLevelStatus = "PENDING", draugininkasStatus = "PENDING").isActiveSharedRequest())
        assertFalse(sharedRequest(topLevelStatus = "REJECTED", draugininkasStatus = "PENDING").isActiveSharedRequest())
        assertFalse(sharedRequest(topLevelStatus = "PENDING", draugininkasStatus = "REJECTED").isActiveSharedRequest())
        assertFalse(sharedRequest(topLevelStatus = "CANCELLED", draugininkasStatus = null).isActiveSharedRequest())
    }

    @Test
    fun `unit leader is detected from active leadership role in the same unit`() {
        val membership = unitMembership(unitId = "unit-1", assignmentType = "MEMBER")
        val member = member(
            roles = listOf(
                leadershipRole(unitId = "unit-1", termStatus = "ACTIVE")
            )
        )

        assertTrue(isUnitLeader(membership, member))
    }

    @Test
    fun `unit leader ignores completed leadership role and different unit roles`() {
        val membership = unitMembership(unitId = "unit-1", assignmentType = "MEMBER")
        val member = member(
            roles = listOf(
                leadershipRole(unitId = "unit-1", termStatus = "COMPLETED"),
                leadershipRole(unitId = "unit-2", termStatus = "ACTIVE")
            )
        )

        assertFalse(isUnitLeader(membership, member))
    }

    @Test
    fun `unit leader includes vadovo padejejas assignment`() {
        val membership = unitMembership(unitId = "unit-1", assignmentType = "VADOVO_PADEJEJAS")

        assertTrue(isUnitLeader(membership, member()))
    }

    private fun sharedRequest(
        topLevelStatus: String,
        draugininkasStatus: String?
    ) = BendrasRequestDto(
        id = "request-1",
        tuntasId = "tuntas-1",
        requestedByUserId = "user-1",
        requestedByUserName = "User",
        itemId = null,
        itemName = "Palapinė",
        itemDescription = null,
        quantity = 1,
        neededByDate = null,
        requestingUnitId = "unit-1",
        requestingUnitName = "Vienetas",
        needsDraugininkasApproval = draugininkasStatus != null,
        draugininkasStatus = draugininkasStatus,
        draugininkasReviewedByUserId = null,
        draugininkasRejectionReason = null,
        topLevelStatus = topLevelStatus,
        topLevelReviewedByUserId = null,
        topLevelRejectionReason = null,
        notes = null,
        items = emptyList(),
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z"
    )

    private fun unitMembership(
        unitId: String,
        assignmentType: String
    ) = UnitMembershipDto(
        id = "membership-1",
        userId = "user-1",
        userName = "Jonas",
        userSurname = "Jonaitis",
        organizationalUnitId = unitId,
        organizationalUnitName = "Vienetas",
        tuntasId = "tuntas-1",
        assignmentType = assignmentType,
        assignedByUserId = null,
        joinedAt = "2026-01-01T00:00:00Z",
        leftAt = null
    )

    private fun member(
        roles: List<MemberLeadershipRoleDto> = emptyList()
    ) = MemberDto(
        userId = "user-1",
        name = "Jonas",
        surname = "Jonaitis",
        email = "jonas@example.com",
        phone = null,
        joinedAt = "2026-01-01T00:00:00Z",
        unitAssignments = emptyList(),
        leadershipRoles = roles,
        leadershipRoleHistory = emptyList(),
        ranks = emptyList()
    )

    private fun leadershipRole(
        unitId: String,
        termStatus: String
    ) = MemberLeadershipRoleDto(
        id = "role-$unitId-$termStatus",
        roleId = "role-1",
        roleName = "Draugininkas",
        organizationalUnitId = unitId,
        organizationalUnitName = "Vienetas",
        assignedByUserId = null,
        assignedAt = "2026-01-01T00:00:00Z",
        startsAt = null,
        expiresAt = null,
        leftAt = null,
        termNumber = 1,
        termStatus = termStatus
    )
}
