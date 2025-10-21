package com.skyd.podaura.model.bean.playlist

import androidx.room.Embedded
import androidx.room.Relation
import com.skyd.fundation.ext.currentTimeMillis
import com.skyd.podaura.ext.isLocalFile
import com.skyd.podaura.model.bean.BaseBean
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.util.coil.localmedia.LocalMedia
import kotlinx.io.files.Path
import kotlinx.serialization.Serializable
import kotlin.time.Clock

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
) : BaseBean {
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
            ?: if (playlistMediaBean.url.isLocalFile()) LocalMedia(Path(playlistMediaBean.url)) else null

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
                    createTime = Clock.currentTimeMillis(),
                ).apply { updateLocalMediaMetadata() },
                article = null,
            )
        }
    }
}