package com.skyd.podaura.ui.screen.read

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadContentSourceTransitionTrackerTest {

    @Test
    fun skipsInitialAndRepeatedSourcesButResetsForRealTransitions() {
        val tracker = ReadContentSourceTransitionTracker(ReadContentSource.Feed)

        assertFalse(tracker.shouldResetScroll(ReadContentSource.Feed))
        assertTrue(tracker.shouldResetScroll(ReadContentSource.FullText))
        assertFalse(tracker.shouldResetScroll(ReadContentSource.FullText))
        assertTrue(tracker.shouldResetScroll(ReadContentSource.Feed))
    }

    @Test
    fun ignoresLoadingBeforeTheFirstContentSource() {
        val tracker = ReadContentSourceTransitionTracker(initialSource = null)

        assertFalse(tracker.shouldResetScroll(null))
        assertFalse(tracker.shouldResetScroll(ReadContentSource.Feed))
    }
}
