package com.skyd.podaura.model.preference.behavior.playlist

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object PlaylistSortAscPreference : BasePreference<Boolean>() {
    private const val PLAYLIST_SORT_ASC = "playlistSortAsc"

    override val default = true
    override val key = booleanPreferencesKey(PLAYLIST_SORT_ASC)
}