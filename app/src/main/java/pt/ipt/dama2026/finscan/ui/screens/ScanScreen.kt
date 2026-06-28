package pt.ipt.dama2026.finscan.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.view.Surface
import androidx.exifinterface.media.ExifInterface
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
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
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
import kotlin.coroutines.resume

private enum class ScanState { PERMISSION, LOCATION_PERMISSION, CAMERA, PROCESSING, ERROR, SUCCESS }

// ─── Location helper ──────────────────────────────────────────────────────────

@SuppressLint("MissingPermission")
private suspend fun getCurrentLocation(context: android.content.Context): Pair<Double, Double>? {
    val fused = LocationServices.getFusedLocationProviderClient(context)
    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!hasFine && !hasCoarse) return null

    return suspendCancellableCoroutine { cont ->
        val cts = CancellationTokenSource()
        val priority = if (hasFine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
        fused.getCurrentLocation(priority, cts.token)
            .addOnSuccessListener { loc -> cont.resume(loc?.let { Pair(it.latitude, it.longitude) }) }
            .addOnFailureListener { cont.resume(null) }
        cont.invokeOnCancellation { cts.cancel() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    // Initial state: ask camera first, then location
    var scanState by remember {
        mutableStateOf(
            when {
                !hasCameraPermission -> ScanState.PERMISSION
                !hasLocationPermission -> ScanState.LOCATION_PERMISSION
                else -> ScanState.CAMERA
            }
        )
    }
    var scannedReceipt by remember { mutableStateOf<ReceiptResponse?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    var showPickerSheet by remember { mutableStateOf(false) }
    var capturedFile by remember { mutableStateOf<File?>(null) }

    // ── API call ──────────────────────────────────────────────────────────────
    suspend fun sendToApi(bytes: ByteArray, mimeType: String, location: Pair<Double, Double>?) {
        try {
            val api = ApiClient.getRetrofit().create(ScanApiService::class.java)
            val imageBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val response = api.scanReceipt(
                ReceiptOCRRequest(
                    imageBase64 = imageBase64,
                    mimeType = mimeType,
                    latitude = location?.first,
                    longitude = location?.second,
                )
            )
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
        }
    }

    // ── Launchers ─────────────────────────────────────────────────────────────
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Camera granted → now ask location
            val locGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            scanState = if (locGranted) ScanState.CAMERA else ScanState.LOCATION_PERMISSION
        } else {
            scanState = ScanState.PERMISSION
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Whether granted or denied, proceed to camera
        scanState = ScanState.CAMERA
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scanState = ScanState.PROCESSING
        scope.launch {
            try {
                val location = getCurrentLocation(context)
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null || bytes.isEmpty()) {
                    errorMessage = context.getString(R.string.scan_error_generic)
                    scanState = ScanState.ERROR
                    return@launch
                }
                sendToApi(bytes, mimeType, location)
            } catch (e: Exception) {
                errorMessage = context.getString(R.string.scan_error_generic)
                scanState = ScanState.ERROR
            }
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scanState = ScanState.PROCESSING
        scope.launch {
            try {
                val location = getCurrentLocation(context)
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null || bytes.isEmpty()) {
                    errorMessage = context.getString(R.string.scan_error_generic)
                    scanState = ScanState.ERROR
                    return@launch
                }
                sendToApi(bytes, mimeType, location)
            } catch (e: Exception) {
                errorMessage = context.getString(R.string.scan_error_generic)
                scanState = ScanState.ERROR
            }
        }
    }

    val onPickPressed: () -> Unit = { showPickerSheet = true }

    // ── Success ───────────────────────────────────────────────────────────────
    if (scanState == ScanState.SUCCESS && scannedReceipt != null) {
        ReceiptDetailScreen(
            receipt = scannedReceipt!!,
            onBack = { scannedReceipt = null; scanState = ScanState.CAMERA }
        )
        return
    }

    // ── Main content ──────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        when (scanState) {
            ScanState.PERMISSION -> PermissionScreen(
                onRequestPermission = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                onPickFile = onPickPressed
            )

            ScanState.LOCATION_PERMISSION -> LocationPermissionScreen(
                onRequestPermission = {
                    locationPermissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                },
                onSkip = { scanState = ScanState.CAMERA }
            )

            ScanState.CAMERA -> CameraContent(
                onPhotoTaken = { file ->
                    capturedFile = file
                    scanState = ScanState.PROCESSING
                    scope.launch {
                        try {
                            val location = getCurrentLocation(context)
                            sendToApi(file.readBytes(), "image/jpeg", location)
                        } catch (e: Exception) {
                            errorMessage = context.getString(R.string.scan_error_generic)
                            scanState = ScanState.ERROR
                        } finally {
                            file.delete()
                            capturedFile = null
                        }
                    }
                },
                onPickFile = onPickPressed
            )

            ScanState.PROCESSING -> {
                val frozen = capturedFile
                if (frozen != null) {
                    FrozenPhotoProcessing(file = frozen)
                } else {
                    GenericProcessingScreen()
                }
            }

            ScanState.ERROR -> ErrorScreen(
                message = errorMessage,
                onRetry = { scanState = ScanState.CAMERA }
            )

            ScanState.SUCCESS -> { /* handled above */ }
        }
    }

    // ── Picker bottom sheet ───────────────────────────────────────────────────
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
                    onClick = { showPickerSheet = false; galleryLauncher.launch("image/*") }
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
        Icon(imageVector = icon, contentDescription = null, tint = IndigoTechnological, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ─── Camera permission screen ─────────────────────────────────────────────────

@Composable
private fun PermissionScreen(onRequestPermission: () -> Unit, onPickFile: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(72.dp), tint = IndigoTechnological)
            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(R.string.scan_permission_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.scan_permission_rationale), style = MaterialTheme.typography.bodyMedium, color = getAdaptiveSubtext(), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onRequestPermission, colors = ButtonDefaults.buttonColors(containerColor = IndigoTechnological), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.scan_grant_permission))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onPickFile, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = IndigoTechnological)) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.scan_pick_file))
            }
        }
    }
}

