package com.skyd.podaura.model.preference.player

import androidx.datastore.preferences.core.intPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object PlayerReplaySecondsPreference : BasePreference<Int>() {
    private const val PLAYER_REPLAY_SECONDS = "playerReplaySeconds"

    val range = -300f..-5f
    override val default = -10
    override val key = intPreferencesKey(PLAYER_REPLAY_SECONDS)
}