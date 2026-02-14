package com.skyd.podaura.model.preference.data.delete

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object KeepUnreadArticlesPreference : BasePreference<Boolean>() {
    private const val KEEP_UNREAD_ARTICLES = "keepUnreadArticles"

    override val default = true
    override val key = booleanPreferencesKey(KEEP_UNREAD_ARTICLES)
}
