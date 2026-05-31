package lt.skautai.android.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import lt.skautai.android.data.remote.AuthApiService
import lt.skautai.android.data.local.dao.ItemDao
import lt.skautai.android.data.local.dao.LocationDao
import lt.skautai.android.data.local.dao.MemberDao
import lt.skautai.android.data.local.dao.OrganizationalUnitDao
import lt.skautai.android.data.local.dao.ReservationDao
import lt.skautai.android.data.local.dao.BendrasRequestDao
import lt.skautai.android.data.local.dao.RequisitionDao
import lt.skautai.android.data.local.dao.EventDao
import lt.skautai.android.data.remote.ItemApiService
import lt.skautai.android.data.remote.OrganizationalUnitApiService
import lt.skautai.android.data.remote.UserApiService
import lt.skautai.android.data.repository.AuthRepository
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.data.repository.OrganizationalUnitRepository
import lt.skautai.android.data.repository.UserRepository
import lt.skautai.android.util.TokenManager
import javax.inject.Singleton
import lt.skautai.android.data.remote.MemberApiService
import lt.skautai.android.data.remote.MyTaskApiService
import lt.skautai.android.data.remote.RoleApiService
import lt.skautai.android.data.remote.InvitationApiService
import lt.skautai.android.data.remote.RequestApiService
import lt.skautai.android.data.remote.RequisitionApiService
import lt.skautai.android.data.remote.ReservationApiService
import lt.skautai.android.data.remote.EventApiService
import lt.skautai.android.data.remote.LocationApiService
import lt.skautai.android.data.remote.SuperAdminApiService
import lt.skautai.android.data.repository.MemberRepository
import lt.skautai.android.data.repository.MyTaskRepository
import lt.skautai.android.data.repository.RoleRepository
import lt.skautai.android.data.repository.InvitationRepository
import lt.skautai.android.data.repository.RequestRepository
import lt.skautai.android.data.repository.RequisitionRepository
import lt.skautai.android.data.repository.ReservationRepository
import lt.skautai.android.data.repository.EventRepository
import lt.skautai.android.data.repository.LocationRepository
import lt.skautai.android.data.repository.RefreshCoordinator
import lt.skautai.android.data.repository.SuperAdminRepository
import lt.skautai.android.data.sync.PendingOperationRepository

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        authApiService: AuthApiService,
        tokenManager: TokenManager,
        userRepository: UserRepository
    ): AuthRepository {
        return AuthRepository(authApiService, tokenManager, userRepository)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        userApiService: UserApiService,
        tokenManager: TokenManager
    ): UserRepository {
        return UserRepository(userApiService, tokenManager)
    }

    @Provides
    @Singleton
    fun provideItemRepository(
        itemApiService: ItemApiService,
        tokenManager: TokenManager,
        itemDao: ItemDao,
        pendingOperationRepository: PendingOperationRepository,
        refreshCoordinator: RefreshCoordinator
    ): ItemRepository {
        return ItemRepository(itemApiService, tokenManager, itemDao, pendingOperationRepository, refreshCoordinator)
    }

    @Provides
    @Singleton
    fun provideOrganizationalUnitRepository(
        orgUnitApiService: OrganizationalUnitApiService,
        tokenManager: TokenManager,
        organizationalUnitDao: OrganizationalUnitDao,
        memberDao: MemberDao,
        pendingOperationRepository: PendingOperationRepository,
        refreshCoordinator: RefreshCoordinator
    ): OrganizationalUnitRepository {
        return OrganizationalUnitRepository(
            orgUnitApiService,
            tokenManager,
            organizationalUnitDao,
            memberDao,
            pendingOperationRepository,
            refreshCoordinator
        )
    }

    @Provides
    @Singleton
    fun provideMemberRepository(
        memberApiService: MemberApiService,
        tokenManager: TokenManager,
        memberDao: MemberDao,
        pendingOperationRepository: PendingOperationRepository,
        refreshCoordinator: RefreshCoordinator
    ): MemberRepository {
        return MemberRepository(memberApiService, tokenManager, memberDao, pendingOperationRepository, refreshCoordinator)
    }

    @Provides
    @Singleton
    fun provideRoleRepository(
        roleApiService: RoleApiService,
        tokenManager: TokenManager
    ): RoleRepository {
        return RoleRepository(roleApiService, tokenManager)
    }

    @Provides
    @Singleton
    fun provideInvitationRepository(
        invitationApiService: InvitationApiService,
        tokenManager: TokenManager
    ): InvitationRepository {
        return InvitationRepository(invitationApiService, tokenManager)
    }

    @Provides
    @Singleton
    fun provideReservationRepository(
        reservationApiService: ReservationApiService,
        tokenManager: TokenManager,
        reservationDao: ReservationDao,
        pendingOperationRepository: PendingOperationRepository,
        refreshCoordinator: RefreshCoordinator
    ): ReservationRepository {
        return ReservationRepository(reservationApiService, tokenManager, reservationDao, pendingOperationRepository, refreshCoordinator)
    }

    @Provides
    @Singleton
    fun provideMyTaskRepository(
        myTaskApiService: MyTaskApiService,
        tokenManager: TokenManager
    ): MyTaskRepository {
        return MyTaskRepository(myTaskApiService, tokenManager)
    }

    @Provides
    @Singleton
    fun provideRequestRepository(
        requestApiService: RequestApiService,
        tokenManager: TokenManager,
        bendrasRequestDao: BendrasRequestDao,
        pendingOperationRepository: PendingOperationRepository,
        refreshCoordinator: RefreshCoordinator
    ): RequestRepository {
        return RequestRepository(requestApiService, tokenManager, bendrasRequestDao, pendingOperationRepository, refreshCoordinator)
    }

    @Provides
    @Singleton
    fun provideRequisitionRepository(
        requisitionApiService: RequisitionApiService,
        tokenManager: TokenManager,
        requisitionDao: RequisitionDao,
        pendingOperationRepository: PendingOperationRepository,
        refreshCoordinator: RefreshCoordinator
    ): RequisitionRepository {
        return RequisitionRepository(requisitionApiService, tokenManager, requisitionDao, pendingOperationRepository, refreshCoordinator)
    }

    @Provides
    @Singleton
    fun provideEventRepository(
        eventApiService: EventApiService,
        tokenManager: TokenManager,
        eventDao: EventDao,
        pendingOperationRepository: PendingOperationRepository,
        refreshCoordinator: RefreshCoordinator
    ): EventRepository {
        return EventRepository(eventApiService, tokenManager, eventDao, pendingOperationRepository, refreshCoordinator)
    }

    @Provides
    @Singleton
    fun provideLocationRepository(
        locationApiService: LocationApiService,
        tokenManager: TokenManager,
        locationDao: LocationDao,
        pendingOperationRepository: PendingOperationRepository,
        refreshCoordinator: RefreshCoordinator
    ): LocationRepository {
        return LocationRepository(locationApiService, tokenManager, locationDao, pendingOperationRepository, refreshCoordinator)
    }

    @Provides
    @Singleton
    fun provideSuperAdminRepository(
        superAdminApiService: SuperAdminApiService,
        tokenManager: TokenManager
    ): SuperAdminRepository {
        return SuperAdminRepository(superAdminApiService, tokenManager)
    }
}
