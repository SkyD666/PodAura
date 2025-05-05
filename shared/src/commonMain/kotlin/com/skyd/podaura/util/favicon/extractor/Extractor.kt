package com.skyd.podaura.util.favicon.extractor

import io.ktor.http.Headers
import kotlin.math.sqrt

interface Extractor {
    fun baseUrl(url: String) = Regex("^.+?[^/:](?=[?/]|$)").find(url)?.value
    fun Headers.isImage() = get("Content-Type")?.startsWith("image/") == true
    fun Headers.isSvg() = get("Content-Type")?.contains("svg", ignoreCase = true) == true

    fun extract(url: String): List<IconData>

    data class IconData(
        val url: String,
        val size: IconSize,
    )

    data class IconSize(
        val width: Int,
        val height: Int,
    ) {
        companion object {
            val EMPTY = IconSize(0, 0)
            val MAX_SIZE = sqrt(Int.MAX_VALUE - 1.0).toInt().let { IconSize(it, it) }
        }
    }
}