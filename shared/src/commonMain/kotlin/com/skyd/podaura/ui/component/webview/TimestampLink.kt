package com.skyd.podaura.ui.component.webview

import com.fleeksoft.ksoup.Ksoup

internal const val TIMESTAMP_LINK_PREFIX = "podaura://timestamp/"

private val timestampRegex = Regex(
    pattern = "(?<![\\d:：])(?:(\\d+)[:：]([0-5]\\d)[:：]([0-5]\\d)|(\\d+)[:：]([0-5]\\d))(?![\\d:：])"
)
private val plainTextUrlRegex = Regex(
    pattern = "(?i)\\b(?:https?://|www\\.)[^\\s<]+"
)

internal fun timestampSecondsFromUri(uri: String): Long? {
    if (!uri.startsWith(TIMESTAMP_LINK_PREFIX)) return null
    return uri.removePrefix(TIMESTAMP_LINK_PREFIX).toLongOrNull()?.takeIf { it >= 0 }
}

internal fun linkifyTimestamps(html: String): String {
    val document = Ksoup.parse(html)
    document.select("a").forEach { element ->
        if (element.attr("href").isNotBlank()) return@forEach
        val seconds = timestampRegex.matchEntire(element.text().trim())
            ?.toTimestampSeconds() ?: return@forEach
        element.attr("href", "$TIMESTAMP_LINK_PREFIX$seconds")
    }
    val ignoredTags = setOf("a", "code", "pre", "script", "style")
    val textNodes = document.body().getAllElements()
        .filterNot { element -> element.tagName().lowercase() in ignoredTags }
        .filterNot { element -> element.parents().any { it.tagName().lowercase() in ignoredTags } }
        .flatMap { it.textNodes() }
        .distinct()

    textNodes.forEach { textNode ->
        val original = textNode.text()
        if (!timestampRegex.containsMatchIn(original)) return@forEach
        val linked = original.linkifyTimestampText() ?: return@forEach
        textNode.before(linked)
        textNode.remove()
    }
    return document.body().html()
}

private fun String.linkifyTimestampText(): String? {
    val urlRanges = plainTextUrlRegex.findAll(this).map { it.range }.toList()
    val timestamps = timestampRegex.findAll(this)
        .mapNotNull { match ->
            if (urlRanges.any { it.overlaps(match.range) }) return@mapNotNull null
            match.toTimestampSeconds()?.let { seconds -> match to seconds }
        }
        .toList()
    if (timestamps.isEmpty()) return null

    return buildString {
        var cursor = 0
        timestamps.forEach { (match, seconds) ->
            append(this@linkifyTimestampText.substring(cursor, match.range.first).escapeHtml())
            append("<a href=\"$TIMESTAMP_LINK_PREFIX$seconds\">")
            append(match.value.escapeHtml())
            append("</a>")
            cursor = match.range.last + 1
        }
        append(this@linkifyTimestampText.substring(cursor).escapeHtml())
    }
}

private fun IntRange.overlaps(other: IntRange): Boolean =
    first <= other.last && other.first <= last

private fun String.escapeHtml(): String = buildString(length) {
    this@escapeHtml.forEach { char ->
        append(
            when (char) {
                '&' -> "&amp;"
                '<' -> "&lt;"
                '>' -> "&gt;"
                '"' -> "&quot;"
                else -> char
            }
        )
    }
}

private fun MatchResult.toTimestampSeconds(): Long? {
    val hours = groups[1]?.value?.toLongOrNull()
    return if (hours != null) {
        val minutes = groups[2]?.value?.toLongOrNull() ?: return null
        val seconds = groups[3]?.value?.toLongOrNull() ?: return null
        safeTimestampSeconds(hours = hours, minutes = minutes, seconds = seconds)
    } else {
        val minutes = groups[4]?.value?.toLongOrNull() ?: return null
        val seconds = groups[5]?.value?.toLongOrNull() ?: return null
        safeTimestampSeconds(hours = 0, minutes = minutes, seconds = seconds)
    }
}

private fun safeTimestampSeconds(hours: Long, minutes: Long, seconds: Long): Long? {
    if (hours > Long.MAX_VALUE / 3600) return null
    val hourSeconds = hours * 3600
    if (minutes > (Long.MAX_VALUE - hourSeconds) / 60) return null
    return hourSeconds + minutes * 60 + seconds
}
