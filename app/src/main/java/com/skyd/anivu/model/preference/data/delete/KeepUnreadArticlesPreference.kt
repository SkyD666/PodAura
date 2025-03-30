package com.skyd.anivu.model.preference.data.delete

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.base.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object KeepUnreadArticlesPreference : BasePreference<Boolean> {
    private const val KEEP_UNREAD_ARTICLES = "keepUnreadArticles"

    override val default = true
    override val key = booleanPreferencesKey(KEEP_UNREAD_ARTICLES)
}
