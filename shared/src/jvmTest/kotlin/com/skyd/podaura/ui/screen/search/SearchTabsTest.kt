package com.skyd.podaura.ui.screen.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.skyd.podaura.ui.component.swipe.SwipeAction
import com.skyd.podaura.ui.component.swipe.SwipeableActionsBox
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.article_screen_name
import podaura.shared.generated.resources.feed_screen_name
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SearchTabsTest {
    @Test
    fun tabsSwitchPagesWithoutReplacingArticleSwipeActions() = checkGestures(LayoutDirection.Ltr)

    @Test
    fun tabSwipesFollowRightToLeftLayout() = checkGestures(LayoutDirection.Rtl)

    private fun checkGestures(layoutDirection: LayoutDirection) = runDesktopComposeUiTest(
        width = 400,
        height = 600,
    ) {
        lateinit var pagerState: PagerState
        var articleActions = 0
        var feedTitle = ""
        var articleTitle = ""
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MaterialTheme {
                    pagerState = rememberPagerState(initialPage = 1, pageCount = { 2 })
                    feedTitle = stringResource(Res.string.feed_screen_name)
                    articleTitle = stringResource(Res.string.article_screen_name)
                    Column(modifier = Modifier.fillMaxSize()) {
                        SearchTabs(
                            pagerState = pagerState,
                            modifier = Modifier.testTag("tabs"),
                        )
                        HorizontalPager(
                            modifier = Modifier.weight(1f),
                            state = pagerState,
                            verticalAlignment = Alignment.Top,
                        ) { page ->
                            if (page == 0) {
                                Text(text = "Subscription result")
                            } else {
                                SwipeableActionsBox(
                                    modifier = Modifier.testTag("article"),
                                    startActions = listOf(
                                        SwipeAction(
                                            onSwipe = { articleActions++ },
                                            icon = {},
                                            background = Color.Green,
                                        )
                                    ),
                                ) {
                                    Text(
                                        text = "Article result",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        onNodeWithTag("article").performTouchInput {
            if (layoutDirection == LayoutDirection.Ltr) {
                swipeRight()
            } else {
                swipeLeft()
            }
        }
        runOnIdle {
            assertEquals(expected = 1, actual = articleActions)
            assertEquals(expected = 1, actual = pagerState.currentPage)
        }
        onNodeWithTag("tabs").performTouchInput {
            if (layoutDirection == LayoutDirection.Ltr) {
                swipeRight()
            } else {
                swipeLeft()
            }
        }
        runOnIdle { assertEquals(expected = 0, actual = pagerState.currentPage) }
        onNodeWithTag("tabs").performTouchInput {
            if (layoutDirection == LayoutDirection.Ltr) {
                swipeLeft()
            } else {
                swipeRight()
            }
        }
        runOnIdle { assertEquals(expected = 1, actual = pagerState.currentPage) }
        onNodeWithText(feedTitle).performClick()
        runOnIdle { assertEquals(expected = 0, actual = pagerState.currentPage) }
        onNodeWithText(articleTitle).performClick()
        runOnIdle {
            assertEquals(expected = 1, actual = pagerState.currentPage)
            assertEquals(expected = 1, actual = articleActions)
        }
    }
}
