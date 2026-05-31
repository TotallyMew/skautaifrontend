package lt.skautai.android.data.local.entity

import androidx.room.Entity

@Entity(tableName = "cache_metadata", primaryKeys = ["tuntasId", "resource", "queryKey"])
data class CacheMetadataEntity(
    val tuntasId: String,
    val resource: String,
    val queryKey: String,
    val lastSuccessfulRefreshAt: Long?,
    val lastAttemptAt: Long,
    val lastError: String?
)
