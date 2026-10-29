package com.skyd.podaura.ui.player.coordinator

import com.skyd.podaura.ui.player.ExternalMediaBatch

/** Owns the batch reference already retained by the coordinator; confined to its actor. */
internal class ExternalPlaybackSession(batch: ExternalMediaBatch) {
    private val batches = mutableListOf(batch)
    private val failedPaths = mutableSetOf<String>()

    fun add(batch: ExternalMediaBatch) { batches += batch }

    fun recordFailure(path: String, playlistPaths: Set<String>): Boolean {
        retainPaths(playlistPaths)
        if (path !in playlistPaths) return false
        failedPaths += path
        return failedPaths.containsAll(playlistPaths)
    }

    fun onFileLoaded(path: String?) {
        failedPaths.remove(path)
    }

    fun retainPaths(playlistPaths: Set<String>) {
        failedPaths.retainAll(playlistPaths)
        val removed = batches.filter { batch -> batch.media.none { it.playbackUrl in playlistPaths } }
        batches.removeAll(removed.toSet())
        removed.forEach { it.release() }
    }

    fun release() = batches.forEach { it.release() }.also { batches.clear() }
}
