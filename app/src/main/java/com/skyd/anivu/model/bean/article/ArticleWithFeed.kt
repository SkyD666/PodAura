package com.skyd.anivu.model.bean.article

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import com.skyd.anivu.model.bean.feed.FeedBean
import kotlinx.parcelize.Parcelize
import java.io.Serializable

/**
 * An [articleWithEnclosure] contains a [feed].
 */
@Parcelize
@kotlinx.serialization.Serializable
data class ArticleWithFeed(
    @Embedded
    var articleWithEnclosure: ArticleWithEnclosureBean,
    @Relation(parentColumn = ArticleBean.FEED_URL_COLUMN, entityColumn = FeedBean.URL_COLUMN)
    var feed: FeedBean,
) : Serializable, Parcelable {
    fun getThumbnail(): String? {
        return articleWithEnclosure.media?.image ?: feed.customIcon ?: feed.icon
    }

    fun getArtist(): String? {
        return articleWithEnclosure.article.author.orEmpty().ifEmpty { feed.title }
    }
}