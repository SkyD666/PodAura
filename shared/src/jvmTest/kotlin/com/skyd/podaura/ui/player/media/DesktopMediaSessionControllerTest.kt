package com.skyd.podaura.ui.player.media

import com.skyd.podaura.model.bean.playlist.PlaylistMediaBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.ui.player.PlayerCommand
import com.skyd.podaura.ui.player.PlayerEvent
import com.skyd.podaura.ui.player.service.PlayerState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopMediaSessionControllerTest {
    @Test
    fun publishesMetadataAvailabilityAndMapsEveryRemoteCommand() = runTest {
        val adapter = RecordingAdapter()
        val commands = mutableListOf<PlayerCommand>()
        val controller = DesktopMediaSessionController(
            adapter = adapter,
            artworkLoader = { null },
            commandSink = commands::add,
            scope = this,
        )

        controller.update(
            playerState(
                paths = listOf(
                    "https://example.com/first.mp3",
                    "https://example.com/second.mp3",
                ),
                index = 1,
                paused = false,
                seekable = true,
                duration = 120L,
                position = 30L,
                speed = 1.5f,
                album = "Season 4",
            ),
            PlayerEvent.PlaybackRestart,
        )

        val snapshot = adapter.updates.single()
        assertEquals("Episode 2", snapshot.title)
        assertEquals("Presenter 2", snapshot.artist)
        assertEquals("Season 4", snapshot.album)
        assertEquals(120.0, snapshot.durationSeconds)
        assertEquals(30.0, snapshot.positionSeconds)
        assertEquals(1.5, snapshot.playbackRate)
        assertEquals(1.5, snapshot.defaultPlaybackRate)
        assertEquals(1, snapshot.queueIndex)
        assertEquals(2, snapshot.queueCount)
        assertTrue(snapshot.canGoPrevious)
        assertFalse(snapshot.canGoNext)
        assertTrue(snapshot.canChangePlaybackPosition)

        adapter.send(DesktopMediaCommand.Play)
        adapter.send(DesktopMediaCommand.Pause)
        adapter.send(DesktopMediaCommand.TogglePlayPause)
        adapter.send(DesktopMediaCommand.Previous)
        adapter.send(DesktopMediaCommand.Next)
        adapter.send(DesktopMediaCommand.ChangePlaybackPosition(42.6))
        advanceUntilIdle()

        assertEquals(
            listOf(
                PlayerCommand.Paused(paused = false),
                PlayerCommand.Paused(paused = true),
                PlayerCommand.PlayOrPause,
                PlayerCommand.PreviousMedia,
                PlayerCommand.NextMedia,
                PlayerCommand.SeekTo(position = 43L),
            ),
            commands,
        )
        controller.close()
    }

    @Test
    fun firstQueueItemDisablesPreviousAndKeepsNextAvailable() = runTest {
        val adapter = RecordingAdapter()
        val controller = controller(adapter)

        controller.update(
            playerState(
                paths = listOf(
                    "https://example.com/first.mp3",
                    "https://example.com/second.mp3",
                ),
                index = 0,
            )
        )

        assertFalse(adapter.updates.single().canGoPrevious)
        assertTrue(adapter.updates.single().canGoNext)
        controller.close()
    }

    @Test
    fun currentPathWinsDuringAStalePlaylistPositionTransition() = runTest {
        val adapter = RecordingAdapter()
        val controller = controller(adapter)

        controller.update(
            playerState(
                paths = listOf(
                    "https://example.com/first.mp3",
                    "https://example.com/second.mp3",
                ),
                index = 1,
                playlistPosition = 0,
            )
        )

        val snapshot = adapter.updates.single()
        assertEquals("Episode 2", snapshot.title)
        assertTrue(snapshot.canGoPrevious)
        assertFalse(snapshot.canGoNext)
        controller.close()
    }

    @Test
    fun unknownOrNonSeekableMediaDisablesPositionCommand() = runTest {
        val adapter = RecordingAdapter()
        val controller = controller(adapter)

        controller.update(
            playerState(
                paths = listOf("https://example.com/live"),
                index = 0,
                seekable = false,
                duration = 0L,
            )
        )

        val snapshot = adapter.updates.single()
        assertNull(snapshot.durationSeconds)
        assertNull(snapshot.positionSeconds)
        assertFalse(snapshot.canChangePlaybackPosition)
        controller.close()
    }

    @Test
    fun normalPositionTicksAreNotRepublishedButSeekCompletionIs() = runTest {
        val adapter = RecordingAdapter()
        val controller = controller(adapter)
        val initial = playerState(
            paths = listOf("https://example.com/episode.mp3"),
            index = 0,
            seekable = true,
            duration = 120L,
            position = 10L,
        )

        controller.update(initial, PlayerEvent.Duration(120L))
        controller.update(initial.copy(position = 11L), PlayerEvent.Position(11L))
        assertEquals(1, adapter.updates.size)

        controller.update(initial.copy(position = 11L), PlayerEvent.Seek)
        controller.update(initial.copy(position = 70L), PlayerEvent.Position(70L))
        assertEquals(2, adapter.updates.size)
        assertEquals(70.0, adapter.updates.last().positionSeconds)
        controller.close()
    }

    @Test
    fun staleArtworkResultCannotOverwriteTheNewMedia() = runTest {
        val adapter = RecordingAdapter()
        val loads = mutableMapOf<String, CompletableDeferred<DesktopArtworkData?>>()
        val loader = DesktopArtworkLoader { source ->
            val key = source.toString()
            withContext(NonCancellable) {
                loads.getOrPut(key, ::CompletableDeferred).await()
            }
        }
        val controller = DesktopMediaSessionController(
            adapter = adapter,
            artworkLoader = loader,
            commandSink = {},
            scope = this,
        )
        val paths = listOf("/music/first.mp3", "/music/second.mp3")

        controller.update(playerState(paths = paths, index = 0))
        runCurrent()
        controller.update(playerState(paths = paths, index = 1))
        runCurrent()

        loads.getValue("LocalMedia(file=/music/first.mp3)").complete(
            DesktopArtworkData(byteArrayOf(1), width = 100, height = 100)
        )
        loads.getValue("LocalMedia(file=/music/second.mp3)").complete(
            DesktopArtworkData(byteArrayOf(2), width = 200, height = 200)
        )
        advanceUntilIdle()

        val artwork = assertIs<DesktopArtwork>(adapter.updates.last().artwork)
        assertTrue(artwork.id.startsWith("/music/second.mp3:"))
        assertContentEquals(byteArrayOf(2), artwork.data.pngBytes)
        controller.close()
    }

    @Test
    fun artworkFailureLeavesControlsAndMetadataActive() = runTest {
        val adapter = RecordingAdapter()
        val controller = DesktopMediaSessionController(
            adapter = adapter,
            artworkLoader = DesktopArtworkLoader { error("decode failed") },
            commandSink = {},
            scope = this,
        )

        controller.update(playerState(paths = listOf("/music/episode.mp3"), index = 0))
        advanceUntilIdle()

        assertFalse(adapter.closed)
        assertEquals("Episode 1", adapter.updates.last().title)
        assertNull(adapter.updates.last().artwork)
        controller.close()
    }

    @Test
    fun adapterFailureDisablesOnlyTheSystemMediaController() = runTest {
        val adapter = RecordingAdapter().apply {
            updateFailure = IllegalStateException("native update failed")
        }
        val commands = mutableListOf<PlayerCommand>()
        val controller = DesktopMediaSessionController(
            adapter = adapter,
            artworkLoader = DesktopArtworkLoader { null },
            commandSink = commands::add,
            scope = this,
        )

        controller.update(playerState(paths = listOf("https://example.com/episode"), index = 0))
        adapter.send(DesktopMediaCommand.TogglePlayPause)
        advanceUntilIdle()

        assertTrue(adapter.closed)
        assertTrue(adapter.clearCount > 0)
        assertTrue(commands.isEmpty())
        controller.close()
    }

    @Test
    fun emptyQueueClearsTheSystemSession() = runTest {
        val adapter = RecordingAdapter()
        val controller = controller(adapter)

        controller.update(playerState(paths = listOf("https://example.com/episode"), index = 0))
        controller.update(PlayerState(), PlayerEvent.Shutdown)

        assertEquals(1, adapter.clearCount)
        controller.close()
        assertTrue(adapter.closed)
    }

    @Test
    fun closingIsIdempotentAndStopsRemoteCommands() = runTest {
        val adapter = RecordingAdapter()
        val commands = mutableListOf<PlayerCommand>()
        val controller = DesktopMediaSessionController(
            adapter = adapter,
            artworkLoader = DesktopArtworkLoader { null },
            commandSink = commands::add,
            scope = this,
        )

        controller.update(playerState(paths = listOf("https://example.com/episode"), index = 0))
        controller.close()
        controller.close()
        adapter.send(DesktopMediaCommand.TogglePlayPause)
        advanceUntilIdle()

        assertEquals(1, adapter.clearCount)
        assertTrue(adapter.closed)
        assertTrue(commands.isEmpty())
    }

    private fun kotlinx.coroutines.test.TestScope.controller(
        adapter: RecordingAdapter,
    ) = DesktopMediaSessionController(
        adapter = adapter,
        artworkLoader = { null },
        commandSink = {},
        scope = this,
    )

    private fun playerState(
        paths: List<String>,
        index: Int,
        paused: Boolean = true,
        seekable: Boolean = false,
        duration: Long = 0L,
        position: Long = 0L,
        speed: Float = 1f,
        album: String? = null,
        playlistPosition: Int = index,
    ): PlayerState {
        val playlist = LinkedHashMap<String, PlaylistMediaWithArticleBean>()
        paths.forEachIndexed { itemIndex, path ->
            val bean = PlaylistMediaBean(
                playlistId = "playlist",
                url = path,
                articleId = null,
                orderPosition = itemIndex.toDouble(),
                createTime = itemIndex.toLong(),
            ).apply {
                title = "Episode ${itemIndex + 1}"
                artist = "Presenter ${itemIndex + 1}"
            }
            playlist[path] = PlaylistMediaWithArticleBean(bean, article = null)
        }
        return PlayerState(
            playlistId = "playlist",
            playlist = playlist,
            mediaStarted = true,
            seekable = seekable,
            path = paths[index],
            speed = speed,
            album = album,
            position = position,
            duration = duration,
            playlistPosition = playlistPosition,
            paused = paused,
            idling = false,
        )
    }

    private class RecordingAdapter : DesktopMediaSessionAdapter {
        val updates = mutableListOf<DesktopMediaSnapshot>()
        var clearCount = 0
        var closed = false
        var updateFailure: Throwable? = null
        private var listener: ((DesktopMediaCommand) -> Unit)? = null

        override fun setCommandListener(listener: (DesktopMediaCommand) -> Unit) {
            this.listener = listener
        }

        override fun update(snapshot: DesktopMediaSnapshot) {
            updateFailure?.let { throw it }
            updates += snapshot
        }

        override fun clear() {
            clearCount++
        }

        override fun close() {
            closed = true
        }

        fun send(command: DesktopMediaCommand) {
            assertIs<(DesktopMediaCommand) -> Unit>(listener).invoke(command)
        }
    }
}
