package com.skyd.podaura.ui.screen.settings.rssconfig.updatenotification

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.skyd.podaura.model.bean.ArticleNotificationRuleBean
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.group.GroupVo
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.more
import podaura.shared.generated.resources.cancel
import podaura.shared.generated.resources.delete
import podaura.shared.generated.resources.notification_delete_question
import podaura.shared.generated.resources.notification_managed_delete_warning
import podaura.shared.generated.resources.notification_custom_label
import podaura.shared.generated.resources.notification_match_help
import podaura.shared.generated.resources.notification_managed_help
import podaura.shared.generated.resources.notification_feeds
import podaura.shared.generated.resources.notification_groups
import podaura.shared.generated.resources.notification_search
import podaura.shared.generated.resources.default_feed_group
import podaura.shared.generated.resources.ok
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class UpdateNotificationDialogTest {
    @Test
    fun customRuleCardEditsButTypeBadgeOnlyShowsInformation() = runComposeUiTest {
        val rule = ArticleNotificationRuleBean(id = 1, name = "Podcast filters", regex = ".*")
        var editCount = 0
        var typeLabel = ""
        var help = ""
        var ok = ""
        setContent {
            MaterialTheme {
                typeLabel = stringResource(Res.string.notification_custom_label)
                help = stringResource(Res.string.notification_match_help)
                ok = stringResource(Res.string.ok)
                RuleItem(
                    rule = rule,
                    ruleListState = RuleListState.Success(listOf(rule), emptyList(), emptyList()),
                    onEdit = { editCount++ },
                    onRemove = {},
                )
            }
        }
        onNodeWithText(typeLabel).performClick()
        onNodeWithText(help).assertExists()
        assertEquals(0, editCount)
        onNodeWithText(ok).performClick()
        onNodeWithText(rule.name).performClick()
        assertEquals(1, editCount)
    }

    @Test
    fun managedRuleCardShowsInformationWithoutEditing() = runComposeUiTest {
        val rule = ArticleNotificationRuleBean(
            id = 1, name = "Managed rule", regex = "",
            feedUrls = listOf("https://example.com/feed"), isManaged = true,
        )
        var help = ""
        setContent {
            MaterialTheme {
                help = stringResource(Res.string.notification_managed_help)
                RuleItem(
                    rule = rule,
                    ruleListState = RuleListState.Success(listOf(rule), emptyList(), emptyList()),
                    onEdit = { error("Managed rules cannot be edited") },
                    onRemove = {},
                )
            }
        }
        onNodeWithText(rule.name).performClick()
        onNodeWithText(help).assertExists()
    }

    @Test
    fun longRegexDoesNotTruncateItselfOrTheFollowingConditions() = runComposeUiTest {
        val regex = List(14) { ".*podcast$it.*" }.joinToString("\n")
        val rule = ArticleNotificationRuleBean(
            id = 1, name = "Long rule", regex = regex,
            feedUrls = listOf("https://example.com/feed"), groupIds = listOf("News group"),
        )
        setContent {
            MaterialTheme {
                LazyColumn {
                    item {
                        RuleItem(
                            rule = rule,
                            ruleListState = RuleListState.Success(listOf(rule), emptyList(), emptyList()),
                            onEdit = {},
                            onRemove = {},
                        )
                    }
                }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        onNodeWithText(regex, substring = true, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            assertTrue(it(layouts))
        }
        assertTrue(layouts.single().lineCount >= 14)
        assertFalse(layouts.single().didOverflowHeight)
        onNodeWithText(rule.feedUrls.single(), substring = true).assertExists()
        onNodeWithText(rule.groupIds.single(), substring = true).assertExists()
    }

    @Test
    fun deletingManagedRuleRequiresConfirmationAndExplainsItsEffect() = runComposeUiTest {
        val rule = ArticleNotificationRuleBean(
            id = 7, name = "Subscription rule", regex = "",
            feedUrls = listOf("https://example.com/feed"), isManaged = true,
        )
        val removedIds = mutableListOf<Int>()
        var question = ""
        var warning = ""
        var cancel = ""
        var delete = ""
        var more = ""
        setContent {
            MaterialTheme {
                question = stringResource(Res.string.notification_delete_question, rule.name)
                warning = stringResource(Res.string.notification_managed_delete_warning)
                cancel = stringResource(Res.string.cancel)
                delete = stringResource(Res.string.delete)
                more = stringResource(Res.string.more)
                RuleItem(
                    rule = rule,
                    ruleListState = RuleListState.Success(listOf(rule), emptyList(), emptyList()),
                    onEdit = { error("Managed rules cannot be edited") },
                    onRemove = { removedIds.add(it) },
                )
            }
        }

        onNodeWithContentDescription(more).performClick()
        onNodeWithText(delete).performClick()
        onNodeWithText(question).assertExists()
        onNodeWithText(warning).assertExists()
        assertEquals(emptyList(), removedIds)
        onNodeWithText(cancel).performClick()
        assertEquals(emptyList(), removedIds)
        onNodeWithContentDescription(more).performClick()
        onNodeWithText(delete).performClick()
        onNodeWithText(delete).performClick()
        assertEquals(listOf(rule.id), removedIds)
    }

    @Test
    fun addDialogMeasuresItsTextFieldsWithoutNestedScrollConstraints() = runComposeUiTest {
        setContent {
            MaterialTheme {
                AddRuleDialog(
                    rule = null,
                    ruleListState = RuleListState.Success(emptyList(), emptyList(), emptyList()),
                    onDismissRequest = {},
                    onAdd = {},
                )
            }
        }
        waitForIdle()
        onAllNodes(hasSetTextAction()).assertCountEquals(2)
    }

    @Test
    fun editDialogMeasuresAndPreservesTheExistingRule() = runComposeUiTest {
        val rule = ArticleNotificationRuleBean(id = 1, name = "PodAura rule", regex = ".*podcast.*")
        setContent {
            MaterialTheme {
                AddRuleDialog(
                    rule = rule,
                    ruleListState = RuleListState.Success(listOf(rule), emptyList(), emptyList()),
                    onDismissRequest = {},
                    onAdd = {},
                )
            }
        }
        waitForIdle()
        onNodeWithText(rule.name).assertExists()
        onNodeWithText(rule.regex).assertExists()
    }

    @Test
    fun targetSearchPreservesMissingTargetsAndRefreshesRenamedFeeds() = runComposeUiTest {
        val url = "https://example.com/feed"
        val missing = "https://example.com/deleted"
        val rule = ArticleNotificationRuleBean(
            id = 1, name = "Targets", regex = "", feedUrls = listOf(url, missing),
            groupIds = listOf(GroupVo.DEFAULT_GROUP_ID),
        )
        var feeds by mutableStateOf(listOf(FeedBean(url, title = "Original title")))
        var feedsLabel = ""
        var groupsLabel = ""
        var defaultGroup = ""
        var search = ""
        var cancel = ""
        setContent {
            MaterialTheme {
                feedsLabel = stringResource(Res.string.notification_feeds)
                groupsLabel = stringResource(Res.string.notification_groups)
                defaultGroup = stringResource(Res.string.default_feed_group)
                search = stringResource(Res.string.notification_search)
                cancel = stringResource(Res.string.cancel)
                AddRuleDialog(
                    rule = rule,
                    ruleListState = RuleListState.Success(listOf(rule), feeds, emptyList()),
                    onDismissRequest = {}, onAdd = {},
                )
            }
        }
        onNodeWithText("$groupsLabel: $defaultGroup").assertExists()
        onNodeWithText("$feedsLabel: Original title, $missing").performClick()
        onNodeWithText(search).performTextInput("ORIGINAL")
        onNodeWithText("Original title").assertExists()
        onNodeWithText(missing).assertDoesNotExist()
        onNodeWithText(search).performTextReplacement("deleted")
        onNodeWithText(missing).assertExists()
        onNodeWithText("Original title").assertDoesNotExist()
        onAllNodesWithText(cancel).let { it[it.fetchSemanticsNodes().lastIndex].performClick() }
        runOnIdle { feeds = listOf(FeedBean(url, title = "Original title", nickname = "Renamed")) }
        onNodeWithText("$feedsLabel: Renamed, $missing").assertExists()
    }
}
