package com.skyd.podaura.ui.screen.download

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.skyd.downloader.Status
import com.skyd.podaura.model.download.DownloadInfoBean
import kotlin.test.Test
import kotlin.test.assertEquals

class DownloadItemTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun completedMediaUsesRowForPlaybackAndDeleteDoesNotTriggerPlayback() =
        runDesktopComposeUiTest(width = 600, height = 300) {
            var playCount = 0
            var deleteCount = 0

            setContent {
                MaterialTheme {
                    DownloadItem(
                        data = downloadItem(fileName = "episode.mp3", isPlayableMedia = true),
                        onPause = {},
                        onResume = {},
                        onRetry = {},
                        onDelete = { deleteCount++ },
                        onPlay = { playCount++ },
                    )
                }
            }

            onNode(hasText("episode.mp3") and hasClickAction()).performClick()
            runOnIdle { assertEquals(1, playCount) }

            onNodeWithContentDescription("Delete").performClick()
            runOnIdle {
                assertEquals(1, playCount)
                assertEquals(1, deleteCount)
            }
            onNodeWithContentDescription("Play").assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun nonMediaAndUnsupportedRowsDoNotExposePlaybackAction() =
        runDesktopComposeUiTest(width = 600, height = 300) {
            setContent {
                MaterialTheme {
                    DownloadItem(
                        data = downloadItem(fileName = "notes.pdf", isPlayableMedia = false),
                        onPause = {},
                        onResume = {},
                        onRetry = {},
                        onDelete = {},
                        onPlay = {},
                    )
                }
            }

            onNode(hasText("notes.pdf") and hasClickAction()).assertDoesNotExist()

            setContent {
                MaterialTheme {
                    DownloadItem(
                        data = downloadItem(fileName = "episode.mp3", isPlayableMedia = true),
                        onPause = {},
                        onResume = {},
                        onRetry = {},
                        onDelete = {},
                        onPlay = null,
                    )
                }
            }

            onNode(hasText("episode.mp3") and hasClickAction()).assertDoesNotExist()
        }

    private fun downloadItem(
        fileName: String,
        isPlayableMedia: Boolean,
    ) = DownloadInfoBean(
        id = fileName,
        url = "https://example.com/$fileName",
        path = "/downloads",
        fileName = fileName,
        status = Status.Success,
        totalBytes = 100,
        downloadedBytes = 100,
        speedInBytePerMs = 0f,
        createTime = 1,
        failureReason = "",
        isPlayableMedia = isPlayableMedia,
    )
}
