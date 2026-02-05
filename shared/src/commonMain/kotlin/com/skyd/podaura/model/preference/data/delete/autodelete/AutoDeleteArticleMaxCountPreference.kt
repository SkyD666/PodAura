package com.skyd.podaura.model.preference.data.delete.autodelete

import androidx.datastore.preferences.core.intPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object AutoDeleteArticleMaxCountPreference : BasePreference<Int>() {
    private const val AUTO_DELETE_ARTICLE_MAX_COUNT = "autoDeleteArticleMaxCount"

    override val default = 500
    override val key = intPreferencesKey(AUTO_DELETE_ARTICLE_MAX_COUNT)
}
