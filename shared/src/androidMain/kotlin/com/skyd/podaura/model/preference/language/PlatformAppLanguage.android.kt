package com.skyd.podaura.model.preference.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat

@Composable
internal actual fun platformSelectedAppLanguage(storedLanguage: AppLanguage): AppLanguage {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        val applicationLocales = AppCompatDelegate.getApplicationLocales()
        if (applicationLocales.isEmpty) {
            AppLanguage.System
        } else {
            AppLanguage.fromLocaleTag(applicationLocales[0]?.toLanguageTag().orEmpty())
                ?: AppLanguage.System
        }
    }
}

@Composable
internal actual fun platformPreferredLanguageTags(): List<String> {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        val locales = ConfigurationCompat.getLocales(configuration)
        buildList {
            for (index in 0 until locales.size()) {
                locales[index]?.toLanguageTag()?.let(::add)
            }
        }
    }
}

internal actual fun setPlatformAppLanguage(language: AppLanguage) {
    AppCompatDelegate.setApplicationLocales(
        language.localeTag
            ?.let(LocaleListCompat::forLanguageTags)
            ?: LocaleListCompat.getEmptyLocaleList()
    )
}
