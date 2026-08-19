package com.skyd.podaura.model.preference.data.delete

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object KeepArticlesWithDownloadTasksPreference : BasePreference<Boolean>() {
    private const val KEEP_ARTICLES_WITH_DOWNLOAD_TASKS = "keepArticlesWithDownloadTasks"

    override val default = true
    override val key = booleanPreferencesKey(KEEP_ARTICLES_WITH_DOWNLOAD_TASKS)
}
