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
    fun consumesTimestampOnlyOncePerLaunchRequest() {
        assertTrue(shouldConsumeStartPosition(requestId = "new", lastRequestId = "old"))
        assertFalse(shouldConsumeStartPosition(requestId = "same", lastRequestId = "same"))
        assertTrue(shouldConsumeStartPosition(requestId = null, lastRequestId = "old"))
    }
}
