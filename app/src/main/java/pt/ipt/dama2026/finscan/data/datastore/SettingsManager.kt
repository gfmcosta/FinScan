package pt.ipt.dama2026.finscan.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Extension property to create DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// Responsible Class to manage settings
class SettingsManager(private val context: Context) {

    companion object {
        // Boolean preference key for dark mode active
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        // String preference key language
        val LANGUAGE_KEY = stringPreferencesKey("language")
        // Boolean preference key for notifications enabled
        val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        
        // Singleton instance - initialized lazily on first access
        @Volatile
        private var instance: SettingsManager? = null
        
        /**
         * Returns singleton instance of SettingsManager
         * Thread-safe using double-checked locking
         */
        fun getInstance(context: Context): SettingsManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsManager(context.applicationContext).also { instance = it }
            }
        }
    }

    // Read theme preference (default to false/light mode if not set)
    val isDarkMode: Flow<Boolean?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }

    // Read notifications enabled preference (default false)
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] ?: false
        }

    // Read language preference (returns system default if not set)
    val language: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException){
                emit(emptyPreferences())
            }else{
                throw exception
            }
        }
        .map { preferences ->
            preferences[LANGUAGE_KEY] ?: java.util.Locale.getDefault().language
        }

    /**
     * Function to set if the user wants dark mode
     * @param enabled represents the permission to activate dark mode
     */
    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }

    /**
     * Function to set the user's preference language
     * @param lang represents the preference language that the user wants to use
     */
    suspend fun setLanguage(lang: String){
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = lang
        }
    }

    /**
     * Persists the language only if no preference is currently stored.
     */
    suspend fun setLanguageIfMissing(lang: String) {
        context.dataStore.edit { preferences ->
            if (preferences[LANGUAGE_KEY] == null) {
                preferences[LANGUAGE_KEY] = lang
            }
        }
    }

    /**
     * Technical function to update the app's resources to a specific language.
     * @param lang The language code (e.g., "pt", "en")
     */
    fun updateResourceLocale(lang: String) {
        val locale = java.util.Locale.forLanguageTag(lang)
        java.util.Locale.setDefault(locale)
        val resources = context.resources
        val config = android.content.res.Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
