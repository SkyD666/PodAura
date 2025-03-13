package com.skyd.anivu.model.preference.behavior.media

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.base.BasePreference

object MediaSubListSortAscPreference : BasePreference<Boolean> {
    private const val MEDIA_SUB_LIST_SORT_ASC = "mediaSubListSortAsc"

    override val default = false
    override val key = booleanPreferencesKey(MEDIA_SUB_LIST_SORT_ASC)
}