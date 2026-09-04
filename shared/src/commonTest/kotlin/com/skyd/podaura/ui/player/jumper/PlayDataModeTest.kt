package com.skyd.podaura.ui.player.jumper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlayDataModeTest {
    @Test
    fun articleListTimestampRoundTrips() {
        val mode = PlayDataMode.ArticleList(
            articleId = "article-id",
            url = "https://example.com/audio.mp3",
            startPositionSeconds = 83,
        )

        assertEquals(mode, PlayDataMode.decodeFromString(mode.encodeToString()))
    }

    @Test
    fun oldArticleListPayloadDefaultsTimestampToNull() {
        val mode = PlayDataMode.decodeFromString(
            """{"type":"com.skyd.podaura.ui.player.jumper.PlayDataMode.ArticleList","articleId":"article-id","url":"https://example.com/audio.mp3"}"""
        ) as PlayDataMode.ArticleList

        assertNull(mode.startPositionSeconds)
    }

    @Test
    fun mediaLibraryListRoundTrips() {
        val mode = PlayDataMode.MediaLibraryList(
            startMediaPath = "/downloads/episode.mp3",
            mediaList = listOf(
                PlayDataMode.MediaLibraryList.PlayMediaListItem(
                    path = "/downloads/episode.mp3",
                    articleId = "article-id",
                    title = "Episode",
                    thumbnail = null,
                )
            ),
        )

        val decoded = PlayDataMode.decodeFromString(mode.encodeToString())
                as PlayDataMode.MediaLibraryList

        assertEquals(mode, decoded)
    }
}
