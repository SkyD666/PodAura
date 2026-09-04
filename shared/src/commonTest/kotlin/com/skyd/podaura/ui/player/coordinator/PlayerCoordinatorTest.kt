package com.skyd.podaura.ui.player.coordinator

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerCoordinatorTest {
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
