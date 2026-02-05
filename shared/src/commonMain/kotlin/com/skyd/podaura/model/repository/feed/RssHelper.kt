package com.skyd.podaura.model.repository.feed

import co.touchlab.kermit.Logger
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.feed.FeedWithArticleBean
import com.skyd.podaura.model.repository.feed.convert.feedToFeedWithArticleBean
import com.skyd.podaura.model.repository.feed.convert.feedUpdateFeedWithArticleBean
import com.skyd.podaura.model.repository.feed.convert.getRssIcon
import com.skyd.podaura.model.repository.feed.convert.rssToFeedWithArticleBean
import com.skyd.podaura.model.repository.feed.convert.rssUpdateFeedWithArticleBean
import com.skyd.podaura.model.repository.feed.rssparser.BaseXml
import com.skyd.podaura.model.repository.feed.rssparser.atom.Feed
import com.skyd.podaura.model.repository.feed.rssparser.rss.Rss
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.xml.xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import nl.adaptivity.xmlutil.serialization.XML

/**
 * Some operations on RSS.
 */
class RssHelper(
    private val httpClientConfig: HttpClientConfig<*>.() -> Unit,
) {
    private val log = Logger.withTag("RssHelper")

    private val module = SerializersModule {
        polymorphic(BaseXml::class) {
            subclass(Rss::class, Rss.serializer())
            subclass(Feed::class, Feed.serializer())
        }
    }

    private fun HttpClientConfig<*>.xmlConfig() {
        install(ContentNegotiation) {
            val xml = XML(module) {
                autoPolymorphic = true
                defaultPolicy {
                    pedantic = false
                    ignoreUnknownChildren()
                }
            }
            listOf(
                ContentType.Application.Xml,
                ContentType.Application.Atom,
                ContentType.Application.Rss,
                ContentType.Text.Xml,
                ContentType.Text.Plain,
            ).forEach { contentType ->
                xml(format = xml, contentType = contentType)
            }
        }
    }

    @Throws(Exception::class)
    suspend fun searchFeed(url: String): FeedWithArticleBean = withContext(Dispatchers.IO) {
        val iconAsync = async { getRssIcon(url) }
        val httpClient = HttpClient {
            httpClientConfig()
            xmlConfig()
        }
        when (val rssData: BaseXml? = httpClient.get(url).body()) {
            is Rss -> rssData.rssToFeedWithArticleBean(url, iconAsync.await())
            is Feed -> rssData.feedToFeedWithArticleBean(url, iconAsync.await())
            else -> error("Not supported XML type")
        }
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
                xmlConfig()
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
            when (val rssData: BaseXml? = currentHttpClient.get(feed.url).body()) {
                is Rss -> rssData.rssUpdateFeedWithArticleBean(
                    url = feed.url,
                    feed = feed,
                    icon = iconAsync.await(),
                    articleTakeWhile = { full || latestLink == null || latestLink != it },
                )

                is Feed -> rssData.feedUpdateFeedWithArticleBean(
                    url = feed.url,
                    feed = feed,
                    icon = iconAsync.await(),
                    articleTakeWhile = { full || latestLink == null || latestLink != it },
                )

                else -> error("Not supported XML type")
            }
        }.onFailure { e ->
            log.e("queryRssXml[${feed.title}]", e)
            throw e
        }.getOrNull()
    }
}
