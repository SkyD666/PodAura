package com.skyd.podaura.util.favicon.extractor

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.request.get
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class HardCodedExtractor(
    private val httpClientConfig: HttpClientConfig<*>.() -> Unit,
) : Extractor {
    private val hardCodedFavicons = arrayOf(
        "/favicon.ico",
        "/apple-touch-icon.png",
        "/apple-touch-icon-precomposed.png",
    )

    override suspend fun extract(url: String): List<Extractor.IconData> = coroutineScope {
        val baseUrl = baseUrl(url) ?: return@coroutineScope emptyList()
        val httpClient = HttpClient(httpClientConfig)
        try {
            hardCodedFavicons.map {
                val faviconUrl = baseUrl + it
                async {
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
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        } finally {
            httpClient.close()
        }
    }
}
