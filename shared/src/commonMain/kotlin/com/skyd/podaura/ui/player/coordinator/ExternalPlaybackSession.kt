package com.skyd.podaura.ui.player.coordinator

import com.skyd.podaura.ui.player.ExternalMediaBatch

/** Owns the batch reference already retained by the coordinator; confined to its actor. */
internal class ExternalPlaybackSession(private val batch: ExternalMediaBatch) {
    private val failedPaths = mutableSetOf<String>()

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
    }

    fun release() = batch.release()
}
