package com.nur.quran.data.hifdh

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** Local, explicitly started recordings. No upload path and no background recording service. */
class HifdhRecordings(private val context: Context, owner: String) {
    private val folder = File(context.filesDir, "hifdh_recordings/" + java.security.MessageDigest.getInstance("SHA-256")
        .digest(owner.toByteArray()).joinToString("") { "%02x".format(it) }).apply { mkdirs() }
    private var recorder: MediaRecorder? = null
    private var pending: File? = null
    val isRecording get() = recorder != null
    @Suppress("DEPRECATION")
    fun start(onLimit: () -> Unit): String {
        check(recorder == null)
        val remaining = 50L * 1024 * 1024 - (folder.listFiles()?.sumOf { it.length() } ?: 0L)
        check(remaining > 64 * 1024) { "Recording storage full" }
        val file = File(folder, java.util.UUID.randomUUID().toString() + ".m4a")
        val instance = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
        try {
            instance.setAudioSource(MediaRecorder.AudioSource.MIC)
            instance.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            instance.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            instance.setAudioEncodingBitRate(64_000)
            instance.setAudioSamplingRate(44_100)
            instance.setOutputFile(file.absolutePath)
            instance.setMaxDuration(180_000)
            instance.setMaxFileSize(minOf(8L * 1024 * 1024, remaining))
            instance.setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED || what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) onLimit()
            }
            instance.prepare(); instance.start()
            recorder = instance; pending = file
            return file.name
        } catch (failure: Exception) { instance.release(); file.delete(); throw failure }
    }
    fun stop(): String? {
        val instance = recorder ?: return null
        val file = pending
        recorder = null; pending = null
        val valid = runCatching { instance.stop() }.isSuccess
        instance.release()
        if (!valid || file == null || file.length() == 0L) { file?.delete(); return null }
        return file.name
    }
    fun file(id: String): File? = id.takeIf { Regex("[a-f0-9-]{36}\\.m4a").matches(it) }?.let { File(folder, it) }?.takeIf { it.isFile }
    fun delete(id: String) { file(id)?.delete() }
    fun files() = folder.listFiles()?.filter { it.extension == "m4a" }?.sortedByDescending { it.lastModified() }.orEmpty()
}
