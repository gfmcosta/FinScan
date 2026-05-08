package pt.ipt.dama2026.finscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import pt.ipt.dama2026.finscan.data.datastore.SettingsManager
import pt.ipt.dama2026.finscan.ui.screens.SplashScreen
import pt.ipt.dama2026.finscan.ui.screens.MainScreen
import pt.ipt.dama2026.finscan.ui.theme.FinScanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // settingsManager data storage instance
        val settingsManager = SettingsManager(this)

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
                
                // Force the configuration and context to use the selected language
                val configAndContext = remember(currentLanguage, currentConfig) {
                    val locale = java.util.Locale.forLanguageTag(currentLanguage)
                    java.util.Locale.setDefault(locale)
                    
                    val config = android.content.res.Configuration(currentConfig)
                    config.setLocale(locale)
                    
                    // Create a context wrapper with the new configuration
                    val newContext = context.createConfigurationContext(config)
                    Pair(config, newContext)
                }

                val configuration = configAndContext.first
                val wrappedContext = configAndContext.second

                // Initialize language if missing (theme is now loaded from DataStore)
                LaunchedEffect(Unit) {
                    settingsManager.setLanguageIfMissing(currentLanguage)
                }

                // Also update the system resources in the background
                LaunchedEffect(currentLanguage) {
                    settingsManager.updateResourceLocale(currentLanguage)
                }

                // Wrap everything with updated configuration and context
                CompositionLocalProvider(
                    LocalConfiguration provides configuration,
                    LocalContext provides wrappedContext
                ) {
                    FinScanTheme(darkTheme = useDarkMode) {
                        MainApp()
                    }
                }
            }
        }
    }
}

@Composable
fun MainApp() {
    val showSplash = remember { mutableStateOf(true) }
    if (showSplash.value) {
        SplashScreen(
            onNavigateToHome = {
                showSplash.value = false
            }
        )
    } else {
        // App main content
        MainScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    FinScanTheme(darkTheme = false) {
        SplashScreen()
    }
}
