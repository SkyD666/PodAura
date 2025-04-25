package com.skyd.anivu.model.preference.appearance.article

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.model.preference.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object ShowArticleTopBarRefreshPreference : BasePreference<Boolean>() {
    private const val SHOW_ARTICLE_TOP_BAR_REFRESH = "showArticleTopBarRefresh"

    override val default = false
    override val key = booleanPreferencesKey(SHOW_ARTICLE_TOP_BAR_REFRESH)
}