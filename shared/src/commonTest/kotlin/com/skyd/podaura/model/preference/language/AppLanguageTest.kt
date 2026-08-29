package com.skyd.podaura.model.preference.language

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppLanguageTest {
    @Test
    fun resolvesFirstSupportedSystemLanguage() {
        assertEquals(
            AppLanguage.SimplifiedChinese,
            AppLanguage.resolveSystemLanguage(listOf("fr-FR", "zh-Hans-CN", "en-US")),
        )
    }

    @Test
    fun fallsBackToEnglishWhenSystemLanguagesAreUnsupported() {
        assertEquals(
            AppLanguage.English,
            AppLanguage.resolveSystemLanguage(listOf("fr-FR", "de-DE")),
        )
    }

    @Test
    fun distinguishesChineseScriptsAndRegions() {
        assertEquals(AppLanguage.TraditionalChinese, AppLanguage.fromLocaleTag("zh-Hant-HK"))
        assertEquals(AppLanguage.TraditionalChinese, AppLanguage.fromLocaleTag("zh_MO"))
        assertEquals(AppLanguage.SimplifiedChinese, AppLanguage.fromLocaleTag("zh-Hans-SG"))
        assertEquals(AppLanguage.SimplifiedChinese, AppLanguage.fromLocaleTag("zh"))
    }

    @Test
    fun matchesSupportedLanguagesByLanguageCode() {
        assertEquals(AppLanguage.English, AppLanguage.fromLocaleTag("en-GB"))
        assertEquals(AppLanguage.Japanese, AppLanguage.fromLocaleTag("ja"))
        assertEquals(AppLanguage.Turkish, AppLanguage.fromLocaleTag("tr-TR"))
        assertEquals(AppLanguage.Esperanto, AppLanguage.fromLocaleTag("eo"))
        assertNull(AppLanguage.fromLocaleTag("fr-FR"))
    }

    @Test
    fun invalidStoredValueUsesSystemDefault() {
        assertEquals(AppLanguage.System, AppLanguage.fromPreferenceValue("unknown"))
    }
}
