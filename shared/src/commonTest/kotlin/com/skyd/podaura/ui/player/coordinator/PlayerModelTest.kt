package com.skyd.podaura.ui.player.coordinator

import com.skyd.podaura.ui.player.PlayerEvent
import com.skyd.podaura.ui.player.PlaybackEnd
import com.skyd.podaura.ui.player.PlaybackEndReason
import com.skyd.podaura.ui.player.PlaybackFailure
import com.skyd.podaura.ui.player.Track
import com.skyd.podaura.ui.player.service.PlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlayerModelTest {
    private fun PlayerModel.endWithFailure(end: PlaybackEnd) {
        onEvent(PlayerEvent.EndFile(end))
        onEvent(PlayerEvent.PlaybackFailed(requireNotNull(end.toPlaybackFailure(autoAdvance = false))))
    }

    @Test
    fun endFileOnlyUpdatesPlaybackStateWithoutCreatingANotification() {
        val model = PlayerModel()
        val end = PlaybackEnd(reason = PlaybackEndReason.Error, path = "failed.mp3")
        model.onEvent(PlayerEvent.EndFile(end))

        assertEquals(end, model.playerState.value.lastPlaybackEnd)
        assertTrue(model.playerState.value.pendingPlaybackFailures.isEmpty())
    }

    @Test
    fun failuresAreRetainedBeforeThePlayerUiSubscribes() = runTest {
        val model = PlayerModel()
        val failure = PlaybackFailure(details = "missing.mp3: File not found")
        model.onEvent(PlayerEvent.PlaybackFailed(failure))
        model.onEvent(PlayerEvent.Position(12))

        assertEquals(listOf(failure), model.playerState.first().pendingPlaybackFailures)
    }

    @Test
    fun externalFailuresSurviveAutomaticAdvanceAndPlaybackRestart() {
        val model = PlayerModel()
        val failure = PlaybackFailure(details = "broken.mp4: Unsupported format")
        model.onEvent(PlayerEvent.EndFile(PlaybackEnd(
            reason = PlaybackEndReason.Error, path = "broken.mp4",
        )))
        model.onEvent(PlayerEvent.PlaybackFailed(failure))
        model.onEvent(PlayerEvent.StartFile("valid.mp4"))
        model.onEvent(PlayerEvent.PlaybackRestart)
        model.onEvent(PlayerEvent.ClearPlaybackEnd)

        assertEquals(listOf(failure), model.playerState.value.pendingPlaybackFailures)
    }

    @Test
    fun acknowledgingAnOldFailureDoesNotConsumeANewerIdenticalFailure() {
        val model = PlayerModel()
        val first = PlaybackFailure(details = "broken.mp4: Unsupported format")
        val second = PlaybackFailure(details = first.details)
        model.onEvent(PlayerEvent.PlaybackFailed(first))
        model.onEvent(PlayerEvent.PlaybackFailed(second))
        assertEquals(first, model.consumePlaybackFailure(first.id))
        assertNull(model.consumePlaybackFailure(first.id))

        assertEquals(listOf(second), model.playerState.value.pendingPlaybackFailures)
    }

    @Test
    fun ordinaryFailuresOfferRetryUntilPlaybackMovesOn() {
        listOf(
            PlayerEvent.StartFile("next.mp3"),
            PlayerEvent.PlaybackRestart,
            PlayerEvent.ClearPlaybackEnd,
        ).forEach { transition ->
            val model = PlayerModel()
            val external = PlaybackFailure(details = "missing.mp3: File not found")
            val end = PlaybackEnd(reason = PlaybackEndReason.Error, path = "failed.mp3")
            model.onEvent(PlayerEvent.PlaybackFailed(external))
            model.endWithFailure(end)
            assertEquals(end, model.playerState.value.pendingPlaybackFailures.last().retryEnd)

            model.onEvent(transition)

            assertEquals(listOf(external), model.playerState.value.pendingPlaybackFailures)
        }
    }

    @Test
    fun dismissingAFailureDoesNotClearThePlaybackEndNeededByThePlayButton() {
        val model = PlayerModel()
        val end = PlaybackEnd(reason = PlaybackEndReason.Error, path = "failed.mp3")
        model.endWithFailure(end)
        val failure = model.playerState.value.pendingPlaybackFailures.single()

        model.consumePlaybackFailure(failure.id)

        assertTrue(model.playerState.value.pendingPlaybackFailures.isEmpty())
        assertEquals(end, model.playerState.value.lastPlaybackEnd)
    }

    @Test
    fun repeatedOrdinaryErrorsReplaceTheirObsoleteRetryNotification() {
        val model = PlayerModel()
        val end = PlaybackEnd(reason = PlaybackEndReason.Error, path = "failed.mp3")
        model.endWithFailure(end)
        val previousId = model.playerState.value.pendingPlaybackFailures.single().id
        model.endWithFailure(end)
        assertNull(model.consumePlaybackFailure(previousId))

        assertEquals(end, model.playerState.value.pendingPlaybackFailures.single().retryEnd)
    }

    @Test
    fun pendingFailuresAreBoundedAndShutdownClearsThem() {
        val model = PlayerModel()
        val failures = List(PlayerModel.MAX_PENDING_FAILURES + 10) {
            PlaybackFailure(details = "$it.mp3: File not found")
        }
        failures.forEach { model.onEvent(PlayerEvent.PlaybackFailed(it)) }

        assertEquals(
            failures.takeLast(PlayerModel.MAX_PENDING_FAILURES),
            model.playerState.value.pendingPlaybackFailures,
        )
        model.onEvent(PlayerEvent.Shutdown)
        assertTrue(model.playerState.value.pendingPlaybackFailures.isEmpty())
    }

    @Test
    fun progressAndQueuedFailuresDoNotRestartTheVisibleNotification() = runTest {
        val model = PlayerModel()
        val first = PlaybackFailure(details = "first.mp3: File not found")
        val second = PlaybackFailure(details = "second.mp3: File not found")
        val visible = mutableListOf<PlaybackFailure?>()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            model.playerState.map { it.pendingPlaybackFailures.firstOrNull() }
                .distinctUntilChanged().collect { visible += it }
        }
        model.onEvent(PlayerEvent.PlaybackFailed(first))
        model.onEvent(PlayerEvent.Position(12))
        model.onEvent(PlayerEvent.PlaybackFailed(second))
        model.onEvent(PlayerEvent.StartFile("valid.mp3"))
        assertEquals(listOf(null, first), visible)

        collector.cancel()
        assertEquals(listOf(first, second), model.playerState.first().pendingPlaybackFailures)
        model.consumePlaybackFailure(first.id)
        assertEquals(second, model.playerState.first().pendingPlaybackFailures.first())
    }

    @Test
    fun consumingAFailureUpdatesStateWithoutEmittingAPlayerEvent() = runTest {
        val model = PlayerModel()
        val failure = PlaybackFailure(details = "missing.mp3: File not found")
        val event = PlayerEvent.PlaybackFailed(failure)
        val events = mutableListOf<PlayerEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            model.newStateByEvent.collect { (_, event) -> events += event }
        }
        model.onEvent(event)

        assertEquals(failure, model.consumePlaybackFailure(failure.id))

        assertTrue(model.playerState.value.pendingPlaybackFailures.isEmpty())
        assertEquals(listOf<PlayerEvent>(event), events)
    }

    @Test
    fun fileLoadedRestoresVideoTracksWhenEarlyTrackNotificationWasClearedByStart() {
        val video = Track(trackId = 1, name = "Video", isAlbumArt = false)
        val audio = Track(trackId = 2, name = "Audio", isAlbumArt = false)
        val model = PlayerModel()
        model.onEvent(PlayerEvent.AllVideoTracks(listOf(video)))
        model.onEvent(PlayerEvent.VideoTrackChanged(1))
        model.onEvent(PlayerEvent.StartFile("video.mp4"))
        assertFalse(model.playerState.value.isVideo)

        model.onEvent(PlayerEvent.FileLoaded(
            videoTracks = listOf(video), audioTracks = listOf(audio), subtitleTracks = emptyList(),
            videoTrackId = 1, audioTrackId = 2, subtitleTrackId = -1,
        ))

        assertTrue(model.playerState.value.isVideo)
        assertEquals(1, model.playerState.value.videoTrackId)
        assertEquals(listOf(audio), model.playerState.value.audioTracks)
        assertEquals(2, model.playerState.value.audioTrackId)
    }

    @Test
    fun loadingAudioAfterVideoDoesNotKeepTheVideoSurfaceVisible() {
        val model = PlayerModel()
        model.onEvent(PlayerEvent.AllVideoTracks(listOf(Track(1, "Video", false))))
        assertTrue(model.playerState.value.isVideo)
        model.onEvent(PlayerEvent.StartFile("audio.mp3"))
        model.onEvent(PlayerEvent.FileLoaded(
            videoTracks = listOf(Track(1, "Cover", true)),
            audioTracks = listOf(Track(2, "Audio", false)), subtitleTracks = emptyList(),
            videoTrackId = 1, audioTrackId = 2, subtitleTrackId = -1,
        ))
        assertFalse(model.playerState.value.isVideo)
    }

    @Test
    fun shutdownClearsTheCurrentPlaybackSession() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val model = PlayerModel()
            model.onEvent(PlayerEvent.StartFile("episode.mp3"))
            model.onEvent(PlayerEvent.Position(42))
            model.onEvent(PlayerEvent.Paused(false))
            advanceUntilIdle()

            model.onEvent(PlayerEvent.Shutdown)
            advanceUntilIdle()

            assertEquals(PlayerState(), model.playerState.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun startingANewFileClearsMediaSpecificStateFromThePreviousFile() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val model = PlayerModel()
            model.onEvent(PlayerEvent.StartFile("first.mp3"))
            model.onEvent(PlayerEvent.Seekable(true))
            model.onEvent(PlayerEvent.Duration(120L))
            model.onEvent(PlayerEvent.Position(80L))
            model.onEvent(PlayerEvent.MediaTitle("Old title"))
            model.onEvent(PlayerEvent.Artist("Old artist"))
            model.onEvent(PlayerEvent.Album("Old album"))
            advanceUntilIdle()

            model.onEvent(PlayerEvent.StartFile("second.mp3"))
            advanceUntilIdle()

            val state = model.playerState.value
            assertEquals("second.mp3", state.path)
            assertEquals(0L, state.position)
            assertEquals(0L, state.duration)
            assertFalse(state.seekable)
            assertNull(state.mediaTitle)
            assertNull(state.artist)
            assertNull(state.album)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun playbackErrorIsRetainedUntilAFileStartsOrItIsExplicitlyCleared() = runTest {
        val model = PlayerModel()
        val playbackEnd = PlaybackEnd(
            reason = PlaybackEndReason.Error,
            errorCode = -13,
            playlistEntryId = 84L,
            path = "failed.mp3",
        )

        model.onEvent(PlayerEvent.EndFile(playbackEnd))
        assertEquals(playbackEnd, assertNotNull(model.playerState.value.lastPlaybackEnd))

        model.onEvent(PlayerEvent.ClearPlaybackEnd)
        assertNull(model.playerState.value.lastPlaybackEnd)

        model.onEvent(PlayerEvent.EndFile(playbackEnd))
        model.onEvent(PlayerEvent.StartFile("next.mp3"))
        assertNull(model.playerState.value.lastPlaybackEnd)
    }
}
