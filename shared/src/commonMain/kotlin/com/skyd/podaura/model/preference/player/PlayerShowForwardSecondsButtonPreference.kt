package com.skyd.podaura.model.preference.player

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object PlayerShowForwardSecondsButtonPreference : BasePreference<Boolean>() {
    private const val PLAYER_SHOW_FORWARD_SECONDS_BUTTON = "playerShowForwardSecondsButton"

    override val default = false
    override val key = booleanPreferencesKey(PLAYER_SHOW_FORWARD_SECONDS_BUTTON)
}