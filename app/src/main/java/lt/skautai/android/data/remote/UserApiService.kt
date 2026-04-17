package lt.skautai.android.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface UserApiService {

    @GET("api/users/me/tuntai")
    suspend fun getMyTuntai(
        @Header("Authorization") token: String
    ): Response<List<UserTuntasDto>>
}