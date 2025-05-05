package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.skyd.podaura.model.preference.preferences
import kotlinx.coroutines.Dispatchers

@Composable
fun SettingsProvider(
    dataStore: DataStore<Preferences>,
    content: @Composable () -> Unit,
) {
    val pref by remember { dataStore.data }.collectAsState(
        initial = null,
        context = Dispatchers.Default
    )
    CompositionLocalProvider(*preferences.map { it.first.provide(pref) }.toTypedArray()) {
        content()
    }
}