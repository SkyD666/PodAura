package com.skyd.podaura.model.repository.download

import com.skyd.downloader.DownloadBatchItem
import com.skyd.downloader.DownloadBatchReceipt
import com.skyd.downloader.Downloader
import com.skyd.podaura.api.model.bean.article.ArticleQuery
import com.skyd.podaura.api.model.bean.article.ArticleSearchScope
import com.skyd.podaura.api.model.bean.download.AttachmentMediaKind
import com.skyd.podaura.api.model.bean.download.AssistantDownloadArticle
import com.skyd.podaura.api.model.bean.download.AssistantDownloadAttachment
import com.skyd.podaura.api.model.bean.download.AssistantDownloadReceipt
import com.skyd.podaura.api.model.bean.download.AssistantDownloadScope
import com.skyd.podaura.api.model.bean.download.AssistantDownloadTarget
import com.skyd.podaura.api.model.repository.IAssistantDownloadRepository
import com.skyd.podaura.media.MediaTypes
import com.skyd.podaura.model.bean.article.ArticleWithEnclosureBean
import com.skyd.podaura.model.bean.article.EnclosureBean
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.download.ArticleDownloadSource
import com.skyd.podaura.model.download.encode
import com.skyd.podaura.model.repository.article.buildArticleRepositoryQuery
import com.skyd.podaura.util.media.LocalMediaKind
import com.skyd.podaura.util.media.detectLocalMediaKind
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareHead
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.fromFilePath
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException

