package com.skyd.anivu.model.preference.player

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.base.BasePreference

object PlayerShowScreenshotButtonPreference : BasePreference<Boolean> {
    private const val PLAYER_SHOW_SCREENSHOT_BUTTON = "playerShowScreenshotButton"

    override val default = true
    override val key = booleanPreferencesKey(PLAYER_SHOW_SCREENSHOT_BUTTON)
}