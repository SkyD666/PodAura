package com.skyd.anivu.model.preference.player

import androidx.datastore.preferences.core.intPreferencesKey
import com.skyd.anivu.model.preference.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object PlayerForwardSecondsButtonValuePreference : BasePreference<Int>() {
    private const val PLAYER_FORWARD_SECONDS_BUTTON_VALUE = "playerForwardSecondsButtonValue"

    val range = -300f..300f
    override val default = 60
    override val key = intPreferencesKey(PLAYER_FORWARD_SECONDS_BUTTON_VALUE)
}