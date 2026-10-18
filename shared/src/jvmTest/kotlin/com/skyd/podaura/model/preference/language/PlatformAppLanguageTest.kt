package com.skyd.podaura.model.preference.language

import com.skyd.fundation.jna.mac.ObjCRuntime
import com.skyd.fundation.jna.mac.ObjCRuntime.getUtf8String
import com.skyd.fundation.jna.mac.ObjCRuntime.invoke
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import com.sun.jna.Pointer
import java.util.Locale
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlatformAppLanguageTest {
    @Test
    fun ownBundleUsesStandardDefaultsInsteadOfAnInvalidSuite() {
        if (platform != Platform.macOS_Jvm) return
        val testSuite = "com.skyd.podaura.tests.${UUID.randomUUID()}"
        MacAppLanguage.set(null, testSuite)
        val bundle = ObjCRuntime.objc_getClass("NSBundle")("mainBundle")
        val identifier = ObjCRuntime.objc_msgSend<Pointer?>(
            bundle, ObjCRuntime.sel_registerName("bundleIdentifier"),
        )?.getUtf8String(0) ?: return
        val preferences = MacAppLanguage.retainedPreferences(identifier)
        try {
            assertEquals(
                ObjCRuntime.objc_getClass("NSUserDefaults")("standardUserDefaults"), preferences,
            )
        } finally {
            preferences("release")
        }
    }

    @Test
    fun nativeLanguageOverrideRestoresSystemPreferences() {
        if (platform != Platform.macOS_Jvm) return
        val suiteName = "com.skyd.podaura.tests.${UUID.randomUUID()}"
        MacAppLanguage.set(null, suiteName)
        val defaults = ObjCRuntime.objc_getClass("NSUserDefaults")("standardUserDefaults")
        fun nsString(value: String) =
            ObjCRuntime.objc_getClass("NSString")("stringWithUTF8String:", value)
        val suite = ObjCRuntime.objc_getClass("NSUserDefaults")("alloc")(
            "initWithSuiteName:", nsString(suiteName),
        )
        fun languages(store: Pointer = defaults): String? = ObjCRuntime.objc_msgSend<Pointer?>(
            store, ObjCRuntime.sel_registerName("stringArrayForKey:"), nsString("AppleLanguages"),
        )?.invoke("componentsJoinedByString:", nsString(","))?.getUtf8String(0)

        val original = languages()
        val displayLocale = Locale.getDefault(Locale.Category.DISPLAY)
        val systemLanguages = MacAppLanguage.systemLanguageTags
        try {
            MacAppLanguage.set(AppLanguage.Japanese.localeTag, suiteName)
            assertEquals("ja-JP", languages())
            assertEquals("ja-JP", languages(suite))
            MacAppLanguage.set(AppLanguage.TraditionalChinese.localeTag, suiteName)
            assertEquals("zh-TW", languages())
            assertEquals("zh-TW", languages(suite))
            assertEquals(displayLocale, Locale.getDefault(Locale.Category.DISPLAY))
            assertEquals(systemLanguages, MacAppLanguage.systemLanguageTags)
            MacAppLanguage.set(null, suiteName)
            val domain = ObjCRuntime.objc_msgSend<Pointer?>(
                defaults, ObjCRuntime.sel_registerName("persistentDomainForName:"), nsString(suiteName),
            )
            assertNull(domain?.let {
                ObjCRuntime.objc_msgSend<Pointer?>(it, ObjCRuntime.sel_registerName("objectForKey:"), nsString("AppleLanguages"))
            })
        } finally {
            MacAppLanguage.set(null, suiteName)
            suite("release")
        }
        assertEquals(original, languages())
    }
}
