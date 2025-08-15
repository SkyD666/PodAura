package com.skyd.podaura.model.preference.player

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object BackgroundPlayPreference : BasePreference<Boolean>() {
    private const val BACKGROUND_PLAY = "backgroundPlay"

    override val default = true
    override val key = booleanPreferencesKey(BACKGROUND_PLAY)
}