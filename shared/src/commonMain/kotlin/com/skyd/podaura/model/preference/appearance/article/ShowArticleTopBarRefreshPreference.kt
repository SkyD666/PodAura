package com.skyd.podaura.model.preference.appearance.article

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object ShowArticleTopBarRefreshPreference : BasePreference<Boolean>() {
    private const val SHOW_ARTICLE_TOP_BAR_REFRESH = "showArticleTopBarRefresh"

    override val default = false
    override val key = booleanPreferencesKey(SHOW_ARTICLE_TOP_BAR_REFRESH)
}