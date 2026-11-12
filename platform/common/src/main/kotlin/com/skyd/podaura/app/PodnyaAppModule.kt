package com.skyd.podaura.app

import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchAction
import com.skyd.podaura.podnya.capabilities.articles.ui.rememberArticleHistoryOpener
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.FeedSearchAction
import com.skyd.podaura.podnya.capabilities.feeds.ui.rememberFeedHistoryOpener
import com.skyd.podaura.podnya.core.AssistantRuntime
import com.skyd.podaura.podnya.di.podnyaModule
import com.skyd.podaura.podnya.ui.PodnyaChat
import com.skyd.podaura.podnya.ui.PodnyaSettings
import com.skyd.podaura.ui.screen.podnya.PodnyaPages
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.podnya_name
import org.jetbrains.compose.resources.getString
import org.koin.dsl.module

/** Application wiring only: shared owns navigation; Podnya owns the page content. */
val podnyaAppModule = module {
    includes(podnyaModule)
    single {
        PodnyaPages(
            available = get<AssistantRuntime>().available,
            title = { getString(Res.string.podnya_name) },
            chat = { callbacks ->
                val openArticle = rememberArticleHistoryOpener(callbacks.onOpenArticle)
                val openFeed = rememberFeedHistoryOpener(callbacks.onOpenFeed)
                PodnyaChat(
                    onBack = callbacks.onBack,
                    onSettings = callbacks.onSettings,
                    onSearch = callbacks.onSearch,
                    onAction = { action ->
                        when (action) {
                            is ArticleSearchAction.OpenArticle -> openArticle(action.articleId)
                            is FeedSearchAction.OpenFeed -> openFeed(action.feedUrl)
                        }
                    },
                    formatDate = callbacks.formatDate,
                )
            },
            settings = { onBack ->
                PodnyaSettings(onBack)
            },
        )
    }
}
