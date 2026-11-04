package com.skyd.podaura.ui.screen.calendar

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.skyd.podaura.ui.theme.PodAuraTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.skia.Image
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.calendar_screen_name
import podaura.shared.generated.resources.feed_screen_refresh_all_subscriptions
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalendarTopBarTest {
    @Test
    fun phoneToolbarOffersRefreshAndBlocksRepeatedClicks() = checkToolbar(360, false)

    @Test
    fun wideToolbarOffersRefreshAndBlocksRepeatedClicks() = checkToolbar(1000, true)

    @OptIn(ExperimentalTestApi::class)
    private fun checkToolbar(width: Int, dark: Boolean) = runDesktopComposeUiTest(width = width, height = 96) {
        var refreshing by mutableStateOf(false)
        var refreshCount = 0
        var refreshLabel = ""
        var calendarLabel = ""
        setContent {
            PodAuraTheme(darkTheme = dark) {
                refreshLabel = stringResource(Res.string.feed_screen_refresh_all_subscriptions)
                calendarLabel = stringResource(Res.string.calendar_screen_name)
                Surface {
                    CalendarTopBar(
                        windowInsets = WindowInsets(0),
                        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
                        refreshing = refreshing,
                        onRefreshAllFeeds = { refreshCount++; refreshing = true },
                    )
                }
            }
        }
        val refresh = onNodeWithContentDescription(refreshLabel).assertIsDisplayed().assertIsEnabled()
        val buttonBounds = refresh.fetchSemanticsNode().boundsInRoot
        val titleBounds = onNodeWithText(calendarLabel).fetchSemanticsNode().boundsInRoot
        assertTrue(buttonBounds.left >= width - 64)
        assertTrue(titleBounds.right < buttonBounds.left)
        val output = File("build/reports/calendar-toolbar-$width.png")
        output.parentFile.mkdirs()
        output.writeBytes(Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).encodeToData()!!.bytes)
        refresh.performClick().assertIsNotEnabled().performTouchInput { click() }
        runOnIdle {
            assertEquals(1, refreshCount)
            refreshing = false
        }
        refresh.assertIsEnabled().performClick()
        runOnIdle { assertEquals(2, refreshCount) }
    }
}
