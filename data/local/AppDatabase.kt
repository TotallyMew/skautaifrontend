package lt.skautai.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import lt.skautai.android.data.local.dao.BendrasRequestDao
import lt.skautai.android.data.local.dao.EventDao
import lt.skautai.android.data.local.dao.ItemDao
import lt.skautai.android.data.local.dao.LocationDao
import lt.skautai.android.data.local.dao.MemberDao
import lt.skautai.android.data.local.dao.OrganizationalUnitDao
import lt.skautai.android.data.local.dao.PendingOperationDao
import lt.skautai.android.data.local.dao.RequisitionDao
import lt.skautai.android.data.local.dao.ReservationDao
import lt.skautai.android.data.local.entity.BendrasRequestEntity
import lt.skautai.android.data.local.entity.EventEntity
import lt.skautai.android.data.local.entity.ItemEntity
import lt.skautai.android.data.local.entity.LocationEntity
import lt.skautai.android.data.local.entity.MemberEntity
import lt.skautai.android.data.local.entity.OrganizationalUnitEntity
import lt.skautai.android.data.local.entity.PendingOperationEntity
import lt.skautai.android.data.local.entity.RequisitionEntity
import lt.skautai.android.data.local.entity.ReservationEntity

@Database(
    entities = [
        ItemEntity::class,
        OrganizationalUnitEntity::class,
        LocationEntity::class,
        MemberEntity::class,
        ReservationEntity::class,
        BendrasRequestEntity::class,
        RequisitionEntity::class,
        EventEntity::class,
        PendingOperationEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun organizationalUnitDao(): OrganizationalUnitDao
    abstract fun locationDao(): LocationDao
    abstract fun memberDao(): MemberDao
    abstract fun reservationDao(): ReservationDao
    abstract fun bendrasRequestDao(): BendrasRequestDao
    abstract fun requisitionDao(): RequisitionDao
    abstract fun eventDao(): EventDao
    abstract fun pendingOperationDao(): PendingOperationDao
}
