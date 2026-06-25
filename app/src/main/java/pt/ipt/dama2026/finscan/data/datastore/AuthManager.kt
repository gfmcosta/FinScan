package pt.ipt.dama2026.finscan.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val PREFERENCES_NAME = "finscan_auth"

private val Context.authDataStore by preferencesDataStore(
    name = PREFERENCES_NAME
)

class AuthManager private constructor(private val context: Context) {

    companion object {
        private var instance: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }
        }

        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val USERNAME_KEY = stringPreferencesKey("auth_username")
        private val NAME_KEY = stringPreferencesKey("auth_name")
        private val EMAIL_KEY = stringPreferencesKey("auth_email")
        private val AVATAR_KEY = stringPreferencesKey("auth_avatar")
    }

    // Flow para observar o token
    val authToken: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }

    val refreshToken: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[REFRESH_TOKEN_KEY]
    }

    // Flow para observar o username
    val username: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[USERNAME_KEY]
    }

    // Flow para observar o nome real do utilizador
    val name: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[NAME_KEY]
    }

    val email: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[EMAIL_KEY]
    }

    val avatar: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[AVATAR_KEY]
    }

    // Flow para verificar se está autenticado
    val isLoggedIn: Flow<Boolean> = context.authDataStore.data.map { preferences ->
        preferences[TOKEN_KEY] != null
    }

    // Guardar token após login
    suspend fun saveToken(token: String, username: String, name: String? = null, refreshToken: String? = null, email: String? = null) {
        context.authDataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[USERNAME_KEY] = username
            name?.let { preferences[NAME_KEY] = it }
            refreshToken?.let { preferences[REFRESH_TOKEN_KEY] = it }
            email?.let { preferences[EMAIL_KEY] = it }
        }
    }

    // Atualizar nome e email após edição do perfil
    suspend fun updateProfile(name: String, email: String) {
        context.authDataStore.edit { preferences ->
            preferences[NAME_KEY] = name
            preferences[EMAIL_KEY] = email
        }
    }

    suspend fun updateUsername(username: String) {
        context.authDataStore.edit { preferences ->
            preferences[USERNAME_KEY] = username
        }
    }

    suspend fun updateAvatar(avatarBase64: String?) {
        context.authDataStore.edit { preferences ->
            if (avatarBase64 != null) preferences[AVATAR_KEY] = avatarBase64
            else preferences.remove(AVATAR_KEY)
        }
    }

    // Atualizar apenas os tokens (usado no refresh silencioso)
    suspend fun updateTokens(accessToken: String, refreshToken: String) {
        context.authDataStore.edit { preferences ->
            preferences[TOKEN_KEY] = accessToken
            preferences[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    // Remover token ao fazer logout
    suspend fun clearAuth() {
        context.authDataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
            preferences.remove(USERNAME_KEY)
            preferences.remove(NAME_KEY)
            preferences.remove(EMAIL_KEY)
            preferences.remove(AVATAR_KEY)
        }
    }

    suspend fun getRefreshTokenSync(): String? {
        return try {
            refreshToken.first()
        } catch (e: Exception) {
            null
        }
    }

    // Obter token sincronamente (usar com cuidado)
    suspend fun getTokenSync(): String? {
        return try {
            authToken.first()
        } catch (e: Exception) {
            null
        }
    }
}
