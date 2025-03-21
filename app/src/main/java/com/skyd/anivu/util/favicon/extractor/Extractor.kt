package com.skyd.anivu.util.favicon.extractor

import com.google.common.math.IntMath.sqrt
import retrofit2.Response
import java.math.RoundingMode

interface Extractor {
    fun baseUrl(url: String) = Regex("^.+?[^/:](?=[?/]|$)").find(url)?.value
    fun <T> Response<T>.isImage() = headers()["Content-Type"]?.startsWith("image/") == true
    fun <T> Response<T>.isSvg() =
        headers()["Content-Type"]?.contains("svg", ignoreCase = true) == true

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
            val MAX_SIZE = IconSize(
                sqrt(Int.MAX_VALUE, RoundingMode.DOWN),
                sqrt(Int.MAX_VALUE, RoundingMode.DOWN)
            )
        }
    }
}