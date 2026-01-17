package com.skyd.podaura.util.favicon

import androidx.compose.ui.util.fastMaxBy
import com.skyd.fundation.di.get
import com.skyd.podaura.util.favicon.extractor.BaseUrlIconTagExtractor
import com.skyd.podaura.util.favicon.extractor.HardCodedExtractor
import com.skyd.podaura.util.favicon.extractor.IconTagExtractor
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class FaviconExtractor {
    private val extractors = listOf(
        get<HardCodedExtractor>(),
        get<IconTagExtractor>(),
        get<BaseUrlIconTagExtractor>(),
    )

    suspend fun extractFavicon(url: String): String? = coroutineScope {
        extractors
            .map { async { it.extract(url) } }
            .awaitAll()
            .flatten()
            .fastMaxBy {
                (it.size.height * it.size.width).coerceIn(Int.MIN_VALUE..Int.MAX_VALUE)
            }?.url
    }
}