class AssistantDownloadRepository(
    private val articleDao: ArticleDao,
    private val downloader: Downloader,
    private val directLinkClient: HttpClient,
) : IAssistantDownloadRepository {
    override suspend fun articlesByIds(articleIds: List<String>): List<AssistantDownloadArticle> =
        articleIds.distinct().chunked(500).flatMap { ids ->
            val articles = articleDao.getArticleWithEnclosureListByIds(ids).associateBy {
                it.article.articleId
            }
            ids.mapNotNull { id -> articles[id]?.toDownloadArticle() }
        }

    override suspend fun articlesInFeed(
        scope: AssistantDownloadScope,
    ): List<AssistantDownloadArticle> {
        val query = buildArticleRepositoryQuery(
            scope = ArticleSearchScope.Restricted(feedUrls = setOf(scope.feedUrl)),
            query = ArticleQuery(
                publishedFromMillis = scope.publishedFromMillis,
                publishedBeforeMillis = scope.publishedBeforeMillis,
                limit = scope.limit ?: Int.MAX_VALUE,
            ),
            searchColumns = emptyList(),
        )
        return articleDao.getArticleList(query).map { it.articleWithEnclosure.toDownloadArticle() }
    }

    override suspend fun checkDirectLink(url: String): Boolean =
        directLinkClient.verifyAssistantDownloadLink(url)

    override suspend fun submit(
        batchId: String,
        submissionId: String,
        targets: List<AssistantDownloadTarget>,
    ): AssistantDownloadReceipt {
        receipt(batchId = batchId, submissionId = submissionId)?.let { return it }
        val articles = targets.filterIsInstance<AssistantDownloadTarget.Attachment>()
            .map { it.articleId }
            .distinct()
            .chunked(500)
            .flatMap { articleDao.getArticleWithFeedListByIds(it) }
            .associateBy { it.articleWithEnclosure.article.articleId }
        val feedDirectories = mutableMapOf<String, String>()
        val failed = mutableSetOf<String>()
        val items = targets.distinctBy { it.key }.mapNotNull { target ->
            try {
                requireNotNull(target.url.downloadHttpUrl())
                when (target) {
                    is AssistantDownloadTarget.Attachment -> {
                        val row = requireNotNull(articles[target.articleId])
                        require(row.articleWithEnclosure.enclosures.any { it.url == target.url })
                        val feed = row.feed
                        val directory =
                            feedDirectories.getOrPut(feed.url) { downloadDirectory(feed) }
                        DownloadBatchItem(
                            key = target.key,
                            url = target.url,
                            path = directory,
                            metadata = ArticleDownloadSource(
                                articleId = target.articleId,
                                feedUrl = feed.url,
                            ).encode(),
                        )
                    }

                    is AssistantDownloadTarget.DirectLink -> {
                        val sourceUrl = target.url.substringBefore('#')
                        DownloadBatchItem(
                            key = target.key,
                            url = sourceUrl,
                            path = downloadDirectory(url = sourceUrl, articleDownloadSource = null),
                            requireFileResponse = true,
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                failed += target.key
                null
            }
        }
        return downloader.enqueueBatch(
            batchId = batchId,
            submissionId = submissionId,
            items = items,
            failedKeys = failed,
        ).toAssistantReceipt()
    }

    override suspend fun receipt(batchId: String, submissionId: String): AssistantDownloadReceipt? =
        downloader.getBatchReceipt(batchId = batchId, submissionId = submissionId)
            ?.toAssistantReceipt()
}

private fun ArticleWithEnclosureBean.toDownloadArticle() = AssistantDownloadArticle(
    articleId = article.articleId,
    title = article.title?.takeUnless { it.isBlank() } ?: article.articleId,
    feedUrl = article.feedUrl,
    attachments = enclosures.map { enclosure ->
        AssistantDownloadAttachment(
            url = enclosure.url,
            type = enclosure.type,
            length = enclosure.length,
            mediaKind = detectLocalMediaKind(enclosure),
        )
    },
)

internal fun detectLocalMediaKind(enclosure: EnclosureBean): AttachmentMediaKind {
    val name = runCatching { Url(enclosure.url).rawSegments.lastOrNull() }.getOrNull().orEmpty()
    val mime = enclosure.type.orEmpty().substringBefore(';').trim().lowercase()
    val kind = if (mime in MediaTypes.genericMimeTypes) {
        val local = detectLocalMediaKind(fileName = name, mimeType = null)
        if (local == LocalMediaKind.Other) {
            val inferredMime = ContentType.fromFilePath(name).firstOrNull()?.toString()
            detectLocalMediaKind(fileName = "", mimeType = inferredMime)
        } else local
    } else {
        val declared = detectLocalMediaKind(fileName = "", mimeType = mime)
        if (declared.isPlayable) detectLocalMediaKind(fileName = name, mimeType = mime) else declared
    }
    return when (kind) {
        LocalMediaKind.Audio -> AttachmentMediaKind.Audio
        LocalMediaKind.Video -> AttachmentMediaKind.Video
        LocalMediaKind.Playlist -> AttachmentMediaKind.Playlist
        LocalMediaKind.Other -> AttachmentMediaKind.Other
    }
}

private fun DownloadBatchReceipt.toAssistantReceipt() = AssistantDownloadReceipt(
    batchId = batchId,
    submissionId = submissionId,
    queued = queued,
    existing = existing,
    failed = failed,
    matchedCount = matchedCount,
    remainingCount = remainingCount,
    limitExceeded = limitExceeded,
)

private val WEB_CONTENT_TYPES = setOf("text/html", "application/xhtml+xml")
private val WEB_EXTENSIONS = setOf(
    "html", "htm", "xhtml", "shtml", "php", "php3", "php4", "phtml",
    "asp", "aspx", "ashx", "jsp", "jspx", "cgi", "do", "action",
)
private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
private val HEAD_UNAVAILABLE_CODES = setOf(400, 401, 403, 404, 405, 501)
private const val MAX_DIRECT_LINK_REDIRECTS = 10

internal suspend fun HttpClient.verifyAssistantDownloadLink(url: String): Boolean {
    var current = url.downloadHttpUrl() ?: return false
    repeat(MAX_DIRECT_LINK_REDIRECTS + 1) {
        val probe = prepareHead(current.toString()).execute { response ->
            if (response.status.value !in REDIRECT_CODES) {
                DirectLinkProbe.Complete(response.allowsFileTransfer())
            } else {
                val next = response.headers[HttpHeaders.Location]?.let { location ->
                    runCatching {
                        URLBuilder(response.request.url).apply {
                            parameters.clear()
                            trailingQuery = false
                            fragment = ""
                            takeFrom(location)
                        }.buildString()
                    }.getOrNull()?.downloadHttpUrl()
                }
                DirectLinkProbe.Redirect(next)
            }
        }
        when (probe) {
            is DirectLinkProbe.Complete -> return probe.eligible
            is DirectLinkProbe.Redirect -> {
                val next = probe.url ?: return false
                if (current.protocol == URLProtocol.HTTPS && next.protocol != URLProtocol.HTTPS) {
                    return false
                }
                current = next
            }
        }
    }
    return false
}

private sealed interface DirectLinkProbe {
    data class Complete(val eligible: Boolean) : DirectLinkProbe
    data class Redirect(val url: Url?) : DirectLinkProbe
}

private fun String.downloadHttpUrl(): Url? {
    if (!startsWith("http://", ignoreCase = true) && !startsWith("https://", ignoreCase = true)) {
        return null
    }
    if (any { it.isWhitespace() || it.isISOControl() }) return null
    return runCatching { Url(this) }.getOrNull()?.takeIf {
        it.host.isNotBlank() && it.user == null && it.password == null &&
                it.protocol in setOf(URLProtocol.HTTP, URLProtocol.HTTPS)
    }
}

private fun HttpResponse.allowsFileTransfer(): Boolean {
    // Some GET-only routes are absent for HEAD. The queued transfer validates its GET before saving.
    if (status.value in HEAD_UNAVAILABLE_CODES) {
        return true
    }
    if (!status.isSuccess()) {
        throw IOException("Unable to verify file link (HTTP ${status.value})")
    }
    if (status.value == 204 || status.value == 205) {
        return false
    }
    val disposition = headers[HttpHeaders.ContentDisposition]?.let {
        runCatching { ContentDisposition.parse(it) }.getOrNull()
    }
    if (disposition?.disposition.equals("attachment", ignoreCase = true)) {
        return true
    }
    val type = headers[HttpHeaders.ContentType]?.substringBefore(';')?.trim()?.lowercase()
    if (type in WEB_CONTENT_TYPES) {
        return false
    }
    return (type != null && '/' in type && type != "*/*") || request.url.hasFileName()
}

private fun Url.hasFileName(): Boolean {
    val name = rawSegments.lastOrNull()?.takeIf { it.isNotEmpty() } ?: return false
    val extension = name.substringAfterLast('.', "").lowercase()
    if (extension in WEB_EXTENSIONS) {
        return false
    }
    return extension in MediaTypes.playableExtensions ||
            extension in MediaTypes.playlistExtensions ||
            ContentType.fromFilePath(name).isNotEmpty()
}
