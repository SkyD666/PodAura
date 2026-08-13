package com.skyd.podaura.model.preference.appearance.article

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.fundation.util.isPhone
import com.skyd.fundation.util.platform
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object ShowArticleTopBarRefreshPreference : BasePreference<Boolean>() {
    private const val SHOW_ARTICLE_TOP_BAR_REFRESH = "showArticleTopBarRefresh"

    override val default = !platform.isPhone
    override val key = booleanPreferencesKey(SHOW_ARTICLE_TOP_BAR_REFRESH)
}
