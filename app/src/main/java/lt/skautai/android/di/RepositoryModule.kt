package lt.skautai.android.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import lt.skautai.android.data.remote.AuthApiService
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
import lt.skautai.android.data.remote.RoleApiService
import lt.skautai.android.data.remote.InvitationApiService
import lt.skautai.android.data.remote.RequestApiService
import lt.skautai.android.data.remote.RequisitionApiService
import lt.skautai.android.data.remote.ReservationApiService
import lt.skautai.android.data.remote.EventApiService
import lt.skautai.android.data.remote.LocationApiService
import lt.skautai.android.data.repository.MemberRepository
import lt.skautai.android.data.repository.RoleRepository
import lt.skautai.android.data.repository.InvitationRepository
import lt.skautai.android.data.repository.RequestRepository
import lt.skautai.android.data.repository.RequisitionRepository
import lt.skautai.android.data.repository.ReservationRepository
import lt.skautai.android.data.repository.EventRepository
import lt.skautai.android.data.repository.LocationRepository

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
        tokenManager: TokenManager
    ): ItemRepository {
        return ItemRepository(itemApiService, tokenManager)
    }

    @Provides
    @Singleton
    fun provideOrganizationalUnitRepository(
        orgUnitApiService: OrganizationalUnitApiService,
        tokenManager: TokenManager
    ): OrganizationalUnitRepository {
        return OrganizationalUnitRepository(orgUnitApiService, tokenManager)
    }

    @Provides
    @Singleton
    fun provideMemberRepository(
        memberApiService: MemberApiService,
        tokenManager: TokenManager
    ): MemberRepository {
        return MemberRepository(memberApiService, tokenManager)
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
        tokenManager: TokenManager
    ): ReservationRepository {
        return ReservationRepository(reservationApiService, tokenManager)
    }

    @Provides
    @Singleton
    fun provideRequestRepository(
        requestApiService: RequestApiService,
        tokenManager: TokenManager
    ): RequestRepository {
        return RequestRepository(requestApiService, tokenManager)
    }

    @Provides
    @Singleton
    fun provideRequisitionRepository(
        requisitionApiService: RequisitionApiService,
        tokenManager: TokenManager
    ): RequisitionRepository {
        return RequisitionRepository(requisitionApiService, tokenManager)
    }

    @Provides
    @Singleton
    fun provideEventRepository(
        eventApiService: EventApiService,
        tokenManager: TokenManager
    ): EventRepository {
        return EventRepository(eventApiService, tokenManager)
    }

    @Provides
    @Singleton
    fun provideLocationRepository(
        locationApiService: LocationApiService,
        tokenManager: TokenManager
    ): LocationRepository {
        return LocationRepository(locationApiService, tokenManager)
    }
}
