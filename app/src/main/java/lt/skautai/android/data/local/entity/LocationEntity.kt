package lt.skautai.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey val id: String,
    val tuntasId: String,
    val name: String,
    val visibility: String,
    val parentLocationId: String?,
    val ownerUserId: String?,
    val ownerUnitId: String?,
    val ownerUnitName: String?,
    val fullPath: String,
    val hasChildren: Boolean,
    val isLeafSelectable: Boolean,
    val isEditable: Boolean,
    val address: String?,
    val description: String?,
    val latitude: Double?,
    val longitude: Double?,
    val createdAt: String
)
