package com.skyd.podaura.ui.screen.article

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import com.skyd.compone.component.navigation.LocalNavBackStack
import com.skyd.compone.component.navigation.newNavBackStack
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.article.ArticleWithEnclosureBean
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.ui.component.navigation.PodAuraSerializersModule
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.skia.Image
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.article_select
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class ArticleSelectionItemTest {
    @Test
    fun narrowRowTogglesSelectionAndDisablesActionsWhileEnqueuing() = checkSelection(width = 320)

    @Test
    fun wideRowTogglesSelectionAndDisablesActionsWhileEnqueuing() = checkSelection(width = 840)

    @OptIn(ExperimentalTestApi::class)
    private fun checkSelection(width: Int) = runDesktopComposeUiTest(width = width, height = 320) {
        var selected by mutableStateOf(false)
        var selectionMode by mutableStateOf(false)
        var enabled by mutableStateOf(true)
        var toggleCount = 0
        var navigationSize: () -> Int = { 0 }
        val title = "A long episode title covering several topics and continuing onto another line"
        val feedName = "A podcast with a long name"
        var selectionLabel = ""
        setContent {
            val backStack = rememberNavBackStack(
                SavedStateConfiguration { serializersModule = PodAuraSerializersModule },
                ArticleRoute(),
            )
            navigationSize = { backStack.size }
            CompositionLocalProvider(LocalNavBackStack provides newNavBackStack(backStack, parent = null)) {
                MaterialTheme {
                    selectionLabel = stringResource(Res.string.article_select)
                    Article1Item(
                        data = ArticleWithFeed(
                            ArticleWithEnclosureBean(
                                ArticleBean(articleId = "episode", feedUrl = "feed", title = title),
                                enclosures = emptyList(), categories = emptyList(), media = null,
                            ),
                            FeedBean(url = "feed", title = feedName),
                        ),
                        onFavorite = { _, _ -> error("Unexpected favorite action") },
                        onRead = { _, _ -> error("Unexpected read action") },
                        onDelete = { error("Unexpected delete action") },
                        onMessage = { error("Unexpected message") },
                        onEditFeedSheet = { error("Unexpected feed action") },
                        selected = if (selectionMode) selected else null,
                        selectionEnabled = enabled,
                        onToggleSelection = { selected = !selected; toggleCount++ },
                        onEnterSelection = { selectionMode = true; selected = true },
                    )
                }
            }
        }
        val titleNode = onNodeWithText(title, useUnmergedTree = true).fetchSemanticsNode()
        val feedNode = onNodeWithText(feedName, useUnmergedTree = true).fetchSemanticsNode()
        val titleBounds = titleNode.boundsInRoot
        val feedBounds = feedNode.boundsInRoot
        val itemBounds = onNodeWithTag("ArticleItem").fetchSemanticsNode().boundsInRoot
        val browseScreenshot = onRoot().captureToImage()
        val browseOutput = File("build/reports/article-browse-$width.png")
        browseOutput.parentFile.mkdirs()
        browseOutput.writeBytes(Image.makeFromBitmap(browseScreenshot.asSkiaBitmap()).encodeToData()!!.bytes)
        onNodeWithTag("ArticleItem").performTouchInput { longClick() }
        onNodeWithText(selectionLabel).performClick()
        assertEquals(titleNode.id, onNodeWithText(title, useUnmergedTree = true).fetchSemanticsNode().id)
        assertEquals(feedNode.id, onNodeWithText(feedName, useUnmergedTree = true).fetchSemanticsNode().id)
        assertEquals(titleBounds, onNodeWithText(title, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot)
        assertEquals(feedBounds, onNodeWithText(feedName, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot)
        assertEquals(itemBounds, onNodeWithTag("ArticleItem").fetchSemanticsNode().boundsInRoot)
        val row = onNode(hasText(title) and hasClickAction())
        row.assertIsSelected()
        runOnIdle { selected = false }
        row.assertIsNotSelected().assertIsOff().performClick().assertIsSelected().assertIsOn()
        runOnIdle {
            assertEquals(1, toggleCount)
            assertEquals(1, navigationSize())
        }
        val screenshot = onRoot().captureToImage()
        val output = File("build/reports/article-selection-$width.png")
        output.parentFile.mkdirs()
        output.writeBytes(Image.makeFromBitmap(screenshot.asSkiaBitmap()).encodeToData()!!.bytes)
        row.performClick().assertIsNotSelected()
        onNodeWithText(feedName, useUnmergedTree = true).performTouchInput { click() }
        row.assertIsSelected()
        runOnIdle { assertEquals(1, navigationSize()) }
        runOnIdle { enabled = false }
        row.assertIsNotEnabled()
        row.performTouchInput { swipeLeft() }
        row.performTouchInput { swipeRight() }
        assertEquals(itemBounds, onNodeWithTag("ArticleItem").fetchSemanticsNode().boundsInRoot)
        runOnIdle {
            assertEquals(3, toggleCount)
            assertEquals(1, navigationSize())
        }
        runOnIdle { selectionMode = false }
        assertEquals(titleNode.id, onNodeWithText(title, useUnmergedTree = true).fetchSemanticsNode().id)
        assertEquals(feedNode.id, onNodeWithText(feedName, useUnmergedTree = true).fetchSemanticsNode().id)
    }
}
