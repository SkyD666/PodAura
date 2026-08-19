package com.skyd.podaura.model.download

import com.skyd.downloader.Status
import com.skyd.podaura.model.bean.feed.FeedBean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DownloadInfoBeanTest {
    @Test
    fun articleDownloadTitleReplacesFileNameAndKeepsFileNameAsSecondaryText() {
        val download = downloadInfo().copy(
            articleDownloadInfo = articleDownloadInfo(articleTitle = "Episode title"),
        )

        assertEquals("Episode title", download.displayTitle)
        assertEquals("original-name.mp3", download.secondaryFileName)
    }

    @Test
    fun missingArticleDownloadTitleFallsBackWithoutDuplicatingFileName() {
        val download = downloadInfo().copy(
            articleDownloadInfo = articleDownloadInfo(articleTitle = " "),
        )

        assertEquals("original-name.mp3", download.displayTitle)
        assertNull(download.secondaryFileName)
    }

    @Test
    fun directDownloadKeepsExistingFileNameOnlyPresentation() {
        val download = downloadInfo()

        assertEquals("original-name.mp3", download.displayTitle)
        assertNull(download.secondaryFileName)
    }

    @Test
    fun imageCandidatesPreferEpisodeThenArticleAndRemoveDuplicates() {
        assertEquals(
            listOf("episode.jpg", "article.jpg"),
            articleDownloadInfo(
                articleTitle = "Episode",
                episodeImage = "episode.jpg",
                articleImage = "article.jpg",
            ).imageCandidates,
        )
        assertEquals(
            listOf("same.jpg"),
            articleDownloadInfo(
                articleTitle = "Episode",
                episodeImage = "same.jpg",
                articleImage = "same.jpg",
            ).imageCandidates,
        )
    }

    private fun downloadInfo() = DownloadInfoBean(
        id = 1,
        url = "https://example.com/original-name.mp3",
        path = "/downloads",
        fileName = "original-name.mp3",
        status = Status.Downloading,
        totalBytes = 100,
        downloadedBytes = 50,
        speedInBytePerMs = 1f,
        createTime = 1,
        failureReason = "",
    )

    private fun articleDownloadInfo(
        articleTitle: String?,
        episodeImage: String? = null,
        articleImage: String? = null,
    ) = ArticleDownloadInfoBean(
        articleTitle = articleTitle,
        episodeImage = episodeImage,
        articleImage = articleImage,
        feed = FeedBean(url = "https://example.com/feed.xml"),
    )
}
