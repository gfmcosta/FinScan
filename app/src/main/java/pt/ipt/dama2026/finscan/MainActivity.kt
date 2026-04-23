package pt.ipt.dama2026.finscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import pt.ipt.dama2026.finscan.ui.screens.SplashScreen
import pt.ipt.dama2026.finscan.ui.theme.FinScanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinScanTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
                    MainApp(paddingValues = paddingValues)
                }
            }
        }
    }
}

@Composable
fun MainApp(paddingValues: PaddingValues = PaddingValues()) {
    val showSplash = remember { mutableStateOf(true) }

    if (showSplash.value) {
        SplashScreen(
            onNavigateToHome = {
                showSplash.value = false
            }
        )
    } else {
        // App main content
        HomeScreen(paddingValues = paddingValues)
    }
}

@Composable
fun HomeScreen(paddingValues: PaddingValues = PaddingValues()) {
    // Home Screen Placeholder
    androidx.compose.material3.Text(
        text = "Bem-vindo ao FinScan!",
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    )
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    FinScanTheme {
        SplashScreen()
    }
}