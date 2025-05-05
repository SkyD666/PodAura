package com.skyd.podaura.model.preference.appearance

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object AmoledDarkModePreference : BasePreference<Boolean>() {
    private const val AMOLED_DARK_MODE = "amoledDarkMode"

    override val default = false
    override val key = booleanPreferencesKey(AMOLED_DARK_MODE)
}