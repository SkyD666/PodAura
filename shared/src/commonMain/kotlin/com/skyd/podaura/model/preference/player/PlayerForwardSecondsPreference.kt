package com.skyd.podaura.model.preference.player

import androidx.datastore.preferences.core.intPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object PlayerForwardSecondsPreference : BasePreference<Int>() {
    private const val PLAYER_FORWARD_SECONDS = "playerForwardSeconds"

    val range = 5f..300f
    override val default = 10
    override val key = intPreferencesKey(PLAYER_FORWARD_SECONDS)
}
