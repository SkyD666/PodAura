package com.skyd.podaura.model.preference.language

import androidx.compose.runtime.Composable
import com.skyd.fundation.jna.mac.ObjCRuntime
import com.skyd.fundation.jna.mac.ObjCRuntime.getUtf8String
import com.skyd.fundation.jna.mac.ObjCRuntime.invoke
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import com.skyd.podaura.BuildKonfig
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import java.util.Locale

@Composable
internal actual fun platformSelectedAppLanguage(storedLanguage: AppLanguage): AppLanguage =
    storedLanguage

@Composable
internal actual fun platformPreferredLanguageTags(): List<String> =
    if (platform == Platform.macOS_Jvm) MacAppLanguage.systemLanguageTags
    else listOf(Locale.getDefault(Locale.Category.DISPLAY).toLanguageTag())

internal actual fun setPlatformAppLanguage(language: AppLanguage) {
    if (platform == Platform.macOS_Jvm) MacAppLanguage.set(language.localeTag)
}

internal object MacAppLanguage {
    // Keep Foundation loaded before AWT initializes AppKit.
    private val foundation = NativeLibrary.getInstance("Foundation")
    private val defaults = ObjCRuntime.objc_getClass("NSUserDefaults")("standardUserDefaults")
    private val argumentDomain = nsString("NSArgumentDomain")("retain")
    private val originalArguments = defaults("volatileDomainForName:", argumentDomain)("copy")
    private val globalDefaults
        get() = ObjCRuntime.objc_msgSend<Pointer?>(
            defaults, ObjCRuntime.sel_registerName("persistentDomainForName:"),
            foundation.getGlobalVariableAddress("NSGlobalDomain").getPointer(0),
        )
    val systemLanguageTags: List<String>
        get() = globalDefaults?.let { global ->
            ObjCRuntime.objc_msgSend<Pointer?>(
                global, ObjCRuntime.sel_registerName("objectForKey:"), nsString("AppleLanguages"),
            )
        }?.let { languages ->
            val count =
                ObjCRuntime.objc_msgSend<Long>(languages, ObjCRuntime.sel_registerName("count"))
            (0 until count).map { languages("objectAtIndex:", it).getUtf8String(0) }
        }?.takeIf { it.isNotEmpty() }
            ?: listOf(Locale.getDefault(Locale.Category.DISPLAY).toLanguageTag())

    fun set(languageTag: String?, suiteName: String = BuildKonfig.packageName) {
        val pool = ObjCRuntime.new("NSAutoreleasePool")
        val arguments = originalArguments("mutableCopy")
        val suite = retainedPreferences(suiteName)
        try {
            val key = nsString("AppleLanguages")
            if (languageTag != null) {
                val languages =
                    ObjCRuntime.objc_getClass("NSArray")("arrayWithObject:", nsString(languageTag))
                arguments.sendVoid("setObject:forKey:", languages, key)
                suite.sendVoid("setObject:forKey:", languages, key)
            } else {
                suite.sendVoid("removeObjectForKey:", key)
            }
            // Cocoa also reads this app's suite before JVM main on the next launch.
            // Never persist a language override in the JDK's or macOS's global domain.
            defaults.sendVoid("setVolatileDomain:forName:", arguments, argumentDomain)
        } finally {
            suite.sendVoid("release")
            arguments.sendVoid("release")
            pool.sendVoid("drain")
        }
    }

    /** NSUserDefaults rejects its own bundle identifier as a suite name. Caller releases the result. */
    internal fun retainedPreferences(suiteName: String): Pointer {
        val bundle = ObjCRuntime.objc_getClass("NSBundle")("mainBundle")
        val identifier = ObjCRuntime.objc_msgSend<Pointer?>(
            bundle, ObjCRuntime.sel_registerName("bundleIdentifier"),
        )?.getUtf8String(0)
        return if (identifier == suiteName) defaults("retain")
        else ObjCRuntime.objc_getClass("NSUserDefaults")("alloc")(
            "initWithSuiteName:",
            nsString(suiteName)
        )
    }

    private fun nsString(value: String) =
        ObjCRuntime.objc_getClass("NSString")("stringWithUTF8String:", value)

    private fun Pointer.sendVoid(selectorName: String, vararg args: Any?) {
        ObjCRuntime.INSTANCE.getFunction("objc_msgSend")
            .invokeVoid(arrayOf(this, ObjCRuntime.sel_registerName(selectorName), *args))
    }
}
