package com.skyd.podaura.model.preference.language

import androidx.compose.runtime.Composable
import com.skyd.podaura.model.preference.dataStore
import kotlinx.coroutines.flow.first

@Composable
internal expect fun platformSelectedAppLanguage(storedLanguage: AppLanguage): AppLanguage

@Composable
internal expect fun platformPreferredLanguageTags(): List<String>

internal expect fun setPlatformAppLanguage(language: AppLanguage)

/** Load the saved language before creating windows and persist Cocoa's next-launch preference. */
internal suspend fun initializePlatformAppLanguage() {
    val value = dataStore.data.first()[AppLanguagePreference.key] ?: AppLanguagePreference.default
    setPlatformAppLanguage(AppLanguage.fromPreferenceValue(value))
}
