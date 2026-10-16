package com.nur.quran.data.sync

import io.appwrite.models.RealtimeSubscription
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Live cross-device sync: subscribes to the `user_sync` collection channel so
 * a change made on the web app (or another device) triggers a pull within
 * seconds — no waiting for the periodic worker. The pull itself still goes
 * through [SyncService.sync], so watermark + union-merge rules apply and a
 * stale/duplicate event can never clobber local data.
 *
 * Lifecycle: [start] on app foreground (signed in only), [stop] on background
 * and logout. Events are debounced; only documents belonging to [userId] are
 * honored.
 */
@Singleton
class SyncRealtime @Inject constructor(
    private val appwriteClient: AppwriteClient,
    private val syncService: SyncService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var subscription: RealtimeSubscription? = null
    private var debounceJob: Job? = null
    private var activeUserId: String? = null

    private val _lastRemoteSyncAt = MutableStateFlow<Long?>(null)
    val lastRemoteSyncAt: StateFlow<Long?> = _lastRemoteSyncAt.asStateFlow()

    /**
     * Opens the collection subscription for [userId]. Safe to call repeatedly;
     * a live subscription for the same user is kept as-is.
     */
    fun start(userId: String) {
        if (activeUserId == userId && subscription != null) return
        stop()
        activeUserId = userId
        runCatching {
            subscription = appwriteClient.realtime.subscribe(
                collectionChannel()
            ) { event ->
                onEvent(userId, event.events, event.payload)
            }
        }.onFailure {
            subscription = null
            activeUserId = null
        }
    }

    fun stop() {
        debounceJob?.cancel()
        debounceJob = null
        runCatching { subscription?.close() }
        subscription = null
        activeUserId = null
    }

    private fun collectionChannel(): String =
        "databases.${AppwriteClient.DATABASE_ID}" +
            ".collections.${AppwriteClient.USER_SYNC_COLLECTION_ID}.documents"

    private fun onEvent(userId: String, events: Collection<String>, payload: Any?) {
        if (!events.any { it.contains(".create") || it.contains(".update") }) return
        @Suppress("UNCHECKED_CAST")
        val payloadUserId = (payload as? Map<*, *>)?.get("userId")?.toString()
        if (payloadUserId != null && payloadUserId != userId) return
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
