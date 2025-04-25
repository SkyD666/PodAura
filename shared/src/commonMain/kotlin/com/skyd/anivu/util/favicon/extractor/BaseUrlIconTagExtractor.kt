package com.skyd.anivu.util.favicon.extractor

import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Factory

@Factory(binds = [])
class BaseUrlIconTagExtractor(httpClient: HttpClient) : IconTagExtractor(httpClient) {
    override fun extract(url: String): List<Extractor.IconData> = runBlocking {
        baseUrl(url)
            ?.let { base -> super.extract(base) }
            .orEmpty()
    }
}