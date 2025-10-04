package com.skyd.podaura.model.repository.feed.convert

import com.skyd.podaura.di.get
import com.skyd.podaura.util.favicon.FaviconExtractor

/*internal*/ suspend fun getRssIcon(url: String): String? = runCatching {
    get<FaviconExtractor>().extractFavicon(url)
}.onFailure { it.printStackTrace() }.getOrNull()

/*internal*/ fun findImg(rawDescription: String): String? {
    // From: https://gitlab.com/spacecowboy/Feeder
    // Using negative lookahead to skip data: urls, being inline base64
    // And capturing original quote to use as ending quote
    val regex = """img.*?src=(["'])((?!data).*?)\1""".toRegex(RegexOption.DOT_MATCHES_ALL)
    // Base64 encoded images can be quite large - and crash database cursors
    return regex.find(rawDescription)?.groupValues?.get(2)?.takeIf { !it.startsWith("data:") }
}