package pt.ipt.dama2026.finscan.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dama2026.finscan.R
import pt.ipt.dama2026.finscan.data.api.ApiClient
import pt.ipt.dama2026.finscan.data.api.models.GenerateReportRequest
import pt.ipt.dama2026.finscan.data.api.models.ReportResponse
import pt.ipt.dama2026.finscan.data.api.services.ReceiptApiService
import pt.ipt.dama2026.finscan.data.api.services.ReportApiService
import pt.ipt.dama2026.finscan.data.datastore.SettingsManager
import pt.ipt.dama2026.finscan.ui.theme.*
import pt.ipt.dama2026.finscan.utils.WebSocketManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ── Date helpers ──────────────────────────────────────────────────────────────

private val API_DATE_FMT  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
private val DISP_DATE_FMT = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
private val DISP_DT_FMT   = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

private fun Long.toApiDate(): String     = API_DATE_FMT.format(Date(this))
private fun Long.toDisplayDate(): String = DISP_DATE_FMT.format(Date(this))
private fun String.toDisplayDate(): String = try {
    DISP_DATE_FMT.format(API_DATE_FMT.parse(this) ?: return this)
} catch (_: Exception) { this }
private fun String.toDisplayDateTime(): String = try {
    val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    DISP_DT_FMT.format(fmt.parse(this) ?: return this)
} catch (_: Exception) { this }

// ── Permission helpers ────────────────────────────────────────────────────────

private enum class ReportsState { PERMISSION, READY }

private fun needsStoragePermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return false
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) != PackageManager.PERMISSION_GRANTED
}

@Composable
private fun StoragePermissionScreen(onRequestPermission: () -> Unit) {
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
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = IndigoTechnological
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.report_storage_permission_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.report_storage_permission_rationale),
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
                Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.report_storage_permission_grant))
            }
        }
    }
}

// ── Status badge ──────────────────────────────────────────────────────────────

@Composable
private fun StatusBadge(status: String) {
    val (label, bg) = when (status) {
        "completed"  -> Pair(stringResource(R.string.report_status_completed),  Color(0xFF4CAF50))
        "generating" -> Pair(stringResource(R.string.report_status_generating), Color(0xFFFF9800))
        else         -> Pair(stringResource(R.string.report_status_failed),     Color(0xFFF44336))
    }
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// ── Report card ───────────────────────────────────────────────────────────────

@Composable
private fun ReportCard(
    report: ReportResponse,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Title row: period + status + delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val periodLabel = when {
                    report.dateFrom == null && report.dateTo == null ->
                        stringResource(R.string.report_period_all_time)
                    else -> {
                        val f = report.dateFrom?.toDisplayDate() ?: "—"
                        val t = report.dateTo?.toDisplayDate()   ?: "—"
                        "$f → $t"
                    }
                }
                Text(
                    text = periodLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                StatusBadge(report.status)
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Created at
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule, null,
                    modifier = Modifier.size(13.dp),
                    tint = getAdaptiveSubtext()
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.report_generated_at, report.createdAt.toDisplayDateTime()),
                    style = MaterialTheme.typography.bodySmall,
                    color = getAdaptiveSubtext()
                )
            }

            // Download button
            if (report.status == "completed") {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoTechnological)
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.report_download))
                }
            }

            // Generating spinner
            if (report.status == "generating") {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(13.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFFFF9800)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.report_generating_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF9800)
                    )
                }
            }
        }
    }
}

// ── Date picker dialog ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    title: String,
    initialMillis: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onConfirm(it) }
                onDismiss()
            }) { Text(stringResource(R.string.receipts_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.receipts_cancel)) }
        }
    ) {
        DatePicker(
            state = state,
            title = { Text(title, modifier = Modifier.padding(start = 24.dp, top = 16.dp)) }
        )
    }
}

// ── PDF download ──────────────────────────────────────────────────────────────

