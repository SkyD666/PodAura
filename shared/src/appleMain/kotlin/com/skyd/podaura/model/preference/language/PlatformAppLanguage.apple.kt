package com.skyd.podaura.model.preference.language

import androidx.compose.runtime.Composable
import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

@Composable
internal actual fun platformSelectedAppLanguage(storedLanguage: AppLanguage): AppLanguage =
    storedLanguage

@Composable
internal actual fun platformPreferredLanguageTags(): List<String> =
    NSLocale.preferredLanguages.map { it as String }

internal actual fun setPlatformAppLanguage(language: AppLanguage) = Unit
