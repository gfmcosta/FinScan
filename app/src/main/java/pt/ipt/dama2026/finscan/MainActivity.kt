package pt.ipt.dama2026.finscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import pt.ipt.dama2026.finscan.data.api.ApiClient
import pt.ipt.dama2026.finscan.data.datastore.SettingsManager
import pt.ipt.dama2026.finscan.data.datastore.AuthManager
import pt.ipt.dama2026.finscan.utils.NotificationHelper
import pt.ipt.dama2026.finscan.utils.WebSocketManager
import pt.ipt.dama2026.finscan.ui.screens.SplashScreen
import pt.ipt.dama2026.finscan.ui.screens.MainScreen
import pt.ipt.dama2026.finscan.ui.screens.auth.AuthNavigationFlow
import pt.ipt.dama2026.finscan.ui.theme.FinScanTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channel (safe to call multiple times)
        NotificationHelper.createChannel(this)

        // Use singleton settingsManager instance
        val settingsManager = SettingsManager.getInstance(this)
        // Use singleton authManager instance
        val authManager = AuthManager.getInstance(this)
        // Initialize ApiClient
        ApiClient.initialize(this)

        setContent {
            // Observe settings - start with null to detect when DataStore has finished reading
            val isDarkModeStored by settingsManager.isDarkMode.collectAsState(initial = null)
            val languageStored by settingsManager.language.collectAsState(initial = null)

            // Only render when we have BOTH language and theme to avoid any flicker
            if (languageStored != null && isDarkModeStored != null) {
                val currentLanguage = languageStored!!
                val useDarkMode = isDarkModeStored!!
                val currentConfig = LocalConfiguration.current
                val context = LocalContext.current

                // Create a new configuration and context based on the current language
                // This ensures that the app's resources are loaded in the correct language
                val configAndContext = remember(currentLanguage) {
                    val locale = Locale.forLanguageTag(currentLanguage)
                    Locale.setDefault(locale)

                    val config = android.content.res.Configuration(currentConfig)
                    config.setLocale(locale)

                    // Create a context wrapper with the new configuration
                    val newContext = context.createConfigurationContext(config)
                    Pair(config, newContext)
                }

                // Extract the updated configuration and context
                val configuration = configAndContext.first
                val wrappedContext = configAndContext.second

                // Initialize language if missing
                LaunchedEffect(Unit) {
                    settingsManager.setLanguageIfMissing(currentLanguage)
                }

                // Update the system resources in the background when language changes
                LaunchedEffect(currentLanguage) {
                    settingsManager.updateResourceLocale(currentLanguage)
                }

                // Wrap everything with updated configuration and context.
                val activity = this@MainActivity
                val window = this@MainActivity.window
                // Update the system bars to match the theme
                SideEffect {
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.isAppearanceLightStatusBars = !useDarkMode
                    controller.isAppearanceLightNavigationBars = !useDarkMode
                }
                // Provide the updated configuration and context to the Composable hierarchy
                CompositionLocalProvider(
                    LocalConfiguration provides configuration,
                    LocalContext provides wrappedContext,
                    LocalActivityResultRegistryOwner provides activity
                ) {
                    // Apply the theme and render the main app
                    FinScanTheme(darkTheme = useDarkMode) {
                        MainApp(authManager)
                    }
                }
            }
        }
    }
}

@Composable
fun MainApp(authManager: AuthManager) {
    val context = LocalContext.current
    // Use initial = null to indicate that we are still loading the login state
    val isLoggedIn by authManager.isLoggedIn.collectAsState(initial = null)
    // State to track if the splash screen has finished
    var splashFinished by remember { mutableStateOf(false) }

    // Connect / disconnect WebSocket whenever login state changes
    LaunchedEffect(isLoggedIn) {
        when (isLoggedIn) {
            true -> WebSocketManager.connect(context)
            false -> WebSocketManager.disconnect()
            else -> Unit
        }
    }

    when (isLoggedIn) {
        // 1. While loading the authentication state, we can show an empty background or the splash
        null -> {
            Box(modifier = Modifier.fillMaxSize())
        }

        // 2. If logged in but splash not finished, show the splash screen
        true if !splashFinished -> {
            SplashScreen(
                onNavigateToHome = {
                    splashFinished = true
                }
            )
        }

        // 3. If not logged in, show the authentication flow
        false -> {
            AuthNavigationFlow(
                onAuthSuccess = {
                    // The AuthManager will update isLoggedIn to true,
                    // which will trigger the branch above (isLoggedIn == true && !splashFinished)
                }
            )
        }

        // 4. If logged in and splash finished, show the main screen
        else -> {
            MainScreen()
        }
    }
}

// Preview Screen
@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    FinScanTheme(darkTheme = false) {
        SplashScreen()
    }
}
