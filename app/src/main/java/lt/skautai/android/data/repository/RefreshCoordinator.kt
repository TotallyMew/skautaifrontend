package lt.skautai.android.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import lt.skautai.android.data.local.dao.CacheMetadataDao
import lt.skautai.android.data.local.entity.CacheMetadataEntity
import lt.skautai.android.util.TokenManager
import kotlinx.coroutines.flow.first

enum class CacheTtl(val millis: Long) {
    DASHBOARD(30_000),
    LIST(120_000),
    REFERENCE(600_000),
    DETAIL(30_000),
    EVENT_OPERATIONAL(15_000)
}

@Singleton
class RefreshCoordinator @Inject constructor(
    private val cacheMetadataDao: CacheMetadataDao,
    private val tokenManager: TokenManager
) {
    suspend fun shouldRefresh(
        resource: String,
        queryKey: String = DEFAULT_QUERY_KEY,
        ttl: CacheTtl,
        force: Boolean = false
    ): Boolean {
        if (force) return true
        val tuntasId = tokenManager.activeTuntasId.first() ?: return true
        val metadata = cacheMetadataDao.get(tuntasId, resource, queryKey) ?: return true
        val refreshedAt = metadata.lastSuccessfulRefreshAt ?: return true
        return System.currentTimeMillis() - refreshedAt >= ttl.millis
    }

    suspend fun recordAttempt(
        resource: String,
        queryKey: String = DEFAULT_QUERY_KEY,
        success: Boolean,
        error: String? = null
    ) {
        val tuntasId = tokenManager.activeTuntasId.first() ?: return
        val now = System.currentTimeMillis()
        val previous = cacheMetadataDao.get(tuntasId, resource, queryKey)
        cacheMetadataDao.upsert(
            CacheMetadataEntity(
                tuntasId = tuntasId,
                resource = resource,
                queryKey = queryKey,
                lastSuccessfulRefreshAt = if (success) now else previous?.lastSuccessfulRefreshAt,
                lastAttemptAt = now,
                lastError = if (success) null else error
            )
        )
    }

    suspend fun lastSuccessfulRefreshInstant(
        resource: String,
        queryKey: String = DEFAULT_QUERY_KEY
    ): String? {
        val tuntasId = tokenManager.activeTuntasId.first() ?: return null
        val refreshedAt = cacheMetadataDao.get(tuntasId, resource, queryKey)
            ?.lastSuccessfulRefreshAt
            ?: return null
        return java.time.Instant.ofEpochMilli(refreshedAt - INCREMENTAL_REFRESH_SAFETY_WINDOW_MILLIS).toString()
    }

    companion object {
        const val DEFAULT_QUERY_KEY = "default"
        private const val INCREMENTAL_REFRESH_SAFETY_WINDOW_MILLIS = 5_000L
    }
}
