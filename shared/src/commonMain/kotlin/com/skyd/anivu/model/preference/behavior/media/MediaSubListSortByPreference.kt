package com.skyd.anivu.model.preference.behavior.media

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.ksp.preference.Preference

@Preference
object MediaSubListSortByPreference : BaseMediaListSortByPreference() {
    private const val MEDIA_SUB_LIST_SORT_BY = "mediaSubListSortBy"

    val values = listOf(DATE, NAME)

    override val default = DATE

    override val key = stringPreferencesKey(MEDIA_SUB_LIST_SORT_BY)
}