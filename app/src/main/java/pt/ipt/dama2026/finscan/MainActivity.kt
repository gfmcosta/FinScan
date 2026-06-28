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
import pt.ipt.dama2026.finscan.data.datastore.SettingsManager
import pt.ipt.dama2026.finscan.data.datastore.AuthManager
import pt.ipt.dama2026.finscan.utils.NotificationHelper
import pt.ipt.dama2026.finscan.utils.WebSocketManager
import pt.ipt.dama2026.finscan.ui.screens.SplashScreen
import pt.ipt.dama2026.finscan.ui.screens.MainScreen
import pt.ipt.dama2026.finscan.ui.screens.auth.AuthNavigationFlow
import pt.ipt.dama2026.finscan.ui.theme.FinScanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channel (safe to call multiple times)
        NotificationHelper.createChannel(this)

        // Use singleton settingsManager instance
        val settingsManager = SettingsManager.getInstance(this)
        val authManager = AuthManager.getInstance(this)
        pt.ipt.dama2026.finscan.data.api.ApiClient.initialize(this)

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
                // Key change: removed currentConfig from remember dependency - only use currentLanguage
                // This ensures the wrapped context is recreated EVERY time the language changes
                val configAndContext = remember(currentLanguage) {
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

                // Update the system resources in the background when language changes
                LaunchedEffect(currentLanguage) {
                    settingsManager.updateResourceLocale(currentLanguage)
                }

                // Wrap everything with updated configuration and context.
                // Also re-provide LocalActivityResultRegistryOwner because overriding
                // LocalContext with a ContextWrapper drops it during recomposition.
                val activity = this@MainActivity
                // Keep status bar / nav bar icon colours in sync with the app's own
                // theme choice — not the system theme. Without this, MIUI and other
                // skins leave light-coloured icons on a white background (invisible)
                // when the system is in dark mode but the app is in light mode.
                val window = this@MainActivity.window
                SideEffect {
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.isAppearanceLightStatusBars     = !useDarkMode
                    controller.isAppearanceLightNavigationBars = !useDarkMode
                }

                CompositionLocalProvider(
                    LocalConfiguration provides configuration,
                    LocalContext provides wrappedContext,
                    LocalActivityResultRegistryOwner provides activity
                ) {
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
    // Usamos null como estado inicial para saber quando o DataStore terminou de ler
    val isLoggedIn by authManager.isLoggedIn.collectAsState(initial = null)
    var splashFinished by remember { mutableStateOf(false) }

    // Connect / disconnect WebSocket whenever login state changes
    LaunchedEffect(isLoggedIn) {
        when (isLoggedIn) {
            true  -> WebSocketManager.connect(context)
            false -> WebSocketManager.disconnect()
            else  -> Unit
        }
    }

    when {
        // 1. Enquanto carrega o estado de autenticação, podemos mostrar um fundo vazio ou a splash
        isLoggedIn == null -> {
            Box(modifier = Modifier.fillMaxSize())
        }

        // 2. Se estiver logado MAS a splash ainda não terminou, mostramos a splash
        isLoggedIn == true && !splashFinished -> {
            SplashScreen(
                onNavigateToHome = {
                    splashFinished = true
                }
            )
        }

        // 3. Se não estiver logado, mostramos o fluxo de autenticação
        isLoggedIn == false -> {
            AuthNavigationFlow(
                onAuthSuccess = {
                    // O AuthManager irá atualizar o isLoggedIn para true,
                    // o que ativará o branch acima (isLoggedIn == true && !splashFinished)
                }
            )
        }

        // 4. Se estiver logado E a splash já terminou, ecrã principal
        else -> {
            MainScreen()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    FinScanTheme(darkTheme = false) {
        SplashScreen()
    }
}
