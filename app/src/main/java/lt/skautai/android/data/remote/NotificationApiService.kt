package lt.skautai.android.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApiService {
    @GET("api/notifications")
    suspend fun getNotifications(
        @Header("Authorization") token: String,
        @Query("unreadOnly") unreadOnly: Boolean = false
    ): Response<NotificationListDto>

    @POST("api/notifications/{id}/read")
    suspend fun markRead(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<MessageResponseDto>

    @POST("api/notifications/read-all")
    suspend fun markAllRead(
        @Header("Authorization") token: String
    ): Response<MessageResponseDto>
}

