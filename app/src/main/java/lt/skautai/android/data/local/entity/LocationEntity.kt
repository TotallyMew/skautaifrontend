package lt.skautai.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey val id: String,
    val tuntasId: String,
    val name: String,
    val address: String?,
    val description: String?,
    val createdAt: String
)
