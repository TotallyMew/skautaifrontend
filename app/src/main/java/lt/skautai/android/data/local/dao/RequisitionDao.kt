package lt.skautai.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import lt.skautai.android.data.local.entity.RequisitionEntity

@Dao
interface RequisitionDao {
    @Query("SELECT * FROM requisitions WHERE tuntasId = :tuntasId ORDER BY createdAt DESC")
    fun observeRequests(tuntasId: String): Flow<List<RequisitionEntity>>

    @Query("SELECT * FROM requisitions WHERE tuntasId = :tuntasId ORDER BY createdAt DESC")
    suspend fun getRequests(tuntasId: String): List<RequisitionEntity>

    @Query("SELECT * FROM requisitions WHERE id = :requestId AND tuntasId = :tuntasId LIMIT 1")
    fun observeRequest(requestId: String, tuntasId: String): Flow<RequisitionEntity?>

    @Query("SELECT * FROM requisitions WHERE id = :requestId AND tuntasId = :tuntasId LIMIT 1")
    suspend fun getRequest(requestId: String, tuntasId: String): RequisitionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(requests: List<RequisitionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(request: RequisitionEntity)

    @Query("DELETE FROM requisitions WHERE tuntasId = :tuntasId")
    suspend fun deleteForTuntas(tuntasId: String)
}
