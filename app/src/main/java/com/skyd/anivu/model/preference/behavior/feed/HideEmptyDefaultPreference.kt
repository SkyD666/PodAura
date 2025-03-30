package com.skyd.anivu.model.preference.behavior.feed

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.base.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object HideEmptyDefaultPreference : BasePreference<Boolean> {
    private const val HIDE_EMPTY_DEFAULT = "hideEmptyDefault"

    override val default = true
    override val key = booleanPreferencesKey(HIDE_EMPTY_DEFAULT)
}