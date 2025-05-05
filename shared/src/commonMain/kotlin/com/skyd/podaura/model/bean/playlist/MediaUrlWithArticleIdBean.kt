package com.skyd.podaura.model.bean.playlist

import com.skyd.podaura.model.bean.MediaBean
import com.skyd.podaura.model.bean.article.EnclosureBean

data class MediaUrlWithArticleIdBean(
    val url: String,
    val articleId: String?,
) {
    companion object {
        fun PlaylistMediaWithArticleBean.toMediaUrlWithArticleIdBean() = MediaUrlWithArticleIdBean(
            url = playlistMediaBean.url,
            articleId = playlistMediaBean.articleId,
        )

        fun EnclosureBean.toMediaUrlWithArticleIdBean() = MediaUrlWithArticleIdBean(
            url = url,
            articleId = articleId,
        )

        fun MediaBean.toMediaUrlWithArticleIdBean() = MediaUrlWithArticleIdBean(
            url = filePath,
            articleId = articleId,
        )
    }
}
