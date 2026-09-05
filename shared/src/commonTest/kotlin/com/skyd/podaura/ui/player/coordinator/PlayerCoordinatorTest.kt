package com.skyd.podaura.ui.player.coordinator

import com.skyd.podaura.ui.player.PlaybackEnd
import com.skyd.podaura.ui.player.PlaybackEndReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerCoordinatorTest {
    @Test
    fun ordinaryPlaybackErrorsKeepTheirRetryTarget() {
        val end = PlaybackEnd(PlaybackEndReason.Error, -13, 42L, "failed.mp3")
        val failure = assertNotNull(end.toPlaybackFailure(autoAdvance = false))
        assertEquals(end, failure.retryEnd)
        assertNull(failure.details)
    }

    @Test
    fun automaticallySkippedErrorsShowTheirStableSourceWithoutRetry() {
        val end = PlaybackEnd(PlaybackEndReason.Error, -17, 42L, "fd://10")
        val failure = assertNotNull(end.toPlaybackFailure(
            autoAdvance = true, source = "content://media/1",
        ))
        assertEquals("content://media/1: Unrecognized or damaged media format", failure.details)
        assertNull(failure.retryEnd)
    }

    @Test
    fun normalEndReasonsDoNotCreateFailureNotifications() {
        PlaybackEndReason.entries.filterNot { it == PlaybackEndReason.Error }.forEach { reason ->
            val end = PlaybackEnd(reason, path = "valid.mp3")
            assertNull(end.toPlaybackFailure(autoAdvance = false))
            assertNull(end.toPlaybackFailure(autoAdvance = true))
        }
    }

    @Test
    fun anUnidentifiedExternalErrorDoesNotCreateAMisleadingNotification() {
        val end = PlaybackEnd(PlaybackEndReason.Error, -13)
        assertNull(end.toPlaybackFailure(autoAdvance = true))
        assertNotNull(end.toPlaybackFailure(autoAdvance = false))
    }

    @Test
    fun seeksImmediatelyWhenTimestampTargetsCurrentMediaAndPlaylist() {
        assertTrue(
            shouldSeekCurrentMedia(
                startPositionSeconds = 83,
                startPath = "current.mp4",
                currentPath = "current.mp4",
                currentPlaylist = listOf("current.mp4"),
                requestedPlaylist = listOf("current.mp4"),
            )
        )
    }

    @Test
    fun waitsForFileLoadedWhenMediaOrPlaylistChanges() {
        assertFalse(
            shouldSeekCurrentMedia(
                startPositionSeconds = 83,
                startPath = "other.mp4",
                currentPath = "current.mp4",
                currentPlaylist = listOf("current.mp4"),
                requestedPlaylist = listOf("other.mp4"),
            )
        )
        assertFalse(
            shouldSeekCurrentMedia(
                startPositionSeconds = 83,
                startPath = "current.mp4",
                currentPath = "current.mp4",
                currentPlaylist = listOf("current.mp4"),
                requestedPlaylist = listOf("current.mp4", "next.mp4"),
            )
        )
    }

    @Test
    fun consumesLoadSideEffectsOnlyOncePerLaunchRequest() {
        assertTrue(shouldConsumeLoadRequest(requestId = "new", lastRequestId = "old"))
        assertFalse(shouldConsumeLoadRequest(requestId = "same", lastRequestId = "same"))
        assertTrue(shouldConsumeLoadRequest(requestId = null, lastRequestId = "old"))
    }
}
