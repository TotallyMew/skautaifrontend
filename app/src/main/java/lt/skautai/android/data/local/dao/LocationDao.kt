package lt.skautai.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import lt.skautai.android.data.local.entity.LocationEntity

@Dao
interface LocationDao {
    @Query("SELECT * FROM locations WHERE tuntasId = :tuntasId ORDER BY LOWER(name)")
    fun observeLocations(tuntasId: String): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE tuntasId = :tuntasId ORDER BY LOWER(name)")
    suspend fun getLocations(tuntasId: String): List<LocationEntity>

    @Query("SELECT * FROM locations WHERE id = :locationId AND tuntasId = :tuntasId LIMIT 1")
    suspend fun getLocation(locationId: String, tuntasId: String): LocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(locations: List<LocationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(location: LocationEntity)

    @Query("DELETE FROM locations WHERE tuntasId = :tuntasId")
    suspend fun deleteForTuntas(tuntasId: String)

    @Query("DELETE FROM locations WHERE id = :locationId AND tuntasId = :tuntasId")
    suspend fun deleteLocation(locationId: String, tuntasId: String)
}
