package pt.ipt.dama2026.finscan.data.datastore

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Preferences DataSore to store authentication data (token, username, etc.)
private const val PREFERENCES_NAME = "finscan_auth"

private val Context.authDataStore by preferencesDataStore(
    name = PREFERENCES_NAME
)

/**
 * Class to manage authentication data using DataStore.
 * It provides methods to save, update, and clear authentication tokens and user information.
 * @param context The application context used to access the DataStore.
 */
class AuthManager private constructor(private val context: Context) {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: AuthManager? = null

        /**
         * Returns instance of AuthManager.
         * @param context The application context.
         * @return AuthManager instance.
         */
        fun getInstance(context: Context): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }
        }

        // Read and write keys for authentication data
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val USERNAME_KEY = stringPreferencesKey("auth_username")
        private val NAME_KEY = stringPreferencesKey("auth_name")
        private val EMAIL_KEY = stringPreferencesKey("auth_email")
        private val AVATAR_KEY = stringPreferencesKey("auth_avatar")
    }

    // Flow to observe the authentication token
    val authToken: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }

    // Flow to observe the refresh token
    val refreshToken: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[REFRESH_TOKEN_KEY]
    }

    // Flow to observe the username
    val username: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[USERNAME_KEY]
    }

    // Flow to observe the name
    val name: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[NAME_KEY]
    }

    // Flow to observe the email
    val email: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[EMAIL_KEY]
    }

    // Flow to observe the avatar
    val avatar: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[AVATAR_KEY]
    }

    // Flow to observe if the user is logged in
    val isLoggedIn: Flow<Boolean> = context.authDataStore.data.map { preferences ->
        preferences[TOKEN_KEY] != null
    }

    /**
     * Save authentication data to DataStore.
     * @param token The authentication token.
     * @param username The username.
     * @param name The user's name.
     * @param refreshToken The refresh token.
     * @param email The user's email.
     */
    suspend fun saveToken(
        token: String,
        username: String,
        name: String? = null,
        refreshToken: String? = null,
        email: String? = null
    ) {
        context.authDataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[USERNAME_KEY] = username
            name?.let { preferences[NAME_KEY] = it }
            refreshToken?.let { preferences[REFRESH_TOKEN_KEY] = it }
            email?.let { preferences[EMAIL_KEY] = it }
        }
    }

    /**
     * Update the user's profile information.
     * @param name The user's name.
     * @param email The user's email.
     */
    suspend fun updateProfile(name: String, email: String) {
        context.authDataStore.edit { preferences ->
            preferences[NAME_KEY] = name
            preferences[EMAIL_KEY] = email
        }
    }

    /**
     * Update the user's username.
     * @param username The new username.
     */
    suspend fun updateUsername(username: String) {
        context.authDataStore.edit { preferences ->
            preferences[USERNAME_KEY] = username
        }
    }

    /**
     * Update the user's avatar filename (as returned by the server after upload).
     * @param filename The avatar filename stored on the server (e.g. "abc123.jpg"), or null to remove.
     */
    suspend fun updateAvatar(filename: String?) {
        context.authDataStore.edit { preferences ->
            if (filename != null) preferences[AVATAR_KEY] = filename
            else preferences.remove(AVATAR_KEY)
        }
    }

    /**
     * Update the authentication tokens.
     * @param accessToken The new access token.
     * @param refreshToken The new refresh token.
     */
    suspend fun updateTokens(accessToken: String, refreshToken: String) {
        context.authDataStore.edit { preferences ->
            preferences[TOKEN_KEY] = accessToken
            preferences[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    /**
     * Clear all authentication data from DataStore.
     */
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

    /**
     * Get the refresh token synchronously.
     * @return The refresh token or null if not found.
     */
    suspend fun getRefreshTokenSync(): String? {
        return try {
            refreshToken.first()
        } catch (e: Exception) {
            Log.e("AuthManager", "Error getting refresh token", e)
            null
        }
    }

    /**
     * Get the authentication token synchronously.
     * @return The authentication token or null if not found.
     */
    suspend fun getTokenSync(): String? {
        return try {
            authToken.first()
        } catch (e: Exception) {
            Log.e("AuthManager", "Error getting token", e)
            null
        }
    }
}
