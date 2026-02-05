package com.skyd.podaura.model.preference.data.delete

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object KeepFavoriteArticlesPreference : BasePreference<Boolean>() {
    private const val KEEP_FAVORITE_ARTICLES = "keepFavoriteArticles"

    override val default = true
    override val key = booleanPreferencesKey(KEEP_FAVORITE_ARTICLES)
}
