package pt.ipt.dama2026.finscan.ui.screens.auth

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONObject
import pt.ipt.dama2026.finscan.R
import pt.ipt.dama2026.finscan.ui.theme.*
import pt.ipt.dama2026.finscan.data.api.ApiClient
import pt.ipt.dama2026.finscan.data.api.models.TokenResponse
import pt.ipt.dama2026.finscan.data.api.services.AuthApiService
import pt.ipt.dama2026.finscan.data.datastore.AuthManager

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo/Header
            Image(
                painter = painterResource(id = R.drawable.playstore_icon),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = stringResource(R.string.auth_login_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = stringResource(R.string.auth_login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(32.dp))

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
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
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
                    .padding(bottom = 8.dp),
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

            Spacer(modifier = Modifier.height(24.dp))

            // Login Button
            Button(
                onClick = {
                    if (validateLoginForm(username, password)) {
                        isLoading = true
                        errorMessage = ""

                        scope.launch {
                            try {
                                val authApi =
                                    ApiClient.getRetrofit().create(AuthApiService::class.java)
                                val response = authApi.login(username, password)

                                if (response.isSuccessful && response.body() != null) {
                                    // Guardar o token e o nome no AuthManager
                                    val tokenResponse = response.body()!!
                                    AuthManager.getInstance(context).saveToken(
                                        token = tokenResponse.accessToken,
                                        username = username,
                                        name = tokenResponse.name,
                                        refreshToken = tokenResponse.refreshToken,
                                        email = tokenResponse.email
                                    )

                                    onLoginSuccess()
                                } else {
                                    val errorBody = response.errorBody()?.string() ?: ""
                                    val detail = try { JSONObject(errorBody).optString("detail") } catch (_: Exception) { "" }
                                    errorMessage = when (detail) {
                                        "email_not_verified" -> context.getString(R.string.auth_error_email_not_verified)
                                        else -> detail.ifEmpty { context.getString(R.string.auth_unknown_error) }
                                    }
                                }
                            } catch (e: Exception) {
                                val msg = e.message ?: ""
                                if (msg.contains("Unable to resolve host") || msg.contains("Failed to connect") || msg.contains("Connection refused") || msg.contains("timeout")) {
                                    errorMessage = context.getString(R.string.auth_error_internet)
                                } else {
                                    errorMessage = context.getString(R.string.auth_unknown_error)
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    } else {
                        errorMessage = context.getString(R.string.auth_error_empty_fields)
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
                        text = stringResource(R.string.auth_login_button),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Forgot Password Link (opcional)
            TextButton(
                onClick = onNavigateToForgotPassword,
                enabled = !isLoading
            ) {
                Text(
                    text = stringResource(R.string.auth_forgot_password),
                    style = MaterialTheme.typography.labelLarge,
                    color = IndigoTechnological
                )
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

            // Register Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.auth_no_account),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(
                    onClick = onNavigateToRegister,
                    enabled = !isLoading
                ) {
                    Text(
                        text = stringResource(R.string.auth_register_link_text),
                        style = MaterialTheme.typography.labelLarge,
                        color = IndigoTechnological
                    )
                }
            }
        }
    }
}
fun validateLoginForm(username: String, password: String): Boolean {
    return username.isNotEmpty() && password.isNotEmpty()
}


@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    FinScanTheme(darkTheme = false) {
        LoginScreen()
    }
}
