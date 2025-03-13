package com.skyd.anivu.model.preference.player

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.base.BasePreference

object BackgroundPlayPreference : BasePreference<Boolean> {
    private const val BACKGROUND_PLAY = "backgroundPlay"

    override val default = false
    override val key = booleanPreferencesKey(BACKGROUND_PLAY)
}