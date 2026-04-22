package lt.skautai.android.data.remote

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class UploadResponseDto(
    val url: String
)

interface UploadApiService {
    @Multipart
    @POST("api/uploads/images")
    suspend fun uploadImage(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part
    ): Response<UploadResponseDto>
}
