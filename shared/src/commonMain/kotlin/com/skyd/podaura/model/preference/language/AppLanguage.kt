package com.skyd.podaura.model.preference.language

enum class AppLanguage(
    val preferenceValue: String,
    val localeTag: String?,
    val nativeDisplayName: String?,
    internal val language: String,
    internal val script: String = "",
    internal val region: String = "",
    internal val isRtl: Boolean = false,
) {
    System(
        preferenceValue = "system",
        localeTag = null,
        nativeDisplayName = null,
        language = "",
    ),
    English(
        preferenceValue = "en",
        localeTag = "en",
        nativeDisplayName = "English",
        language = "en",
    ),
    SimplifiedChinese(
        preferenceValue = "zh-CN",
        localeTag = "zh-CN",
        nativeDisplayName = "简体中文",
        language = "zh",
        script = "Hans",
        region = "CN",
    ),
    TraditionalChinese(
        preferenceValue = "zh-TW",
        localeTag = "zh-TW",
        nativeDisplayName = "繁體中文",
        language = "zh",
        script = "Hant",
        region = "TW",
    ),
    Japanese(
        preferenceValue = "ja-JP",
        localeTag = "ja-JP",
        nativeDisplayName = "日本語",
        language = "ja",
        region = "JP",
    ),
    Turkish(
        preferenceValue = "tr",
        localeTag = "tr",
        nativeDisplayName = "Türkçe",
        language = "tr",
    ),
    Esperanto(
        preferenceValue = "eo-UY",
        localeTag = "eo-UY",
        nativeDisplayName = "Esperanto",
        language = "eo",
        region = "UY",
    );

    companion object {
        fun fromPreferenceValue(value: String): AppLanguage =
            entries.firstOrNull { it.preferenceValue.equals(value, ignoreCase = true) } ?: System

        fun resolveSystemLanguage(preferredLanguageTags: List<String>): AppLanguage =
            preferredLanguageTags.firstNotNullOfOrNull(::fromLocaleTag) ?: English

        fun fromLocaleTag(languageTag: String): AppLanguage? {
            val parts = languageTag
                .replace('_', '-')
                .split('-')
                .filter { it.isNotBlank() }
            val language = parts.firstOrNull()?.lowercase() ?: return null
            val script = parts.firstOrNull { it.length == 4 }?.lowercase()
            val region = parts
                .firstOrNull { it.length == 2 && it.lowercase() != language }
                ?.uppercase()

            return when (language) {
                "en" -> English
                "zh" -> when {
                    script == "hant" || region in setOf("TW", "HK", "MO") -> TraditionalChinese
                    else -> SimplifiedChinese
                }

                "ja" -> Japanese
                "tr" -> Turkish
                "eo" -> Esperanto
                else -> null
            }
        }
    }
}
