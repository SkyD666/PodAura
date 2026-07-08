package com.skyd.podaura.model.bean.article

import androidx.room3.Embedded
import androidx.room3.Relation
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
    @Relation(
        parentColumns = [ArticleBean.FEED_URL_COLUMN],
        entityColumns = [FeedBean.URL_COLUMN]
    )
    var feed: FeedBean,
) : BaseBean {
    fun getThumbnail(): String? {
        return articleWithEnclosure.media?.image ?: feed.customIcon ?: feed.icon
    }

    fun getArtist(): String? {
        return articleWithEnclosure.article.author.orEmpty().ifEmpty { feed.title }
    }
}
