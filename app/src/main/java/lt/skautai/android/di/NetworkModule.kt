package lt.skautai.android.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import lt.skautai.android.BuildConfig
import lt.skautai.android.data.remote.AuthApiService
import lt.skautai.android.data.remote.EventApiService
import lt.skautai.android.data.remote.InvitationApiService
import lt.skautai.android.data.remote.ItemApiService
import lt.skautai.android.data.remote.LocationApiService
import lt.skautai.android.data.remote.MemberApiService
import lt.skautai.android.data.remote.OrganizationalUnitApiService
import lt.skautai.android.data.remote.RequestApiService
import lt.skautai.android.data.remote.RequisitionApiService
import lt.skautai.android.data.remote.ReservationApiService
import lt.skautai.android.data.remote.RoleApiService
import lt.skautai.android.data.remote.SuperAdminApiService
import lt.skautai.android.data.remote.UploadApiService
import lt.skautai.android.data.remote.UserApiService
import lt.skautai.android.util.Constants
import lt.skautai.android.util.AuthHeaderInterceptor
import lt.skautai.android.util.NetworkErrorInterceptor
import lt.skautai.android.util.TokenManager
import lt.skautai.android.util.TokenRefreshAuthenticator
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager {
        return TokenManager(context)
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideNetworkErrorInterceptor(): NetworkErrorInterceptor = NetworkErrorInterceptor()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        networkErrorInterceptor: NetworkErrorInterceptor,
        authHeaderInterceptor: AuthHeaderInterceptor,
        tokenRefreshAuthenticator: TokenRefreshAuthenticator
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .authenticator(tokenRefreshAuthenticator)
            .addInterceptor(networkErrorInterceptor)
            .addInterceptor(authHeaderInterceptor)
            .addInterceptor(loggingInterceptor)

        val apiHost = BuildConfig.API_HOST.trim()
        val apiCertPin = BuildConfig.API_CERT_PIN.trim()
        if (Constants.BASE_URL.startsWith("https://") && apiHost.isNotEmpty() && apiCertPin.startsWith("sha256/")) {
            builder.certificatePinner(
                CertificatePinner.Builder()
                    .add(apiHost, apiCertPin)
                    .build()
            )
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideUserApiService(retrofit: Retrofit): UserApiService {
        return retrofit.create(UserApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideItemApiService(retrofit: Retrofit): ItemApiService {
        return retrofit.create(ItemApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideOrganizationalUnitApiService(retrofit: Retrofit): OrganizationalUnitApiService {
        return retrofit.create(OrganizationalUnitApiService::class.java)
    }
    @Provides
    @Singleton
    fun provideMemberApiService(retrofit: Retrofit): MemberApiService {
        return retrofit.create(MemberApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRoleApiService(retrofit: Retrofit): RoleApiService {
        return retrofit.create(RoleApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideInvitationApiService(retrofit: Retrofit): InvitationApiService {
        return retrofit.create(InvitationApiService::class.java)
    }


    @Provides
    @Singleton
    fun provideReservationApiService(retrofit: Retrofit): ReservationApiService {
        return retrofit.create(ReservationApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRequestApiService(retrofit: Retrofit): RequestApiService {
        return retrofit.create(RequestApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRequisitionApiService(retrofit: Retrofit): RequisitionApiService {
        return retrofit.create(RequisitionApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideEventApiService(retrofit: Retrofit): EventApiService {
        return retrofit.create(EventApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideLocationApiService(retrofit: Retrofit): LocationApiService {
        return retrofit.create(LocationApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideUploadApiService(retrofit: Retrofit): UploadApiService {
        return retrofit.create(UploadApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideSuperAdminApiService(retrofit: Retrofit): SuperAdminApiService {
        return retrofit.create(SuperAdminApiService::class.java)
    }
}
