package com.skyd.anivu.model.preference.behavior.media

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.model.preference.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object MediaListSortAscPreference : BasePreference<Boolean>() {
    private const val MEDIA_LIST_SORT_ASC = "mediaListSortAsc"

    override val default = false
    override val key = booleanPreferencesKey(MEDIA_LIST_SORT_ASC)
}