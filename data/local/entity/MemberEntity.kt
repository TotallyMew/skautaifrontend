package lt.skautai.android.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "members",
    primaryKeys = ["tuntasId", "userId"]
)
data class MemberEntity(
    val tuntasId: String,
    val userId: String,
    val name: String,
    val surname: String,
    val email: String,
    val phone: String?,
    val joinedAt: String,
    val unitAssignmentsJson: String,
    val leadershipRolesJson: String,
    val leadershipRoleHistoryJson: String,
    val ranksJson: String
)
