package pt.ipt.dama2026.finscan.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import pt.ipt.dama2026.finscan.R
import pt.ipt.dama2026.finscan.data.api.ApiClient
import pt.ipt.dama2026.finscan.data.api.models.ReceiptOCRRequest
import pt.ipt.dama2026.finscan.data.api.models.ReceiptResponse
import pt.ipt.dama2026.finscan.data.api.services.ScanApiService
import pt.ipt.dama2026.finscan.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

private enum class ScanState { PERMISSION, CAMERA, PROCESSING, ERROR, SUCCESS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var scanState by remember {
        mutableStateOf(
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) ScanState.CAMERA else ScanState.PERMISSION
        )
    }
    var scannedReceipt by remember { mutableStateOf<ReceiptResponse?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    var showPickerSheet by remember { mutableStateOf(false) }

    // Shared: read a URI and send to API
    suspend fun sendUri(uri: Uri) {
        try {
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null || bytes.isEmpty()) {
                errorMessage = context.getString(R.string.scan_error_generic)
                scanState = ScanState.ERROR
                return
            }
            val api = ApiClient.getRetrofit().create(ScanApiService::class.java)
            val imageBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val response = api.scanReceipt(ReceiptOCRRequest(imageBase64 = imageBase64, mimeType = mimeType))
            if (response.isSuccessful && response.body() != null) {
                scannedReceipt = response.body()
                scanState = ScanState.SUCCESS
            } else {
                val isNotReceipt = response.code() == 422 &&
                    response.errorBody()?.string()?.contains("not_a_receipt") == true
                errorMessage = if (isNotReceipt) {
                    context.getString(R.string.scan_error_not_receipt)
                } else {
                    context.getString(R.string.scan_error_generic)
                }
                scanState = ScanState.ERROR
            }
        } catch (e: Exception) {
            Log.e("ScanScreen", "File scan failed", e)
            errorMessage = context.getString(R.string.auth_error_internet)
            scanState = ScanState.ERROR
        }
    }

    // Gallery launcher — images only
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scanState = ScanState.PROCESSING
        scope.launch { sendUri(uri) }
    }

    // File launcher — images + PDFs
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scanState = ScanState.PROCESSING
        scope.launch { sendUri(uri) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        scanState = if (granted) ScanState.CAMERA else ScanState.PERMISSION
    }

    val onPickPressed: () -> Unit = { showPickerSheet = true }

    // After a successful scan, show the detail screen for validation
    if (scanState == ScanState.SUCCESS && scannedReceipt != null) {
        ReceiptDetailScreen(
            receipt = scannedReceipt!!,
            onBack = {
                scannedReceipt = null
                scanState = ScanState.CAMERA
            }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (scanState) {
            ScanState.PERMISSION -> {
                PermissionScreen(
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onPickFile = onPickPressed
                )
            }
            ScanState.CAMERA, ScanState.PROCESSING -> {
                CameraContent(
                    isProcessing = scanState == ScanState.PROCESSING,
                    onPhotoTaken = { file ->
                        scanState = ScanState.PROCESSING
                        scope.launch {
                            try {
                                val api = ApiClient.getRetrofit().create(ScanApiService::class.java)
                                val imageBase64 = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
                                val response = api.scanReceipt(ReceiptOCRRequest(imageBase64 = imageBase64))
                                if (response.isSuccessful && response.body() != null) {
                                    scannedReceipt = response.body()
                                    scanState = ScanState.SUCCESS
                                } else {
                                    val isNotReceipt = response.code() == 422 &&
                                        response.errorBody()?.string()?.contains("not_a_receipt") == true
                                    errorMessage = if (isNotReceipt) {
                                        context.getString(R.string.scan_error_not_receipt)
                                    } else {
                                        context.getString(R.string.scan_error_generic)
                                    }
                                    scanState = ScanState.ERROR
                                }
                            } catch (e: Exception) {
                                Log.e("ScanScreen", "Scan failed", e)
                                errorMessage = context.getString(R.string.auth_error_internet)
                                scanState = ScanState.ERROR
                            } finally {
                                file.delete()
                            }
                        }
                    },
                    onPickFile = onPickPressed
                )
            }
            ScanState.ERROR -> {
                ErrorScreen(
                    message = errorMessage,
                    onRetry = { scanState = ScanState.CAMERA }
                )
            }
            ScanState.SUCCESS -> { /* handled above */ }
        }
    }

    // Bottom sheet — choose between gallery or files
    if (showPickerSheet) {
        ModalBottomSheet(onDismissRequest = { showPickerSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = stringResource(R.string.scan_pick_source_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                HorizontalDivider()
                PickerOption(
                    icon = Icons.Default.PhotoLibrary,
                    label = stringResource(R.string.scan_pick_gallery),
                    onClick = {
                        showPickerSheet = false
                        galleryLauncher.launch("image/*")
                    }
                )
                PickerOption(
                    icon = Icons.Default.FolderOpen,
                    label = stringResource(R.string.scan_pick_files),
                    onClick = {
                        showPickerSheet = false
                        fileLauncher.launch(arrayOf("image/*", "application/pdf"))
                    }
                )
            }
        }
    }
}

@Composable
private fun PickerOption(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = IndigoTechnological,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ─── Permission screen ────────────────────────────────────────────────────────

@Composable
private fun PermissionScreen(
    onRequestPermission: () -> Unit,
    onPickFile: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = IndigoTechnological
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.scan_permission_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.scan_permission_rationale),
                style = MaterialTheme.typography.bodyMedium,
                color = getAdaptiveSubtext(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoTechnological),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.scan_grant_permission))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onPickFile,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = IndigoTechnological)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.scan_pick_file))
            }
        }
    }
}

// ─── Error screen ─────────────────────────────────────────────────────────────

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = Color(0xFFEF4444)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.scan_error_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = getAdaptiveSubtext(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoTechnological),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.scan_retry))
            }
        }
    }
}

// ─── Camera content ───────────────────────────────────────────────────────────

@Composable
private fun CameraContent(
    isProcessing: Boolean,
    onPhotoTaken: (File) -> Unit,
    onPickFile: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e("ScanScreen", "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            try {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            } catch (_: Exception) {}
            executor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Top instruction banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.45f))
                .statusBarsPadding()
                .padding(vertical = 20.dp, horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.scan_instruction),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }

        // Bottom bar — gallery button left, capture button centre
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.45f))
                .navigationBarsPadding()
                .padding(vertical = 32.dp, horizontal = 32.dp)
        ) {
            // Gallery/file picker button — left
            IconButton(
                onClick = onPickFile,
                enabled = !isProcessing,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(52.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = stringResource(R.string.scan_pick_file),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Capture button — centre
            CaptureButton(
                enabled = !isProcessing,
                onClick = {
                    val photoFile = File(
                        context.cacheDir,
                        "receipt_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
                    )
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                    imageCapture.takePicture(
                        outputOptions,
                        executor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                onPhotoTaken(photoFile)
                            }
                            override fun onError(exception: ImageCaptureException) {
                                Log.e("ScanScreen", "Photo capture failed", exception)
                            }
                        }
                    )
                },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Processing overlay
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(52.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.scan_processing),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.scan_processing_subtitle),
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 48.dp)
                    )
                }
            }
        }
    }
}

// ─── Capture button ───────────────────────────────────────────────────────────

@Composable
private fun CaptureButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .background(if (enabled) IndigoTechnological else Color.Gray, CircleShape)
            )
        }
    }
}
