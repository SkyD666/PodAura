package com.skyd.podaura.model.preference.player

import androidx.datastore.preferences.core.intPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object PlayerForwardSecondsButtonValuePreference : BasePreference<Int>() {
    private const val PLAYER_FORWARD_SECONDS_BUTTON_VALUE = "playerForwardSecondsButtonValue"

    val range = -300f..300f
    override val default = 60
    override val key = intPreferencesKey(PLAYER_FORWARD_SECONDS_BUTTON_VALUE)
}