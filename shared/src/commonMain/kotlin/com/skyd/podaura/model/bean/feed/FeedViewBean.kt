package com.skyd.podaura.model.bean.feed

import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Embedded
import com.skyd.podaura.model.bean.BaseBean
import com.skyd.podaura.model.bean.article.ARTICLE_TABLE_NAME
import com.skyd.podaura.model.bean.article.ArticleBean
import kotlinx.serialization.Serializable

const val FEED_VIEW_NAME = "FeedView"

@Serializable
@DatabaseView(
    value = "SELECT " +
            "  $FEED_TABLE_NAME.*, " +
            "  IFNULL(ArticleCount.totalCount, 0) AS ${FeedViewBean.ARTICLE_COUNT_COLUMN}, " +
            "  IFNULL(ArticleCount.unreadCount, 0) AS ${FeedViewBean.UNREAD_ARTICLE_COUNT_COLUMN} " +
            "FROM " +
            "  $FEED_TABLE_NAME " +
            "LEFT JOIN (" +
            "  SELECT " +
            "    ${ArticleBean.FEED_URL_COLUMN}, " +
            "    COUNT(1) AS totalCount, " +
            "    COUNT(CASE WHEN ${ArticleBean.IS_READ_COLUMN} = 0 THEN 1 END) AS unreadCount " +
            "  FROM " +
            "    $ARTICLE_TABLE_NAME " +
            "  GROUP BY " +
            "    ${ArticleBean.FEED_URL_COLUMN}" +
            ") AS ArticleCount " +
            "  ON $FEED_TABLE_NAME.${FeedBean.URL_COLUMN} = ArticleCount.${ArticleBean.FEED_URL_COLUMN}",
    viewName = FEED_VIEW_NAME
)
data class FeedViewBean(
    @Embedded
    val feed: FeedBean,
    @ColumnInfo(name = ARTICLE_COUNT_COLUMN)
    var articleCount: Int = 0,
    @ColumnInfo(name = UNREAD_ARTICLE_COUNT_COLUMN)
    var unreadArticleCount: Int = 0,
) : BaseBean {
    companion object {
        const val ARTICLE_COUNT_COLUMN = "articleCount"
        const val UNREAD_ARTICLE_COUNT_COLUMN = "unreadArticleCount"
    }
}