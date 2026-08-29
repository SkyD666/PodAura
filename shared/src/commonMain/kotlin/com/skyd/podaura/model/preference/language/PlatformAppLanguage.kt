package com.skyd.podaura.model.preference.language

import androidx.compose.runtime.Composable

@Composable
internal expect fun platformSelectedAppLanguage(storedLanguage: AppLanguage): AppLanguage

@Composable
internal expect fun platformPreferredLanguageTags(): List<String>

internal expect fun setPlatformAppLanguage(language: AppLanguage)
