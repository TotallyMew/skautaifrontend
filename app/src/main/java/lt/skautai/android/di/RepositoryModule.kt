package lt.skautai.android.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import lt.skautai.android.data.remote.AuthApiService
import lt.skautai.android.data.remote.ItemApiService
import lt.skautai.android.data.remote.UserApiService
import lt.skautai.android.data.repository.AuthRepository
import lt.skautai.android.data.repository.ItemRepository
import lt.skautai.android.data.repository.UserRepository
import lt.skautai.android.util.TokenManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        authApiService: AuthApiService,
        tokenManager: TokenManager
    ): AuthRepository {
        return AuthRepository(authApiService, tokenManager)
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
}