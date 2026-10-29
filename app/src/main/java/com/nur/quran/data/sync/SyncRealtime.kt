package com.nur.quran.data.sync

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import com.nur.quran.data.db.dao.QuranDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.collect
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
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SyncRealtime @Inject constructor(
    private val syncService: SyncService,
    private val rnApiClient: RnApiClient,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val dao: QuranDao,
    @ApplicationContext private val context: Context,
) {
    companion object {
        /**
         * Periodic reconciliation cadence (also uploads local edits with a live socket) —
         * README P4/P5 documents "≤60s + on-resume".
         */
        const val POLL_FALLBACK_MS = 60_000L

        /** Reconnect pause after a healthy socket session ended. */
        const val RECONNECT_MS = 1_000L
        const val NORMAL_CLOSE = 1000

        /** Frame the RN backend sends to a user's open sockets. */
        const val FRAME_TYPE_SYNC_UPDATED = "sync.updated"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var debounceJob: Job? = null
    @Volatile private var activeUserId: String? = null
    private var localPushJob: Job? = null
    private var localRetryJob: Job? = null
    @Volatile private var localDirty = false
    private var firstLocalChange = 0L

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
    @Synchronized
    fun start(userId: String) = startRn(userId)

    @Synchronized
    fun stop() {
        localPushJob?.cancel();localPushJob=null
        localRetryJob?.cancel();localRetryJob=null
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
                launch { localChanges().collect { scheduleLocalPush(userId) } }
                launch { connectivityChanges().collect { if (activeUserId == userId) { syncService.sync();scheduleLocalPush(userId) } } }
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
        syncService.sync()
        var failures = 0
        while (true) {
            try {
                val rejected = rnRejectedToken
                val token = if (rejected != null) {
                    rnRejectedToken = null
                    rnApiClient.refreshRejectedAccessToken(rejected)
                } else rnApiClient.ensureValidAccessToken()
                if (!token.isNullOrBlank() && awaitRnSocket(userId, token)) failures = 0
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // The independent poll loop keeps sync alive while reconnecting.
            }
            runCatching { syncService.sync() }
            delay((minOf(30_000L, RECONNECT_MS * (1L shl minOf(failures++,5))) * kotlin.random.Random.nextDouble(0.8,1.2)).toLong())
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
            val done = java.util.concurrent.atomic.AtomicBoolean(false)
            fun finish(value: Boolean) {
                if (done.compareAndSet(false,true)) {
                    cont.resume(value)
                }
            }
            val request = Request.Builder().url(rnSocketUrl())
                .header("Authorization", "Bearer $token").build()
            val socket = okHttpClient.newBuilder().pingInterval(20, java.util.concurrent.TimeUnit.SECONDS).build().newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    if (!cont.isActive || activeUserId != userId || rnApiClient.storedUserId() != userId) { webSocket.cancel();return }
                    rnSocket = webSocket
                    connected = true
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    onRnFrame(text, userId)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (rnSocket === webSocket) rnSocket = null
                    if (activeUserId == userId && (response?.code == 401 || response?.code == 403)) rnRejectedToken = token
                    finish(connected)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    if (activeUserId == userId && code == 1008) rnRejectedToken = token
                    webSocket.close(code, reason)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (activeUserId == userId && code == 1008) rnRejectedToken = token
                    if (rnSocket === webSocket) rnSocket = null
                    finish(connected)
                }
            })
            cont.invokeOnCancellation {
                runCatching { socket.close(NORMAL_CLOSE, "cancelled") }
                if (rnSocket === socket) rnSocket = null
            }
        }

    /** Observe actual local stores instead of waiting for the 60-second safety poll. */
    private fun localChanges() = merge(
        callbackFlow<Unit> {
            val prefs = listOf("hifdh_settings","Settings","PlannerSettings","tour_prefs","library_sync")
                .map { context.getSharedPreferences(it,Context.MODE_PRIVATE) }
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _,_ -> trySend(Unit) }
            prefs.forEach { it.registerOnSharedPreferenceChangeListener(listener) }
            awaitClose { prefs.forEach { it.unregisterOnSharedPreferenceChangeListener(listener) } }
        },
        dao.getAllBookmarks().drop(1).map { Unit },
        dao.getAllCollections().drop(1).map { Unit },
        dao.getAllCollectionItems().drop(1).map { Unit },
        dao.getAllReadingSessions().drop(1).map { Unit },
        dao.getRecentlyReadHistory().drop(1).map { Unit },
        dao.observePlannerCache().drop(1).map { Unit },
    )

    private fun connectivityChanges() = callbackFlow<Unit> {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(Unit) }
        }
        manager.registerDefaultNetworkCallback(callback)
        awaitClose { runCatching { manager.unregisterNetworkCallback(callback) } }
    }

    private fun scheduleLocalPush(userId: String) {
        if (activeUserId != userId) return
        localDirty = true
        val now = System.nanoTime() / 1_000_000L
        if (firstLocalChange == 0L) firstLocalChange = now
        localPushJob?.cancel()
        localPushJob = scope.launch {
            delay(minOf(300L,maxOf(0L,2_000L - (now-firstLocalChange))))
            firstLocalChange = 0L
            if (localRetryJob?.isActive == true) return@launch
            localRetryJob = scope.launch {
                var failures = 0
                while (activeUserId == userId && rnApiClient.storedUserId() == userId) {
                    localDirty = false
                    val saved = syncService.push()
                    if (saved && !localDirty) break
                    if (saved) { failures=0;delay(300) }
                    else delay((minOf(30_000L,1_000L * (1L shl minOf(failures++,5))) * kotlin.random.Random.nextDouble(0.8,1.2)).toLong())
                }
            }
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
            delay(100)
            val result = runCatching { syncService.sync() }.getOrNull()
            if (result == "pulled") {
                _lastRemoteSyncAt.value = System.currentTimeMillis()
            }
        }
    }
}
