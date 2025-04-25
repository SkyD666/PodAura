package com.skyd.anivu.model.preference.data.delete.autodelete

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.model.preference.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object AutoDeleteArticleUseBeforePreference : BasePreference<Boolean>() {
    private const val AUTO_DELETE_ARTICLE_USE_BEFORE = "autoDeleteArticleUseBefore"

    override val default = true
    override val key = booleanPreferencesKey(AUTO_DELETE_ARTICLE_USE_BEFORE)
}
