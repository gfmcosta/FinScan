package pt.ipt.dama2026.finscan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import pt.ipt.dama2026.finscan.ui.theme.AmberAlert
import pt.ipt.dama2026.finscan.ui.theme.EmeraldGreen
import pt.ipt.dama2026.finscan.ui.theme.IndigoTechnological
import kotlin.time.Duration.Companion.milliseconds

/**
 * Represents the different types of toasts.
 */
enum class ToastType {
    SUCCESS, ERROR, WARNING, INFO
}

/**
 * Represents the state of a toast.
 */
data class ToastState(
    val message: String = "",
    val type: ToastType = ToastType.INFO,
    val isVisible: Boolean = false
)

/**
 * A composable that displays a toast message.
 * @param state The state of the toast.
 * @param onDismiss Callback to be invoked when the toast is dismissed.
 * @param duration Duration of the toast in milliseconds.
 * @param alignment Alignment of the toast. Default is [Alignment.BottomCenter].
 */
@Composable
fun CustomToast(
    state: ToastState,
    onDismiss: () -> Unit = {},
    duration: Long = 3000,
    alignment: Alignment = Alignment.BottomCenter
) {
    if (state.isVisible) {
        LaunchedEffect(state) {
            delay(duration.milliseconds)
            onDismiss()
        }

        val padding = if (alignment == Alignment.TopCenter)
            PaddingValues(top = 32.dp, start = 16.dp, end = 16.dp)
        else
            PaddingValues(bottom = 32.dp, start = 16.dp, end = 16.dp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = alignment
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = getToastBackgroundColor(state.type)
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        imageVector = getToastIcon(state.type),
                        contentDescription = null,
                        tint = getToastIconColor(state.type),
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 12.dp)
                    )

                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = getToastTextColor(state.type),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Returns the background color for a toast based on its type.
 * @param type The type of the toast.
 * @return The background color.
 */
@Composable
private fun getToastBackgroundColor(type: ToastType): Color {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val alpha = if (isDark) 0.25f else 0.12f
    return when (type) {
        ToastType.SUCCESS -> EmeraldGreen.copy(alpha = alpha)
        ToastType.ERROR -> Color(0xFFEF4444).copy(alpha = alpha)
        ToastType.WARNING -> AmberAlert.copy(alpha = alpha)
        ToastType.INFO -> IndigoTechnological.copy(alpha = alpha)
    }
}

/**
 * Returns the text color for a toast based on its type.
 * @param type The type of the toast.
 * @return The text color.
 */
@Composable
private fun getToastTextColor(type: ToastType): Color {
    return when (type) {
        ToastType.SUCCESS -> EmeraldGreen
        ToastType.ERROR -> Color(0xFFDC2626)
        ToastType.WARNING -> AmberAlert
        ToastType.INFO -> IndigoTechnological
    }
}

/**
 * Returns the icon color for a toast based on its type.
 * @param type The type of the toast.
 * @return The icon color.
 */
@Composable
private fun getToastIconColor(type: ToastType): Color {
    return when (type) {
        ToastType.SUCCESS -> EmeraldGreen
        ToastType.ERROR -> Color(0xFFEF4444)
        ToastType.WARNING -> AmberAlert
        ToastType.INFO -> IndigoTechnological
    }
}

/**
 * Returns the icon for a toast based on its type.
 * @param type The type of the toast.
 * @return The icon.
 */
private fun getToastIcon(type: ToastType): ImageVector {
    return when (type) {
        ToastType.SUCCESS -> Icons.Default.CheckCircle
        ToastType.ERROR -> Icons.Default.Error
        ToastType.WARNING -> Icons.Default.Warning
        ToastType.INFO -> Icons.Default.Info
    }
}
