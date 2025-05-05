package com.skyd.podaura.model.preference.behavior.media

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.ksp.preference.Preference

@Preference
object MediaListSortByPreference : BaseMediaListSortByPreference() {
    private const val MEDIA_LIST_SORT_BY = "mediaListSortBy"

    val values = listOf(DATE, NAME, FILE_COUNT)

    override val default = DATE

    override val key = stringPreferencesKey(MEDIA_LIST_SORT_BY)
}
