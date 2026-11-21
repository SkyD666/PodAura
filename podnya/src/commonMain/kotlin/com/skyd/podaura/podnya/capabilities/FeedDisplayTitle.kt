package com.skyd.podaura.podnya.capabilities

import com.fleeksoft.ksoup.Ksoup
import com.skyd.podaura.api.model.bean.feed.FeedRecord

internal fun FeedRecord.displayTitle() =
    Ksoup.parse((nickname?.takeIf(String::isNotBlank) ?: title).orEmpty().take(16_384))
        .text()
        .take(200)
