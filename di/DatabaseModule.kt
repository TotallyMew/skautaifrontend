package lt.skautai.android.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import lt.skautai.android.data.local.AppDatabase
import lt.skautai.android.data.local.dao.BendrasRequestDao
import lt.skautai.android.data.local.dao.EventDao
import lt.skautai.android.data.local.dao.ItemDao
import lt.skautai.android.data.local.dao.LocationDao
import lt.skautai.android.data.local.dao.MemberDao
import lt.skautai.android.data.local.dao.OrganizationalUnitDao
import lt.skautai.android.data.local.dao.PendingOperationDao
import lt.skautai.android.data.local.dao.RequisitionDao
import lt.skautai.android.data.local.dao.ReservationDao

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `locations` (`id` TEXT NOT NULL, `tuntasId` TEXT NOT NULL, `name` TEXT NOT NULL, `address` TEXT, `description` TEXT, `createdAt` TEXT NOT NULL, PRIMARY KEY(`id`))
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `members` (`tuntasId` TEXT NOT NULL, `userId` TEXT NOT NULL, `name` TEXT NOT NULL, `surname` TEXT NOT NULL, `email` TEXT NOT NULL, `phone` TEXT, `joinedAt` TEXT NOT NULL, `unitAssignmentsJson` TEXT NOT NULL, `leadershipRolesJson` TEXT NOT NULL, `leadershipRoleHistoryJson` TEXT NOT NULL, `ranksJson` TEXT NOT NULL, PRIMARY KEY(`tuntasId`, `userId`))
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reservations` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `tuntasId` TEXT NOT NULL, `reservedByUserId` TEXT NOT NULL, `reservedByName` TEXT, `approvedByUserId` TEXT, `requestingUnitId` TEXT, `requestingUnitName` TEXT, `eventId` TEXT, `totalItems` INTEGER NOT NULL, `totalQuantity` INTEGER NOT NULL, `startDate` TEXT NOT NULL, `endDate` TEXT NOT NULL, `status` TEXT NOT NULL, `unitReviewStatus` TEXT, `unitReviewedByUserId` TEXT, `unitReviewedAt` TEXT, `topLevelReviewStatus` TEXT, `topLevelReviewedByUserId` TEXT, `topLevelReviewedAt` TEXT, `pickupAt` TEXT, `pickupProposalStatus` TEXT NOT NULL, `pickupProposedAt` TEXT, `pickupProposedByUserId` TEXT, `pickupRespondedAt` TEXT, `pickupRespondedByUserId` TEXT, `returnAt` TEXT, `returnProposalStatus` TEXT NOT NULL, `returnProposedAt` TEXT, `returnProposedByUserId` TEXT, `returnRespondedAt` TEXT, `returnRespondedByUserId` TEXT, `notes` TEXT, `itemsJson` TEXT NOT NULL, `itemIdsIndex` TEXT NOT NULL, `createdAt` TEXT NOT NULL, `updatedAt` TEXT NOT NULL, PRIMARY KEY(`id`))
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `bendras_requests` (`id` TEXT NOT NULL, `tuntasId` TEXT NOT NULL, `requestedByUserId` TEXT NOT NULL, `itemId` TEXT, `itemName` TEXT NOT NULL, `itemDescription` TEXT, `quantity` INTEGER NOT NULL, `neededByDate` TEXT, `requestingUnitId` TEXT, `requestingUnitName` TEXT, `needsDraugininkasApproval` INTEGER NOT NULL, `draugininkasStatus` TEXT, `draugininkasReviewedByUserId` TEXT, `draugininkasRejectionReason` TEXT, `topLevelStatus` TEXT NOT NULL, `topLevelReviewedByUserId` TEXT, `topLevelRejectionReason` TEXT, `notes` TEXT, `itemsJson` TEXT NOT NULL, `createdAt` TEXT NOT NULL, `updatedAt` TEXT NOT NULL, PRIMARY KEY(`id`))
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `requisitions` (`id` TEXT NOT NULL, `tuntasId` TEXT NOT NULL, `createdByUserId` TEXT NOT NULL, `requestingUnitId` TEXT, `requestingUnitName` TEXT, `status` TEXT NOT NULL, `unitReviewStatus` TEXT NOT NULL, `unitReviewedByUserId` TEXT, `unitReviewedAt` TEXT, `topLevelReviewStatus` TEXT NOT NULL, `topLevelReviewedByUserId` TEXT, `topLevelReviewedAt` TEXT, `reviewLevel` TEXT NOT NULL, `lastAction` TEXT NOT NULL, `neededByDate` TEXT, `notes` TEXT, `itemsJson` TEXT NOT NULL, `createdAt` TEXT NOT NULL, `updatedAt` TEXT NOT NULL, PRIMARY KEY(`id`))
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `events` (`id` TEXT NOT NULL, `tuntasId` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `startDate` TEXT NOT NULL, `endDate` TEXT NOT NULL, `locationId` TEXT, `organizationalUnitId` TEXT, `createdByUserId` TEXT, `status` TEXT NOT NULL, `notes` TEXT, `createdAt` TEXT NOT NULL, `eventRolesJson` TEXT NOT NULL, `stovyklaDetailsJson` TEXT, PRIMARY KEY(`id`))
                    """.trimIndent()
                )
            }
        }
        val migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pending_operations` (`id` TEXT NOT NULL, `tuntasId` TEXT NOT NULL, `entityType` TEXT NOT NULL, `entityId` TEXT NOT NULL, `operationType` TEXT NOT NULL, `payloadJson` TEXT NOT NULL, `createdAt` TEXT NOT NULL, `attemptCount` INTEGER NOT NULL, `lastError` TEXT, `status` TEXT NOT NULL, PRIMARY KEY(`id`))
                    """.trimIndent()
                )
            }
        }
        val migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `pending_operations` ADD COLUMN `userId` TEXT NOT NULL DEFAULT ''")
            }
        }
        val migration4To5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reservations_eventId` ON `reservations`(`eventId`)")
            }
        }

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "skautai_inventory.db"
        )
            .addMigrations(migration1To2, migration2To3, migration3To4, migration4To5)
            .build()
    }

    @Provides
    fun provideItemDao(database: AppDatabase): ItemDao = database.itemDao()

    @Provides
    fun provideOrganizationalUnitDao(database: AppDatabase): OrganizationalUnitDao =
        database.organizationalUnitDao()

    @Provides
    fun provideLocationDao(database: AppDatabase): LocationDao = database.locationDao()

    @Provides
    fun provideMemberDao(database: AppDatabase): MemberDao = database.memberDao()

    @Provides
    fun provideReservationDao(database: AppDatabase): ReservationDao = database.reservationDao()

    @Provides
    fun provideBendrasRequestDao(database: AppDatabase): BendrasRequestDao =
        database.bendrasRequestDao()

    @Provides
    fun provideRequisitionDao(database: AppDatabase): RequisitionDao = database.requisitionDao()

    @Provides
    fun provideEventDao(database: AppDatabase): EventDao = database.eventDao()

    @Provides
    fun providePendingOperationDao(database: AppDatabase): PendingOperationDao =
        database.pendingOperationDao()
}
