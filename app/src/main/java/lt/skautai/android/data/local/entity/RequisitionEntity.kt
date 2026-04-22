package lt.skautai.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "requisitions")
data class RequisitionEntity(
    @PrimaryKey val id: String,
    val tuntasId: String,
    val createdByUserId: String,
    val requestingUnitId: String?,
    val requestingUnitName: String?,
    val status: String,
    val unitReviewStatus: String,
    val unitReviewedByUserId: String?,
    val unitReviewedAt: String?,
    val topLevelReviewStatus: String,
    val topLevelReviewedByUserId: String?,
    val topLevelReviewedAt: String?,
    val reviewLevel: String,
    val lastAction: String,
    val neededByDate: String?,
    val notes: String?,
    val itemsJson: String,
    val createdAt: String,
    val updatedAt: String
)
