package com.skyd.podaura.model.preference.data.delete.autodelete

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object AutoDeleteArticleKeepFavoritePreference : BasePreference<Boolean>() {
    private const val AUTO_DELETE_ARTICLE_KEEP_FAVORITE = "autoDeleteArticleKeepFavorite"

    override val default = true
    override val key = booleanPreferencesKey(AUTO_DELETE_ARTICLE_KEEP_FAVORITE)
}
