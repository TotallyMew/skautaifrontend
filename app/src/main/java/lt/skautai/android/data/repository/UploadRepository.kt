package lt.skautai.android.data.repository

import lt.skautai.android.util.userFacingException

import android.content.Context
import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import java.io.File
import java.io.IOException
import java.util.UUID
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import lt.skautai.android.data.remote.UploadApiService
import lt.skautai.android.util.TokenManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import lt.skautai.android.util.Constants
import lt.skautai.android.util.errorMessage

@Singleton
class UploadRepository @Inject constructor(
    private val uploadApiService: UploadApiService,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val STAGED_DOCUMENT_PREFIX = "staged-document://"

        fun isStagedDocumentUrl(url: String): Boolean = url.startsWith(STAGED_DOCUMENT_PREFIX)
    }

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
                Result.failure(Exception(response.errorMessage("Nepavyko ikelti nuotraukos")))
            }
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    suspend fun uploadDocument(uri: Uri): Result<String> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: "application/pdf"
            val extension = extensionForMimeType(mimeType)
            val displayName = displayName(uri) ?: "invoice.$extension"
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return Result.failure(Exception("Nepavyko perskaityti dokumento"))
            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData(
                name = "file",
                filename = displayName,
                body = requestBody
            )

            val response = uploadApiService.uploadDocument("Bearer $token", filePart)
            if (response.isSuccessful) {
                Result.success(response.body()!!.url)
            } else {
                Result.failure(Exception(response.errorMessage("Nepavyko ikelti dokumento")))
            }
        } catch (e: IOException) {
            stageDocument(uri)
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    suspend fun downloadEventPurchaseInvoice(eventId: String, purchaseId: String, invoiceFileUrl: String?): Result<Long> {
        return try {
            val token = tokenManager.token.first()
                ?: return Result.failure(Exception("Nav prisijungta"))
            val tuntasId = tokenManager.activeTuntasId.first()
                ?: return Result.failure(Exception("Tuntas nepasirinktas"))
            val url = Constants.BASE_URL.trimEnd('/') +
                "/api/events/$eventId/purchases/$purchaseId/invoice/download"
            val request = DownloadManager.Request(Uri.parse(url))
                .addRequestHeader("Authorization", "Bearer $token")
                .addRequestHeader("X-Tuntas-Id", tuntasId)
                .setTitle("Sąskaita faktūra")
                .setDescription("Renginio pirkimo sąskaita")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "saskaita-$purchaseId.${invoiceExtension(invoiceFileUrl)}"
                )
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            Result.success(manager.enqueue(request))
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }

    private fun invoiceExtension(invoiceFileUrl: String?): String {
        val extension = invoiceFileUrl?.substringAfterLast('.', "")?.lowercase().orEmpty()
        return if (extension in setOf("pdf", "jpg", "jpeg", "png")) extension else "pdf"
    }

    private fun extensionForMimeType(mimeType: String): String {
        return when (mimeType.lowercase()) {
            "application/pdf" -> "pdf"
            "image/png" -> "png"
            "image/jpeg", "image/jpg" -> "jpg"
            else -> "pdf"
        }
    }

    private fun displayName(uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        }
    }

    private fun stageDocument(uri: Uri): Result<String> {
        return try {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: "application/pdf"
            val extension = extensionForMimeType(mimeType)
            val originalName = displayName(uri) ?: "invoice.$extension"
            val safeName = originalName.substringAfterLast('/').ifBlank { "invoice.$extension" }
            val stagedDir = File(context.filesDir, "staged-documents").apply { mkdirs() }
            val stagedFile = File(stagedDir, "${UUID.randomUUID()}-$safeName")
            resolver.openInputStream(uri)?.use { input ->
                stagedFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return Result.failure(Exception("Nepavyko perskaityti dokumento"))
            Result.success(
                "$STAGED_DOCUMENT_PREFIX${Uri.encode(stagedFile.absolutePath)}?name=${Uri.encode(safeName)}&mime=${Uri.encode(mimeType)}"
            )
        } catch (e: Exception) {
            Result.failure(e.userFacingException())
        }
    }
}
