package com.nur.quran.data.audio

import com.nur.quran.data.db.isLoggableReadingSession

/**
 * Web parity for GlobalAudioPlayer.jsx listening segments: accumulates wall
 * time while audio plays and emits a loggable `listening` session on
 * pause/track-change (>= 10s, like `flushListening`). Pure time math so it
 * stays JVM-testable; callers persist via `logReadingSession`.
 */
data class ListeningSegment(val durationSeconds: Long, val chapterId: Int?)

class ListeningSessionTracker {
    private var segmentStartMs: Long? = null
    private var segmentChapterId: Int? = null

    /** Call on every play/pause transition; returns a segment when one closes. */
    fun onPlayingChanged(playing: Boolean, chapterId: Int?, nowMs: Long): ListeningSegment? {
        if (playing) {
            if (segmentStartMs == null) {
                segmentStartMs = nowMs
                segmentChapterId = chapterId
            }
            return null
        }
        return flush(nowMs)
    }

    /** Close the open segment; null when nothing was playing or it was < 10s. */
    fun flush(nowMs: Long): ListeningSegment? {
        val start = segmentStartMs ?: return null
        segmentStartMs = null
        val chapterId = segmentChapterId
        segmentChapterId = null
        val durationSeconds = (nowMs - start) / 1000
        if (!isLoggableReadingSession(durationSeconds)) return null
        return ListeningSegment(durationSeconds = durationSeconds, chapterId = chapterId)
    }
}
