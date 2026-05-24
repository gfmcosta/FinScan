package pt.ipt.dama2026.finscan.ui.screens.auth

import android.annotation.SuppressLint
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.json.JSONObject
import pt.ipt.dama2026.finscan.R
import pt.ipt.dama2026.finscan.ui.theme.*
import pt.ipt.dama2026.finscan.ui.components.CustomToast
import pt.ipt.dama2026.finscan.ui.components.ToastState
import pt.ipt.dama2026.finscan.ui.components.ToastType
import pt.ipt.dama2026.finscan.data.api.ApiClient
import pt.ipt.dama2026.finscan.data.api.models.RegisterRequest
import pt.ipt.dama2026.finscan.data.api.services.AuthApiService

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var username by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var toastState by remember { mutableStateOf(ToastState()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        // Compensação extra para igualar ao ecrã de Language
        Spacer(modifier = Modifier.statusBarsPadding())

        // Header with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier
                    .size(24.dp)
                    .clickable(enabled = !isLoading) { onNavigateToLogin() },
                tint = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.auth_register_title),
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
            // Subtitle
            Text(
                text = stringResource(R.string.auth_register_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
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

            // Username TextField
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    errorMessage = ""
                },
                label = { Text(stringResource(R.string.auth_username_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IndigoTechnological,
                    focusedLabelColor = IndigoTechnological
                ),
                enabled = !isLoading
            )

            // Name TextField
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = ""
                },
                label = { Text(stringResource(R.string.auth_name_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IndigoTechnological,
                    focusedLabelColor = IndigoTechnological
                ),
                enabled = !isLoading
            )

            // Email TextField
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = ""
                },
                label = { Text(stringResource(R.string.auth_email_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IndigoTechnological,
                    focusedLabelColor = IndigoTechnological
                ),
                enabled = !isLoading
            )

            // Password TextField
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = ""
                },
                label = { Text(stringResource(R.string.auth_password_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = IndigoTechnological
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IndigoTechnological,
                    focusedLabelColor = IndigoTechnological
                ),
                enabled = !isLoading
            )

            // Confirm Password TextField
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    errorMessage = ""
                },
                label = { Text(stringResource(R.string.auth_confirm_password_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(
                        onClick = { confirmPasswordVisible = !confirmPasswordVisible },
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = IndigoTechnological
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IndigoTechnological,
                    focusedLabelColor = IndigoTechnological
                ),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Register Button
            Button(
                onClick = {
                    val validationError = validateRegisterForm(username, name, email, password, confirmPassword, context)
                    if (validationError == null) {
                        isLoading = true
                        errorMessage = ""
                        
                        scope.launch {
                            try {
                                val authApi = ApiClient.getRetrofit().create(AuthApiService::class.java)
                                val request = RegisterRequest(
                                    username = username,
                                    name = name,
                                    email = email,
                                    password = password,
                                    role = "user"
                                )
                                val response = authApi.register(request)
                                
                                if (response.isSuccessful) {
                                    toastState = ToastState(
                                        message = context.getString(R.string.auth_register_success),
                                        type = ToastType.SUCCESS,
                                        isVisible = true
                                    )
                                    delay(1500)
                                    onNavigateToLogin()
                                } else {
                                    val errorBody = response.errorBody()?.string() ?: ""
                                    val parsedMessage = try {
                                        val jsonObject = JSONObject(errorBody)
                                        jsonObject.optString("detail", context.getString(R.string.auth_register_success))
                                    } catch (e: Exception) {
                                        errorBody.ifEmpty { context.getString(R.string.auth_unknown_error) }
                                    }
                                    errorMessage = parsedMessage
                                }
                            } catch (e: Exception) {
                                e.message?.let {
                                    if(it.contains("Unable to resolve host")){
                                        errorMessage = context.getString(R.string.auth_error_internet)
                                    }else{
                                        errorMessage = e.message ?: context.getString(R.string.auth_unknown_error)
                                    }
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    } else {
                        errorMessage = validationError
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IndigoTechnological
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.auth_register_button),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Divider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                )
                Text(
                    text = stringResource(R.string.auth_or),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                HorizontalDivider(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.auth_has_account),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(
                    onClick = onNavigateToLogin,
                    enabled = !isLoading
                ) {
                    Text(
                        text = stringResource(R.string.auth_login_link_text),
                        style = MaterialTheme.typography.labelLarge,
                        color = IndigoTechnological
                    )
                }
            }
        }
    }

    CustomToast(
        state = toastState,
        onDismiss = { toastState = toastState.copy(isVisible = false) }
    )
}

private fun validateRegisterForm(
    username: String,
    name: String,
    email: String,
    password: String,
    confirmPassword: String,
    context: android.content.Context
): String? {
    return when {
        username.isEmpty() -> context.getString(R.string.auth_error_empty_username)
        username.length < 3 -> context.getString(R.string.auth_error_username_short)
        name.isEmpty() -> context.getString(R.string.auth_error_empty_name)
        email.isEmpty() -> context.getString(R.string.auth_error_empty_email)
        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> context.getString(R.string.auth_error_invalid_email)
        password.isEmpty() -> context.getString(R.string.auth_error_empty_password)
        password.length < 6 -> context.getString(R.string.auth_error_password_short)
        confirmPassword.isEmpty() -> context.getString(R.string.auth_error_empty_confirm_password)
        password != confirmPassword -> context.getString(R.string.auth_error_password_mismatch)
        else -> null
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    FinScanTheme(darkTheme = false) {
        RegisterScreen()
    }
}
