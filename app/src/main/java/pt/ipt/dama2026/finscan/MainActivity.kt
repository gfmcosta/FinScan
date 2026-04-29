package pt.ipt.dama2026.finscan

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
            // get the darkMode setting
            val isDarkModeStored by settingsManager.isDarkMode.collectAsState(initial = null)
            // if this setting doesn't exist it will use the system preference
            val useDarkMode = isDarkModeStored ?: isSystemInDarkTheme()

            FinScanTheme(darkTheme = useDarkMode) {
                MainApp()
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
    FinScanTheme {
        SplashScreen()
    }
}