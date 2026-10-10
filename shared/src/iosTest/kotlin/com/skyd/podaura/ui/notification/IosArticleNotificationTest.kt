package com.skyd.podaura.ui.notification

import com.skyd.podaura.model.bean.ArticleNotificationRuleBean
import com.skyd.podaura.ui.component.UuidList
import com.skyd.podaura.ui.screen.article.ArticleRoute
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSPropertyListBinaryFormat_v1_0
import platform.Foundation.NSPropertyListSerialization
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class IosArticleNotificationTest {
    @Test
    fun eachNotificationRetainsItsOwnArticleBatchInAnOsSerializablePayload() = runBlocking<Unit> {
        val ids = List(5_000) { Uuid.random().toString() }
        val rule = ArticleNotificationRuleBean(id = 1, name = "News", regex = ".*")
        val content = articleNotificationContent((ids + ids.first()).map { it to rule })
        val data = assertNotNull(NSPropertyListSerialization.dataWithPropertyList(
            content.userInfo, NSPropertyListBinaryFormat_v1_0, 0u, null,
        ))
        val restored = NSPropertyListSerialization.propertyListWithData(data, 0u, null, null) as Map<Any?, *>
        assertEquals(ArticleRoute(articleIds = UuidList(ids)), articleNotificationRoute(restored))
        val otherIds = listOf(Uuid.random().toString())
        val other = articleNotificationContent(otherIds.map { it to rule })
        assertEquals(ArticleRoute(articleIds = UuidList(otherIds)), articleNotificationRoute(other.userInfo))
        assertEquals(ArticleRoute(articleIds = UuidList(ids)), articleNotificationRoute(content.userInfo))
        assertNotNull(content.sound)
    }

    @Test
    fun invalidOrEmptyPayloadNeverOpensAnUnfilteredArticleList() {
        listOf<Map<Any?, *>>(
            emptyMap<Any?, Any>(), mapOf("articleIds" to emptyList<String>()),
            mapOf("articleIds" to listOf("invalid")), mapOf("articleIds" to "invalid"),
            mapOf("articleIds" to listOf(Uuid.random().toString(), 42)),
        ).forEach { assertNull(articleNotificationRoute(it)) }
    }

    @Test
    fun notificationNavigationIsBufferedUntilTheUiIsReady() {
        while (iosArticleNotificationRequests.tryReceive().isSuccess) Unit
        val route = ArticleRoute(articleIds = UuidList(listOf(Uuid.random().toString())))
        iosArticleNotificationRequests.trySend(route)
        assertEquals(route, iosArticleNotificationRequests.tryReceive().getOrNull())
    }
}
