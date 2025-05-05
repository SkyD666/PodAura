package com.skyd.podaura.model.repository

import co.touchlab.kermit.Logger
import com.rometools.modules.itunes.EntryInformation
import com.rometools.modules.itunes.FeedInformation
import com.rometools.modules.mediarss.MediaEntryModule
import com.rometools.modules.mediarss.MediaModule
import com.rometools.modules.mediarss.types.Rating
import com.rometools.modules.mediarss.types.UrlReference
import com.rometools.rome.feed.module.Module
import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import com.skyd.podaura.ext.encodeURL
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.article.ArticleCategoryBean
import com.skyd.podaura.model.bean.article.ArticleWithEnclosureBean
import com.skyd.podaura.model.bean.article.EnclosureBean
import com.skyd.podaura.model.bean.article.RssMediaBean
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.feed.FeedWithArticleBean
import com.skyd.podaura.util.favicon.FaviconExtractor
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import java.io.InputStream
import java.util.Date
import kotlin.uuid.Uuid

/**
 * Some operations on RSS.
 */
@Factory(binds = [])
class RssHelper(
    private val httpClientConfig: HttpClientConfig<*>.() -> Unit,
    private val faviconExtractor: FaviconExtractor,
) {
    private val log = Logger.withTag("RssHelper")

    @Throws(Exception::class)
    suspend fun searchFeed(url: String): FeedWithArticleBean = withContext(Dispatchers.IO) {
        val iconAsync = async { getRssIcon(url) }
        val httpClient = HttpClient(httpClientConfig)
        val syndFeed = SyndFeedInput().build(XmlReader(inputStream(httpClient, url)))
        val feed = FeedBean(
            url = url,
            title = syndFeed.title,
            description = syndFeed.description,
            link = syndFeed.link,
            icon = getIcon(syndFeed) ?: iconAsync.await(),
        )
        val list = syndFeed.entries.map { article(feed, it) }
        FeedWithArticleBean(feed, list)
    }

    suspend fun queryRssXml(
        feed: FeedBean,
        full: Boolean,
        latestLink: String?,        // 日期最新的文章链接，更新时不会take比这个文章更旧的文章
    ): FeedWithArticleBean? = withContext(Dispatchers.IO) {
        runCatching {
            val iconAsync = async { getRssIcon(feed.url) }
            val currentHttpClient = HttpClient {
                httpClientConfig()
                install(
                    createClientPlugin("RssPlugin") {
                        onRequest { request, _ ->
                            feed.requestHeaders?.headers?.forEach { (t, u) ->
                                request.headers[t] = u
                            }
                        }
                    }
                )
            }
            inputStream(currentHttpClient, feed.url).use { inputStream ->
                SyndFeedInput().apply { isPreserveWireFeed = true }
                    .build(XmlReader(inputStream))
                    .let { syndFeed ->
                        FeedWithArticleBean(
                            feed = feed.copy(
                                title = syndFeed.title,
                                description = syndFeed.description,
                                link = syndFeed.link,
                                icon = getIcon(syndFeed) ?: iconAsync.await(),
                            ),
                            articles = syndFeed.entries
                                .asSequence()
                                .run {
                                    if (feed.sortXmlArticlesOnUpdate) {
                                        sortedByDescending { it.publishedDate }
                                    } else {
                                        this
                                    }
                                }
                                .takeWhile { full || latestLink == null || latestLink != it.link }
                                .map { article(feed, it) }
                                .toList(),
                        )
                    }
            }
        }.onFailure { e ->
            e.printStackTrace()
            log.e("queryRssXml[${feed.title}]: ${e.message}")
            throw e
        }.getOrNull()
    }

    private fun getIcon(syndFeed: SyndFeed): String? =
        getMediaRssIcon(syndFeed)
            ?: syndFeed.image?.url
            ?: syndFeed.icon?.url

    private fun article(
        feed: FeedBean,
        syndEntry: SyndEntry,
    ): ArticleWithEnclosureBean {
        val desc = syndEntry.description?.value
        val content = syndEntry.contents
            .takeIf { it.isNotEmpty() }
            ?.let { list -> list.joinToString("\n") { it.value } }
//        Log.i(
//            "RLog",
//            "request rss:\n" +
//                    "name: ${feed.title}\n" +
//                    "feedUrl: ${feed.url}\n" +
//                    "url: ${syndEntry.link}\n" +
//                    "title: ${syndEntry.title}\n" +
//                    "desc: ${desc}\n" +
//                    "content: ${content}\n"
//        )
        val articleId = Uuid.random().toString()
        val rssMedia = getRssMedia(articleId = articleId, modules = syndEntry.modules)
        val enclosures = syndEntry.enclosures.map {
            EnclosureBean(
                articleId = articleId,
                url = it.url.orEmpty().encodeURL(),
                length = it.length,
                type = it.type,
            )
        }
        val enclosuresFromMedia = getEnclosuresFromMedia(
            articleId = articleId,
            modules = syndEntry.modules,
        )
        return ArticleWithEnclosureBean(
            article = ArticleBean(
                articleId = articleId,
                feedUrl = feed.url,
                date = (syndEntry.publishedDate ?: syndEntry.updatedDate ?: Date()).time,
                title = syndEntry.title.toString(),
                author = syndEntry.author,
                description = content ?: desc,
                content = content,
                image = findImg((content ?: desc) ?: ""),
                link = syndEntry.link,
                guid = syndEntry.uri,
                updateAt = Date().time,
            ),
            enclosures = enclosures + enclosuresFromMedia,
            categories = syndEntry.categories.map { it.name }.filter { it.isNotBlank() }.map {
                ArticleCategoryBean(articleId = articleId, category = it)
            },
            media = rssMedia,
        )
    }

    private fun getRssMedia(articleId: String, modules: List<Module>): RssMediaBean? {
        modules.forEach { module ->
            val media = when (module) {
                is EntryInformation -> {
                    RssMediaBean(
                        articleId = articleId,
                        duration = module.duration?.milliseconds,
                        adult = module.explicit,
                        image = module.image?.toString(),
                        episode = module.episode?.toString(),
                    )
                }

                is MediaEntryModule -> {
                    val content = module.mediaContents.firstOrNull()
                    RssMediaBean(
                        articleId = articleId,
                        duration = content?.duration,
                        adult = content?.metadata?.ratings?.any { it == Rating.ADULT } == true,
                        image = content?.metadata?.thumbnail?.firstOrNull()?.url?.toString(),
                        episode = null,
                    )
                }

                else -> null
            }
            if (media != null) return media
        }
        return null
    }

    private fun getEnclosuresFromMedia(
        articleId: String,
        modules: List<Module>
    ): List<EnclosureBean> = buildList {
        modules.asSequence().forEach { module ->
            if (module is MediaEntryModule) {
                module.mediaGroups.forEach { group ->
                    addAll(
                        group.contents.orEmpty().mapNotNull { content ->
                            val url = (content.reference as? UrlReference)?.url?.toString()
                                ?: return@mapNotNull null
                            EnclosureBean(
                                articleId = articleId,
                                url = url.encodeURL(),
                                length = content.fileSize ?: 0L,
                                type = content.type,
                            )
                        }
                    )
                    addAll(
                        group.metadata.peerLinks.orEmpty().map { peerLink ->
                            EnclosureBean(
                                articleId = articleId,
                                url = peerLink.href.toString().encodeURL(),
                                length = 0,
                                type = peerLink.type,
                            )
                        }
                    )
                }
            }
        }
    }

    private fun getMediaRssIcon(syndFeed: SyndFeed): String? {
        var icon: String?
        syndFeed.modules.forEach { module ->
            icon = when (module) {
                is FeedInformation -> module.image?.toString()
                is MediaModule -> module.metadata?.thumbnail?.firstOrNull()?.url?.toString()

                else -> null
            }
            if (icon != null) return icon
        }
        return null
    }

    private suspend fun getRssIcon(url: String): String? = runCatching {
        faviconExtractor.extractFavicon(url).apply { log.e("getRssIcon: $this") }
    }.onFailure { it.printStackTrace() }.getOrNull()

    private fun findImg(rawDescription: String): String? {
        // From: https://gitlab.com/spacecowboy/Feeder
        // Using negative lookahead to skip data: urls, being inline base64
        // And capturing original quote to use as ending quote
        val regex = """img.*?src=(["'])((?!data).*?)\1""".toRegex(RegexOption.DOT_MATCHES_ALL)
        // Base64 encoded images can be quite large - and crash database cursors
        return regex.find(rawDescription)?.groupValues?.get(2)?.takeIf { !it.startsWith("data:") }
    }

    private suspend fun inputStream(
        client: HttpClient,
        url: String,
    ): InputStream = client.prepareGet(url).execute().bodyAsChannel().toInputStream()
}
