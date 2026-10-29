package com.skyd.podaura.model.playlist

import com.skyd.podaura.model.bean.playlist.movePlaylistItem
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.model.repository.playlist.PlaylistMediaRepository
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first

/** Owns playlist edits and dispatches committed source changes to playback. */
class PlaylistController(private val repository: PlaylistMediaRepository) {
    suspend fun remove(playlistId: String, urls: List<String>) = repository.removeMedia(playlistId, urls)

    /** Collected for the lifetime of one active source, independent of the visible queue UI. */
    suspend fun followPlaylist(playlistId: String, playback: PlaylistPlayback) {
        require(playlistId.isNotEmpty())
        repository.observePlaylistMediaList(playlistId).conflate().collect {
            try {
                // Read inside the player's serialized operation, not when this notification was
                // emitted: a queued old snapshot must never undo a newer drag or restore a delete.
                playback.synchronizePlaylist(playlistId) {
                    repository.observePlaylistMediaList(playlistId).first()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                // Playback reports the failure; keep following subsequent committed changes.
                Logger.w(throwable = error, tag = "PlaylistController") { "Could not synchronize playlist" }
            }
        }
    }

    suspend fun reorder(
        playlistId: String,
        fromUrl: String,
        toUrl: String,
        displayedOrder: List<String>? = null,
        fromQueue: Boolean = false,
        playback: PlaylistPlayback? = null,
    ) {
        if (fromUrl == toUrl) return
        require(playlistId.isNotEmpty() || fromQueue) { "A saved playlist is required" }
        val expectedQueue = if (fromQueue) checkNotNull(displayedOrder) else null
        val update: suspend ((List<String>) -> Unit) -> Unit = { applyToPlayback ->
            if (playlistId.isEmpty()) {
                applyToPlayback(movePlaylistItem(checkNotNull(displayedOrder), fromUrl, toUrl))
            } else {
                repository.reorderPlaylistMedia(
                    playlistId, fromUrl, toUrl, displayedOrder, fromQueue, applyToPlayback,
                )
            }
        }

        // Reserve the matching playback queue while saving, so a load/delete cannot intervene.
        // The playlist owns the save and dispatches its result through the supplied engine action.
        val synchronized = playback?.withQueueOrder(playlistId, expectedQueue, update) == true
        if (!synchronized) {
            check(!fromQueue) { "The playback queue is no longer available" }
            update {} // An inactive saved playlist has no playback queue to synchronize.
        }
    }
}

/** Playback-only boundary; implementations neither choose nor persist playlist sorting rules. */
interface PlaylistPlayback {
    /** Reads the latest committed contents while queue operations are serialized; ignores other sources. */
    suspend fun synchronizePlaylist(
        playlistId: String,
        readPlaylist: suspend () -> List<PlaylistMediaWithArticleBean>,
    )

    /**
     * Returns false without running [update] when this source is not playing.
     * Otherwise serializes [update] with queue changes and provides a stable-URL order action.
     * A non-null [expectedOrder] must match the current queue's items before [update] runs.
     * Failure attempts to restore the original native order and propagates; it never returns false.
     * The action is valid only inside [update], whose completion includes the caller's commit.
     */
    suspend fun withQueueOrder(
        playlistId: String,
        expectedOrder: List<String>?,
        update: suspend (applyOrder: (List<String>) -> Unit) -> Unit,
    ): Boolean
}
