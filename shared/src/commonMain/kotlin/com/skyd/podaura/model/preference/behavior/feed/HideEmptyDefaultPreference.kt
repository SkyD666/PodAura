package com.skyd.podaura.model.preference.behavior.feed

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object HideEmptyDefaultPreference : BasePreference<Boolean>() {
    private const val HIDE_EMPTY_DEFAULT = "hideEmptyDefault"

    override val default = true
    override val key = booleanPreferencesKey(HIDE_EMPTY_DEFAULT)
}
