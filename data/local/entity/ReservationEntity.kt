package lt.skautai.android.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "reservations", indices = [Index("eventId")])
data class ReservationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val tuntasId: String,
    val reservedByUserId: String,
    val reservedByName: String?,
    val approvedByUserId: String?,
    val requestingUnitId: String?,
    val requestingUnitName: String?,
    val eventId: String?,
    val totalItems: Int,
    val totalQuantity: Int,
    val startDate: String,
    val endDate: String,
    val status: String,
    val unitReviewStatus: String?,
    val unitReviewedByUserId: String?,
    val unitReviewedAt: String?,
    val topLevelReviewStatus: String?,
    val topLevelReviewedByUserId: String?,
    val topLevelReviewedAt: String?,
    val pickupAt: String?,
    val pickupProposalStatus: String,
    val pickupProposedAt: String?,
    val pickupProposedByUserId: String?,
    val pickupRespondedAt: String?,
    val pickupRespondedByUserId: String?,
    val returnAt: String?,
    val returnProposalStatus: String,
    val returnProposedAt: String?,
    val returnProposedByUserId: String?,
    val returnRespondedAt: String?,
    val returnRespondedByUserId: String?,
    val notes: String?,
    val itemsJson: String,
    val itemIdsIndex: String,
    val createdAt: String,
    val updatedAt: String
)
