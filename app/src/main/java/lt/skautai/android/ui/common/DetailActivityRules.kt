package lt.skautai.android.ui.common

import lt.skautai.android.data.remote.BendrasRequestDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.UnitMembershipDto

internal fun String.isActiveReservationStatus(): Boolean =
    this !in setOf("REJECTED", "CANCELLED", "RETURNED", "COMPLETED")

internal fun String.isActiveRequestStatus(): Boolean =
    this !in setOf("REJECTED", "CANCELLED", "FULFILLED", "COMPLETED")

internal fun BendrasRequestDto.isActiveSharedRequest(): Boolean =
    topLevelStatus !in setOf("REJECTED", "CANCELLED", "FULFILLED", "COMPLETED") &&
        draugininkasStatus !in setOf("REJECTED", "CANCELLED")

internal fun isUnitLeader(
    membership: UnitMembershipDto,
    member: MemberDto?
): Boolean =
    member?.leadershipRoles
        .orEmpty()
        .any { role ->
            role.termStatus == "ACTIVE" && role.organizationalUnitId == membership.organizationalUnitId
        } ||
        membership.assignmentType == "VADOVO_PADEJEJAS"
