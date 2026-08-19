package com.skyd.podaura.model.bean

import com.skyd.podaura.model.bean.article.ArticleWithEnclosureBean
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.ext.asPlatformFile
import com.skyd.podaura.util.fileicon.fileIcon
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.isRegularFile
import io.github.vinceglb.filekit.lastModified
import io.github.vinceglb.filekit.mimeType
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.size
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.DrawableResource

@Serializable
data class MediaBean(
    val displayName: String? = null,
    val filePath: String,
    val parentPath: String = "",
    val fileCount: Int,
    val articleWithEnclosure: ArticleWithEnclosureBean?,
    val feedBean: FeedBean?,
) : BaseBean {
    val path: PlatformFile get() = filePath.asPlatformFile()
    val name: String get() = path.name
    val mimetype: String by lazy { runCatching { path.mimeType()?.toString() }.getOrNull() ?: "*/*" }
    val size: Long get() = runCatching { path.size() }.getOrDefault(0)
    val date: Long get() = runCatching { path.lastModified().toEpochMilliseconds() }.getOrDefault(0)
    val isMedia: Boolean get() = mimetype.startsWith("video/") || mimetype.startsWith("audio/")
    val isDir: Boolean get() = runCatching { path.isDirectory() }.getOrDefault(false)
    val isFile: Boolean get() = runCatching { path.isRegularFile() }.getOrDefault(false)
    val icon: DrawableResource by lazy { path.fileIcon().resource }
    val articleId get() = articleWithEnclosure?.article?.articleId
    val feedUrl get() = feedBean?.url
    val cover: String
        get() = articleWithEnclosure?.media?.image ?: feedBean?.icon ?: filePath
}
