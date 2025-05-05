package com.skyd.podaura.model.preference.behavior.feed

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object HideMutedFeedPreference : BasePreference<Boolean>() {
    private const val HIDE_MUTED_FEED = "hideMutedFeed"

    override val default = true
    override val key = booleanPreferencesKey(HIDE_MUTED_FEED)
}