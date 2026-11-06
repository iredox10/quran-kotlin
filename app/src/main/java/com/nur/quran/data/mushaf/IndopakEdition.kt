package com.nur.quran.data.mushaf

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.security.MessageDigest

/** Explicit opt-in download. Reading never creates an HTTP request. */
object IndopakEdition {
    const val DOWNLOAD_URL = "https://archive.org/download/QuranAl-majeed-13Line/1-QuraanAlMajeed13Line-Complete.pdf"
    // Printed Quran pages 2–848 occupy PDF pages 4–850 (one-based).
    const val FIRST_PAGE = 2
    const val LAST_PAGE = 848
    const val BYTE_COUNT = 34113171L
    private val lock = Mutex()
    fun file(context: Context) = File(context.filesDir, "mushafs/indopak-13-line.pdf")
    fun installed(context: Context) = file(context).let { it.isFile && it.length() == BYTE_COUNT }

    suspend fun download(context: Context, progress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        lock.withLock {
            if (installed(context)) return@withLock
            val target = file(context)
            check(target.parentFile!!.mkdirs() || target.parentFile!!.isDirectory)
            val pending = File(target.parentFile, "indopak.part")
            var offset = pending.takeIf { it.length() < BYTE_COUNT }?.length() ?: 0L
            val connection = URL(DOWNLOAD_URL).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            if (offset > 0) connection.setRequestProperty("Range", "bytes=$offset-")
            try {
                val status = connection.responseCode
                check(status == 200 || status == 206) { "Download unavailable. Please try again." }
                if (status == 200) offset = 0L
                if (status == 206) check(connection.getHeaderField("Content-Range")?.startsWith("bytes $offset-") == true) {
                    "Invalid download range"
                }
                progress(offset.toFloat() / BYTE_COUNT)
                connection.inputStream.use { input ->
                    java.io.FileOutputStream(pending, offset > 0).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var total = offset
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            total += read
                            check(total <= BYTE_COUNT) { "Invalid edition download" }
                            output.write(buffer, 0, read)
                            progress((total.toFloat() / BYTE_COUNT).coerceIn(0f, 1f))
                        }
                        check(total == BYTE_COUNT) { "Download incomplete. Please try again." }
                    }
                }
                val digest = MessageDigest.getInstance("SHA-1")
                pending.inputStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        digest.update(buffer, 0, read)
                    }
                }
                if (digest.digest().joinToString("") { "%02x".format(it) } != "3819c14fe04efa0596a159d4a6f277570453ec41") {
                    pending.delete()
                    error("Download verification failed. Please try again.")
                }
                ParcelFileDescriptor.open(pending, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                    PdfRenderer(descriptor).use { check(it.pageCount == 874) { "Invalid edition" } }
                }
                check(pending.renameTo(target)) { "Could not save the edition" }
            } finally {
                connection.disconnect()
                // Keep incomplete bytes for a user-invoked retry; never expose them to the reader.
            }
        }
    }
}
