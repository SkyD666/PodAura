package com.skyd.podaura.util.favicon.extractor

import io.ktor.client.HttpClientConfig
import kotlinx.coroutines.runBlocking

class BaseUrlIconTagExtractor(
    httpClientConfig: HttpClientConfig<*>.() -> Unit
) : IconTagExtractor(httpClientConfig) {
    override fun extract(url: String): List<Extractor.IconData> = runBlocking {
        baseUrl(url)
            ?.let { base -> super.extract(base) }
            .orEmpty()
    }
}