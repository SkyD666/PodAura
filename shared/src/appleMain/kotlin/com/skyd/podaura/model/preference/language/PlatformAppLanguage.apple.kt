package com.skyd.podaura.model.preference.language

import androidx.compose.runtime.Composable
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import com.skyd.podaura.BuildKonfig
import platform.Foundation.NSArgumentDomain
import platform.Foundation.NSBundle
import platform.Foundation.NSGlobalDomain
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

@Composable
internal actual fun platformSelectedAppLanguage(storedLanguage: AppLanguage): AppLanguage =
    storedLanguage

@Composable
internal actual fun platformPreferredLanguageTags(): List<String> =
    if (platform == Platform.macOS_Native) MacAppLanguage.systemLanguageTags
    else NSLocale.preferredLanguages.map { it as String }

internal actual fun setPlatformAppLanguage(language: AppLanguage) {
    if (platform == Platform.macOS_Native) MacAppLanguage.set(language.localeTag)
}

internal object MacAppLanguage {
    // The app suite can contain our previous override; "Follow system" must use the global list.
    val systemLanguageTags: List<String>
        get() = (NSUserDefaults.standardUserDefaults
            .persistentDomainForName(NSGlobalDomain)?.get("AppleLanguages") as? List<*>)
            ?.filterIsInstance<String>()?.takeIf { it.isNotEmpty() }
            ?: NSLocale.preferredLanguages.map { it as String }
    private val originalArguments =
        NSUserDefaults.standardUserDefaults.volatileDomainForName(NSArgumentDomain)

    fun set(languageTag: String?, suiteName: String = BuildKonfig.packageName) {
        val arguments = originalArguments.toMutableMap()
        val suite = if (NSBundle.mainBundle.bundleIdentifier == suiteName) {
            NSUserDefaults.standardUserDefaults
        } else NSUserDefaults(suiteName = suiteName)
        if (languageTag != null) {
            val languages = listOf(languageTag)
            arguments["AppleLanguages"] = languages
            suite.setObject(languages, "AppleLanguages")
        } else {
            suite.removeObjectForKey("AppleLanguages")
        }
        NSUserDefaults.standardUserDefaults.setVolatileDomain(arguments, NSArgumentDomain)
    }
}
