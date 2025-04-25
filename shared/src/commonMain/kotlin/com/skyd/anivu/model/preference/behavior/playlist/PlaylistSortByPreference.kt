package com.skyd.anivu.model.preference.behavior.playlist

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.ksp.preference.Preference

@Preference
object PlaylistSortByPreference : BasePlaylistSortByPreference() {
    private const val PLAYLIST_SORT_BY = "playlistSortBy"

    val values = listOf(MANUAL, NAME, MEDIA_COUNT, CREATE_TIME)

    override val default = MANUAL

    override val key = stringPreferencesKey(PLAYLIST_SORT_BY)
}
