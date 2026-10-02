package com.skyd.podaura.util.favicon.extractor

import io.ktor.client.HttpClientConfig

class BaseUrlIconTagExtractor(
    httpClientConfig: HttpClientConfig<*>.() -> Unit
) : IconTagExtractor(httpClientConfig) {
    override suspend fun extract(url: String): List<Extractor.IconData> =
        baseUrl(url)
            ?.let { base -> super.extract(base) }
            .orEmpty()
}
