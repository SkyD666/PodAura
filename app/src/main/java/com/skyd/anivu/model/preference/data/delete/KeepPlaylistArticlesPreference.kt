package com.skyd.anivu.model.preference.data.delete

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.base.BasePreference
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.put
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object KeepPlaylistArticlesPreference : BasePreference<Boolean> {
    private const val KEEP_PLAYLIST_ARTICLES = "keepPlaylistArticles"
    override val default = true

    val key = booleanPreferencesKey(KEEP_PLAYLIST_ARTICLES)

    fun put(context: Context, scope: CoroutineScope, value: Boolean) {
        scope.launch(Dispatchers.IO) {
            context.dataStore.put(key, value)
        }
    }

    override fun fromPreferences(preferences: Preferences): Boolean = preferences[key] ?: default
}
