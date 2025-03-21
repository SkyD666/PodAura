package com.skyd.anivu.model.preference.data.delete

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.base.BasePreference

object KeepPlaylistArticlesPreference : BasePreference<Boolean> {
    private const val KEEP_PLAYLIST_ARTICLES = "keepPlaylistArticles"

    override val default = true
    override val key = booleanPreferencesKey(KEEP_PLAYLIST_ARTICLES)
}
