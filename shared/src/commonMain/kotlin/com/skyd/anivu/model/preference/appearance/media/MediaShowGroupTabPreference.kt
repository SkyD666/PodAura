package com.skyd.anivu.model.preference.appearance.media

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.model.preference.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object MediaShowGroupTabPreference : BasePreference<Boolean>() {
    private const val MEDIA_SHOW_GROUP_TAB = "mediaShowGroupTab"

    override val default = true
    override val key = booleanPreferencesKey(MEDIA_SHOW_GROUP_TAB)
}