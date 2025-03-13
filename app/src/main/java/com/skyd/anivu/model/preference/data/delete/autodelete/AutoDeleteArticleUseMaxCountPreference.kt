package com.skyd.anivu.model.preference.data.delete.autodelete

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.base.BasePreference

object AutoDeleteArticleUseMaxCountPreference : BasePreference<Boolean> {
    private const val AUTO_DELETE_ARTICLE_USE_MAX_COUNT = "autoDeleteArticleUseMaxCount"

    override val default = false
    override val key = booleanPreferencesKey(AUTO_DELETE_ARTICLE_USE_MAX_COUNT)
}
