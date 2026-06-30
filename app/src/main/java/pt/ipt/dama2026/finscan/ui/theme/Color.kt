package pt.ipt.dama2026.finscan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Light Palette
val IndigoTechnological = Color(0xFF6366F1)      // Primary
val EmeraldGreen = Color(0xFF10B981)             // Secondary - Success
val OffWhite = Color(0xFFF8FAFC)                 // Background Color
val SlateDark = Color(0xFF1E293B)                // Main Text
val AmberAlert = Color(0xFFF59E0B)               // Alerts

// Dark Palette - Specific
val DarkBackground = Color(0xFF0F172A)
val DarkSurface = Color(0xFF1E293B)

// Settings Item Colors
val SettingsDarkModeColor = Color(0xFF374151)
val SettingsEditProfileColor = Color(0xFFF97316)
val SettingsChangePasswordColor = Color(0xFF3B82F6)
val SettingsNotificationsColor = Color(0xFF22C55E)
val SettingsLanguageColor = Color(0xFF6366F1)
val SettingsLogoutColor = Color(0xFFEF4444)
val SettingsAboutUsColor = Color(0xFF009688)

// Additional Settings Colors
val SettingsProfilePlaceholderColor = Color.LightGray
val SettingsSubtextColor = Color.Gray
val SettingsArrowColor = Color.LightGray
val SettingsIconTintColor = Color.White
val SettingsArrowDarkColor = Color(0xFF94A3B8)

// Home Screen Colors
val HomeMonthlyCardGradientEnd = Color(0xFF818CF8)
val HomeNavBarGrey = Color.Gray
val LightBlue = Color(0xFF00BCD4)

// Splash Screen Colors
val SplashGradientEnd = Color(0xFFE0E7FF)
val SplashDarkGradientEnd = Color(0xFF1E293B)

// Helper to check if the current theme is dark based on background luminance
@Composable
fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

// Dynamic Colors Helpers
@Composable
fun getAdaptiveSubtext() = if (isDarkTheme()) Color.LightGray else SettingsSubtextColor

@Composable
fun getAdaptiveControlColor() = if (isDarkTheme()) SettingsArrowDarkColor else SettingsArrowColor
