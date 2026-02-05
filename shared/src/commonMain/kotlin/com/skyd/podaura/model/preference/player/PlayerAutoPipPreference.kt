package com.skyd.podaura.model.preference.player

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object PlayerAutoPipPreference : BasePreference<Boolean>() {
    private const val PLAYER_AUTO_PIP = "playerAutoPip"

    override val default = false
    override val key = booleanPreferencesKey(PLAYER_AUTO_PIP)
}