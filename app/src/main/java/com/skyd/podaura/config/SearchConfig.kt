package com.skyd.podaura.config

import com.skyd.podaura.model.bean.article.ARTICLE_TABLE_NAME
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.feed.FEED_TABLE_NAME
import com.skyd.podaura.model.bean.feed.FEED_VIEW_NAME
import com.skyd.podaura.model.bean.feed.FeedBean

val allSearchDomain: Map<String, List<String>> = mutableMapOf(
    FEED_TABLE_NAME to listOf(
        FeedBean.URL_COLUMN,
        FeedBean.TITLE_COLUMN,
        FeedBean.DESCRIPTION_COLUMN,
        FeedBean.LINK_COLUMN,
        FeedBean.ICON_COLUMN,
    ),
    ARTICLE_TABLE_NAME to listOf(
        ArticleBean.TITLE_COLUMN,
        ArticleBean.AUTHOR_COLUMN,
        ArticleBean.DESCRIPTION_COLUMN,
        ArticleBean.CONTENT_COLUMN,
        ArticleBean.LINK_COLUMN,
    ),
).apply {
    put(FEED_VIEW_NAME, getValue(FEED_TABLE_NAME))
}
