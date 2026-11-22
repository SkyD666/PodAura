package com.skyd.podaura.app

import com.skyd.podaura.di.articleQueryModule
import com.skyd.podaura.di.feedQueryModule
import com.skyd.podaura.podnya.capabilities.articles.articleSearchModule
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchAction
import com.skyd.podaura.podnya.capabilities.articles.ui.rememberArticleHistoryOpener
import com.skyd.podaura.podnya.capabilities.feeds.feedSearchModule
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.FeedSearchAction
import com.skyd.podaura.podnya.capabilities.feeds.ui.rememberFeedHistoryOpener
import com.skyd.podaura.ui.screen.article.ArticleRoute
import com.skyd.podaura.ui.screen.read.ReadRoute

internal val articleSearchPlugin = PodnyaPlugin(
    id = "articles",
    modules = arrayOf(articleSearchModule, articleQueryModule),
) { _, navigate ->
    val open = rememberArticleHistoryOpener(onOpen = { navigate(ReadRoute(articleId = it)) })
    return@PodnyaPlugin { _, action ->
        if (action is ArticleSearchAction.OpenArticle) open(action.articleId)
    }
}

internal val feedSearchPlugin = PodnyaPlugin(
    id = "feeds",
    modules = arrayOf(feedSearchModule, feedQueryModule),
) { _, navigate ->
    val open = rememberFeedHistoryOpener(onOpen = { navigate(ArticleRoute(feedUrls = listOf(it))) })
    return@PodnyaPlugin { _, action ->
        if (action is FeedSearchAction.OpenFeed) open(action.feedUrl)
    }
}
