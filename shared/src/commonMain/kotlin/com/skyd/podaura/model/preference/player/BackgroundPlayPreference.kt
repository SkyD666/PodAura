package com.skyd.podaura.model.preference.player

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference
import com.skyd.podaura.model.preference.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Preference
object BackgroundPlayPreference : BasePreference<Boolean>() {
    private const val BACKGROUND_PLAY = "backgroundPlay"

    override val default = true
    override val key = booleanPreferencesKey(BACKGROUND_PLAY)

    override fun put(scope: CoroutineScope, value: Boolean) {
        scope.launch {
            dataStore.edit { update(it, value) }
        }
    }

    internal fun update(preferences: MutablePreferences, value: Boolean) {
        preferences[key] = value
        if (!value) preferences[DirectBackgroundPlayPreference.key] = false
    }
}
