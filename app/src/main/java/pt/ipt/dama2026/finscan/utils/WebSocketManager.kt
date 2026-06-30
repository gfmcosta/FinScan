package pt.ipt.dama2026.finscan.utils

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import pt.ipt.dama2026.finscan.data.api.ApiClient
import pt.ipt.dama2026.finscan.data.datastore.AuthManager
import pt.ipt.dama2026.finscan.data.datastore.SettingsManager
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "WebSocketManager"

// Send "ping" every 25s
private const val PING_INTERVAL_MS = 25_000L

// Wait 5s before reconnecting
private const val RECONNECT_DELAY_MS = 5_000L

/**
 * Singleton that maintains a persistent WebSocket connection to the backend
 * notification service while the user is logged in.
 *
 * Call [connect] after login and [disconnect] on logout.
 * Incoming notification messages automatically fire a local notification
 * via [NotificationHelper] (if the user has enabled notifications).
 */
object WebSocketManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient = OkHttpClient.Builder()
        .pingInterval(0, TimeUnit.SECONDS)
        .build()

    private var socket: WebSocket? = null
    private var isConnected = false
    private var shouldReconnect = false

    // Emits whenever a report-ready notification is received. Collect in ReportsScreen to refresh immediately.
    private val _reportReadyEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val reportReadyEvent: SharedFlow<Unit> = _reportReadyEvent.asSharedFlow()

    /**
     * Connects to the backend WebSocket service.
     * @param context The application context.
     */
    fun connect(context: Context) {
        if (isConnected) return
        shouldReconnect = true
        scope.launch { openSocket(context) }
    }

    /**
     * Disconnects from the backend WebSocket service.
     */
    fun disconnect() {
        shouldReconnect = false
        socket?.close(1000, "Logout")
        socket = null
        isConnected = false
        Log.i(TAG, "Disconnected")
    }

    /**
     * Opens a WebSocket connection to the backend notification service.
     * @param context The application context.
     */
    private suspend fun openSocket(context: Context) {
        val token = AuthManager.getInstance(context).getTokenSync() ?: run {
            Log.w(TAG, "No token — skipping WS connect")
            return
        }

        // Build WS URL from the HTTP base URL (http→ws, https→wss)
        val baseUrl = ApiClient.ROOT_URL
            .replace("https://", "wss://")
            .replace("http://", "ws://")
        val wsUrl = "${baseUrl}api/v1/notifications/ws?token=$token"

        val request = Request.Builder().url(wsUrl).build()
        socket = httpClient.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                Log.i(TAG, "Connected to $wsUrl")
                // Start heartbeat pings
                scope.launch {
                    while (isConnected) {
                        delay(PING_INTERVAL_MS.milliseconds)
                        webSocket.send("ping")
                    }
                }
            }

            @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Message: $text")
                handleMessage(context, text)
            }

            @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handleMessage(context, bytes.utf8())
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Log.w(TAG, "WS failure: ${t.message}")
                scheduleReconnect(context)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                Log.i(TAG, "WS closed: $code $reason")
                if (code != 1000) scheduleReconnect(context)
            }
        })
    }

    /**
     * Handles an incoming WebSocket message.
     * @param context The application context.
     * @param text The message text.
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun handleMessage(context: Context, text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type")
            if (type == "notification") {
                val title = json.optString("title", "FinScan")
                val body = json.optString("body", "")
                val action = json.optJSONObject("data")?.optString("action") ?: ""
                scope.launch {
                    // Trigger an immediate UI refresh in ReportsScreen
                    if (action == "open_reports") {
                        _reportReadyEvent.emit(Unit)
                    }
                    // Fire local notification if the user has enabled them
                    val enabled = SettingsManager.getInstance(context)
                        .notificationsEnabled.first()
                    if (enabled) {
                        NotificationHelper.sendReportReady(context, title, body)
                    }
                }
            }
            // "pong" and "connected" frames are silently ignored
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse message: $text", e)
        }
    }

    /**
     * Schedules a reconnection attempt after a delay.
     * @param context The application context.
     */
    private fun scheduleReconnect(context: Context) {
        if (!shouldReconnect) return
        Log.i(TAG, "Reconnecting in ${RECONNECT_DELAY_MS}ms…")
        scope.launch {
            delay(RECONNECT_DELAY_MS.milliseconds)
            if (shouldReconnect) openSocket(context)
        }
    }
}
