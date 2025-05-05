package com.skyd.podaura.model.bean.article

import androidx.room.Embedded
import androidx.room.Relation
import com.skyd.podaura.model.bean.BaseBean
import com.skyd.podaura.model.bean.feed.FeedBean
import kotlinx.serialization.Serializable

/**
 * An [articleWithEnclosure] contains a [feed].
 */
@Serializable
data class ArticleWithFeed(
    @Embedded
    var articleWithEnclosure: ArticleWithEnclosureBean,
    @Relation(parentColumn = ArticleBean.FEED_URL_COLUMN, entityColumn = FeedBean.URL_COLUMN)
    var feed: FeedBean,
) : BaseBean {
    fun getThumbnail(): String? {
        return articleWithEnclosure.media?.image ?: feed.customIcon ?: feed.icon
    }

    fun getArtist(): String? {
        return articleWithEnclosure.article.author.orEmpty().ifEmpty { feed.title }
    }
}