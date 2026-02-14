package com.skyd.podaura.model.preference

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import com.skyd.podaura.ext.put
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

abstract class BasePreference<T> {
    abstract val key: Preferences.Key<T>
    abstract val default: T
    val local: ProvidableCompositionLocal<T> = compositionLocalOf { default }

    val current: T
        @ReadOnlyComposable
        @Composable
        get() = local.current

    fun provide(pref: Preferences?): ProvidedValue<T> {
        return local provides (pref?.get(key) ?: default)
    }

    open fun put(scope: CoroutineScope, value: T) {
        scope.launch(Dispatchers.IO) {
            dataStore.put(key, value)
        }
    }

    open fun fromPreferences(preferences: Preferences): T = preferences[key] ?: default
}
