package com.skyd.anivu.model.preference.data.delete.autodelete

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.base.BasePreference

object AutoDeleteArticleKeepPlaylistPreference : BasePreference<Boolean> {
    private const val AUTO_DELETE_ARTICLE_KEEP_PLAYLIST = "autoDeleteArticleKeepPlaylist"

    override val default = true
    override val key = booleanPreferencesKey(AUTO_DELETE_ARTICLE_KEEP_PLAYLIST)
}
