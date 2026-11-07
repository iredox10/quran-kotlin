package com.nur.quran.data.hifdh

/** Linear history scan: a confirmed confusion pair needs clean later recall of both ayahs. */
fun unresolvedHifdhMistakes(events: List<HifdhReviewEvent>): List<HifdhReviewEvent> {
    val resolved = events.asSequence().filter { it.outcome == "resolved" }.mapNotNull { it.relatedEventId }.toSet()
    val lastPass = mutableMapOf<String, Long>()
    for (event in events) if (event.independentPass) lastPass[event.verseKey] = maxOf(lastPass[event.verseKey] ?: 0L, event.timestamp)
    return events.filter { event ->
        val mistake = event.outcome in setOf("mistake", "confusion") || (event.outcome == "review" && event.rating == 1 && event.task != "CORRECTION")
        val repaired = (lastPass[event.verseKey] ?: 0L) > event.timestamp &&
            (event.relatedVerse == null || (lastPass[event.relatedVerse] ?: 0L) > event.timestamp)
        mistake && event.id !in resolved && !repaired
    }
}
