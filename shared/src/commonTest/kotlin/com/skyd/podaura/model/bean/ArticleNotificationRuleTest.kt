package com.skyd.podaura.model.bean

import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.article.ArticleWithEnclosureBean
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArticleNotificationRuleTest {
    private fun article(
        url: String = "a",
        title: String = "Daily Tech news",
        description: String? = null,
        content: String? = null
    ) =
        ArticleWithEnclosureBean(
            ArticleBean(
                articleId = "id", feedUrl = url, title = title,
                description = description, content = content
            ), emptyList(), emptyList(), null
        )

    @Test
    fun regexMatchesTitleDescriptionOrContent() {
        val rule = ArticleNotificationRuleBean(name = "Content", regex = "(?i).*tech.*")
        assertTrue(rule.match(article()))
        assertTrue(rule.match(article(title = "Episode", description = "TECH")))
        assertTrue(rule.match(article(title = "Episode", content = "About tech today")))
        assertFalse(rule.match(article(title = "Cooking")))
        assertTrue(rule.copy(regex = "a.b").match(article(title = "axb")))
    }

    @Test
    fun regexRetainsFullMatchAndCaseSensitivityAndRejectsInvalidPatterns() {
        val rule = ArticleNotificationRuleBean(name = "Regex", regex = "Tech")
        assertFalse(rule.match(article()))
        assertTrue(rule.copy(regex = ".*Tech.*").match(article()))
        assertFalse(rule.copy(regex = ".*tech.*").match(article()))
        assertFalse(rule.copy(regex = "[").match(article()))
    }

    @Test
    fun contentFeedAndGroupConditionsAllHaveToMatch() {
        val rule = ArticleNotificationRuleBean(
            name = "Scopes", regex = ".*Tech.*",
            feedUrls = listOf("a", "b"), groupIds = listOf("science", "technology")
        )
        assertTrue(rule.match(article("a"), "science"))
        assertTrue(rule.match(article("b"), "technology"))
        assertFalse(rule.match(article("c"), "science"))
        assertFalse(rule.match(article("a"), "other"))
        assertFalse(rule.match(article("a", title = "Cooking"), "science"))
        assertFalse(rule.match(article("a"), null))
    }

    @Test
    fun emptyContentAllowsScopeOnlyButAnEntirelyEmptyRuleNeverMatches() {
        val empty = ArticleNotificationRuleBean(name = "Empty", regex = "")
        assertFalse(empty.isValid())
        assertFalse(empty.match(article()))
        assertTrue(empty.copy(feedUrls = listOf("a")).match(article()))
        assertTrue(empty.copy(groupIds = listOf("science")).match(article(), "science"))
        assertTrue(empty.copy(groupIds = listOf("default")).match(article(), null))
        assertFalse(empty.copy(groupIds = listOf("default")).match(article(), "science"))
        assertFalse(empty.copy(regex = "   ").isValid())
    }

    @Test
    fun managedRulesHaveExactlyOneFeedAndNoOtherConditions() {
        val managed = ArticleNotificationRuleBean(
            name = "Feed", regex = "", isManaged = true,
            feedUrls = listOf("a")
        )
        assertTrue(managed.isValid())
        assertTrue(managed.match(article("a")))
        assertFalse(managed.match(article("b")))
        assertFalse(managed.copy(feedUrls = listOf("a", "b")).isValid())
        assertFalse(managed.copy(regex = ".*").isValid())
        assertFalse(managed.copy(groupIds = listOf("group")).isValid())
    }
}
