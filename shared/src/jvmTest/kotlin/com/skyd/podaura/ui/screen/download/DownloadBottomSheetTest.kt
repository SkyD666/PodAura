package com.skyd.podaura.ui.screen.download

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.skyd.downloader.Status
import com.skyd.podaura.model.download.DownloadInfoBean
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.close
import podaura.shared.generated.resources.download_screen_add_download
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DownloadBottomSheetTest {
    private val items = List(40) { index ->
        DownloadInfoBean(
            id = index.toString(),
            url = "https://example.test/$index",
            path = "/downloads",
            fileName = "Episode $index",
            status = Status.Downloading,
            totalBytes = 100,
            downloadedBytes = 50,
            speedInBytePerMs = 0f,
            createTime = index.toLong(),
            failureReason = "",
        )
    }

    @Test
    fun focusUsesQueueOrderAndFallsBackWhenTasksAreGone() {
        val focus = DownloadListFocus(queued = setOf("39", "27"), existing = setOf("9", "7"))
        assertEquals(27, focus.initialIndex(items))
        assertEquals(7, focus.initialIndex(items.take(20)))
        assertEquals(0, focus.initialIndex(items.take(4)))
    }

    @Test
    fun partialSheetShowsLastMatchAndProgressUpdatesRespectManualScroll() =
        runDesktopComposeUiTest(width = 900, height = 800) {
            var visible by mutableStateOf(true)
            var tasks by mutableStateOf(items)
            var added = false
            var addLabel = ""
            var closeLabel = ""
            val safeBottom = 48.dp
            setContent {
                MaterialTheme {
                    addLabel = stringResource(Res.string.download_screen_add_download)
                    closeLabel = stringResource(Res.string.close)
                    if (visible) {
                        DownloadSheetLayout(
                            onDismissRequest = { visible = false },
                            onAdd = { added = true },
                            windowInsets = { WindowInsets(bottom = safeBottom) },
                        ) {
                            DownloadList(
                                downloadInfoBeanList = tasks,
                                focus = DownloadListFocus(queued = setOf("39"), existing = setOf("0")),
                                modifier = Modifier,
                                contentPadding = PaddingValues(0.dp),
                                onPlay = null,
                            )
                        }
                    }
                }
            }
            val last = onNodeWithText("Episode 39")
            last.assertIsDisplayed()
            val bounds = onNode(hasScrollToIndexAction()).getUnclippedBoundsInRoot()
            assertTrue(bounds.top >= 400.dp, "Expected a half-height sheet: $bounds")
            assertEquals(800.dp - safeBottom, bounds.bottom)
            assertTrue(last.getUnclippedBoundsInRoot().bottom <= bounds.bottom)
            assertTrue(bounds.right - bounds.left < 900.dp)
            onNode(hasScrollToIndexAction()).performScrollToIndex(12)
            runOnIdle { tasks = tasks.map { it.copy(downloadedBytes = 75) } }
            onNodeWithText("Episode 12").assertIsDisplayed()
            last.assertDoesNotExist()
            onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
                .performSemanticsAction(SemanticsActions.Expand)
            val expandedBounds = onNode(hasScrollToIndexAction()).getUnclippedBoundsInRoot()
            assertTrue(expandedBounds.top < bounds.top)
            assertEquals(bounds.bottom, expandedBounds.bottom)
            onNodeWithContentDescription(addLabel).performClick()
            runOnIdle { assertTrue(added) }
            onNodeWithContentDescription(closeLabel).performClick()
            onNodeWithText("Episode 12").assertDoesNotExist()
            runOnIdle { visible = true }
            last.assertIsDisplayed()
        }
}
