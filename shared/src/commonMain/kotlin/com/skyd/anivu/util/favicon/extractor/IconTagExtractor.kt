package com.skyd.anivu.util.favicon.extractor

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.http.charset
import io.ktor.http.contentType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readBuffer
import io.ktor.utils.io.readByteArray
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Factory

@Factory(binds = [])
open class IconTagExtractor(private val httpClient: HttpClient) : Extractor {
    override fun extract(url: String): List<Extractor.IconData> = runBlocking {
        try {
            val html = httpClient.prepareGet(url).execute { httpResponse ->
                val channel: ByteReadChannel = httpResponse.body()
                val bytes = channel.readByteArray(
                    (128 * 1024L).coerceAtMost(channel.readBuffer().size).toInt()
                )
                String(bytes, httpResponse.contentType()?.charset() ?: Charsets.UTF_8)
            }
            extractIconFromHtml(html).map {
                it.copy(
                    url = when {
                        it.url.startsWith("//") -> "http:" + it.url
                        it.url.startsWith("/") -> url + it.url
                        else -> it.url
                    },
                )
            }.map {
                async {
                    runCatching {
                        it.takeIf { httpClient.get(it.url).headers.isImage() }
                    }.getOrNull()
                }
            }.awaitAll().filterNotNull()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

fun extractIconFromHtml(html: String): List<Extractor.IconData> {
    return Regex("(?i)<link[^>]+rel=[\"'](?:shortcut\\s+icon|icon|apple-touch-icon)[\"'][^>]*>")
        .findAll(html)
        .mapNotNull { it.groups[0]?.value }
        .distinct()
        .mapNotNull { linkTag ->
            val faviconUrl = Regex("href\\s*=\\s*\"([^\"]*)\"")
                .find(linkTag)
                ?.groupValues
                ?.getOrNull(1)
                ?: return@mapNotNull null

            val (width, height) = Regex("sizes\\s*=\\s*\"([^\"]*)\"")
                .find(linkTag)
                ?.groupValues
                ?.getOrNull(1)
                ?.split("x")
                ?.map { it.toIntOrNull() ?: 0 }
                ?.run { if (size < 2) listOf(0, 0) else this }
                .run { this ?: listOf(0, 0) }
                .take(2)

            val isSvg = Regex("type\\s*=\\s*\"([^\"]*)\"")
                .find(linkTag)
                ?.groupValues
                ?.firstOrNull()
                ?.contains("svg", ignoreCase = true) == true

            Extractor.IconData(
                url = faviconUrl,
                size = if (isSvg) Extractor.IconSize.MAX_SIZE
                else Extractor.IconSize(width = width, height = height)
            )
        }
        .toList()
}