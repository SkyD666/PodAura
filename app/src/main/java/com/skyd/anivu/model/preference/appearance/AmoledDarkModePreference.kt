package com.skyd.anivu.model.preference.appearance

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.base.BasePreference

object AmoledDarkModePreference : BasePreference<Boolean> {
    private const val AMOLED_DARK_MODE = "amoledDarkMode"

    override val default = false
    override val key = booleanPreferencesKey(AMOLED_DARK_MODE)
}