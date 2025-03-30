package com.skyd.anivu.model.preference.player

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.base.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object PlayerShow85sButtonPreference : BasePreference<Boolean> {
    private const val PLAYER_SHOW_85S_BUTTON = "playerShow85sButton"

    override val default = false
    override val key = booleanPreferencesKey(PLAYER_SHOW_85S_BUTTON)
}