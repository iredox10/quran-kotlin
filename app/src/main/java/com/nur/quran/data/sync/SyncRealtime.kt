package com.nur.quran.data.sync

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nur.quran.data.api.rn.RnApiClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * Live cross-device sync: listens to the authenticated Heroku WebSocket so
 * a change made on the web app (or another device) triggers a pull within
 * seconds — no waiting for the periodic worker. The pull itself still goes
 * through [SyncService.sync], so watermark + union-merge rules apply and a
 * stale/duplicate event can never clobber local data.
 *
 * Lifecycle: [start] on app foreground (signed in only), [stop] on background
 * and logout. Events are debounced; only documents belonging to [userId] are
 * honored.
 *
 * The backend sends notifications over
 * `GET wss://…/ws` with a Bearer header — the server tells every open socket of
 * the user `{ type: "sync.updated", changedAt }` (no payload; clients pull)
 * after any PUT /v1/sync by that user. A reconciliation loop runs
 * [SyncService.sync] every 60s, including while connected, to upload local edits.
 * Foregrounding and socket interruptions also trigger an immediate sync.
 */
@Singleton
class SyncRealtime @Inject constructor(
    private val syncService: SyncService,
    private val rnApiClient: RnApiClient,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
) {
    companion object {
        /**
         * Periodic reconciliation cadence (also uploads local edits with a live socket) —
         * README P4/P5 documents "≤60s + on-resume".
         */
        const val POLL_FALLBACK_MS = 60_000L

        /** Reconnect pause after a healthy socket session ended. */
        const val RECONNECT_MS = 5_000L
        const val NORMAL_CLOSE = 1000

        /** Frame the RN backend sends to a user's open sockets. */
        const val FRAME_TYPE_SYNC_UPDATED = "sync.updated"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var debounceJob: Job? = null
    private var activeUserId: String? = null

    /** Heroku supervision loop with its live socket (stopped via [stop]). */
    private var rnJob: Job? = null
    private var rnSocket: WebSocket? = null
    @Volatile private var rnRejectedToken: String? = null

    private val _lastRemoteSyncAt = MutableStateFlow<Long?>(null)
    val lastRemoteSyncAt: StateFlow<Long?> = _lastRemoteSyncAt.asStateFlow()

    private val rnMessageType = object : TypeToken<Map<String, Any?>>() {}.type

    /**
     * Opens the subscription/monitoring for [userId]. Safe to call repeatedly;
     * a live subscription for the same user is kept as-is.
     */
    fun start(userId: String) = startRn(userId)

    fun stop() {
        debounceJob?.cancel()
        debounceJob = null
        rnJob?.cancel()
        rnJob = null
        runCatching { rnSocket?.close(NORMAL_CLOSE, "stop") }
        rnSocket = null
        activeUserId = null
        rnRejectedToken = null
    }

    private fun startRn(userId: String) {
        if (activeUserId == userId && rnJob != null) return
        stop()
        activeUserId = userId
        rnJob = scope.launch {
            kotlinx.coroutines.coroutineScope {
                // Reconcile local edits even while the WebSocket remains connected.
                launch {
                    while (true) {
                        delay(POLL_FALLBACK_MS)
                        syncService.sync()
                    }
                }
                rnListenLoop(userId)
            }
        }
    }

    /**
     * Socket first, polls as the fallback: every session cut (or failure to
     * open) triggers one immediate [SyncService.sync] (≤ the documented floor;
     * the debounced frames keep live changes fast) and the loop re-tries the
     * socket — cadence [POLL_FALLBACK_MS] while down. A signed-out token keeps
     * the polite poll; the caller stops this loop on logout.
     */
    private suspend fun rnListenLoop(userId: String) {
        syncService.sync() // Pull on foreground even when the socket stays open indefinitely.
        while (true) {
            try {
                val rejected = rnRejectedToken
                val token = if (rejected != null) {
                    rnRejectedToken = null
                    rnApiClient.refreshRejectedAccessToken(rejected)
                } else rnApiClient.ensureValidAccessToken()
                if (!token.isNullOrBlank()) awaitRnSocket(userId, token)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // The independent poll loop keeps sync alive while reconnecting.
            }
            runCatching { syncService.sync() }
            delay(RECONNECT_MS)
        }
    }

    /** URL of the RN realtime socket: same host, token at upgrade (wss://…/ws). */
    internal fun rnSocketUrl(): String {
        val httpsUrl = rnApiClient.baseUrl.trimEnd('/') + "/ws"
        return httpsUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://")
    }

    /**
     * Opens the WS and suspends until it closes or fails. Frames recognized as
     * `sync.updated` (contract: no payload, clients pull) debounce into a sync.
     * Returns true when the socket had connected at least once.
     */
    private suspend fun awaitRnSocket(userId: String, token: String): Boolean =
        suspendCancellableCoroutine { cont ->
            var connected = false
            var done = false
            fun finish(value: Boolean) {
                if (!done) {
                    done = true
                    cont.resume(value)
                }
            }
            val request = Request.Builder().url(rnSocketUrl())
                .header("Authorization", "Bearer $token").build()
            val socket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    rnSocket = webSocket
                    connected = true
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    onRnFrame(text, userId)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    rnSocket = null
                    if (response?.code == 401 || response?.code == 403) rnRejectedToken = token
                    finish(connected)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    if (code == 1008) rnRejectedToken = token
                    webSocket.close(code, reason)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (code == 1008) rnRejectedToken = token
                    rnSocket = null
                    finish(connected)
                }
            })
            cont.invokeOnCancellation {
                runCatching { socket.close(NORMAL_CLOSE, "cancelled") }
                rnSocket = null
            }
        }

    private fun onRnFrame(text: String, userId: String) {
        val frame = runCatching {
            gson.fromJson<Map<String, Any?>>(text, rnMessageType)
        }.getOrNull() ?: return
        val type = frame["type"]?.toString() ?: return
        if (type != FRAME_TYPE_SYNC_UPDATED || activeUserId != userId ||
            rnApiClient.storedUserId() != userId) return
        // Contract: no payload — the socket only announces "some sync changed for
        // this user"; the pull itself validates the watermark + owner.
        debouncedSync()
    }

    private fun debouncedSync() {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(2000)
            val result = runCatching { syncService.sync() }.getOrNull()
            if (result == "pulled") {
                _lastRemoteSyncAt.value = System.currentTimeMillis()
            }
        }
    }
}
