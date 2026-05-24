package pt.ipt.dama2026.finscan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import pt.ipt.dama2026.finscan.R
import pt.ipt.dama2026.finscan.data.api.ApiClient
import pt.ipt.dama2026.finscan.data.api.models.ChangePasswordRequest
import pt.ipt.dama2026.finscan.data.api.services.AuthApiService
import pt.ipt.dama2026.finscan.ui.components.CustomToast
import pt.ipt.dama2026.finscan.ui.components.ToastState
import pt.ipt.dama2026.finscan.ui.components.ToastType
import pt.ipt.dama2026.finscan.ui.theme.*

@Composable
fun ChangePasswordScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var toastState by remember { mutableStateOf(ToastState()) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            // Header with back button (Consistent with Language Screen)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(enabled = !isLoading) { onBack() },
                    tint = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.change_password_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.change_password_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = getAdaptiveSubtext(),
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // Error Message
                if (errorMessage.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AmberAlert.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = AmberAlert,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Current Password
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it; errorMessage = "" },
                    label = { Text(stringResource(R.string.change_password_current_hint)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                            Icon(
                                imageVector = if (currentPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = IndigoTechnological
                            )
                        }
                    },
                    enabled = !isLoading
                )

                // New Password
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; errorMessage = "" },
                    label = { Text(stringResource(R.string.change_password_new_hint)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(
                                imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = IndigoTechnological
                            )
                        }
                    },
                    enabled = !isLoading
                )

                // Confirm New Password
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMessage = "" },
                    label = { Text(stringResource(R.string.change_password_confirm_hint)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = IndigoTechnological
                            )
                        }
                    },
                    enabled = !isLoading
                )

                // Submit Button
                Button(
                    onClick = {
                        val validationError = validateChangePassword(context, currentPassword, newPassword, confirmPassword)
                        if (validationError == null) {
                            isLoading = true
                            scope.launch {
                                try {
                                    val authApi = ApiClient.getRetrofit().create(AuthApiService::class.java)
                                    val response = authApi.changePassword(
                                        ChangePasswordRequest(currentPassword, newPassword)
                                    )
                                    
                                    if (response.isSuccessful) {
                                        toastState = ToastState(
                                            message = context.getString(R.string.change_password_success),
                                            type = ToastType.SUCCESS,
                                            isVisible = true
                                        )
                                        delay(2000)
                                        onBack()
                                    } else {
                                        val errorBody = response.errorBody()?.string() ?: ""
                                        errorMessage = try {
                                            JSONObject(errorBody).optString("detail", context.getString(R.string.auth_unknown_error))
                                        } catch (e: Exception) {
                                            errorBody.ifEmpty { context.getString(R.string.auth_unknown_error) }
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: context.getString(R.string.auth_unknown_error)
                                } finally {
                                    isLoading = false
                                }
                            }
                        } else {
                            errorMessage = validationError
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoTechnological),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(text = stringResource(R.string.change_password_button), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        CustomToast(
            state = toastState,
            onDismiss = { toastState = toastState.copy(isVisible = false) }
        )
    }
}

private fun validateChangePassword(
    context: android.content.Context,
    current: String,
    new: String,
    confirm: String
): String? {
    return when {
        current.isEmpty() -> context.getString(R.string.change_password_error_current_empty)
        new.isEmpty() -> context.getString(R.string.change_password_error_new_empty)
        new.length < 6 -> context.getString(R.string.change_password_error_new_short)
        new != confirm -> context.getString(R.string.change_password_error_mismatch)
        else -> null
    }
}

@Preview(showBackground = true)
@Composable
fun ChangePasswordScreenPreview() {
    FinScanTheme(darkTheme = false) {
        ChangePasswordScreen()
    }
}
