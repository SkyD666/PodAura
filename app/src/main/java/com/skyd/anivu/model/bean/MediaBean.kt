package com.skyd.anivu.model.bean

import com.skyd.anivu.base.BaseBean
import com.skyd.anivu.ext.getMimeType
import com.skyd.anivu.model.bean.article.ArticleWithEnclosureBean
import com.skyd.anivu.model.bean.feed.FeedBean
import com.skyd.anivu.model.serializer.FileSerializer
import com.skyd.anivu.util.fileicon.getFileIcon
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class MediaBean(
    val displayName: String? = null,
    @Serializable(with = FileSerializer::class)
    val file: File,
    val fileCount: Int,
    val articleWithEnclosure: ArticleWithEnclosureBean?,
    val feedBean: FeedBean?,
) : BaseBean {
    val name: String get() = file.name
    val mimetype: String by lazy { file.getMimeType() ?: "*/*" }
    val size: Long get() = file.length()
    val date: Long get() = file.lastModified()
    val isMedia: Boolean get() = mimetype.startsWith("video/") || mimetype.startsWith("audio/")
    val isDir: Boolean get() = file.isDirectory
    val isFile: Boolean get() = file.isFile
    val icon: Int by lazy { getFileIcon(mimetype).resourceId }
    val articleId get() = articleWithEnclosure?.article?.articleId
    val feedUrl get() = feedBean?.url
    val cover: String? get() = articleWithEnclosure?.media?.image ?: feedBean?.icon ?: file.path
}