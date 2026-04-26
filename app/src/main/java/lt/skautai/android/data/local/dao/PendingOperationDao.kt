package lt.skautai.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import lt.skautai.android.data.local.entity.PendingOperationEntity

@Dao
interface PendingOperationDao {
    @Query(
        """
        SELECT * FROM pending_operations
        WHERE status = 'PENDING' AND userId = :userId
        ORDER BY createdAt ASC
        """
    )
    suspend fun getPendingOperations(userId: String): List<PendingOperationEntity>

    @Query("SELECT COUNT(*) FROM pending_operations WHERE userId = :userId AND status IN ('PENDING', 'SYNCING')")
    fun observePendingCount(userId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_operations WHERE userId = :userId AND status = 'FAILED'")
    fun observeFailedCount(userId: String): Flow<Int>

    @Query(
        """
        SELECT * FROM pending_operations
        WHERE userId = :userId AND status IN ('PENDING', 'SYNCING', 'FAILED')
        ORDER BY createdAt DESC
        """
    )
    fun observeVisibleOperations(userId: String): Flow<List<PendingOperationEntity>>

    @Query("SELECT COUNT(*) FROM pending_operations WHERE userId = :userId AND entityType = :entityType AND entityId = :entityId AND status IN ('PENDING', 'SYNCING')")
    fun observePendingCountForEntity(userId: String, entityType: String, entityId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_operations WHERE userId = :userId AND entityType = :entityType AND entityId = :entityId AND status = 'FAILED'")
    fun observeFailedCountForEntity(userId: String, entityType: String, entityId: String): Flow<Int>

    @Query("SELECT * FROM pending_operations WHERE userId = :userId AND entityType = :entityType AND entityId = :entityId AND operationType = :operationType AND status IN ('PENDING', 'FAILED') LIMIT 1")
    suspend fun findOperation(userId: String, entityType: String, entityId: String, operationType: String): PendingOperationEntity?

    @Query("SELECT * FROM pending_operations WHERE userId = :userId AND entityType = :entityType AND entityId = :entityId AND operationType = :operationType LIMIT 1")
    suspend fun findOperationAnyStatus(userId: String, entityType: String, entityId: String, operationType: String): PendingOperationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(operation: PendingOperationEntity)

    @Query("UPDATE pending_operations SET payloadJson = :payloadJson, lastError = NULL, status = 'PENDING' WHERE id = :operationId")
    suspend fun updatePayload(operationId: String, payloadJson: String)

    @Query("UPDATE pending_operations SET status = 'SYNCING', attemptCount = attemptCount + 1, lastError = NULL WHERE id = :operationId")
    suspend fun markSyncing(operationId: String)

    @Query("UPDATE pending_operations SET status = 'SYNCED', lastError = NULL WHERE id = :operationId")
    suspend fun markSynced(operationId: String)

    @Query("UPDATE pending_operations SET status = 'FAILED', lastError = :error WHERE id = :operationId")
    suspend fun markFailed(operationId: String, error: String)

    @Query("UPDATE pending_operations SET status = 'PENDING', lastError = :error WHERE id = :operationId")
    suspend fun markPendingError(operationId: String, error: String)

    @Query("UPDATE pending_operations SET status = 'PENDING', lastError = COALESCE(lastError, 'Sinchronizavimas buvo nutrauktas') WHERE userId = :userId AND status = 'SYNCING'")
    suspend fun resetSyncingToPending(userId: String)

    @Query("UPDATE pending_operations SET status = 'PENDING', lastError = NULL WHERE userId = :userId AND status = 'FAILED'")
    suspend fun retryFailed(userId: String)

    @Query("DELETE FROM pending_operations WHERE userId = :userId AND status = 'SYNCED'")
    suspend fun deleteSynced(userId: String)

    @Query("DELETE FROM pending_operations WHERE id = :operationId")
    suspend fun deleteOperation(operationId: String)
}
