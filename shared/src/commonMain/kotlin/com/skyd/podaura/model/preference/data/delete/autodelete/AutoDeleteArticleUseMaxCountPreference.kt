package com.skyd.podaura.model.preference.data.delete.autodelete

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object AutoDeleteArticleUseMaxCountPreference : BasePreference<Boolean>() {
    private const val AUTO_DELETE_ARTICLE_USE_MAX_COUNT = "autoDeleteArticleUseMaxCount"

    override val default = false
    override val key = booleanPreferencesKey(AUTO_DELETE_ARTICLE_USE_MAX_COUNT)
}
