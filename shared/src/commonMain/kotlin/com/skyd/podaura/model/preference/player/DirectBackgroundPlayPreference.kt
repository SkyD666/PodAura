package com.skyd.podaura.model.preference.player

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.ext.get
import com.skyd.podaura.model.preference.BasePreference
import com.skyd.podaura.model.preference.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Preference
object DirectBackgroundPlayPreference : BasePreference<Boolean>() {
    private const val DIRECT_BACKGROUND_PLAY = "directBackgroundPlay"
    override val default = false
    override val key = booleanPreferencesKey(DIRECT_BACKGROUND_PLAY)

    override fun put(scope: CoroutineScope, value: Boolean) {
        scope.launch {
            dataStore.edit { update(it, value) }
        }
    }

    internal fun update(preferences: MutablePreferences, value: Boolean) {
        preferences[key] = value && preferences[BackgroundPlayPreference]
    }

    fun enabled(): Boolean = runBlocking {
        dataStore.data.first()
            .let { it[BackgroundPlayPreference] && it[DirectBackgroundPlayPreference] }
    }
}
