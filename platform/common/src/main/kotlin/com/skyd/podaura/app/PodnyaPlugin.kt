package com.skyd.podaura.app

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import com.skyd.podaura.di.articleQueryModule
import com.skyd.podaura.di.assistantDownloadHostModule
import com.skyd.podaura.di.feedQueryModule
import com.skyd.podaura.podnya.capabilities.articles.articleSearchModule
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchAction
import com.skyd.podaura.podnya.capabilities.articles.ui.rememberArticleHistoryOpener
import com.skyd.podaura.podnya.capabilities.downloads.OpenDownloads
import com.skyd.podaura.podnya.capabilities.downloads.downloadModule
import com.skyd.podaura.podnya.capabilities.feeds.feedSearchModule
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.FeedSearchAction
import com.skyd.podaura.podnya.capabilities.feeds.ui.rememberFeedHistoryOpener
import com.skyd.podaura.podnya.core.AssistantAction
import com.skyd.podaura.ui.screen.article.ArticleRoute
import com.skyd.podaura.ui.screen.download.DownloadRoute
import com.skyd.podaura.ui.screen.read.ReadRoute
import org.koin.core.module.Module

/** All dependencies and host actions installed by one application registration. */
internal class PodnyaPlugin(
    vararg val modules: Module,
    val bindNavigation: @Composable (navigate: (NavKey) -> Unit) -> (AssistantAction) -> Unit = { {} },
)

@Composable
internal fun List<PodnyaPlugin>.bindNavigation(navigate: (NavKey) -> Unit): (AssistantAction) -> Unit {
    val handlers = map { it.bindNavigation(navigate) }
    return { action -> handlers.forEach { it(action) } }
}

internal val articleSearchPlugin =
    PodnyaPlugin(articleSearchModule, articleQueryModule) { navigate ->
        val open = rememberArticleHistoryOpener(onOpen = { navigate(ReadRoute(articleId = it)) })
        return@PodnyaPlugin { action -> if (action is ArticleSearchAction.OpenArticle) open(action.articleId) }
    }

internal val feedSearchPlugin = PodnyaPlugin(feedSearchModule, feedQueryModule) { navigate ->
    val open = rememberFeedHistoryOpener(onOpen = { navigate(ArticleRoute(feedUrls = listOf(it))) })
    return@PodnyaPlugin { action -> if (action is FeedSearchAction.OpenFeed) open(action.feedUrl) }
}

internal val downloadPlugin =
    PodnyaPlugin(downloadModule, assistantDownloadHostModule) { navigate ->
        { action -> if (action == OpenDownloads) navigate(DownloadRoute()) }
    }
