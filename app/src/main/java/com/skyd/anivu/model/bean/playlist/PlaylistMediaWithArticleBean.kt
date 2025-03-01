package com.skyd.anivu.model.bean.playlist

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import com.skyd.anivu.ext.isLocalFile
import com.skyd.anivu.model.bean.article.ArticleBean
import com.skyd.anivu.model.bean.article.ArticleWithFeed
import com.skyd.anivu.util.coil.localmedia.LocalMedia
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.io.File

@Parcelize
@Serializable
data class PlaylistMediaWithArticleBean(
    @Embedded
    val playlistMediaBean: PlaylistMediaBean,
    @Relation(
        entity = ArticleBean::class,
        parentColumn = PlaylistMediaBean.ARTICLE_ID_COLUMN,
        entityColumn = ArticleBean.ARTICLE_ID_COLUMN,
    )
    val article: ArticleWithFeed?,
) : java.io.Serializable, Parcelable {
    val title: String
        get() = article?.articleWithEnclosure?.article?.title
            ?: playlistMediaBean.title
            ?: playlistMediaBean.url.substringAfterLast("/")

    val duration: Long?
        get() = article?.articleWithEnclosure?.media?.duration
            ?: playlistMediaBean.duration

    val artist: String?
        get() = article?.getArtist()
            ?: playlistMediaBean.artist

    val thumbnailAny: Any?
        get() = article?.getThumbnail()
            ?: if (playlistMediaBean.url.isLocalFile()) LocalMedia(File(playlistMediaBean.url)) else null

    val thumbnail: String?
        get() = article?.getThumbnail()

    companion object {
        val PlaylistMediaWithArticleBean.articleId get() = article?.articleWithEnclosure?.article?.articleId

        fun fromUrl(
            playlistId: String,
            url: String,
            orderPosition: Double,
        ): PlaylistMediaWithArticleBean {
            return PlaylistMediaWithArticleBean(
                playlistMediaBean = PlaylistMediaBean(
                    playlistId = playlistId,
                    url = url,
                    articleId = null,
                    orderPosition = orderPosition,
                    createTime = System.currentTimeMillis(),
                ).apply { updateLocalMediaMetadata() },
                article = null,
            )
        }
    }
}