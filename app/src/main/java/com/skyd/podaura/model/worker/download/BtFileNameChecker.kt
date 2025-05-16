package com.skyd.podaura.model.worker.download

object BtFileNameChecker {
    private const val RESOLUTION =
        """\b(720p|1080p|2160p|4k|uhd|3840x2160|1920x1080)\b"""
    private const val AUDIO =
        """\b(dts([-_]?(hd|es|x))?|ac3|aac(5\.1)?|truehd|ddp5\.1|atmos|flac)\b"""
    private const val SUBTITLE =
        """\b(subs?|chs|cht|eng|french|spanish|multi[-_]?(lang|sub))\b"""
    private const val CODEC =
        """\b(x26[45]|h\.?264|h\.?265|hevc|av1|divx|xvid)\b"""
    private const val SOURCE =
        """\b(blu[-_]?ray|web[-_]?dl|web[-_]?rip|hd[-_]?rip|dvd[-_]?rip|hdtv)\b"""
    private const val WEBSITE =
        """\b(1377x|nyaa|acg.rip|dmhy|bangumi.moe)\b"""

    private val patterns = mapOf(
        "resolution" to Regex(RESOLUTION, RegexOption.IGNORE_CASE),
        "audio" to Regex(AUDIO, RegexOption.IGNORE_CASE),
        "subtitle" to Regex(SUBTITLE, RegexOption.IGNORE_CASE),
        "codec" to Regex(CODEC, RegexOption.IGNORE_CASE),
        "source" to Regex(SOURCE, RegexOption.IGNORE_CASE),
        "website" to Regex(WEBSITE, RegexOption.IGNORE_CASE),
    )

    private val weights: Map<String, Int> = mapOf(
        "website" to 4,
        "resolution" to 2,
        "codec" to 2,
        "source" to 1,
        "audio" to 1,
        "subtitle" to 1,
    )

    fun check(text: String): Boolean {
        val processedName = text
            .lowercase()
            .replace(Regex("[\\W_]"), " ")
            .replace(Regex("\\s+"), " ")

        var total = 0
        patterns.forEach { (key, pattern) ->
            if (pattern.containsMatchIn(processedName)) {
                total += (weights[key] ?: 0)
                if (total >= 4) return false
            }
        }
        return true
    }
}