package pt.ipt.dama2026.finscan.ui.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import pt.ipt.dama2026.finscan.ui.theme.FinScanTheme

@Composable
fun AuthNavigationFlow(
    onAuthSuccess: () -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf("login") }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "authScreenTransition"
    ) { screen ->
        when (screen) {
            "login" -> LoginScreen(
                onLoginSuccess = onAuthSuccess,
                onNavigateToRegister = { currentScreen = "register" },
                onNavigateToForgotPassword = { currentScreen = "forgot_password" }
            )
            "register" -> RegisterScreen(
                onRegisterSuccess = onAuthSuccess,
                onNavigateToLogin = { currentScreen = "login" }
            )
            "forgot_password" -> ForgotPasswordScreen(
                onNavigateBack = { currentScreen = "login" }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AuthNavigationFlowPreview() {
    FinScanTheme(darkTheme = false) {
        AuthNavigationFlow()
    }
}
