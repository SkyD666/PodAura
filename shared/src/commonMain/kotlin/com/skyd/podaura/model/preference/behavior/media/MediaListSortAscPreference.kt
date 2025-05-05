package com.skyd.podaura.model.preference.behavior.media

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object MediaListSortAscPreference : BasePreference<Boolean>() {
    private const val MEDIA_LIST_SORT_ASC = "mediaListSortAsc"

    override val default = false
    override val key = booleanPreferencesKey(MEDIA_LIST_SORT_ASC)
}