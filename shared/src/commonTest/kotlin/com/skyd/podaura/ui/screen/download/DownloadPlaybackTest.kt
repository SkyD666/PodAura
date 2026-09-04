package com.skyd.podaura.ui.screen.download

import com.skyd.downloader.Status
import com.skyd.podaura.model.download.ArticleDownloadSource
import com.skyd.podaura.model.download.DownloadInfoBean
import com.skyd.podaura.ui.player.jumper.PlayDataMode
import kotlin.test.Test
import kotlin.test.assertEquals

class DownloadPlaybackTest {
    @Test
    fun createsSingleItemLocalPlaylistThatStartsPlayback() {
        val item = DownloadInfoBean(
            id = "download-id",
            url = "https://example.com/episode.mp3",
            path = "/downloads",
            fileName = "episode.mp3",
            status = Status.Success,
            totalBytes = 100,
            downloadedBytes = 100,
            speedInBytePerMs = 0f,
            createTime = 1,
            failureReason = "",
            articleDownloadSource = ArticleDownloadSource(
                articleId = "article-id",
                feedUrl = "https://example.com/feed.xml",
            ),
            isPlayableMedia = true,
        )

        val mode = item.toPlayDataMode("/downloads/episode.mp3")

        assertEquals("/downloads/episode.mp3", mode.startMediaPath)
        assertEquals(1, mode.mediaList.size)
        assertEquals("/downloads/episode.mp3", mode.mediaList.single().path)
        assertEquals("article-id", mode.mediaList.single().articleId)
        assertEquals("episode.mp3", mode.mediaList.single().title)
    }
}
