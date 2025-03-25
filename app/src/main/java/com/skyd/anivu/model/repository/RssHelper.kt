package com.skyd.anivu.model.repository

import android.util.Log
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
import com.skyd.anivu.ext.toEncodedUrl
import com.skyd.anivu.model.bean.article.ArticleBean
import com.skyd.anivu.model.bean.article.ArticleCategoryBean
import com.skyd.anivu.model.bean.article.ArticleWithEnclosureBean
import com.skyd.anivu.model.bean.article.EnclosureBean
import com.skyd.anivu.model.bean.article.RssMediaBean
import com.skyd.anivu.model.bean.feed.FeedBean
import com.skyd.anivu.model.bean.feed.FeedWithArticleBean
import com.skyd.anivu.util.favicon.FaviconExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.executeAsync
import java.io.InputStream
import java.util.Date
import java.util.UUID
import javax.inject.Inject

/**
 * Some operations on RSS.
 */
class RssHelper @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val faviconExtractor: FaviconExtractor,
) {

    @Throws(Exception::class)
    suspend fun searchFeed(url: String): FeedWithArticleBean = withContext(Dispatchers.IO) {
        val iconAsync = async { getRssIcon(url) }
        val syndFeed = SyndFeedInput().build(XmlReader(inputStream(okHttpClient, url)))
        val feed = FeedBean(
            url = url,
            title = syndFeed.title,
            description = syndFeed.description,
            link = syndFeed.link,
            icon = getMediaRssIcon(syndFeed)
                ?: syndFeed.icon?.link
                ?: iconAsync.await(),
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
            val currentOkHttpClient = okHttpClient.newBuilder().addNetworkInterceptor(
                Interceptor { chain ->
                    val authorized = chain.request().newBuilder().apply {
                        feed.requestHeaders?.headers?.forEach { (t, u) -> addHeader(t, u) }
                    }.build()
                    chain.proceed(authorized)
                }
            ).build()
            inputStream(currentOkHttpClient, feed.url).use { inputStream ->
                SyndFeedInput().apply { isPreserveWireFeed = true }
                    .build(XmlReader(inputStream))
                    .let { syndFeed ->
                        FeedWithArticleBean(
                            feed = feed.copy(
                                title = syndFeed.title,
                                description = syndFeed.description,
                                link = syndFeed.link,
                                icon = getMediaRssIcon(syndFeed)
                                    ?: syndFeed.icon?.link
                                    ?: iconAsync.await(),
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
            Log.e("RLog", "queryRssXml[${feed.title}]: ${e.message}")
            throw e
        }.getOrNull()
    }

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
        val articleId = UUID.randomUUID().toString()
        val rssMedia = getRssMedia(articleId = articleId, modules = syndEntry.modules)
        val enclosures = syndEntry.enclosures.map {
            EnclosureBean(
                articleId = articleId,
                url = it.url.orEmpty().toEncodedUrl(),
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
                                url = url.toEncodedUrl(),
                                length = content.fileSize ?: 0L,
                                type = content.type,
                            )
                        }
                    )
                    addAll(
                        group.metadata.peerLinks.orEmpty().map { peerLink ->
                            EnclosureBean(
                                articleId = articleId,
                                url = peerLink.href.toString().toEncodedUrl(),
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
        faviconExtractor.extractFavicon(url).apply { Log.e("TAG", "getRssIcon: $this") }
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
        client: OkHttpClient,
        url: String,
    ): InputStream = response(client, url).body.byteStream()

    private suspend fun response(
        client: OkHttpClient,
        url: String,
    ) = client.newCall(Request.Builder().url(url).build()).executeAsync()
}
