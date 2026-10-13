package com.skyd.podaura.model.preference.player

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object PauseOnScreenOffPreference : BasePreference<Boolean>() {
    private const val PAUSE_ON_SCREEN_OFF = "pauseOnScreenOff"

    override val default = true
    override val key = booleanPreferencesKey(PAUSE_ON_SCREEN_OFF)
}
