package com.skyd.anivu.model.preference.behavior.playlist

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.base.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object PlaylistMediaSortAscPreference : BasePreference<Boolean> {
    private const val PLAYLIST_MEDIA_SORT_ASC = "playlistMediaSortAsc"

    override val default = true
    override val key = booleanPreferencesKey(PLAYLIST_MEDIA_SORT_ASC)
}