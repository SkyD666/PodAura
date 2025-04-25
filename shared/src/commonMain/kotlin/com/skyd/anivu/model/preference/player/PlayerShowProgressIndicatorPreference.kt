package com.skyd.anivu.model.preference.player

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.model.preference.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object PlayerShowProgressIndicatorPreference : BasePreference<Boolean>() {
    private const val PLAYER_SHOW_PROGRESS_INDICATOR = "playerShowProgressIndicator"

    override val default = false
    override val key = booleanPreferencesKey(PLAYER_SHOW_PROGRESS_INDICATOR)
}