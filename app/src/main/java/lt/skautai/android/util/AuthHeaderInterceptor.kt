package lt.skautai.android.util

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthHeaderInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        if (!path.startsWith("/api/") || path == "/api/auth/login" || path == "/api/auth/register" ||
            path == "/api/auth/register/invite" || path == "/api/auth/refresh"
        ) {
            return chain.proceed(request)
        }

        val builder = request.newBuilder()
        runBlocking {
            if (request.header("Authorization").isNullOrBlank()) {
                tokenManager.token.first()?.takeIf { it.isNotBlank() }?.let {
                    builder.header("Authorization", "Bearer $it")
                }
            }

            val shouldAttachTuntasHeader = !path.startsWith("/api/super-admin")
            if (shouldAttachTuntasHeader && request.header("X-Tuntas-Id").isNullOrBlank()) {
                tokenManager.activeTuntasId.first()?.takeIf { it.isNotBlank() }?.let {
                    builder.header("X-Tuntas-Id", it)
                }
            }
        }

        return chain.proceed(builder.build())
    }
}
