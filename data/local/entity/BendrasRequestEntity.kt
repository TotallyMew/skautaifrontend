package lt.skautai.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bendras_requests")
data class BendrasRequestEntity(
    @PrimaryKey val id: String,
    val tuntasId: String,
    val requestedByUserId: String,
    val itemId: String?,
    val itemName: String,
    val itemDescription: String?,
    val quantity: Int,
    val neededByDate: String?,
    val requestingUnitId: String?,
    val requestingUnitName: String?,
    val needsDraugininkasApproval: Boolean,
    val draugininkasStatus: String?,
    val draugininkasReviewedByUserId: String?,
    val draugininkasRejectionReason: String?,
    val topLevelStatus: String,
    val topLevelReviewedByUserId: String?,
    val topLevelRejectionReason: String?,
    val notes: String?,
    val itemsJson: String,
    val createdAt: String,
    val updatedAt: String
)
