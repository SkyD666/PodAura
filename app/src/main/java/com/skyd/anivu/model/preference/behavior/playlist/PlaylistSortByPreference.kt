package com.skyd.anivu.model.preference.behavior.playlist

import androidx.datastore.preferences.core.stringPreferencesKey

object PlaylistSortByPreference : BasePlaylistSortByPreference() {
    private const val PLAYLIST_SORT_BY = "playlistSortBy"

    val values = listOf(Manual, Name, MediaCount, CreateTime)

    override val default = Manual

    override val key = stringPreferencesKey(PLAYLIST_SORT_BY)
}
