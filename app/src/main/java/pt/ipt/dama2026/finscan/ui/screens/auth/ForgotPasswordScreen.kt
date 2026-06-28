package pt.ipt.dama2026.finscan.ui.screens.auth

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONObject
import pt.ipt.dama2026.finscan.R
import pt.ipt.dama2026.finscan.data.api.ApiClient
import pt.ipt.dama2026.finscan.data.api.models.ForgotPasswordRequest
import pt.ipt.dama2026.finscan.data.api.models.ResetPasswordRequest
import pt.ipt.dama2026.finscan.data.api.services.AuthApiService
import pt.ipt.dama2026.finscan.ui.theme.AmberAlert
import pt.ipt.dama2026.finscan.ui.theme.IndigoTechnological
import pt.ipt.dama2026.finscan.ui.theme.getAdaptiveSubtext

@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var step by remember { mutableIntStateOf(1) }
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp)
    ) {
        // Compensação extra para igualar ao ecrã de Language (que está dentro de outro Scaffold)
        Spacer(modifier = Modifier.statusBarsPadding()) 

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onNavigateBack() },
                tint = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.auth_forgot_password),
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (step == 1) 
                    stringResource(R.string.forgot_password_instruction_email)
                else 
                    stringResource(R.string.forgot_password_instruction_code),
                style = MaterialTheme.typography.bodyMedium,
                color = getAdaptiveSubtext(),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Feedback Messages
            if (errorMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberAlert.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = errorMessage, style = MaterialTheme.typography.bodySmall, color = AmberAlert, modifier = Modifier.padding(12.dp))
                }
            }
            if (successMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = successMessage, style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50), modifier = Modifier.padding(12.dp))
                }
            }

            if (step == 1) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = "" },
                    label = { Text(stringResource(R.string.auth_email_hint)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    enabled = !isLoading
                )

                Button(
                    onClick = {
                        if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            isLoading = true
                            errorMessage = ""
                            scope.launch {
                                try {
                                    val authApi = ApiClient.getRetrofit().create(AuthApiService::class.java)
                                    val response = authApi.forgotPassword(ForgotPasswordRequest(email))
                                    if (response.isSuccessful) {
                                        successMessage = context.getString(R.string.forgot_password_code_sent)
                                        step = 2
                                    } else {
                                        val errorBody = response.errorBody()?.string() ?: ""
                                        errorMessage = JSONObject(errorBody).optString("detail", context.getString(R.string.auth_unknown_error))
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: context.getString(R.string.auth_unknown_error)
                                } finally {
                                    isLoading = false
                                }
                            }
                        } else {
                            errorMessage = context.getString(R.string.auth_error_invalid_email)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoTechnological),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(text = stringResource(R.string.auth_send_code_button))
                    }
                }
            } else {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it; errorMessage = "" },
                    label = { Text(stringResource(R.string.forgot_password_code_hint)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isLoading
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; errorMessage = "" },
                    label = { Text(stringResource(R.string.change_password_new_hint)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                        }
                    },
                    enabled = !isLoading
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMessage = "" },
                    label = { Text(stringResource(R.string.change_password_confirm_hint)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                        }
                    },
                    enabled = !isLoading
                )

                Button(
                    onClick = {
                        if (code.length == 6 && newPassword.length >= 6 && newPassword == confirmPassword) {
                            isLoading = true
                            errorMessage = ""
                            scope.launch {
                                try {
                                    val authApi = ApiClient.getRetrofit().create(AuthApiService::class.java)
                                    val response = authApi.resetPassword(ResetPasswordRequest(email, code, newPassword))
                                    if (response.isSuccessful) {
                                        successMessage = context.getString(R.string.change_password_success)
                                        kotlinx.coroutines.delay(2000)
                                        onNavigateBack()
                                    } else {
                                        val errorBody = response.errorBody()?.string() ?: ""
                                        errorMessage = JSONObject(errorBody).optString("detail", context.getString(R.string.auth_unknown_error))
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: context.getString(R.string.auth_unknown_error)
                                } finally {
                                    isLoading = false
                                }
                            }
                        } else {
                            errorMessage = when {
                                code.length != 6 -> context.getString(R.string.forgot_password_error_invalid_code)
                                newPassword.length < 6 -> context.getString(R.string.change_password_error_new_short)
                                newPassword != confirmPassword -> context.getString(R.string.change_password_error_mismatch)
                                else -> context.getString(R.string.auth_error_empty_fields)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoTechnological),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(text = stringResource(R.string.auth_reset_password_button))
                    }
                }
                
                TextButton(
                    onClick = { step = 1; successMessage = "" },
                    modifier = Modifier.padding(top = 16.dp),
                    enabled = !isLoading
                ) {
                    Text(text = stringResource(R.string.forgot_password_resend_email), color = IndigoTechnological)
                }
            }
        }
    }
}
