package com.skyd.podaura.model.bean

import com.skyd.fundation.ext.lastModifiedTime
import com.skyd.podaura.model.bean.article.ArticleWithEnclosureBean
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.util.fileicon.fileIcon
import com.skyd.podaura.util.fileicon.mimeType
import kotlinx.io.files.FileMetadata
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.DrawableResource

@Serializable
data class MediaBean(
    val displayName: String? = null,
    val filePath: String,
    val fileCount: Int,
    val articleWithEnclosure: ArticleWithEnclosureBean?,
    val feedBean: FeedBean?,
) : BaseBean {
    val path: Path get() = Path(filePath)
    val fileMetadata: FileMetadata? get() = SystemFileSystem.metadataOrNull(path)
    val name: String get() = path.name
    val mimetype: String by lazy { path.mimeType() ?: "*/*" }
    val size: Long get() = fileMetadata?.size ?: 0
    val date: Long get() = path.lastModifiedTime ?: 0
    val isMedia: Boolean get() = mimetype.startsWith("video/") || mimetype.startsWith("audio/")
    val isDir: Boolean get() = fileMetadata?.isDirectory == true
    val isFile: Boolean get() = fileMetadata?.isRegularFile == true
    val icon: DrawableResource by lazy { path.fileIcon().resource }
    val articleId get() = articleWithEnclosure?.article?.articleId
    val feedUrl get() = feedBean?.url
    val cover: String?
        get() = articleWithEnclosure?.media?.image ?: feedBean?.icon ?: filePath.toString()
}