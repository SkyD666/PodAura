package com.skyd.anivu.model.preference.behavior.feed

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.base.BasePreference

object HideMutedFeedPreference : BasePreference<Boolean> {
    private const val HIDE_MUTED_FEED = "hideMutedFeed"

    override val default = true
    override val key = booleanPreferencesKey(HIDE_MUTED_FEED)
}