package com.skyd.podaura.model.preference.data.delete

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object KeepPlaylistArticlesPreference : BasePreference<Boolean>() {
    private const val KEEP_PLAYLIST_ARTICLES = "keepPlaylistArticles"

    override val default = true
    override val key = booleanPreferencesKey(KEEP_PLAYLIST_ARTICLES)
}
