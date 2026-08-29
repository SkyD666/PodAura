@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.LayoutDirection.Rtl
import com.skyd.fundation.locale.setFormattingLocale
import com.skyd.podaura.model.preference.language.AppLanguage
import com.skyd.podaura.model.preference.language.platformPreferredLanguageTags
import org.jetbrains.compose.resources.ComposeEnvironment
import org.jetbrains.compose.resources.LanguageQualifier
import org.jetbrains.compose.resources.LocalComposeEnvironment
import org.jetbrains.compose.resources.RegionQualifier
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.ScriptQualifier
import org.jetbrains.compose.resources.getResourceEnvironment
import org.jetbrains.compose.resources.getSystemEnvironment
import kotlin.concurrent.atomics.AtomicReference

private val currentEffectiveLanguage = AtomicReference(AppLanguage.English)

@Composable
fun AppLanguageProvider(
    selectedLanguage: AppLanguage,
    content: @Composable () -> Unit,
) {
    val preferredLanguageTags = platformPreferredLanguageTags()
    val effectiveLanguage = remember(selectedLanguage, preferredLanguageTags) {
        if (selectedLanguage == AppLanguage.System) {
            AppLanguage.resolveSystemLanguage(preferredLanguageTags)
        } else {
            selectedLanguage
        }
    }
    val defaultEnvironment = LocalComposeEnvironment.current
    val appEnvironment = remember(defaultEnvironment, effectiveLanguage) {
        object : ComposeEnvironment {
            @Composable
            override fun rememberEnvironment(): ResourceEnvironment =
                defaultEnvironment.rememberEnvironment().withLanguage(effectiveLanguage)
        }
    }

    // Resource and formatter lookups can happen while children are first composed.
    remember(effectiveLanguage) {
        currentEffectiveLanguage.store(effectiveLanguage)
        getResourceEnvironment = ::getAppResourceEnvironment
        setFormattingLocale(checkNotNull(effectiveLanguage.localeTag))
    }

    CompositionLocalProvider(
        LocalComposeEnvironment provides appEnvironment,
        LocalLayoutDirection provides if (effectiveLanguage.isRtl) Rtl else Ltr,
        content = content,
    )
}

private fun getAppResourceEnvironment(): ResourceEnvironment =
    getSystemEnvironment().withLanguage(currentEffectiveLanguage.load())

private fun ResourceEnvironment.withLanguage(language: AppLanguage): ResourceEnvironment =
    ResourceEnvironment(
        language = LanguageQualifier(language.language),
        script = ScriptQualifier(language.script),
        region = RegionQualifier(language.region),
        theme = this.theme,
        density = this.density,
    )
