package com.skyd.anivu.model.preference.data.delete.autodelete

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.model.preference.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object AutoDeleteArticleKeepUnreadPreference : BasePreference<Boolean>() {
    private const val AUTO_DELETE_ARTICLE_KEEP_UNREAD = "autoDeleteArticleKeepUnread"

    override val default = true
    override val key = booleanPreferencesKey(AUTO_DELETE_ARTICLE_KEEP_UNREAD)
}
