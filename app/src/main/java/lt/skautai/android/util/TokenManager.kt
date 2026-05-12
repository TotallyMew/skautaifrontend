package lt.skautai.android.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val TOKEN_KEY = stringPreferencesKey("jwt_token")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        val USER_ID_KEY = stringPreferencesKey("user_id")
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        val USER_TYPE_KEY = stringPreferencesKey("user_type")
        val ACTIVE_TUNTAS_ID_KEY = stringPreferencesKey("active_tuntas_id")
        val ACTIVE_TUNTAS_NAME_KEY = stringPreferencesKey("active_tuntas_name")
        val ACTIVE_ORG_UNIT_ID_KEY = stringPreferencesKey("active_org_unit_id")
        val PERMISSIONS_KEY = stringPreferencesKey("permissions")
        // Per-tuntas cache: "tuntasId1=perm1,perm2;tuntasId2=perm3" (max 5 entries)
        val PERMISSIONS_CACHE_KEY = stringPreferencesKey("permissions_cache")
        val LEADERSHIP_UNIT_IDS_KEY = stringPreferencesKey("leadership_unit_ids")
    }

    private fun enc(value: String?) = EncryptionHelper.encrypt(value ?: "")
    private fun dec(value: String?) = value?.let { EncryptionHelper.decrypt(it) }?.takeIf { it.isNotEmpty() }

    val token: Flow<String?> = context.dataStore.data.map { dec(it[TOKEN_KEY]) }
    val refreshToken: Flow<String?> = context.dataStore.data.map { dec(it[REFRESH_TOKEN_KEY]) }
    val userId: Flow<String?> = context.dataStore.data.map { dec(it[USER_ID_KEY]) }
    val userName: Flow<String?> = context.dataStore.data.map { dec(it[USER_NAME_KEY]) }
    val userEmail: Flow<String?> = context.dataStore.data.map { dec(it[USER_EMAIL_KEY]) }
    val userType: Flow<String?> = context.dataStore.data.map { dec(it[USER_TYPE_KEY]) }
    val activeTuntasId: Flow<String?> = context.dataStore.data.map { dec(it[ACTIVE_TUNTAS_ID_KEY]) }
    val activeTuntasName: Flow<String?> = context.dataStore.data.map { dec(it[ACTIVE_TUNTAS_NAME_KEY]) }
    val activeOrgUnitId: Flow<String?> = context.dataStore.data.map { dec(it[ACTIVE_ORG_UNIT_ID_KEY]) }
    val permissions: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        dec(prefs[PERMISSIONS_KEY])?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    }
    val leadershipUnitIds: Flow<List<String>> = context.dataStore.data.map { prefs ->
        dec(prefs[LEADERSHIP_UNIT_IDS_KEY])?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun saveToken(
        token: String,
        refreshToken: String?,
        userId: String,
        name: String,
        email: String,
        type: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = enc(token)
            if (refreshToken.isNullOrBlank()) {
                prefs.remove(REFRESH_TOKEN_KEY)
            } else {
                prefs[REFRESH_TOKEN_KEY] = enc(refreshToken)
            }
            prefs[USER_ID_KEY] = enc(userId)
            prefs[USER_NAME_KEY] = enc(name)
            prefs[USER_EMAIL_KEY] = enc(email)
            prefs[USER_TYPE_KEY] = enc(type)
        }
    }

    suspend fun updateTokens(token: String, refreshToken: String?) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = enc(token)
            if (refreshToken.isNullOrBlank()) {
                prefs.remove(REFRESH_TOKEN_KEY)
            } else {
                prefs[REFRESH_TOKEN_KEY] = enc(refreshToken)
            }
        }
    }

    suspend fun setActiveTuntas(tuntasId: String, tuntasName: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[ACTIVE_TUNTAS_ID_KEY] = enc(tuntasId)
            if (tuntasName.isNullOrBlank()) {
                prefs.remove(ACTIVE_TUNTAS_NAME_KEY)
            } else {
                prefs[ACTIVE_TUNTAS_NAME_KEY] = enc(tuntasName)
            }
        }
    }

    suspend fun clearActiveTuntas() {
        context.dataStore.edit { prefs ->
            prefs.remove(ACTIVE_TUNTAS_ID_KEY)
            prefs.remove(ACTIVE_TUNTAS_NAME_KEY)
            prefs.remove(ACTIVE_ORG_UNIT_ID_KEY)
            prefs.remove(PERMISSIONS_KEY)
            prefs.remove(LEADERSHIP_UNIT_IDS_KEY)
        }
    }

    suspend fun setActiveOrgUnit(orgUnitId: String?) {
        context.dataStore.edit { prefs ->
            if (orgUnitId.isNullOrBlank()) {
                prefs.remove(ACTIVE_ORG_UNIT_ID_KEY)
            } else {
                prefs[ACTIVE_ORG_UNIT_ID_KEY] = enc(orgUnitId)
            }
        }
    }

    suspend fun savePermissions(perms: List<String>) {
        context.dataStore.edit { it[PERMISSIONS_KEY] = enc(perms.joinToString(",")) }
    }

    suspend fun saveLeadershipUnitIds(ids: List<String>) {
        context.dataStore.edit { it[LEADERSHIP_UNIT_IDS_KEY] = enc(ids.joinToString(",")) }
    }

    suspend fun updateUserIdentity(
        name: String,
        email: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[USER_NAME_KEY] = enc(name)
            prefs[USER_EMAIL_KEY] = enc(email)
        }
    }

    suspend fun permissionsForTuntas(tuntasId: String): Set<String>? {
        val raw = dec(context.dataStore.data.first()[PERMISSIONS_CACHE_KEY]) ?: return null
        val entry = raw.split(";").firstOrNull { it.startsWith("$tuntasId=") } ?: return null
        val permsStr = entry.substringAfter("=")
        return permsStr.split(",").filter { it.isNotBlank() }.toSet()
    }

    suspend fun cachePermissionsForTuntas(tuntasId: String, perms: List<String>) {
        context.dataStore.edit { prefs ->
            val existing = dec(prefs[PERMISSIONS_CACHE_KEY]) ?: ""
            val entries = existing.split(";")
                .filter { it.isNotBlank() && !it.startsWith("$tuntasId=") }
                .toMutableList()
            entries.add("$tuntasId=${perms.joinToString(",")}")
            val limited = if (entries.size > 5) entries.takeLast(5) else entries
            prefs[PERMISSIONS_CACHE_KEY] = enc(limited.joinToString(";"))
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
