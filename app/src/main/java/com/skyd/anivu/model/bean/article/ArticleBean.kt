package com.skyd.anivu.model.bean.article

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.skyd.anivu.base.BaseBean
import com.skyd.anivu.model.bean.feed.FeedBean
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

const val ARTICLE_TABLE_NAME = "Article"

@Parcelize
@Serializable
@Entity(
    tableName = ARTICLE_TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = FeedBean::class,
            parentColumns = [FeedBean.URL_COLUMN],
            childColumns = [ArticleBean.FEED_URL_COLUMN],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(ArticleBean.ARTICLE_ID_COLUMN),
        Index(ArticleBean.FEED_URL_COLUMN),
    ]
)
data class ArticleBean(
    @PrimaryKey
    @ColumnInfo(name = ARTICLE_ID_COLUMN)
    val articleId: String,
    @ColumnInfo(name = FEED_URL_COLUMN)
    val feedUrl: String,
    @ColumnInfo(name = TITLE_COLUMN)
    val title: String? = null,
    @ColumnInfo(name = DATE_COLUMN)
    val date: Long? = null,
    @ColumnInfo(name = AUTHOR_COLUMN)
    var author: String? = null,
    @ColumnInfo(name = DESCRIPTION_COLUMN)
    val description: String? = null,
    @ColumnInfo(name = CONTENT_COLUMN)
    var content: String? = null,
    @ColumnInfo(name = IMAGE_COLUMN)
    val image: String? = null,
    @ColumnInfo(name = LINK_COLUMN)
    var link: String? = null,
    @ColumnInfo(name = GUID_COLUMN)
    var guid: String? = null,
    @ColumnInfo(name = UPDATE_AT_COLUMN)
    var updateAt: Long? = null,
    @ColumnInfo(name = IS_READ_COLUMN)
    var isRead: Boolean = false,
    @ColumnInfo(name = IS_FAVORITE_COLUMN)
    var isFavorite: Boolean = false,
) : BaseBean, Parcelable {
    companion object {
        const val ARTICLE_ID_COLUMN = "articleId"
        const val FEED_URL_COLUMN = "feedUrl"
        const val TITLE_COLUMN = "title"
        const val DATE_COLUMN = "date"
        const val AUTHOR_COLUMN = "author"
        const val DESCRIPTION_COLUMN = "description"
        const val CONTENT_COLUMN = "content"
        const val IMAGE_COLUMN = "image"
        const val LINK_COLUMN = "link"
        const val GUID_COLUMN = "guid"
        const val UPDATE_AT_COLUMN = "updateAt"
        const val IS_READ_COLUMN = "isRead"
        const val IS_FAVORITE_COLUMN = "isFavorite"
    }
}