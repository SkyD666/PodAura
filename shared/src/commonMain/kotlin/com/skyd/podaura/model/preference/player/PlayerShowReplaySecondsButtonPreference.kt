package com.skyd.podaura.model.preference.player

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object PlayerShowReplaySecondsButtonPreference : BasePreference<Boolean>() {
    private const val PLAYER_SHOW_REPLAY_SECONDS_BUTTON = "playerShowReplaySecondsButton"

    override val default = false
    override val key = booleanPreferencesKey(PLAYER_SHOW_REPLAY_SECONDS_BUTTON)
}