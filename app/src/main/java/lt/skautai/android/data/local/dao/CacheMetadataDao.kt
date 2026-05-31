package lt.skautai.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import lt.skautai.android.data.local.entity.CacheMetadataEntity

@Dao
interface CacheMetadataDao {
    @Query(
        """
        SELECT * FROM cache_metadata
        WHERE tuntasId = :tuntasId AND resource = :resource AND queryKey = :queryKey
        LIMIT 1
        """
    )
    suspend fun get(tuntasId: String, resource: String, queryKey: String): CacheMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: CacheMetadataEntity)

    @Query("DELETE FROM cache_metadata WHERE tuntasId = :tuntasId")
    suspend fun deleteForTuntas(tuntasId: String)
}
