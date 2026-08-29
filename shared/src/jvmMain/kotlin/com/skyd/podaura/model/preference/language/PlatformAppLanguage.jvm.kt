package com.skyd.podaura.model.preference.language

import androidx.compose.runtime.Composable
import java.util.Locale

@Composable
internal actual fun platformSelectedAppLanguage(storedLanguage: AppLanguage): AppLanguage =
    storedLanguage

@Composable
internal actual fun platformPreferredLanguageTags(): List<String> =
    listOf(Locale.getDefault(Locale.Category.DISPLAY).toLanguageTag())

internal actual fun setPlatformAppLanguage(language: AppLanguage) = Unit
