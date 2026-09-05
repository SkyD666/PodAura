package com.skyd.podaura.ui.player

import com.skyd.podaura.ui.player.jumper.PlayDataMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayerEntryTest {
    private val requests = listOf(
        PlayerOpenRequest.Media(PlayDataMode.Playlist("playlist", null), "media-request"),
        PlayerOpenRequest.Files(emptyList(), "file-request"),
        PlayerOpenRequest.Resume,
    )

    @Test
    fun everyRequestChecksConsentExactlyOnceBeforePlatformPlayback() {
        var checks = 0
        val opened = mutableListOf<PlayerOpenRequest>()
        val entry = object : PlayerEntry({ checks++; true }) {
            override fun openAccepted(request: PlayerOpenRequest) {
                assertEquals(opened.size + 1, checks)
                opened += request
            }
            override fun showTerms() = error("Accepted user must not be redirected")
        }
        requests.forEach(entry::open)
        assertEquals(requests, opened)
        assertEquals(requests.size, checks)
    }

    @Test
    fun rejectionDropsRequestsAndOnlyANewRequestCanPlayAfterAcceptance() {
        var accepted = false
        var redirects = 0
        val opened = mutableListOf<PlayerOpenRequest>()
        val entry = object : PlayerEntry({ accepted }) {
            override fun openAccepted(request: PlayerOpenRequest) { opened += request }
            override fun showTerms() { redirects++ }
        }
        requests.forEach(entry::open)
        assertEquals(requests.size, redirects)
        assertTrue(opened.isEmpty())
        accepted = true
        assertTrue(opened.isEmpty())
        entry.open(PlayerOpenRequest.Resume)
        assertEquals(listOf<PlayerOpenRequest>(PlayerOpenRequest.Resume), opened)
    }
}
