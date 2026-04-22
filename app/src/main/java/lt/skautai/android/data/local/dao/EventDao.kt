package lt.skautai.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import lt.skautai.android.data.local.entity.EventEntity

@Dao
interface EventDao {
    @Query(
        """
        SELECT * FROM events
        WHERE tuntasId = :tuntasId
            AND (:type IS NULL OR type = :type)
            AND (:status IS NULL OR status = :status)
        ORDER BY startDate DESC
        """
    )
    fun observeEvents(tuntasId: String, type: String?, status: String?): Flow<List<EventEntity>>

    @Query(
        """
        SELECT * FROM events
        WHERE tuntasId = :tuntasId
            AND (:type IS NULL OR type = :type)
            AND (:status IS NULL OR status = :status)
        ORDER BY startDate DESC
        """
    )
    suspend fun getEvents(tuntasId: String, type: String?, status: String?): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :eventId AND tuntasId = :tuntasId LIMIT 1")
    fun observeEvent(eventId: String, tuntasId: String): Flow<EventEntity?>

    @Query("SELECT * FROM events WHERE id = :eventId AND tuntasId = :tuntasId LIMIT 1")
    suspend fun getEvent(eventId: String, tuntasId: String): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(events: List<EventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: EventEntity)

    @Query("DELETE FROM events WHERE tuntasId = :tuntasId AND (:type IS NULL OR type = :type) AND (:status IS NULL OR status = :status)")
    suspend fun deleteForQuery(tuntasId: String, type: String?, status: String?)

    @Query("DELETE FROM events WHERE id = :eventId AND tuntasId = :tuntasId")
    suspend fun deleteEvent(eventId: String, tuntasId: String)
}
