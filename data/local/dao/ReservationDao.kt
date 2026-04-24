package lt.skautai.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import lt.skautai.android.data.local.entity.ReservationEntity

@Dao
interface ReservationDao {
    @Query(
        """
        SELECT * FROM reservations
        WHERE tuntasId = :tuntasId
            AND (:status IS NULL OR status = :status)
            AND (:itemId IS NULL OR itemIdsIndex LIKE '%|' || :itemId || '|%')
        ORDER BY startDate DESC, createdAt DESC
        """
    )
    fun observeReservations(tuntasId: String, itemId: String?, status: String?): Flow<List<ReservationEntity>>

    @Query(
        """
        SELECT * FROM reservations
        WHERE tuntasId = :tuntasId
            AND (:status IS NULL OR status = :status)
            AND (:itemId IS NULL OR itemIdsIndex LIKE '%|' || :itemId || '|%')
        ORDER BY startDate DESC, createdAt DESC
        """
    )
    suspend fun getReservations(tuntasId: String, itemId: String?, status: String?): List<ReservationEntity>

    @Query("SELECT * FROM reservations WHERE id = :reservationId AND tuntasId = :tuntasId LIMIT 1")
    fun observeReservation(reservationId: String, tuntasId: String): Flow<ReservationEntity?>

    @Query("SELECT * FROM reservations WHERE id = :reservationId AND tuntasId = :tuntasId LIMIT 1")
    suspend fun getReservation(reservationId: String, tuntasId: String): ReservationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(reservations: List<ReservationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reservation: ReservationEntity)

    @Query("DELETE FROM reservations WHERE tuntasId = :tuntasId AND (:status IS NULL OR status = :status)")
    suspend fun deleteForQuery(tuntasId: String, status: String?)

    @Query("DELETE FROM reservations WHERE id = :reservationId AND tuntasId = :tuntasId")
    suspend fun deleteReservation(reservationId: String, tuntasId: String)

    @Query("SELECT * FROM reservations WHERE tuntasId = :tuntasId AND eventId = :eventId ORDER BY startDate ASC")
    fun observeReservationsForEvent(tuntasId: String, eventId: String): Flow<List<ReservationEntity>>

    @Query("SELECT * FROM reservations WHERE tuntasId = :tuntasId AND eventId = :eventId ORDER BY startDate ASC")
    suspend fun getReservationsForEvent(tuntasId: String, eventId: String): List<ReservationEntity>
}
