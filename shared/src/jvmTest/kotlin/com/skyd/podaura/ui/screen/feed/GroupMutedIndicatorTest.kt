package com.skyd.podaura.ui.screen.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.skyd.podaura.model.bean.group.GroupVo
import com.skyd.podaura.model.bean.group.GroupWithFeedCount
import com.skyd.podaura.model.preference.behavior.feed.HideMutedFeedPreference
import com.skyd.podaura.ui.screen.feed.item.Group1Item
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.skia.Image
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.collapse
import podaura.shared.generated.resources.expend
import podaura.shared.generated.resources.feed_group_all_muted
import podaura.shared.generated.resources.feed_group_muted_count
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class GroupMutedIndicatorTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun compactIndicatorsDescribeMuteStateWithoutOwnClickAction() {
        for (largeText in listOf(false, true)) {
            runDesktopComposeUiTest(width = 360, height = 360) {
                val data = mutableStateOf(GroupWithFeedCount(GroupVo("group", "Podcasts", false), 4, 0))
                val hideMuted = mutableStateOf(true)
                var partialMuted = ""
                var allMuted = ""
                var expand = ""
                var collapse = ""
                var articleOpens = 0
                setContent {
                    CompositionLocalProvider(
                        HideMutedFeedPreference.local provides hideMuted.value,
                        LocalDensity provides Density(LocalDensity.current.density, if (largeText) 2f else 1f),
                    ) {
                        MaterialTheme(colorScheme = if (largeText) darkColorScheme() else lightColorScheme()) {
                            partialMuted = pluralStringResource(Res.plurals.feed_group_muted_count, 2, 2)
                            allMuted = stringResource(Res.string.feed_group_all_muted)
                            expand = stringResource(Res.string.expend)
                            collapse = stringResource(Res.string.collapse)
                            Surface(Modifier.fillMaxSize().testTag("group-preview")) {
                                Box(Modifier.padding(horizontal = 16.dp)) {
                                    Group1Item(
                                        index = 0,
                                        data = data.value,
                                        onExpandChange = { group, expanded ->
                                            data.value = data.value.copy(group = GroupVo(group.groupId, group.name, expanded))
                                        },
                                        isEmpty = { true },
                                        onShowAllArticles = { articleOpens++ },
                                    )
                                }
                            }
                        }
                    }
                }
                fun capture(state: String) {
                    val output = File("build/reports/group-muted-indicator/$state-large-$largeText.png")
                    output.parentFile.mkdirs()
                    output.writeBytes(
                        Image.makeFromBitmap(onNodeWithTag("group-preview").captureToImage().asSkiaBitmap())
                            .encodeToData()!!.bytes
                    )
                }
                val rowHeight = onNodeWithText("Podcasts").fetchSemanticsNode().boundsInRoot.height
                onNodeWithContentDescription(partialMuted).assertDoesNotExist()
                onNodeWithContentDescription(allMuted).assertDoesNotExist()
                runOnIdle { data.value = data.value.copy(mutedFeedCount = 2) }
                assertEquals(rowHeight, onNodeWithText("Podcasts").fetchSemanticsNode().boundsInRoot.height)
                onNodeWithContentDescription(partialMuted, useUnmergedTree = true)
                    .assertIsDisplayed()
                    .assertHasNoClickAction()
                onNodeWithText(partialMuted).assertDoesNotExist()
                capture("compact-partial")
                onNodeWithContentDescription(partialMuted, useUnmergedTree = true)
                    .performTouchInput { click() }
                onNodeWithText(partialMuted).assertDoesNotExist()
                assertEquals(1, articleOpens)
                assertFalse(data.value.group.isExpanded)
                runOnIdle { hideMuted.value = false }
                onNodeWithContentDescription(partialMuted).assertIsDisplayed()
                runOnIdle { data.value = data.value.copy(mutedFeedCount = 4) }
                assertEquals(rowHeight, onNodeWithText("Podcasts").fetchSemanticsNode().boundsInRoot.height)
                onNodeWithContentDescription(allMuted, useUnmergedTree = true)
                    .assertIsDisplayed()
                    .assertHasNoClickAction()
                onNodeWithContentDescription(partialMuted).assertDoesNotExist()
                capture("compact-all")
                onNodeWithContentDescription(allMuted, useUnmergedTree = true)
                    .performTouchInput { click() }
                onNodeWithText(allMuted).assertDoesNotExist()
                assertEquals(2, articleOpens)
                assertFalse(data.value.group.isExpanded)
                runOnIdle { hideMuted.value = true }
                onNodeWithContentDescription(allMuted).assertIsDisplayed()
                onNodeWithContentDescription(expand).performClick()
                onNodeWithContentDescription(allMuted).assertIsDisplayed()
                onNodeWithContentDescription(collapse).performClick()
                onNodeWithText("Podcasts").performClick()
                assertEquals(3, articleOpens)
                runOnIdle { data.value = data.value.copy(feedCount = 0, mutedFeedCount = 0) }
                onNodeWithContentDescription(allMuted).assertDoesNotExist()
                onNodeWithText("Podcasts").assertIsDisplayed()
                runOnIdle {
                    data.value = GroupWithFeedCount(
                        GroupVo("group", "A very long podcast group title that should stay on one line", false),
                        1_000, 123,
                    )
                }
                assertEquals(
                    rowHeight,
                    onNodeWithText(data.value.group.name).fetchSemanticsNode().boundsInRoot.height,
                )
                capture("compact-long-title")
            }
        }
    }
}
