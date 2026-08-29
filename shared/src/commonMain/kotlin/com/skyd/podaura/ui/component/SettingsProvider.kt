package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.skyd.podaura.ext.put
import com.skyd.podaura.model.preference.language.AppLanguage
import com.skyd.podaura.model.preference.language.AppLanguagePreference
import com.skyd.podaura.model.preference.language.platformSelectedAppLanguage
import com.skyd.podaura.model.preference.preferences
import kotlinx.coroutines.Dispatchers

@Composable
fun SettingsProvider(
    dataStore: DataStore<Preferences>,
    content: @Composable () -> Unit,
) {
    val prefs by remember { dataStore.data }.collectAsState(
        initial = null,
        context = Dispatchers.Default
    )
    CompositionLocalProvider(*preferences.map { it.first provide prefs }.toTypedArray()) {
        val storedLanguage = AppLanguage.fromPreferenceValue(AppLanguagePreference.current)
        val selectedLanguage = platformSelectedAppLanguage(storedLanguage)
        LaunchedEffect(selectedLanguage) {
            if (selectedLanguage != storedLanguage) {
                dataStore.put(AppLanguagePreference.key, selectedLanguage.preferenceValue)
            }
        }
        CompositionLocalProvider(
            AppLanguagePreference.local provides selectedLanguage.preferenceValue,
        ) {
            AppLanguageProvider(selectedLanguage = selectedLanguage, content = content)
        }
    }
}
