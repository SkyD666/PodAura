package com.skyd.podaura.model.repository.feed.convert

import com.skyd.fundation.di.get
import com.skyd.podaura.util.favicon.FaviconExtractor
import kotlinx.coroutines.CancellationException

internal suspend fun getRssIcon(url: String): String? = try {
    get<FaviconExtractor>().extractFavicon(url)
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    e.printStackTrace()
    null
}

internal fun findImg(rawDescription: String): String? {
    // From: https://gitlab.com/spacecowboy/Feeder
    // Using negative lookahead to skip data: urls, being inline base64
    // And capturing original quote to use as ending quote
    val regex = """img.*?src=(["'])((?!data).*?)\1""".toRegex(RegexOption.DOT_MATCHES_ALL)
    // Base64 encoded images can be quite large - and crash database cursors
    return regex.find(rawDescription)?.groupValues?.get(2)?.takeIf { !it.startsWith("data:") }
}
