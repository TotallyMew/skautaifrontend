package lt.skautai.android.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
        val USER_ID_KEY = stringPreferencesKey("user_id")
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        val USER_TYPE_KEY = stringPreferencesKey("user_type")
        val ACTIVE_TUNTAS_ID_KEY = stringPreferencesKey("active_tuntas_id")
        val ACTIVE_TUNTAS_NAME_KEY = stringPreferencesKey("active_tuntas_name")
        val ACTIVE_ORG_UNIT_ID_KEY = stringPreferencesKey("active_org_unit_id")
        val PERMISSIONS_KEY = stringPreferencesKey("permissions")
    }

    val token: Flow<String?> = context.dataStore.data.map { it[TOKEN_KEY] }
    val userId: Flow<String?> = context.dataStore.data.map { it[USER_ID_KEY] }
    val userName: Flow<String?> = context.dataStore.data.map { it[USER_NAME_KEY] }
    val userEmail: Flow<String?> = context.dataStore.data.map { it[USER_EMAIL_KEY] }
    val userType: Flow<String?> = context.dataStore.data.map { it[USER_TYPE_KEY] }
    val activeTuntasId: Flow<String?> = context.dataStore.data.map { it[ACTIVE_TUNTAS_ID_KEY] }
    val activeTuntasName: Flow<String?> = context.dataStore.data.map { it[ACTIVE_TUNTAS_NAME_KEY] }
    val activeOrgUnitId: Flow<String?> = context.dataStore.data.map { it[ACTIVE_ORG_UNIT_ID_KEY] }
    val permissions: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[PERMISSIONS_KEY]?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    }

    suspend fun saveToken(
        token: String,
        userId: String,
        name: String,
        email: String,
        type: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[USER_ID_KEY] = userId
            prefs[USER_NAME_KEY] = name
            prefs[USER_EMAIL_KEY] = email
            prefs[USER_TYPE_KEY] = type
        }
    }

    suspend fun setActiveTuntas(tuntasId: String, tuntasName: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[ACTIVE_TUNTAS_ID_KEY] = tuntasId
            if (tuntasName.isNullOrBlank()) {
                prefs.remove(ACTIVE_TUNTAS_NAME_KEY)
            } else {
                prefs[ACTIVE_TUNTAS_NAME_KEY] = tuntasName
            }
        }
    }

    suspend fun setActiveOrgUnit(orgUnitId: String?) {
        context.dataStore.edit { prefs ->
            if (orgUnitId.isNullOrBlank()) {
                prefs.remove(ACTIVE_ORG_UNIT_ID_KEY)
            } else {
                prefs[ACTIVE_ORG_UNIT_ID_KEY] = orgUnitId
            }
        }
    }

    suspend fun savePermissions(perms: List<String>) {
        context.dataStore.edit { it[PERMISSIONS_KEY] = perms.joinToString(",") }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
