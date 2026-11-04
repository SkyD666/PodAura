package com.skyd.podaura.ui.notification

import com.skyd.podaura.model.bean.ArticleNotificationRuleBean
import com.skyd.podaura.model.bean.article.ArticleBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import platform.Foundation.NSBundle
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class MacosArticleNotificationTest {
    @Test
    fun refreshDeliveryDoesNotCrashInAnUnbundledExecutable() = runTest {
        assertFalse(NSBundle.mainBundle.bundlePath.endsWith(".app"))
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val id = Uuid.random().toString()
            val rule = ArticleNotificationRuleBean(id = 1, name = "News", regex = ".*")
            PlatformArticleNotification.initialize()
            PlatformArticleNotification.requestPermission(showSettingsIfDenied = false)
            PlatformArticleNotification.sendNotification(listOf(id to rule))
            PlatformAutoDownload.autoDownload(
                mapOf("https://example.com/feed" to listOf(ArticleBean(id, "https://example.com/feed"))),
            )
        } finally {
            Dispatchers.resetMain()
        }
    }
}
