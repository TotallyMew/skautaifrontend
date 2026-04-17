package lt.skautai.android.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface MemberApiService {

    @GET("api/members")
    suspend fun getMembers(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String
    ): Response<MemberListDto>

    @GET("api/members/{userId}")
    suspend fun getMember(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Path("userId") userId: String
    ): Response<MemberDto>
}