package lt.skautai.android.util

import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import lt.skautai.android.data.remote.RefreshTokenRequestDto
import lt.skautai.android.data.remote.TokenResponseDto
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenRefreshAuthenticator @Inject constructor(
    private val tokenManager: TokenManager
) : Authenticator {

    private val gson = Gson()
    private val refreshClient = OkHttpClient()

    @Synchronized
    override fun authenticate(route: okhttp3.Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null

        val path = response.request.url.encodedPath
        if (path == "/api/auth/login" || path == "/api/auth/register" ||
            path == "/api/auth/register/invite" || path == "/api/auth/refresh"
        ) {
            return null
        }

        val failedToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()

        val currentToken = runBlocking { tokenManager.token.first() }
        if (!currentToken.isNullOrBlank() && currentToken != failedToken) {
            return response.request.newBuilder()
                .header("Authorization", "Bearer $currentToken")
                .build()
        }

        val currentRefreshToken = runBlocking { tokenManager.refreshToken.first() } ?: return null
        val refreshedSession = refreshSession(currentRefreshToken) ?: run {
            runBlocking { tokenManager.clearAll() }
            return null
        }

        runBlocking {
            tokenManager.saveToken(
                token = refreshedSession.token,
                refreshToken = refreshedSession.refreshToken,
                userId = refreshedSession.userId,
                name = refreshedSession.name,
                email = refreshedSession.email,
                type = refreshedSession.type
            )
        }

        return response.request.newBuilder()
            .header("Authorization", "Bearer ${refreshedSession.token}")
            .build()
    }

    private fun refreshSession(refreshToken: String): TokenResponseDto? {
        val body = gson.toJson(RefreshTokenRequestDto(refreshToken))
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${Constants.BASE_URL}api/auth/refresh")
            .post(body)
            .build()

        refreshClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val rawBody = response.body?.string().orEmpty()
            if (rawBody.isBlank()) return null
            return gson.fromJson(rawBody, TokenResponseDto::class.java)
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}
