package lt.skautai.android.ui.common

import lt.skautai.android.data.remote.MemberDto

fun isScoutReadOnlyMember(member: MemberDto): Boolean {
    val hasActiveLeadership = member.leadershipRoles.any { it.termStatus == "ACTIVE" }
    val readOnlyRanks = setOf("Skautas", "Patyres skautas")
    return !hasActiveLeadership && member.ranks.any { it.roleName in readOnlyRanks }
}
