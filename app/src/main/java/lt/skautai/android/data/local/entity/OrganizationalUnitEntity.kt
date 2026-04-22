package lt.skautai.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "organizational_units")
data class OrganizationalUnitEntity(
    @PrimaryKey val id: String,
    val tuntasId: String,
    val name: String,
    val type: String,
    val subtype: String?,
    val acceptedRankId: String?,
    val acceptedRankName: String?,
    val memberCount: Int,
    val itemCount: Int,
    val createdAt: String
)
