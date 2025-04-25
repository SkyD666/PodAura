package com.skyd.anivu.model.preference.behavior.feed

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.model.preference.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object HideMutedFeedPreference : BasePreference<Boolean>() {
    private const val HIDE_MUTED_FEED = "hideMutedFeed"

    override val default = true
    override val key = booleanPreferencesKey(HIDE_MUTED_FEED)
}