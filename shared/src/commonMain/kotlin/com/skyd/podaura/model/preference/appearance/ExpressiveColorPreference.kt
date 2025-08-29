package com.skyd.podaura.model.preference.appearance

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object ExpressiveColorPreference : BasePreference<Boolean>() {
    private const val EXPRESSIVE_COLOR = "expressiveColor"

    override val default = false
    override val key = booleanPreferencesKey(EXPRESSIVE_COLOR)
}