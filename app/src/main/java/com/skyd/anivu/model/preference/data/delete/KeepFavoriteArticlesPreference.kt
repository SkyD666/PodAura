package com.skyd.anivu.model.preference.data.delete

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.base.BasePreference

object KeepFavoriteArticlesPreference : BasePreference<Boolean> {
    private const val KEEP_FAVORITE_ARTICLES = "keepFavoriteArticles"

    override val default = true
    override val key = booleanPreferencesKey(KEEP_FAVORITE_ARTICLES)
}
