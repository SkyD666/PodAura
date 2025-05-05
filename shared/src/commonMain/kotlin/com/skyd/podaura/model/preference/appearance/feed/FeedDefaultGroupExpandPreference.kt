package com.skyd.podaura.model.preference.appearance.feed

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object FeedDefaultGroupExpandPreference : BasePreference<Boolean>() {
    private const val FEED_DEFAULT_GROUP_EXPAND = "feedDefaultGroupExpand"

    override val default = true
    override val key = booleanPreferencesKey(FEED_DEFAULT_GROUP_EXPAND)
}
