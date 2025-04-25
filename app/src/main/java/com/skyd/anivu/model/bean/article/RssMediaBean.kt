package com.skyd.anivu.model.bean.article

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.skyd.anivu.model.bean.BaseBean
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

const val RSS_MEDIA_TABLE_NAME = "RssMedia"

@Parcelize
@Serializable
@Entity(
    tableName = RSS_MEDIA_TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = ArticleBean::class,
            parentColumns = [ArticleBean.ARTICLE_ID_COLUMN],
            childColumns = [RssMediaBean.ARTICLE_ID_COLUMN],
            onDelete = ForeignKey.CASCADE
        )
    ],
)
data class RssMediaBean(
    @PrimaryKey
    @ColumnInfo(name = ARTICLE_ID_COLUMN)
    val articleId: String,
    @ColumnInfo(name = DURATION_COLUMN)
    val duration: Long? = null,
    @ColumnInfo(name = ADULT_COLUMN)
    var adult: Boolean = false,
    @ColumnInfo(name = IMAGE_COLUMN)
    val image: String? = null,
    @ColumnInfo(name = EPISODE_COLUMN)
    var episode: String? = null,
) : BaseBean, Parcelable {
    companion object {
        const val ARTICLE_ID_COLUMN = "articleId"
        const val DURATION_COLUMN = "duration"
        const val ADULT_COLUMN = "adult"
        const val IMAGE_COLUMN = "image"
        const val EPISODE_COLUMN = "episode"
    }
}