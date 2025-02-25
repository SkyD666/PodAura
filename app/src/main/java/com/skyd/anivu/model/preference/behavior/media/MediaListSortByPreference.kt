package com.skyd.anivu.model.preference.behavior.media

import androidx.datastore.preferences.core.stringPreferencesKey

object MediaListSortByPreference : BaseMediaListSortByPreference() {
    private const val MEDIA_LIST_SORT_BY = "mediaListSortBy"

    val values = listOf(Date, Name, FileCount)

    override val default = Date

    override val key = stringPreferencesKey(MEDIA_LIST_SORT_BY)
}
