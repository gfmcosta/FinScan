package pt.ipt.dama2026.finscan.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.json.JSONObject
import pt.ipt.dama2026.finscan.R
import pt.ipt.dama2026.finscan.data.api.ApiClient
import pt.ipt.dama2026.finscan.data.api.models.UpdateProfileRequest
import pt.ipt.dama2026.finscan.data.api.services.AuthApiService
import pt.ipt.dama2026.finscan.data.datastore.AuthManager
import pt.ipt.dama2026.finscan.ui.components.CustomToast
import pt.ipt.dama2026.finscan.ui.components.ToastState
import pt.ipt.dama2026.finscan.ui.components.ToastType
import pt.ipt.dama2026.finscan.ui.theme.*
import java.io.ByteArrayOutputStream

// ---------- helpers ----------

private fun uriToResizedBase64(context: android.content.Context, uri: Uri): Pair<Bitmap, String>? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val raw = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        val scaled = Bitmap.createScaledBitmap(raw, 512, 512, true)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, out)
        val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        Pair(scaled, b64)
    } catch (e: Exception) {
        null
    }
}

// ---------- screen ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authManager = remember { AuthManager.getInstance(context) }
    val api = remember { ApiClient.getRetrofit().create(AuthApiService::class.java) }

    val storedName by authManager.name.collectAsState(initial = "")
    val storedEmail by authManager.email.collectAsState(initial = "")
    val storedUsername by authManager.username.collectAsState(initial = "")
    // avatar = filename stored on server (e.g. "abc123.jpg")
    val storedAvatarFilename by authManager.avatar.collectAsState(initial = null)

    // Initialised from DataStore immediately; LaunchedEffect will overwrite with fresh API data.
    // Keys are intentionally omitted so a late DataStore emission can't race-reset what the API filled in.
    var name by remember { mutableStateOf(storedName ?: "") }
    var email by remember { mutableStateOf(storedEmail ?: "") }
    var username by remember { mutableStateOf(storedUsername ?: "") }

    // localPreview: bitmap chosen from gallery but not yet uploaded
    var localPreview by remember { mutableStateOf<Bitmap?>(null) }
    // pendingBase64: base64 to send on save
    var pendingBase64 by remember { mutableStateOf<String?>(null) }
    // pendingNewEmail: email awaiting confirmation
    var pendingNewEmail by remember { mutableStateOf<String?>(null) }

    var nameError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showAvatarSheet by remember { mutableStateOf(false) }
    var toastState by remember { mutableStateOf(ToastState()) }

    // Load fresh data from /users/me on first render
    LaunchedEffect(Unit) {
        try {
            val resp = api.getMe()
            if (resp.isSuccessful) {
                val user = resp.body()!!
                name = user.name ?: storedName ?: ""
                email = user.email
                username = user.username
                if (user.avatar != null && user.avatar != storedAvatarFilename) {
                    authManager.updateAvatar(user.avatar)
                }
            }
        } catch (_: Exception) {}
    }

    // Image pickers
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch {
                val result = uriToResizedBase64(context, it)
                if (result != null) {
                    localPreview = result.first
                    pendingBase64 = result.second
                }
            }
        }
    }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch {
                val result = uriToResizedBase64(context, it)
                if (result != null) {
                    localPreview = result.first
                    pendingBase64 = result.second
                }
            }
        }
    }

    // Avatar picker bottom sheet
    if (showAvatarSheet) {
        ModalBottomSheet(onDismissRequest = { showAvatarSheet = false }) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(
                    stringResource(R.string.pick_source_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAvatarSheet = false; galleryLauncher.launch("image/*") }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = IndigoTechnological)
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(R.string.pick_gallery))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAvatarSheet = false; fileLauncher.launch(arrayOf("image/*")) }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = IndigoTechnological)
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(R.string.pick_files))
                }
                // Remove option if there's any avatar (server or local preview)
                if (localPreview != null || storedAvatarFilename != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAvatarSheet = false
                                localPreview = null
                                pendingBase64 = ""  // empty string = signal to remove on save
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(R.string.edit_profile_remove_photo), color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp).clickable(onClick = onBack)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.edit_profile_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Avatar
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(SettingsProfilePlaceholderColor)
                        .clickable { showAvatarSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        // Locally picked image (not yet uploaded) — show immediately
                        localPreview != null -> Image(
                            bitmap = localPreview!!.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Server avatar — load from URL
                        storedAvatarFilename != null && pendingBase64 != "" -> AsyncImage(
                            model = ApiClient.avatarUrl(storedAvatarFilename),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // No avatar
                        else -> Icon(
                            Icons.Default.Person, contentDescription = null,
                            modifier = Modifier.size(52.dp), tint = SettingsIconTintColor
                        )
                    }
                }
                // Camera badge
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-28).dp, y = (-28).dp)
                        .clip(CircleShape)
                        .background(IndigoTechnological)
                        .clickable { showAvatarSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt, contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                }
            }

            // Form
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {

                // Username — no spaces allowed
                OutlinedTextField(
                    value = username,
                    onValueChange = { input ->
                        if (!input.contains(' ')) { username = input; usernameError = "" }
                    },
                    label = { Text(stringResource(R.string.edit_profile_username_label)) },
                    singleLine = true,
                    isError = usernameError.isNotEmpty(),
                    supportingText = if (usernameError.isNotEmpty()) {{ Text(usernameError) }} else null,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = IndigoTechnological),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Name — no digits, no leading space
                OutlinedTextField(
                    value = name,
                    onValueChange = { input ->
                        if (!input.any { it.isDigit() } && !input.startsWith(' ')) { name = input; nameError = "" }
                    },
                    label = { Text(stringResource(R.string.edit_profile_name_label)) },
                    singleLine = true,
                    isError = nameError.isNotEmpty(),
                    supportingText = if (nameError.isNotEmpty()) {{ Text(nameError) }} else null,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = IndigoTechnological),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email — no spaces allowed
                OutlinedTextField(
                    value = email,
                    onValueChange = { input ->
                        if (!input.contains(' ')) { email = input; emailError = ""; pendingNewEmail = null }
                    },
                    label = { Text(stringResource(R.string.edit_profile_email_label)) },
                    singleLine = true,
                    isError = emailError.isNotEmpty(),
                    supportingText = if (emailError.isNotEmpty()) {{ Text(emailError) }} else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = IndigoTechnological),
                    modifier = Modifier.fillMaxWidth()
                )

                // Email pending confirmation info card
                if (pendingNewEmail != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = IndigoTechnological.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MarkEmailUnread, contentDescription = null, tint = IndigoTechnological, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.edit_profile_email_pending, pendingNewEmail!!),
                                style = MaterialTheme.typography.bodySmall,
                                color = IndigoTechnological
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Save button
                Button(
                    onClick = {
                        var valid = true
                        // Username
                        when {
                            username.isBlank()      -> { usernameError = context.getString(R.string.edit_profile_username_empty); valid = false }
                            username.contains(' ')  -> { usernameError = context.getString(R.string.auth_error_username_spaces); valid = false }
                            username.length < 3     -> { usernameError = context.getString(R.string.auth_error_username_short); valid = false }
                        }
                        // Name
                        when {
                            name.isBlank()              -> { nameError = context.getString(R.string.edit_profile_name_empty); valid = false }
                            name.startsWith(' ')        -> { nameError = context.getString(R.string.auth_error_name_leading_space); valid = false }
                            name.any { it.isDigit() }   -> { nameError = context.getString(R.string.auth_error_name_has_numbers); valid = false }
                        }
                        // Email
                        val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
                        when {
                            email.isBlank()            -> { emailError = context.getString(R.string.edit_profile_email_invalid); valid = false }
                            email.contains(' ')        -> { emailError = context.getString(R.string.auth_error_email_spaces); valid = false }
                            !emailRegex.matches(email.trim()) -> { emailError = context.getString(R.string.edit_profile_email_invalid); valid = false }
                        }
                        if (!valid) return@Button

                        isLoading = true
                        scope.launch {
                            try {
                                val currentEmail = storedEmail ?: ""
                                val emailChanged = email.trim() != currentEmail
                                val resp = api.updateProfile(
                                    UpdateProfileRequest(
                                        username = username.trim(),
                                        name = name.trim(),
                                        email = if (emailChanged) email.trim() else null,
                                        // null = no change, "" = remove, base64 = new image
                                        avatarBase64 = pendingBase64
                                    )
                                )
                                if (resp.isSuccessful && resp.body() != null) {
                                    val updated = resp.body()!!
                                    // Persist updated fields locally
                                    authManager.updateProfile(name.trim(), updated.email)
                                    authManager.updateUsername(updated.username)
                                    // Store new filename (or null if removed)
                                    authManager.updateAvatar(updated.avatar)
                                    // Clear local preview now that server has the file
                                    localPreview = null
                                    pendingBase64 = null
                                    if (emailChanged) {
                                        // Keep the new email visible in the field;
                                        // the pending card explains it awaits confirmation.
                                        pendingNewEmail = email.trim()
                                    }
                                    toastState = ToastState(
                                        message = context.getString(R.string.edit_profile_success),
                                        type = ToastType.SUCCESS, isVisible = true
                                    )
                                } else {
                                    val detail = try {
                                        JSONObject(resp.errorBody()?.string() ?: "").optString("detail")
                                    } catch (_: Exception) { "" }
                                    when (detail) {
                                        "username_taken" -> usernameError = context.getString(R.string.edit_profile_username_taken)
                                        "email_taken" -> emailError = context.getString(R.string.edit_profile_email_taken)
                                        else -> toastState = ToastState(
                                            message = context.getString(R.string.edit_profile_error),
                                            type = ToastType.ERROR, isVisible = true
                                        )
                                    }
                                }
                            } catch (_: Exception) {
                                toastState = ToastState(
                                    message = context.getString(R.string.auth_error_internet),
                                    type = ToastType.ERROR, isVisible = true
                                )
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoTechnological)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.edit_profile_save), style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        CustomToast(state = toastState, onDismiss = { toastState = toastState.copy(isVisible = false) })
    }
}
