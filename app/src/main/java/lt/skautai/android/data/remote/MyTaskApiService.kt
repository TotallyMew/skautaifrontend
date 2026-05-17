package lt.skautai.android.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface MyTaskApiService {

    @GET("api/tasks/my")
    suspend fun getMyTasks(
        @Header("Authorization") token: String,
        @Header("X-Tuntas-Id") tuntasId: String
    ): Response<MyTaskListDto>
}
