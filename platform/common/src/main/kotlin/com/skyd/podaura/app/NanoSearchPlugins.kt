package com.skyd.podaura.app

import com.skyd.podaura.di.articleQueryModule
import com.skyd.podaura.di.feedQueryModule
import com.skyd.podaura.nano.capabilities.articles.articleSearchModule
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleSearchAction
import com.skyd.podaura.nano.capabilities.articles.ui.rememberArticleHistoryOpener
import com.skyd.podaura.nano.capabilities.feeds.feedSearchModule
import com.skyd.podaura.nano.capabilities.feeds.model.bean.FeedSearchAction
import com.skyd.podaura.nano.capabilities.feeds.ui.rememberFeedHistoryOpener
import com.skyd.podaura.ui.screen.article.ArticleRoute
import com.skyd.podaura.ui.screen.read.ReadRoute

internal val articleSearchPlugin = NanoPlugin(
    id = "articles",
    modules = arrayOf(articleSearchModule, articleQueryModule),
) { _, navigate ->
    val open = rememberArticleHistoryOpener(onOpen = { navigate(ReadRoute(articleId = it)) })
    return@NanoPlugin { _, action ->
        if (action is ArticleSearchAction.OpenArticle) open(action.articleId)
    }
}

internal val feedSearchPlugin = NanoPlugin(
    id = "feeds",
    modules = arrayOf(feedSearchModule, feedQueryModule),
) { _, navigate ->
    val open = rememberFeedHistoryOpener(onOpen = { navigate(ArticleRoute(feedUrls = listOf(it))) })
    return@NanoPlugin { _, action ->
        if (action is FeedSearchAction.OpenFeed) open(action.feedUrl)
    }
}
