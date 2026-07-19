package lt.skautai.android.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface InvitationApiService {

    @GET("api/invitations/options")
    suspend fun getInvitationOptions(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String
    ): Response<InvitationOptionsDto>

    @POST("api/invitations")
    suspend fun createInvitation(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Body request: CreateInvitationRequestDto
    ): Response<InvitationResponseDto>

    @POST("api/invitations/accept")
    suspend fun acceptInvitation(
        @Header("Authorization") token: String,
        @Body request: AcceptInvitationRequestDto
    ): Response<InvitationResponseDto>
}
