package com.skyd.anivu.util.favicon.extractor

import com.skyd.anivu.model.service.HttpService
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okio.BufferedSource
import okio.ByteString.Companion.decodeHex
import okio.Options
import retrofit2.Retrofit
import java.io.IOException
import java.nio.charset.Charset
import javax.inject.Inject

open class IconTagExtractor @Inject constructor(
    private val retrofit: Retrofit,
) : Extractor {
    override fun extract(url: String): List<Extractor.IconData> = runBlocking {
        try {
            val html = retrofit.create(HttpService::class.java).body(url).run {
                source().use { source ->
                    source.readString(
                        byteCount = (128 * 1024L).coerceAtMost(source.buffer.size),
                        charset = source.readBomAsCharset(
                            contentType()?.charset() ?: Charsets.UTF_8
                        )
                    )
                }
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
                        retrofit.create(HttpService::class.java).head(it.url).run {
                            if (isSuccessful && isImage()) it else null
                        }
                    }.getOrNull()
                }
            }.mapNotNull {
                it.await()
            }
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


internal val UNICODE_BOMS = Options.of(
    "efbbbf".decodeHex(), // UTF-8
    "feff".decodeHex(), // UTF-16BE
    "fffe".decodeHex(), // UTF-16LE
    "0000ffff".decodeHex(), // UTF-32BE
    "ffff0000".decodeHex() // UTF-32LE
)

@Throws(IOException::class)
internal fun BufferedSource.readBomAsCharset(default: Charset): Charset {
    return when (select(UNICODE_BOMS)) {
        0 -> Charsets.UTF_8
        1 -> Charsets.UTF_16BE
        2 -> Charsets.UTF_16LE
        3 -> Charsets.UTF_32BE
        4 -> Charsets.UTF_32LE
        -1 -> default
        else -> throw AssertionError()
    }
}