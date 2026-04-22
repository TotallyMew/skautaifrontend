package lt.skautai.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import lt.skautai.android.data.local.entity.MemberEntity

@Dao
interface MemberDao {
    @Query("SELECT * FROM members WHERE tuntasId = :tuntasId ORDER BY LOWER(surname), LOWER(name)")
    fun observeMembers(tuntasId: String): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE tuntasId = :tuntasId ORDER BY LOWER(surname), LOWER(name)")
    suspend fun getMembers(tuntasId: String): List<MemberEntity>

    @Query("SELECT * FROM members WHERE userId = :userId AND tuntasId = :tuntasId LIMIT 1")
    fun observeMember(userId: String, tuntasId: String): Flow<MemberEntity?>

    @Query("SELECT * FROM members WHERE userId = :userId AND tuntasId = :tuntasId LIMIT 1")
    suspend fun getMember(userId: String, tuntasId: String): MemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(members: List<MemberEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(member: MemberEntity)

    @Query("DELETE FROM members WHERE tuntasId = :tuntasId")
    suspend fun deleteForTuntas(tuntasId: String)

    @Query("DELETE FROM members WHERE userId = :userId AND tuntasId = :tuntasId")
    suspend fun deleteMember(userId: String, tuntasId: String)
}
