package com.skyd.podaura.model.preference.behavior.playlist

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object ReverseLoadArticlePlaylistPreference : BasePreference<Boolean>() {
    private const val REVERSE_LOAD_ARTICLE_PLAYLIST = "reverseLoadArticlePlaylist"

    override val default = true
    override val key = booleanPreferencesKey(REVERSE_LOAD_ARTICLE_PLAYLIST)
}