package com.skyd.anivu.util.favicon.extractor

import com.skyd.anivu.model.service.HttpService
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import javax.inject.Inject

class HardCodedExtractor @Inject constructor(
    private val retrofit: Retrofit,
) : Extractor {
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
                        retrofit.create(HttpService::class.java).head(faviconUrl).run {
                            if (isSuccessful && isImage()) {
                                Extractor.IconData(
                                    url = faviconUrl,
                                    size = if (isSvg() ||
                                        faviconUrl.endsWith(".svg", ignoreCase = true)
                                    ) {
                                        Extractor.IconSize.MAX_SIZE
                                    } else {
                                        Extractor.IconSize.EMPTY
                                    },
                                )
                            } else null
                        }
                    } catch (e: Exception) {
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