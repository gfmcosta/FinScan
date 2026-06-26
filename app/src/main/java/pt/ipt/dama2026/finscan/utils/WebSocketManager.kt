package pt.ipt.dama2026.finscan.utils

import android.content.Context
import android.util.Log
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

private const val TAG = "WebSocketManager"
private const val PING_INTERVAL_MS = 25_000L   // send "ping" every 25 s
private const val RECONNECT_DELAY_MS = 5_000L  // wait 5 s before reconnecting

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
        .pingInterval(0, TimeUnit.SECONDS)   // we handle pings manually
        .build()

    private var socket: WebSocket? = null
    private var isConnected = false
    private var shouldReconnect = false

    /** Emits whenever a report-ready notification is received. Collect in ReportsScreen to refresh immediately. */
    private val _reportReadyEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val reportReadyEvent: SharedFlow<Unit> = _reportReadyEvent.asSharedFlow()

    // ── Public API ────────────────────────────────────────────────────────────

    fun connect(context: Context) {
        if (isConnected) return
        shouldReconnect = true
        scope.launch { openSocket(context) }
    }

    fun disconnect() {
        shouldReconnect = false
        socket?.close(1000, "Logout")
        socket = null
        isConnected = false
        Log.i(TAG, "Disconnected")
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private suspend fun openSocket(context: Context) {
        val token = AuthManager.getInstance(context).getTokenSync() ?: run {
            Log.w(TAG, "No token — skipping WS connect")
            return
        }

        // Build WS URL from the HTTP base URL (http→ws, https→wss)
        val baseUrl = ApiClient.ROOT_URL
            .replace("https://", "wss://")
            .replace("http://",  "ws://")
        val wsUrl = "${baseUrl}api/v1/notifications/ws?token=$token"

        val request = Request.Builder().url(wsUrl).build()
        socket = httpClient.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(ws: WebSocket, response: Response) {
                isConnected = true
                Log.i(TAG, "Connected to $wsUrl")
                // Start heartbeat pings
                scope.launch {
                    while (isConnected) {
                        delay(PING_INTERVAL_MS)
                        ws.send("ping")
                    }
                }
            }

            override fun onMessage(ws: WebSocket, text: String) {
                Log.d(TAG, "Message: $text")
                handleMessage(context, text)
            }

            override fun onMessage(ws: WebSocket, bytes: ByteString) {
                handleMessage(context, bytes.utf8())
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Log.w(TAG, "WS failure: ${t.message}")
                scheduleReconnect(context)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                isConnected = false
                Log.i(TAG, "WS closed: $code $reason")
                if (code != 1000) scheduleReconnect(context)
            }
        })
    }

    private fun handleMessage(context: Context, text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type")
            if (type == "notification") {
                val title  = json.optString("title", "FinScan")
                val body   = json.optString("body", "")
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

    private fun scheduleReconnect(context: Context) {
        if (!shouldReconnect) return
        Log.i(TAG, "Reconnecting in ${RECONNECT_DELAY_MS}ms…")
        scope.launch {
            delay(RECONNECT_DELAY_MS)
            if (shouldReconnect) openSocket(context)
        }
    }
}
