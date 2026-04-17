package lt.skautai.android.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface InvitationApiService {

    @POST("api/invitations")
    suspend fun createInvitation(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String,
        @Body request: CreateInvitationRequestDto
    ): Response<InvitationResponseDto>
}