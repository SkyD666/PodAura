package com.skyd.podaura.model.preference.data.delete.autodelete

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object AutoDeleteArticleKeepUnreadPreference : BasePreference<Boolean>() {
    private const val AUTO_DELETE_ARTICLE_KEEP_UNREAD = "autoDeleteArticleKeepUnread"

    override val default = true
    override val key = booleanPreferencesKey(AUTO_DELETE_ARTICLE_KEEP_UNREAD)
}
