package com.skyd.podaura.ui.screen.download

import com.skyd.podaura.ui.component.navigation.deeplink.DeepLinkPattern
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol

actual val DownloadDeepLinkRoute.Companion.deepLinkPatterns: List<DeepLinkPattern<DownloadDeepLinkRoute>>
    get() = listOf("http", "https").map {
        DeepLinkPattern(
            DownloadDeepLinkRoute.serializer(),
            urlPattern = URLBuilder(
                protocol = URLProtocol(name = it, defaultPort = -1)
            ).build(),
            urlOnlyProtocol = true,
        )
    }
