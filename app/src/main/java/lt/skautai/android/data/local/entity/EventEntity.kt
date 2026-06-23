package lt.skautai.android.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "events",
    indices = [
        Index(
            value = ["tuntasId", "startDate", "endDate", "status"],
            name = "index_events_calendar"
        ),
        Index(
            value = ["tuntasId", "status", "type", "startDate"],
            name = "index_events_list_filters"
        )
    ]
)
data class EventEntity(
    @PrimaryKey val id: String,
    val tuntasId: String,
    val name: String,
    val type: String,
    val customTypeLabel: String?,
    val startDate: String,
    val endDate: String,
    val locationId: String?,
    val organizationalUnitId: String?,
    val createdByUserId: String?,
    val status: String,
    val notes: String?,
    val createdAt: String,
    val eventRolesJson: String,
    val stovyklaDetailsJson: String?
)
