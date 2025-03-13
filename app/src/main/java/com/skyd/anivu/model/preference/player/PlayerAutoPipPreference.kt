package com.skyd.anivu.model.preference.player

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.base.BasePreference

object PlayerAutoPipPreference : BasePreference<Boolean> {
    private const val PLAYER_AUTO_PIP = "playerAutoPip"

    override val default = false
    override val key = booleanPreferencesKey(PLAYER_AUTO_PIP)
}