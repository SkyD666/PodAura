package com.skyd.podaura.model.preference.behavior.playlist

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object PlaylistMediaSortAscPreference : BasePreference<Boolean>() {
    private const val PLAYLIST_MEDIA_SORT_ASC = "playlistMediaSortAsc"

    override val default = true
    override val key = booleanPreferencesKey(PLAYLIST_MEDIA_SORT_ASC)
}
