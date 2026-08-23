package com.skyd.podaura.ui.player.mini

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class MiniPlayerNavigationTest {
    @Test
    fun metadataMarksEntryForMiniPlayer() {
        val entry = NavEntry<NavKey>(
            key = MarkedRoute,
            metadata = miniPlayerMetadata(),
            content = {},
        )

        assertTrue(entry.metadata.hasMiniPlayerMetadata())
    }

    @Test
    fun miniPlayerEntryPreservesExistingMetadata() {
        val provider = entryProvider<NavKey> {
            miniPlayerEntry<MarkedRoute>(metadata = mapOf("existing" to 1)) {}
            entry<UnmarkedRoute> {}
        }

        val markedMetadata = provider(MarkedRoute).metadata
        assertTrue(markedMetadata.hasMiniPlayerMetadata())
        assertEquals(1, markedMetadata["existing"])
        assertFalse(provider(UnmarkedRoute).metadata.hasMiniPlayerMetadata())
    }

    private data object MarkedRoute : NavKey
    private data object UnmarkedRoute : NavKey
}
