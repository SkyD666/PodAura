package com.skyd.podaura.model.worker.rsssync

/** Advance the starting feed so a slow prefix cannot monopolize every bounded refresh window. */
fun rotateRssSyncFeeds(feedUrls: List<String>, lastStartingFeed: String?): List<String> {
    val sorted = feedUrls.sorted()
    if (sorted.isEmpty()) return sorted
    val start = sorted.indexOfFirst { lastStartingFeed == null || it > lastStartingFeed }
        .coerceAtLeast(0)
    // ponytail: rotate one starting feed per window; every feed leads within N windows.
    return sorted.drop(start) + sorted.take(start)
}
