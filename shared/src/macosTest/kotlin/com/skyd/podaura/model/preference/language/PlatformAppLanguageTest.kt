package com.skyd.podaura.model.preference.language

import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlatformAppLanguageTest {
    @Test
    fun nativeLanguageOverrideRestoresSystemPreferences() {
        val defaults = NSUserDefaults.standardUserDefaults
        val original = defaults.stringArrayForKey("AppleLanguages")
        val suiteName = "com.skyd.podaura.tests.${NSUUID().UUIDString}"
        val suite = NSUserDefaults(suiteName = suiteName)
        val systemLanguages = MacAppLanguage.systemLanguageTags
        try {
            MacAppLanguage.set(AppLanguage.Japanese.localeTag, suiteName)
            assertEquals(listOf("ja-JP"), defaults.stringArrayForKey("AppleLanguages"))
            assertEquals(listOf("ja-JP"), suite.stringArrayForKey("AppleLanguages"))
            MacAppLanguage.set(AppLanguage.TraditionalChinese.localeTag, suiteName)
            assertEquals(listOf("zh-TW"), defaults.stringArrayForKey("AppleLanguages"))
            assertEquals(listOf("zh-TW"), suite.stringArrayForKey("AppleLanguages"))
            assertEquals(systemLanguages, MacAppLanguage.systemLanguageTags)
            MacAppLanguage.set(null, suiteName)
            assertNull(defaults.persistentDomainForName(suiteName)?.get("AppleLanguages"))
        } finally {
            MacAppLanguage.set(null, suiteName)
        }
        assertEquals(original, defaults.stringArrayForKey("AppleLanguages"))
    }
}
