package com.skyd.anivu.model.bean

import com.skyd.anivu.base.BaseBean
import com.skyd.anivu.ext.getMimeType
import com.skyd.anivu.model.bean.article.ArticleWithEnclosureBean
import com.skyd.anivu.model.bean.feed.FeedBean
import com.skyd.anivu.util.fileicon.getFileIcon
import java.io.File

data class MediaBean(
    val displayName: String? = null,
    val file: File,
    val fileCount: Int,
    val articleWithEnclosure: ArticleWithEnclosureBean?,
    val feedBean: FeedBean?,
) : BaseBean {
    var name: String = file.name
    val mimetype: String by lazy { file.getMimeType() ?: "*/*" }
    var size: Long = file.length()
    var date: Long = file.lastModified()
    val isMedia: Boolean = mimetype.startsWith("video/") || mimetype.startsWith("audio/")
    val isDir: Boolean = file.isDirectory
    val isFile: Boolean = file.isFile
    val icon: Int by lazy { getFileIcon(mimetype).resourceId }
    val articleId = articleWithEnclosure?.article?.articleId
    val feedUrl = feedBean?.url
}