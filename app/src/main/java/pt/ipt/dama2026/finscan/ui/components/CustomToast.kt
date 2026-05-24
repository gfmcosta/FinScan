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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import pt.ipt.dama2026.finscan.ui.theme.AmberAlert
import pt.ipt.dama2026.finscan.ui.theme.EmeraldGreen
import pt.ipt.dama2026.finscan.ui.theme.IndigoTechnological

enum class ToastType {
    SUCCESS, ERROR, WARNING, INFO
}

data class ToastState(
    val message: String = "",
    val type: ToastType = ToastType.INFO,
    val isVisible: Boolean = false
)

@Composable
fun CustomToast(
    state: ToastState,
    onDismiss: () -> Unit = {},
    duration: Long = 3000
) {
    if (state.isVisible) {
        LaunchedEffect(state) {
            delay(duration)
            onDismiss()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = getToastBackgroundColor(state.type),
                        shape = RoundedCornerShape(12.dp)
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

@Composable
private fun getToastBackgroundColor(type: ToastType): Color {
    return when (type) {
        ToastType.SUCCESS -> EmeraldGreen.copy(alpha = 0.15f)
        ToastType.ERROR -> Color(0xFFEF4444).copy(alpha = 0.15f)
        ToastType.WARNING -> AmberAlert.copy(alpha = 0.15f)
        ToastType.INFO -> IndigoTechnological.copy(alpha = 0.15f)
    }
}

@Composable
private fun getToastTextColor(type: ToastType): Color {
    return when (type) {
        ToastType.SUCCESS -> EmeraldGreen
        ToastType.ERROR -> Color(0xFFDC2626)
        ToastType.WARNING -> AmberAlert
        ToastType.INFO -> IndigoTechnological
    }
}

@Composable
private fun getToastIconColor(type: ToastType): Color {
    return when (type) {
        ToastType.SUCCESS -> EmeraldGreen
        ToastType.ERROR -> Color(0xFFEF4444)
        ToastType.WARNING -> AmberAlert
        ToastType.INFO -> IndigoTechnological
    }
}

private fun getToastIcon(type: ToastType): ImageVector {
    return when (type) {
        ToastType.SUCCESS -> Icons.Default.CheckCircle
        ToastType.ERROR -> Icons.Default.Error
        ToastType.WARNING -> Icons.Default.Warning
        ToastType.INFO -> Icons.Default.Info
    }
}
