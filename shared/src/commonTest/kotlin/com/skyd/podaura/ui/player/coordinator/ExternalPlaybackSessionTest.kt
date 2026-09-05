package com.skyd.podaura.ui.player.coordinator

import com.skyd.podaura.ui.player.ExternalMedia
import com.skyd.podaura.ui.player.ExternalMediaBatch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExternalPlaybackSessionTest {
    @Test
    fun successfulRetryPreventsAnOldFailureFromStoppingTheQueue() {
        val session = session()
        val paths = setOf("first.mp3", "second.mp3")
        assertFalse(session.recordFailure("first.mp3", paths))

        session.onFileLoaded("first.mp3")

        assertFalse(session.recordFailure("second.mp3", paths))
        assertTrue(session.recordFailure("first.mp3", paths))
    }

    @Test
    fun repeatedErrorsOnlyCountOnceUntilEveryCurrentFileFails() {
        val session = session()
        val paths = setOf("first.mp3", "second.mp3")
        repeat(3) { assertFalse(session.recordFailure("first.mp3", paths)) }
        assertTrue(session.recordFailure("second.mp3", paths))
    }

    @Test
    fun loadingAnUnknownFileDoesNotClearOtherFailures() {
        val session = session()
        val paths = setOf("first.mp3", "second.mp3")
        assertFalse(session.recordFailure("first.mp3", paths))
        session.onFileLoaded(null)
        session.onFileLoaded("other.mp3")
        assertTrue(session.recordFailure("second.mp3", paths))
    }

    @Test
    fun emptyQueuesAndErrorsOutsideTheCurrentQueueDoNotTriggerStop() {
        val session = session()
        assertFalse(session.recordFailure("removed.mp3", emptySet()))
        assertFalse(session.recordFailure("removed.mp3", setOf("current.mp3")))
        assertFalse(session.recordFailure("current.mp3", setOf("removed.mp3", "current.mp3")))
    }

    @Test
    fun removedFilesLoseTheirFailureStateEvenIfLaterReadded() {
        val session = session()
        val paths = setOf("first.mp3", "second.mp3")
        assertFalse(session.recordFailure("first.mp3", paths))

        session.retainPaths(setOf("second.mp3"))

        assertFalse(session.recordFailure("second.mp3", paths))
        assertTrue(session.recordFailure("second.mp3", setOf("second.mp3")))
    }

    @Test
    fun replacingTheSessionDoesNotCarryFailuresIntoTheNewBatch() {
        val first = session()
        val paths = setOf("first.mp3", "second.mp3")
        assertFalse(first.recordFailure("first.mp3", paths))
        first.release()

        val replacement = session()
        assertFalse(replacement.recordFailure("second.mp3", paths))
        assertTrue(replacement.recordFailure("first.mp3", paths))
    }

    @Test
    fun failureTrackingDoesNotReleaseThePlayersMediaAccess() {
        var closed = 0
        val media = ExternalMedia("content://media/1", "fd://10") { closed++ }
        val batch = ExternalMediaBatch(listOf(media), emptyList())
        assertTrue(batch.retain())
        val session = ExternalPlaybackSession(batch)
        batch.release()

        assertTrue(session.recordFailure("fd://10", setOf("fd://10")))
        session.onFileLoaded("fd://10")
        session.retainPaths(emptySet())
        assertEquals(0, closed)

        session.release()
        assertEquals(1, closed)
        assertFalse(batch.retain())
    }

    private fun session() = ExternalPlaybackSession(ExternalMediaBatch(emptyList(), emptyList()))
}
