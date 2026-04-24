package lt.skautai.android.data.local.mapper

import com.google.gson.reflect.TypeToken
import lt.skautai.android.data.local.entity.MemberEntity
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.MemberLeadershipRoleDto
import lt.skautai.android.data.remote.MemberRankDto
import lt.skautai.android.data.remote.MemberUnitAssignmentDto

private val unitAssignmentsType = object : TypeToken<List<MemberUnitAssignmentDto>>() {}.type
private val leadershipRolesType = object : TypeToken<List<MemberLeadershipRoleDto>>() {}.type
private val ranksType = object : TypeToken<List<MemberRankDto>>() {}.type

fun MemberDto.toEntity(tuntasId: String): MemberEntity = MemberEntity(
    tuntasId = tuntasId,
    userId = userId,
    name = name,
    surname = surname,
    email = email,
    phone = phone,
    joinedAt = joinedAt,
    unitAssignmentsJson = localGson.toJson(unitAssignments.orEmpty()),
    leadershipRolesJson = localGson.toJson(leadershipRoles),
    leadershipRoleHistoryJson = localGson.toJson(leadershipRoleHistory),
    ranksJson = localGson.toJson(ranks)
)

fun MemberEntity.toDto(): MemberDto = MemberDto(
    userId = userId,
    name = name,
    surname = surname,
    email = email,
    phone = phone,
    joinedAt = joinedAt,
    unitAssignments = fromJsonListOrEmpty(unitAssignmentsJson, unitAssignmentsType),
    leadershipRoles = fromJsonListOrEmpty(leadershipRolesJson, leadershipRolesType),
    leadershipRoleHistory = fromJsonListOrEmpty(leadershipRoleHistoryJson, leadershipRolesType),
    ranks = fromJsonListOrEmpty(ranksJson, ranksType)
)

fun List<MemberEntity>.toMemberDtos(): List<MemberDto> = map { it.toDto() }

fun List<MemberDto>.toMemberEntities(tuntasId: String): List<MemberEntity> = map { it.toEntity(tuntasId) }
