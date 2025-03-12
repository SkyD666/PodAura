package com.skyd.anivu.model.preference.data.delete.autodelete

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.skyd.anivu.base.BasePreference
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.put
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AutoDeleteArticleMaxCountPreference : BasePreference<Int> {
    private const val AUTO_DELETE_ARTICLE_MAX_COUNT = "autoDeleteArticleMaxCount"

    override val default = 500

    val key = intPreferencesKey(AUTO_DELETE_ARTICLE_MAX_COUNT)

    fun put(context: Context, scope: CoroutineScope, value: Int) {
        scope.launch(Dispatchers.IO) {
            context.dataStore.put(key, value)
        }
    }

    override fun fromPreferences(preferences: Preferences): Int = preferences[key] ?: default
}
