package com.nur.quran.data.sync

import com.google.gson.Gson

internal class SyncRig(
    val prefs: FakePrefs,
    val dao: RecordingQuranDao,
    val service: SyncService,
    val builder: SnapshotBuilder,
) {
    /** `SnapshotBuilder.build()` + `snapshotToMap()` — the payload a push would upload. */
    fun snapshot(): Map<String, Any?> = kotlinx.coroutines.runBlocking { builder.snapshotToMap(builder.build()) }

    /** Runs the apply path exactly as `pull()` does after the watermark opens. */
    fun applyRemote(state: Map<String, Any?>) =
        kotlinx.coroutines.runBlocking { service.applyRemoteState(state) }
}

internal fun syncRig(
    seedPrefs: Map<String, Any?> = emptyMap(),
    rnClient: com.nur.quran.data.api.rn.RnApiClient = com.nur.quran.data.api.rn.signedOutRnApiClient(),
): SyncRig {
    val prefs = FakePrefs().apply { values.putAll(seedPrefs) }
    val context = FakePrefsContext(prefs)
    val recording = RecordingQuranDao()
    val builder = SnapshotBuilder(Gson(), context, recording.dao)
    return SyncRig(
        prefs,
        recording,
        SyncService(rnClient, builder, recording.dao, context),
        builder,
    )
}

/** Allocates an Android instance without running its constructor. */
internal fun <T> allocateUnchecked(clazz: Class<T>): T {
    val unsafe = Class.forName("sun.misc.Unsafe")
    val field = unsafe.getDeclaredField("theUnsafe").apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    return unsafe.getMethod("allocateInstance", Class::class.java).invoke(field.get(null), clazz) as T
}
