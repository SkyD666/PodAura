package com.skyd.podaura.ui.screen.feed

import com.skyd.podaura.model.bean.feed.FeedBean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeedPartialStateChangeTest {

    @Test
    fun refreshAllFeedsTracksProgressWithoutShowingWaitingDialog() {
        val initial = FeedState.initial()

        val started = FeedPartialStateChange.RefreshAllFeeds.Started.reduce(initial)
        val succeeded = FeedPartialStateChange.RefreshAllFeeds.Success.reduce(started)
        val failed = FeedPartialStateChange.RefreshAllFeeds.Failed("network error")
            .reduce(started)

        assertTrue(started.refreshAllFeedsInProgress)
        assertFalse(started.loadingDialog)
        assertFalse(succeeded.refreshAllFeedsInProgress)
        assertFalse(failed.refreshAllFeedsInProgress)
        assertEquals(initial.loadingDialog, succeeded.loadingDialog)
        assertEquals(initial.loadingDialog, failed.loadingDialog)
    }

    @Test
    fun refreshAllFeedsExcludesMutedSubscriptions() {
        val feeds = listOf(
            FeedBean(url = "https://example.com/active.xml"),
            FeedBean(url = "https://example.com/muted.xml", mute = true),
        )

        assertEquals(
            listOf("https://example.com/active.xml"),
            feeds.unmutedFeedUrls(),
        )
    }
}
