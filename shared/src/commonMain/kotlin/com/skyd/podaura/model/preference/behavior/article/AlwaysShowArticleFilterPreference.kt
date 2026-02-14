package com.skyd.podaura.model.preference.behavior.article

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object AlwaysShowArticleFilterPreference : BasePreference<Boolean>() {
    private const val ALWAYS_SHOW_ARTICLE_FILTER = "alwaysShowArticleFilter"

    override val default = false
    override val key = booleanPreferencesKey(ALWAYS_SHOW_ARTICLE_FILTER)
}
