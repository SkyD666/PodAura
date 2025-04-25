package com.skyd.anivu.model.preference.behavior.playlist

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.model.preference.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object PlaylistSortAscPreference : BasePreference<Boolean>() {
    private const val PLAYLIST_SORT_ASC = "playlistSortAsc"

    override val default = true
    override val key = booleanPreferencesKey(PLAYLIST_SORT_ASC)
}