package com.skyd.podaura.model.preference.appearance.media

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object MediaShowGroupTabPreference : BasePreference<Boolean>() {
    private const val MEDIA_SHOW_GROUP_TAB = "mediaShowGroupTab"

    override val default = true
    override val key = booleanPreferencesKey(MEDIA_SHOW_GROUP_TAB)
}
