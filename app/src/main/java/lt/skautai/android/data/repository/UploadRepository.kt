package lt.skautai.android.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import lt.skautai.android.data.remote.UploadApiService
import lt.skautai.android.util.TokenManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class UploadRepository @Inject constructor(
    private val uploadApiService: UploadApiService,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) {
    suspend fun uploadImage(uri: Uri): Result<String> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: "image/jpeg"
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return Result.failure(Exception("Nepavyko perskaityti nuotraukos"))
            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData(
                name = "file",
                filename = "inventory-photo.${mimeType.substringAfter('/', "jpg")}",
                body = requestBody
            )

            val response = uploadApiService.uploadImage("Bearer $token", filePart)
            if (response.isSuccessful) {
                Result.success(response.body()!!.url)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Nepavyko ikelti nuotraukos"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
