package pt.ipt.dama2026.finscan.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
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
        private val USERNAME_KEY = stringPreferencesKey("auth_username")
        private val NAME_KEY = stringPreferencesKey("auth_name")
    }

    // Flow para observar o token
    val authToken: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }

    // Flow para observar o username
    val username: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[USERNAME_KEY]
    }

    // Flow para observar o nome real do utilizador
    val name: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[NAME_KEY]
    }

    // Flow para verificar se está autenticado
    val isLoggedIn: Flow<Boolean> = context.authDataStore.data.map { preferences ->
        preferences[TOKEN_KEY] != null
    }

    // Guardar token após login
    suspend fun saveToken(token: String, username: String, name: String? = null) {
        context.authDataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[USERNAME_KEY] = username
            name?.let { preferences[NAME_KEY] = it }
        }
    }

    // Remover token ao fazer logout
    suspend fun clearAuth() {
        context.authDataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(USERNAME_KEY)
            preferences.remove(NAME_KEY)
        }
    }

    // Obter token sincronamente (usar com cuidado)
    suspend fun getTokenSync(): String? {
        return authToken.firstOrNull()
    }
}
