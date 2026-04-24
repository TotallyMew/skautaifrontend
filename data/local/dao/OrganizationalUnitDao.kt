package lt.skautai.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import lt.skautai.android.data.local.entity.OrganizationalUnitEntity

@Dao
interface OrganizationalUnitDao {
    @Query(
        """
        SELECT * FROM organizational_units
        WHERE tuntasId = :tuntasId
            AND (:type IS NULL OR type = :type)
        ORDER BY LOWER(name)
        """
    )
    fun observeUnits(tuntasId: String, type: String?): Flow<List<OrganizationalUnitEntity>>

    @Query(
        """
        SELECT * FROM organizational_units
        WHERE tuntasId = :tuntasId
            AND (:type IS NULL OR type = :type)
        ORDER BY LOWER(name)
        """
    )
    suspend fun getUnits(tuntasId: String, type: String?): List<OrganizationalUnitEntity>

    @Query("SELECT * FROM organizational_units WHERE id = :unitId AND tuntasId = :tuntasId LIMIT 1")
    suspend fun getUnit(unitId: String, tuntasId: String): OrganizationalUnitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(units: List<OrganizationalUnitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(unit: OrganizationalUnitEntity)

    @Query("DELETE FROM organizational_units WHERE tuntasId = :tuntasId AND (:type IS NULL OR type = :type)")
    suspend fun deleteForQuery(tuntasId: String, type: String?)

    @Query("DELETE FROM organizational_units WHERE id = :unitId AND tuntasId = :tuntasId")
    suspend fun deleteUnit(unitId: String, tuntasId: String)
}
