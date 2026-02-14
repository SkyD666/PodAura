package com.skyd.podaura.model.preference.data.delete.autodelete

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object AutoDeleteArticleUseBeforePreference : BasePreference<Boolean>() {
    private const val AUTO_DELETE_ARTICLE_USE_BEFORE = "autoDeleteArticleUseBefore"

    override val default = true
    override val key = booleanPreferencesKey(AUTO_DELETE_ARTICLE_USE_BEFORE)
}
