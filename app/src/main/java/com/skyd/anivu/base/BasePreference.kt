package com.skyd.anivu.base

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.put
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface BasePreference<T> {
    val key: Preferences.Key<T>
    val default: T

    fun put(context: Context, scope: CoroutineScope, value: T) {
        scope.launch(Dispatchers.IO) {
            context.dataStore.put(key, value)
        }
    }

    fun fromPreferences(preferences: Preferences): T = preferences[key] ?: default
}