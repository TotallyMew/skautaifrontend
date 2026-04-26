package lt.skautai.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val tuntasId: String,
    val custodianId: String?,
    val custodianName: String?,
    val origin: String,
    val name: String,
    val description: String?,
    val type: String,
    val category: String,
    val condition: String,
    val quantity: Int,
    val locationId: String?,
    val locationName: String?,
    val locationPath: String?,
    val temporaryStorageLabel: String?,
    val sourceSharedItemId: String?,
    val quantityBreakdownJson: String,
    val totalQuantityAcrossCustodians: Int,
    val responsibleUserId: String?,
    val photoUrl: String?,
    val purchaseDate: String?,
    val purchasePrice: Double?,
    val notes: String?,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)