private suspend fun downloadAndOpenPdf(
    context: Context,
    api: ReportApiService,
    reportId: Int,
): Boolean {
    return try {
        val response = api.downloadReport(reportId)
        if (!response.isSuccessful) return false

        val dir  = File(context.cacheDir, "reports").also { it.mkdirs() }
        val file = File(dir, "report_$reportId.pdf")
        file.outputStream().use { out -> response.body()?.byteStream()?.copyTo(out) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        withContext(Dispatchers.Main) { context.startActivity(intent) }
        true
    } catch (_: Exception) { false }
}

// ── Month names ───────────────────────────────────────────────────────────────

private fun monthNames(locale: String): List<String> {
    val javaLocale = java.util.Locale.forLanguageTag(locale)
    // getMonths() returns 13 entries (last one empty), take only the first 12
    return java.text.DateFormatSymbols(javaLocale).months.take(12)
        .map { it.replaceFirstChar { c -> c.uppercase() } }
}

// ── Main screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen() {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val api     = remember { ApiClient.getRetrofit().create(ReportApiService::class.java) }
    val currentLocale by SettingsManager.getInstance(context).language.collectAsState(initial = "en")

    // ── Permission gate ───────────────────────────────────────────────────────
    var reportsState by remember {
        mutableStateOf(
            if (needsStoragePermission(context)) ReportsState.PERMISSION else ReportsState.READY
        )
    }
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> reportsState = if (granted) ReportsState.READY else ReportsState.PERMISSION }

    if (reportsState == ReportsState.PERMISSION) {
        StoragePermissionScreen(
            onRequestPermission = {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        )
        return
    }

    // ── State ─────────────────────────────────────────────────────────────────
    val PAGE_SIZE = 10

    var reports      by remember { mutableStateOf<List<ReportResponse>>(emptyList()) }
    var isLoading    by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var hasReceiptsInRange by remember { mutableStateOf(true) } // true by default to avoid initial flash
    var isRangeChecking    by remember { mutableStateOf(false) }
    var currentPage  by remember { mutableIntStateOf(1) }
    var hasNextPage  by remember { mutableStateOf(false) }

    // Filter state
    var filterMonth  by remember { mutableStateOf<Int?>(null) }
    var filterYear   by remember { mutableStateOf<Int?>(null) }
    var showMonthMenu by remember { mutableStateOf(false) }
    var showYearMenu  by remember { mutableStateOf(false) }
    val currentYear  = Calendar.getInstance().get(Calendar.YEAR)
    val years        = (currentYear downTo currentYear - 5).toList()

    // Generate form state
    var sinceForever   by remember { mutableStateOf(false) }
    var dateFromMs     by remember { mutableStateOf<Long?>(null) }
    var dateToMs       by remember { mutableStateOf<Long?>(null) }
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker   by remember { mutableStateOf(false) }
    var isGenerating   by remember { mutableStateOf(false) }
    var dateError      by remember { mutableStateOf("") }

    val todayMs = remember { Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
    }.timeInMillis }

    fun validateDates(from: Long?, to: Long?): String {
        val futureMsg  = context.getString(R.string.report_error_future_date)
        val orderMsg   = context.getString(R.string.report_error_date_order)
        if (from != null && from > todayMs) return futureMsg
        if (to   != null && to   > todayMs) return futureMsg
        if (from != null && to != null && from > to) return orderMsg
        return ""
    }

    // Delete dialog
    var reportToDelete by remember { mutableStateOf<ReportResponse?>(null) }

    // ── Load page ─────────────────────────────────────────────────────────────
    suspend fun loadPage(page: Int) {
        isLoading = true
        errorMessage = ""
        try {
            val resp = api.getReports(
                skip  = (page - 1) * PAGE_SIZE,
                limit = PAGE_SIZE + 1,
                month = filterMonth,
                year  = filterYear,
            )
            if (resp.isSuccessful) {
                val body = resp.body() ?: emptyList()
                hasNextPage = body.size > PAGE_SIZE
                reports = if (hasNextPage) body.dropLast(1) else body
            } else {
                errorMessage = context.getString(R.string.report_load_error)
            }
        } catch (_: Exception) {
            errorMessage = context.getString(R.string.auth_error_internet)
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadPage(1) }

    // Re-load when filters change
    LaunchedEffect(filterMonth, filterYear) {
        currentPage = 1
        loadPage(1)
    }

    // Check receipts for the selected date range (re-fires whenever dates or toggle change)
    LaunchedEffect(sinceForever, dateFromMs, dateToMs) {
        isRangeChecking = true
        try {
            val receiptApi = ApiClient.getRetrofit().create(ReceiptApiService::class.java)
            val fromStr = if (!sinceForever) dateFromMs?.toApiDate() else null
            val toStr   = if (!sinceForever) dateToMs?.toApiDate()   else null
            val resp = receiptApi.getStats(startDate = fromStr, endDate = toStr)
            hasReceiptsInRange = resp.isSuccessful &&
                (resp.body()?.byCategory?.isNotEmpty() == true)
        } catch (_: Exception) {
            hasReceiptsInRange = true // don't block on network error
        } finally {
            isRangeChecking = false
        }
    }

    // ── Polling while any report is "generating" (UI update only) ────────────
    // Notifications are fired exclusively by WebSocketManager to avoid duplicates.
    val hasGenerating = reports.any { it.status == "generating" }
    LaunchedEffect(hasGenerating) {
        if (hasGenerating) {
            while (true) {
                delay(3_000)
                loadPage(currentPage)
                if (reports.none { it.status == "generating" }) break
            }
        }
    }

    // ── WebSocket-triggered refresh ───────────────────────────────────────────
    // When the backend sends a "report ready" notification the WS manager emits
    // an event here so the list refreshes immediately — before the next poll tick.
    LaunchedEffect(Unit) {
        WebSocketManager.reportReadyEvent.collect {
            loadPage(currentPage)
        }
    }

    // ── Date pickers ──────────────────────────────────────────────────────────
    if (showFromPicker) {
        DatePickerModal(
            title = stringResource(R.string.report_date_from),
            initialMillis = dateFromMs,
            onDismiss = { showFromPicker = false },
            onConfirm = { ms ->
                dateFromMs = ms
                dateError = validateDates(ms, dateToMs)
            }
        )
    }
    if (showToPicker) {
        DatePickerModal(
            title = stringResource(R.string.report_date_to),
            initialMillis = dateToMs,
            onDismiss = { showToPicker = false },
            onConfirm = { ms ->
                dateToMs = ms
                dateError = validateDates(dateFromMs, ms)
            }
        )
    }

    // ── Delete dialog ─────────────────────────────────────────────────────────
    if (reportToDelete != null) {
        val deleteTitle   = stringResource(R.string.report_delete_title)
        val deleteMessage = stringResource(R.string.report_delete_message)
        val deleteLabel   = stringResource(R.string.receipts_delete)
        val cancelLabel   = stringResource(R.string.receipts_cancel)
        AlertDialog(
            onDismissRequest = { reportToDelete = null },
            title = { Text(deleteTitle) },
            text  = { Text(deleteMessage) },
            confirmButton = {
                TextButton(onClick = {
                    val id = reportToDelete!!.id
                    reportToDelete = null
                    scope.launch {
                        try {
                            val resp = api.deleteReport(id)
                            if (resp.isSuccessful) {
                                reports = reports.filter { it.id != id }
                            } else {
                                errorMessage = context.getString(R.string.report_delete_error)
                            }
                        } catch (_: Exception) {
                            errorMessage = context.getString(R.string.auth_error_internet)
                        }
                    }
                }) { Text(deleteLabel, color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { reportToDelete = null }) {
                    Text(cancelLabel)
                }
            }
        )
    }

    // ── Layout (same structure as ReceiptsScreen / CategoriesScreen) ──────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {

        // ── Header row ────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.reports_label),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Generate card ─────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.report_generate_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))

                // Since-forever toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.report_since_forever),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = sinceForever,
                        onCheckedChange = { sinceForever = it; dateError = "" },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = IndigoTechnological,
                            checkedTrackColor = IndigoTechnological.copy(alpha = 0.4f)
                        )
                    )
                }

                if (!sinceForever) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showFromPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (dateFromMs != null)
                                "${stringResource(R.string.report_date_from)}: ${dateFromMs!!.toDisplayDate()}"
                            else stringResource(R.string.report_date_from)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showToPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = if (dateError.isNotEmpty() && dateToMs != null)
                            ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (dateToMs != null)
                                "${stringResource(R.string.report_date_to)}: ${dateToMs!!.toDisplayDate()}"
                            else stringResource(R.string.report_date_to)
                        )
                    }
                    if (dateError.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ErrorOutline, null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                dateError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                if (!hasReceiptsInRange && !isRangeChecking) {
                    val noReceiptsMsg = if (!sinceForever && (dateFromMs != null || dateToMs != null))
                        stringResource(R.string.report_no_receipts_in_range)
                    else
                        stringResource(R.string.report_no_receipts)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info, null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            noReceiptsMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        val fromStr = if (!sinceForever) dateFromMs?.toApiDate() else null
                        val toStr   = if (!sinceForever) dateToMs?.toApiDate()   else null
                        // Final guard (shouldn't reach here, but just in case)
                        if (!sinceForever) {
                            val err = validateDates(dateFromMs, dateToMs)
                            if (err.isNotEmpty()) { dateError = err; return@Button }
                        }
                        isGenerating = true
                        errorMessage = ""
                        scope.launch {
                            try {
                                // Read current locale from SettingsManager
                                val locale = SettingsManager.getInstance(context).language.first()
                                val resp = api.generateReport(
                                    GenerateReportRequest(fromStr, toStr, locale)
                                )
                                if (resp.isSuccessful && resp.body() != null) {
                                    reports = listOf(resp.body()!!) + reports
                                } else {
                                    errorMessage = context.getString(R.string.report_generate_error)
                                }
                            } catch (_: Exception) {
                                errorMessage = context.getString(R.string.report_generate_error)
                            } finally {
                                isGenerating = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoTechnological),
                    enabled = !isGenerating && dateError.isEmpty() && hasReceiptsInRange && !isRangeChecking
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Assessment, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.report_generate_button))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Filter row (month + year) ─────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.report_history_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )

            // Month dropdown
            Box {
                OutlinedButton(
                    onClick = { showMonthMenu = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    val months = monthNames(currentLocale)
                    Text(
                        if (filterMonth != null) months[filterMonth!! - 1].take(3) else stringResource(R.string.report_filter_month),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp))
                }
                DropdownMenu(expanded = showMonthMenu, onDismissRequest = { showMonthMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.report_filter_all)) },
                        onClick = { filterMonth = null; showMonthMenu = false }
                    )
                    monthNames(currentLocale).forEachIndexed { i, name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = { filterMonth = i + 1; showMonthMenu = false }
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Year dropdown
            Box {
                OutlinedButton(
                    onClick = { showYearMenu = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        filterYear?.toString() ?: stringResource(R.string.report_filter_year),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp))
                }
                DropdownMenu(expanded = showYearMenu, onDismissRequest = { showYearMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.report_filter_all)) },
                        onClick = { filterYear = null; showYearMenu = false }
                    )
                    years.forEach { y ->
                        DropdownMenuItem(
                            text = { Text(y.toString()) },
                            onClick = { filterYear = y; showYearMenu = false }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Error card ────────────────────────────────────────────────────────
        if (errorMessage.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AmberAlert.copy(alpha = 0.1f))
            ) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(16.dp),
                    color = AmberAlert,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── List / empty / loading ────────────────────────────────────────────
        if (isLoading && reports.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IndigoTechnological)
            }
        } else if (reports.isEmpty() && !isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Assessment,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = getAdaptiveSubtext()
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.report_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = getAdaptiveSubtext(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(reports, key = { it.id }) { report ->
                    ReportCard(
                        report = report,
                        onDownload = {
                            scope.launch(Dispatchers.IO) {
                                downloadAndOpenPdf(context, api, report.id)
                            }
                        },
                        onDelete = { reportToDelete = report }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Pagination ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        val prev = currentPage - 1
                        currentPage = prev
                        scope.launch { loadPage(prev) }
                    },
                    enabled = currentPage > 1
                ) {
                    Icon(Icons.Default.ChevronLeft, null)
                    Text(stringResource(R.string.pagination_prev))
                }
                Text(
                    stringResource(R.string.pagination_page, currentPage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(
                    onClick = {
                        val next = currentPage + 1
                        currentPage = next
                        scope.launch { loadPage(next) }
                    },
                    enabled = hasNextPage
                ) {
                    Text(stringResource(R.string.pagination_next))
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }
    }
}