// ─── Location permission screen ───────────────────────────────────────────────

@Composable
private fun LocationPermissionScreen(onRequestPermission: () -> Unit, onSkip: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(72.dp), tint = IndigoTechnological)
            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(R.string.scan_location_permission_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.scan_location_permission_rationale), style = MaterialTheme.typography.bodyMedium, color = getAdaptiveSubtext(), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onRequestPermission, colors = ButtonDefaults.buttonColors(containerColor = IndigoTechnological), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.scan_grant_location))
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.scan_skip_location), color = getAdaptiveSubtext())
            }
        }
    }
}

// ─── Error screen ─────────────────────────────────────────────────────────────

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(72.dp), tint = Color(0xFFEF4444))
            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(R.string.scan_error_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = getAdaptiveSubtext(), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = IndigoTechnological), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.scan_retry))
            }
        }
    }
}

// ─── Camera content ───────────────────────────────────────────────────────────

@Composable
private fun CameraContent(onPhotoTaken: (File) -> Unit, onPickFile: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display?.rotation ?: Surface.ROTATION_0
            } else {
                @Suppress("DEPRECATION")
                (context.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay.rotation
            }
            imageCapture.targetRotation = rotation
            val preview = Preview.Builder()
                .setTargetRotation(rotation)
                .build()
                .also { it.surfaceProvider = previewView.surfaceProvider }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (e: Exception) { Log.e("ScanScreen", "Camera binding failed", e) }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            try { ProcessCameraProvider.getInstance(context).get().unbindAll() } catch (_: Exception) {}
            executor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Top instruction
        Box(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.45f)).statusBarsPadding()
                .padding(vertical = 20.dp, horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.scan_instruction), color = Color.White, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        }

        // Bottom bar
        Box(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.45f)).navigationBarsPadding()
                .padding(vertical = 32.dp, horizontal = 32.dp)
        ) {
            // Gallery button — left
            IconButton(
                onClick = onPickFile,
                modifier = Modifier.align(Alignment.CenterStart).size(52.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = stringResource(R.string.scan_pick_file), tint = Color.White, modifier = Modifier.size(28.dp))
            }

            // Capture button — centre
            CaptureButton(
                enabled = true,
                modifier = Modifier.align(Alignment.Center),
                onClick = {
                    val photoFile = File(context.cacheDir, "receipt_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg")
                    imageCapture.takePicture(ImageCapture.OutputFileOptions.Builder(photoFile).build(), executor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) { onPhotoTaken(photoFile) }
                            override fun onError(e: ImageCaptureException) { Log.e("ScanScreen", "Capture failed", e) }
                        })
                }
            )
        }

    }
}

// ─── Frozen photo + processing overlay (shown after capture, camera disposed) ──

private fun rotateBitmapToExif(file: File): Bitmap? {
    val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
    val exif = try { ExifInterface(file.absolutePath) } catch (_: Exception) { return bitmap }
    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90  -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (degrees == 0f) return bitmap
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        .also { if (it != bitmap) bitmap.recycle() }
}

@Composable
private fun FrozenPhotoProcessing(file: File) {
    val bitmap = remember(file) { rotateBitmapToExif(file) }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (bitmap != null) {
            AndroidView(
                factory = { ctx ->
                    android.widget.ImageView(ctx).apply {
                        scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    }
                },
                update = { view -> view.setImageBitmap(bitmap) },
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(52.dp), strokeWidth = 3.dp)
                Spacer(modifier = Modifier.height(20.dp))
                Text(stringResource(R.string.scan_processing), color = Color.White, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.scan_processing_subtitle), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 48.dp))
            }
        }
    }
}

// ─── Generic loading screen (gallery / PDF) ───────────────────────────────────

@Composable
private fun GenericProcessingScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = IndigoTechnological, modifier = Modifier.size(52.dp), strokeWidth = 3.dp)
            Spacer(modifier = Modifier.height(20.dp))
            Text(stringResource(R.string.scan_processing), color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.scan_processing_subtitle), color = getAdaptiveSubtext(), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 48.dp))
        }
    }
}

// ─── Capture button ───────────────────────────────────────────────────────────

@Composable
private fun CaptureButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier.size(80.dp)) {
        Box(modifier = Modifier.size(80.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(62.dp).background(if (enabled) IndigoTechnological else Color.Gray, CircleShape))
        }
    }
}
