package com.skyd.downloader.download

import com.skyd.downloader.db.DownloadEntity
import com.skyd.downloader.download.DownloadRequest.Companion.toDownloadRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class DownloadRequestTest {
    @Test
    fun metadataUpgradeIsStickyForTheLifetimeOfATask() {
        val directEntity = entity(metadata = null)
        val metadataRequest = request(metadata = "source-metadata")
        val upgraded = directEntity.withMetadataFrom(metadataRequest)

        assertEquals("source-metadata", upgraded.metadata)
        assertEquals(
            "source-metadata",
            upgraded.withMetadataFrom(request(metadata = null)).metadata,
        )
    }

    @Test
    fun retryRequestRetainsTaskMetadata() {
        assertEquals(
            "source-metadata",
            entity(metadata = "source-metadata").toDownloadRequest().metadata,
        )
    }

    private fun entity(metadata: String?) = DownloadEntity(
        id = 1,
        url = "https://example.com/episode.mp3",
        path = "/downloads",
        fileName = "episode.mp3",
        metadata = metadata,
    )

    private fun request(metadata: String?) = DownloadRequest(
        url = "https://example.com/episode.mp3",
        path = "/downloads",
        fileName = "episode.mp3",
        metadata = metadata,
        id = 1,
    )
}
