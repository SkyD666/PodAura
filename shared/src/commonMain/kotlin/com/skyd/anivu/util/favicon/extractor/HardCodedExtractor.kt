package com.skyd.anivu.util.favicon.extractor

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Factory

@Factory(binds = [])
class HardCodedExtractor(private val httpClient: HttpClient) : Extractor {
    private val hardCodedFavicons = arrayOf(
        "/favicon.ico",
        "/apple-touch-icon.png",
        "/apple-touch-icon-precomposed.png",
    )

    override fun extract(url: String): List<Extractor.IconData> = runBlocking {
        try {
            val baseUrl = baseUrl(url) ?: return@runBlocking emptyList()
            val request = mutableListOf<Deferred<Extractor.IconData?>>()
            hardCodedFavicons.forEach {
                val faviconUrl = baseUrl + it
                request += async {
                    try {
                        val headers = httpClient.get(faviconUrl).headers
                        if (headers.isImage()) {
                            Extractor.IconData(
                                url = faviconUrl,
                                size = if (headers.isSvg() ||
                                    faviconUrl.endsWith(".svg", ignoreCase = true)
                                ) {
                                    Extractor.IconSize.MAX_SIZE
                                } else {
                                    Extractor.IconSize.EMPTY
                                },
                            )
                        } else null
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            request.mapNotNull { it.await() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}