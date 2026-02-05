package com.skyd.podaura.model.preference.behavior.media

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object MediaSubListSortAscPreference : BasePreference<Boolean>() {
    private const val MEDIA_SUB_LIST_SORT_ASC = "mediaSubListSortAsc"

    override val default = false
    override val key = booleanPreferencesKey(MEDIA_SUB_LIST_SORT_ASC)
}
