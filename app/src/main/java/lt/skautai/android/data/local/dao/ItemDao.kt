package lt.skautai.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import lt.skautai.android.data.local.entity.ItemEntity

@Dao
interface ItemDao {
    @Query(
        """
        SELECT * FROM items
        WHERE tuntasId = :tuntasId
            AND (:custodianId IS NULL OR custodianId = :custodianId)
            AND (:sharedOnly = 0 OR (custodianId IS NULL AND type != 'INDIVIDUAL'))
            AND (:createdByUserId IS NULL OR createdByUserId = :createdByUserId)
            AND (:status IS NULL OR status = :status)
            AND (:type IS NULL OR type = :type)
            AND (:category IS NULL OR category = :category)
        ORDER BY LOWER(name)
        """
    )
    fun observeItems(
        tuntasId: String,
        custodianId: String?,
        sharedOnly: Boolean,
        createdByUserId: String?,
        status: String?,
        type: String?,
        category: String?
    ): Flow<List<ItemEntity>>

    @Query(
        """
        SELECT * FROM items
        WHERE tuntasId = :tuntasId
            AND (:custodianId IS NULL OR custodianId = :custodianId)
            AND (:sharedOnly = 0 OR (custodianId IS NULL AND type != 'INDIVIDUAL'))
            AND (:createdByUserId IS NULL OR createdByUserId = :createdByUserId)
            AND (:status IS NULL OR status = :status)
            AND (:type IS NULL OR type = :type)
            AND (:category IS NULL OR category = :category)
        ORDER BY LOWER(name)
        """
    )
    suspend fun getItems(
        tuntasId: String,
        custodianId: String?,
        sharedOnly: Boolean,
        createdByUserId: String?,
        status: String?,
        type: String?,
        category: String?
    ): List<ItemEntity>

    @Query("SELECT * FROM items WHERE id = :itemId AND tuntasId = :tuntasId LIMIT 1")
    suspend fun getItem(itemId: String, tuntasId: String): ItemEntity?

    @Query("SELECT * FROM items WHERE id = :itemId AND tuntasId = :tuntasId LIMIT 1")
    fun observeItem(itemId: String, tuntasId: String): Flow<ItemEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ItemEntity)

    @Query(
        """
        DELETE FROM items
        WHERE tuntasId = :tuntasId
            AND (:custodianId IS NULL OR custodianId = :custodianId)
            AND (:sharedOnly = 0 OR (custodianId IS NULL AND type != 'INDIVIDUAL'))
            AND (:createdByUserId IS NULL OR createdByUserId = :createdByUserId)
            AND (:status IS NULL OR status = :status)
            AND (:type IS NULL OR type = :type)
            AND (:category IS NULL OR category = :category)
        """
    )
    suspend fun deleteForQuery(
        tuntasId: String,
        custodianId: String?,
        sharedOnly: Boolean,
        createdByUserId: String?,
        status: String?,
        type: String?,
        category: String?
    )

    @Query("DELETE FROM items WHERE id = :itemId AND tuntasId = :tuntasId")
    suspend fun deleteItem(itemId: String, tuntasId: String)
}
