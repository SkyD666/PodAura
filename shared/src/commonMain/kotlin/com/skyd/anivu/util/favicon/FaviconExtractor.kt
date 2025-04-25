package com.skyd.anivu.util.favicon

import androidx.compose.ui.util.fastMaxBy
import com.skyd.anivu.di.get
import com.skyd.anivu.util.favicon.extractor.BaseUrlIconTagExtractor
import com.skyd.anivu.util.favicon.extractor.HardCodedExtractor
import com.skyd.anivu.util.favicon.extractor.IconTagExtractor
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.koin.core.annotation.Factory

@Factory(binds = [])
class FaviconExtractor {
    private val extractors = listOf(
        get<HardCodedExtractor>(),
        get<IconTagExtractor>(),
        get<BaseUrlIconTagExtractor>(),
    )

    suspend fun extractFavicon(url: String): String? = coroutineScope {
        extractors
            .map { async { it.extract(url) } }
            .map { it.await() }
            .flatten()
            .fastMaxBy {
                (it.size.height * it.size.width).coerceIn(Int.MIN_VALUE..Int.MAX_VALUE)
            }?.url
    }
}