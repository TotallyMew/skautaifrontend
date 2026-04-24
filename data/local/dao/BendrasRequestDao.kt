package lt.skautai.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import lt.skautai.android.data.local.entity.BendrasRequestEntity

@Dao
interface BendrasRequestDao {
    @Query("SELECT * FROM bendras_requests WHERE tuntasId = :tuntasId ORDER BY createdAt DESC")
    fun observeRequests(tuntasId: String): Flow<List<BendrasRequestEntity>>

    @Query("SELECT * FROM bendras_requests WHERE tuntasId = :tuntasId ORDER BY createdAt DESC")
    suspend fun getRequests(tuntasId: String): List<BendrasRequestEntity>

    @Query("SELECT * FROM bendras_requests WHERE id = :requestId AND tuntasId = :tuntasId LIMIT 1")
    fun observeRequest(requestId: String, tuntasId: String): Flow<BendrasRequestEntity?>

    @Query("SELECT * FROM bendras_requests WHERE id = :requestId AND tuntasId = :tuntasId LIMIT 1")
    suspend fun getRequest(requestId: String, tuntasId: String): BendrasRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(requests: List<BendrasRequestEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(request: BendrasRequestEntity)

    @Query("DELETE FROM bendras_requests WHERE tuntasId = :tuntasId")
    suspend fun deleteForTuntas(tuntasId: String)

    @Query("DELETE FROM bendras_requests WHERE id = :requestId AND tuntasId = :tuntasId")
    suspend fun deleteRequest(requestId: String, tuntasId: String)
}
